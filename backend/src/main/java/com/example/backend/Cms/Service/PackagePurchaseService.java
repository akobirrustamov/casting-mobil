package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.CurrencyPackage;
import com.example.backend.Cms.Entity.Purchase;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Entity.UserBalance;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.FundingSource;
import com.example.backend.Cms.Enums.PurchaseType;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Cms.Payment.PaymentProvider;
import com.example.backend.Cms.Repository.CurrencyPackageRepo;
import com.example.backend.Cms.Repository.PurchaseRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Repository.UserBalanceRepo;
import com.example.backend.Entity.User;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Yulduz va tanga paketini sotib olish (ТЗ §44).
 *
 * <h2>Ikki yo'l, ikki xil holat</h2>
 * ТЗ: «Donation sotib olish: 1) user internal balance; 2) payment
 * system». Bu ikkisi bir xil emas:
 * <ul>
 *   <li><b>Ichki balans</b> — BUGUN ishlaydi. Pul allaqachon hisobda,
 *       tashqi provayder kerak emas;</li>
 *   <li><b>To'lov tizimi</b> — provayder ulanmagan, 503 qaytaradi.</li>
 * </ul>
 *
 * <h2>Nima uchun soxta muvaffaqiyat yo'q</h2>
 * Buyurtmachi talabi: «Mavjud bo'lmasa fake payment gateway yaratib
 * production-ready deb ko'rsatma.» Soxta «to'landi» javobi eng xavfli
 * variant bo'lardi: foydalanuvchi yulduz olardi, pul esa hech qayerdan
 * kelmasdi va buni faqat oy oxirida hisob-kitobda payqashardi.
 */
@Service
@RequiredArgsConstructor
public class PackagePurchaseService {

    private final CurrencyPackageRepo packageRepo;
    private final UserBalanceRepo balanceRepo;
    private final UserAccountRepo accountRepo;
    private final PurchaseRepo purchaseRepo;
    private final CurrencyPricingService pricingService;
    private final PaymentProvider paymentProvider;
    private final AuditService auditService;

    /**
     * Paketni sotib oladi va valyutani hisobga qo'shadi.
     *
     * <h2>Nima uchun bitta tranzaksiyada</h2>
     * Pulni yechish, valyutani qo'shish va yozuvni saqlash ajralib
     * qolsa, oradagi nosozlik foydalanuvchini pulsiz ham, yulduzsiz ham
     * qoldirardi.
     */
    @Transactional
    public Purchase buy(User buyer, Long packageId, FundingSource source) {
        if (buyer == null) {
            throw BusinessException.accessDenied("Sotib olish uchun tizimga kiring");
        }
        if (source == null) {
            throw BusinessException.validation("To'lov usuli tanlanmagan");
        }

        CurrencyPackage pack = packageRepo.findById(packageId)
                .orElseThrow(() -> BusinessException.notFound("CurrencyPackage", packageId));

        if (Boolean.FALSE.equals(pack.getActive())) {
            throw BusinessException.validation("Bu paket sotuvda emas");
        }

        // ⚠️ Narxi belgilanmagan paket sotib olinmaydi. V5 barcha
        // paketlarni 0.00 narx bilan qo'shgan — tekshirilmasa ular BEPUL
        // berilardi (§40).
        BigDecimal price = pricingService.effectivePrice(pack);
        if (price == null) {
            throw BusinessException.validation(
                    "Bu paketning narxi hali belgilanmagan");
        }

        UserAccount account = accountRepo.findByUserId(buyer.getId()).orElse(null);
        if (account != null && account.getStatus() == UserStatus.BLOCKED) {
            throw BusinessException.accessDenied("Hisobingiz bloklangan");
        }

        return switch (source) {
            case INTERNAL_BALANCE -> buyFromBalance(buyer, pack, price);
            case PAYMENT_SYSTEM -> buyViaProvider(buyer, pack, price);
        };
    }

    /** Ichki hisobdan to'lash — bu yo'l bugun ishlaydi. */
    private Purchase buyFromBalance(User buyer, CurrencyPackage pack, BigDecimal price) {
        UserBalance balance = balanceRepo.findByUserId(buyer.getId())
                .orElseThrow(() -> notEnoughMoney(price));

        BigDecimal money = balance.getMoneyBalance() == null
                ? BigDecimal.ZERO : balance.getMoneyBalance();
        if (money.compareTo(price) < 0) {
            throw notEnoughMoney(price);
        }

        balance.setMoneyBalance(money.subtract(price));
        credit(balance, pack.getKind(), pack.getAmount());

        try {
            balanceRepo.saveAndFlush(balance);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Ikki xarid bir vaqtda: ikkalasi ham eski balansni o'qib,
            // ikkalasi ham yechishi mumkin edi.
            throw new BusinessException("BALANCE_CONFLICT",
                    "Balans shu payt o'zgardi. Qayta urinib ko'ring",
                    HttpStatus.CONFLICT);
        }

        return record(buyer, pack, price, "internal-balance");
    }

    /**
     * Tashqi provayder orqali.
     *
     * ⚠️ Provayder ulanmagan — {@code init} istisno tashlaydi va
     * xaridning HECH QANDAY qismi bajarilmaydi. Yozuv ham yaratilmaydi:
     * to'lanmagan xarid tarixda turishi kerak emas.
     */
    private Purchase buyViaProvider(User buyer, CurrencyPackage pack, BigDecimal price) {
        String orderId = "pkg-" + pack.getId() + "-" + UUID.randomUUID();
        paymentProvider.init(orderId, price, "UZS");

        // Bu yerga faqat provayder ulangandan keyin yetib kelinadi.
        // O'shanda xarid provayder tasdig'idan (webhook) keyin
        // yakunlanishi kerak — bu yerda emas.
        throw new BusinessException("PAYMENT_FLOW_INCOMPLETE",
                "To'lov oqimi provayder ulangandan keyin yakunlanadi",
                HttpStatus.NOT_IMPLEMENTED);
    }

    private void credit(UserBalance balance, CurrencyKind kind, Long amount) {
        long add = amount == null ? 0L : amount;
        if (kind == CurrencyKind.STARS) {
            balance.setStarsBalance(nz(balance.getStarsBalance()) + add);
        } else {
            balance.setCoinBalance(nz(balance.getCoinBalance()) + add);
        }
    }

    /** ⚠️ O'zgarmas moliyaviy yozuv (§42). */
    private Purchase record(User buyer, CurrencyPackage pack,
                            BigDecimal price, String reference) {
        Purchase purchase = purchaseRepo.save(Purchase.builder()
                .user(buyer)
                .type(PurchaseType.CURRENCY_PACKAGE)
                .targetId(pack.getId())
                .amount(price)
                .currency("UZS")
                .paymentReference(reference)
                .createdAt(LocalDateTime.now())
                .build());

        auditService.log(buyer, "CURRENCY_PACKAGE_PURCHASED",
                "CurrencyPackage", pack.getId(), null,
                Map.of("kind", pack.getKind(),
                        "amount", String.valueOf(pack.getAmount()),
                        "price", price.toPlainString()));
        return purchase;
    }

    private BusinessException notEnoughMoney(BigDecimal price) {
        return new BusinessException("INSUFFICIENT_BALANCE",
                "Hisobingizda mablag' yetarli emas. Kerak: " + price.toPlainString() + " UZS",
                HttpStatus.PAYMENT_REQUIRED);
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }
}
