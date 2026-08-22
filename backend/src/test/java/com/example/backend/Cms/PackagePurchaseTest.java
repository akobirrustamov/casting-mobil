package com.example.backend.Cms;

import com.example.backend.Cms.Entity.CurrencyPackage;
import com.example.backend.Cms.Entity.Purchase;
import com.example.backend.Cms.Entity.UserBalance;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.FundingSource;
import com.example.backend.Cms.Enums.PurchaseType;
import com.example.backend.Cms.Payment.PaymentNotConfiguredException;
import com.example.backend.Cms.Repository.CurrencyPackageRepo;
import com.example.backend.Cms.Repository.PurchaseRepo;
import com.example.backend.Cms.Repository.UserBalanceRepo;
import com.example.backend.Cms.Service.PackagePurchaseService;
import com.example.backend.Cms.Service.SettingKeys;
import com.example.backend.Cms.Service.SettingsService;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §44 — paket sotib olish.
 *
 * <h2>Audit natijasi</h2>
 * ТЗ: «Hozir mavjud payment integration bo'lsa audit qilib reuse qil.»
 * Loyihada hech qanday to'lov integratsiyasi TOPILMADI — na eski casting
 * kodida, na yangi modulda. Shuning uchun faqat chegara belgilandi.
 *
 * <h2>Ikki yo'l, ikki xil holat</h2>
 * <ul>
 *   <li><b>Ichki balans</b> — BUGUN ishlaydi;</li>
 *   <li><b>To'lov tizimi</b> — provayder ulanmagan, 503.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PackagePurchaseTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private PackagePurchaseService purchaseService;
    @Autowired private UserAdminService userAdminService;
    @Autowired private SettingsService settingsService;
    @Autowired private CurrencyPackageRepo packageRepo;
    @Autowired private UserBalanceRepo balanceRepo;
    @Autowired private PurchaseRepo purchaseRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    @BeforeEach
    void setRate() {
        // Kurs belgilanmasa paketlar sotib olinmaydi (§40) — bu holat
        // alohida testda tekshiriladi.
        settingsService.update(null, SettingKeys.STAR_RATE, "500");
    }

    // -------------------------------------------------------- ichki balans

    @Nested
    @DisplayName("Ichki hisobdan to'lash")
    class FromBalance {

        @Test
        @DisplayName("Pul yechiladi, yulduz qo'shiladi")
        void moneyOutCurrencyIn() {
            User buyer = buyerWithMoney("100000.00");
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);

            Purchase purchase = purchaseService.buy(buyer, pack.getId(),
                    FundingSource.INTERNAL_BALANCE);

            UserBalance balance = balanceRepo.findByUserId(buyer.getId()).orElseThrow();
            // 100 ta yulduz × 500 so'm = 50 000
            assertThat(purchase.getAmount()).isEqualByComparingTo("50000");
            assertThat(balance.getMoneyBalance()).isEqualByComparingTo("50000.00");
            assertThat(balance.getStarsBalance()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Mablag' yetmasa hech narsa o'zgarmaydi")
        void insufficientMoneyChangesNothing() {
            User buyer = buyerWithMoney("1000.00");
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);

            assertThatThrownBy(() -> purchaseService.buy(buyer, pack.getId(),
                    FundingSource.INTERNAL_BALANCE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("yetarli emas");

            UserBalance balance = balanceRepo.findByUserId(buyer.getId()).orElseThrow();
            assertThat(balance.getMoneyBalance()).isEqualByComparingTo("1000.00");
            assertThat(balance.getStarsBalance()).isZero();
        }

        @Test
        @DisplayName("Xarid O'ZGARMAS yozuv sifatida saqlanadi")
        void purchaseIsRecorded() {
            User buyer = buyerWithMoney("100000.00");
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);

            Purchase purchase = purchaseService.buy(buyer, pack.getId(),
                    FundingSource.INTERNAL_BALANCE);

            assertThat(purchaseRepo.findById(purchase.getId())).isPresent();
            assertThat(purchase.getType()).isEqualTo(PurchaseType.CURRENCY_PACKAGE);
            assertThat(purchase.getTargetId()).isEqualTo(pack.getId());
            assertThat(purchase.getCurrency()).isEqualTo("UZS");
        }

        @Test
        @DisplayName("Paketning o'z narxi hurmat qilinadi — chegirma")
        void discountedPackageUsesOwnPrice() {
            User buyer = buyerWithMoney("500000.00");
            // Kurs bo'yicha 1000 × 500 = 500 000, lekin chegirma bor.
            CurrencyPackage pack = pack(CurrencyKind.STARS, 1000L,
                    new BigDecimal("400000.00"));

            Purchase purchase = purchaseService.buy(buyer, pack.getId(),
                    FundingSource.INTERNAL_BALANCE);

            assertThat(purchase.getAmount()).isEqualByComparingTo("400000.00");
        }

        @Test
        @DisplayName("Tanga paketi tanga balansiga tushadi")
        void coinPackageCreditsCoinBalance() {
            settingsService.update(null, SettingKeys.COIN_RATE, "100");
            User buyer = buyerWithMoney("100000.00");
            CurrencyPackage pack = pack(CurrencyKind.UZCASTING_COIN, 50L, BigDecimal.ZERO);

            purchaseService.buy(buyer, pack.getId(), FundingSource.INTERNAL_BALANCE);

            UserBalance balance = balanceRepo.findByUserId(buyer.getId()).orElseThrow();
            assertThat(balance.getCoinBalance()).isEqualTo(50L);
            assertThat(balance.getStarsBalance()).isZero();
        }
    }

    // ------------------------------------------------------- to'lov tizimi

    @Nested
    @DisplayName("To'lov tizimi orqali")
    class ViaProvider {

        @Test
        @DisplayName("⚠️ Soxta muvaffaqiyat QAYTARMAYDI")
        void neverFakesSuccess() {
            User buyer = buyerWithMoney("0.00");
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);

            // Soxta «to'landi» eng xavfli variant: foydalanuvchi yulduz
            // olardi, pul esa hech qayerdan kelmasdi va buni faqat oy
            // oxirida hisob-kitobda payqashardi.
            assertThatThrownBy(() -> purchaseService.buy(buyer, pack.getId(),
                    FundingSource.PAYMENT_SYSTEM))
                    .isInstanceOf(PaymentNotConfiguredException.class);
        }

        @Test
        @DisplayName("Muvaffaqiyatsiz to'lovda yulduz BERILMAYDI")
        void failedPaymentGrantsNothing() {
            User buyer = buyerWithMoney("0.00");
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);

            assertThatThrownBy(() -> purchaseService.buy(buyer, pack.getId(),
                    FundingSource.PAYMENT_SYSTEM))
                    .isInstanceOf(BusinessException.class);

            assertThat(balanceRepo.findByUserId(buyer.getId()).orElseThrow()
                    .getStarsBalance()).isZero();
        }

        @Test
        @DisplayName("Muvaffaqiyatsiz to'lovda XARID YOZUVI ham yaratilmaydi")
        void failedPaymentLeavesNoPurchaseRecord() {
            User buyer = buyerWithMoney("0.00");
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);
            long before = purchaseRepo.count();

            assertThatThrownBy(() -> purchaseService.buy(buyer, pack.getId(),
                    FundingSource.PAYMENT_SYSTEM))
                    .isInstanceOf(BusinessException.class);

            // To'lanmagan xarid tarixda turishi kerak emas.
            assertThat(purchaseRepo.count()).isEqualTo(before);
        }
    }

    // -------------------------------------------------------------- rad

    @Nested
    @DisplayName("Rad etish holatlari")
    class Rejections {

        @Test
        @DisplayName("⚠️ Narxi belgilanmagan paket BEPUL berilmaydi")
        void unpricedPackageIsNotFree() {
            settingsService.update(null, SettingKeys.STAR_RATE, "0");
            User buyer = buyerWithMoney("100000.00");
            CurrencyPackage pack = pack(CurrencyKind.STARS, 1000L, BigDecimal.ZERO);

            // V5 barcha paketlarni 0.00 narx bilan qo'shgan. Tekshirilmasa
            // ular bepul berilardi.
            assertThatThrownBy(() -> purchaseService.buy(buyer, pack.getId(),
                    FundingSource.INTERNAL_BALANCE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("narxi hali belgilanmagan");

            assertThat(balanceRepo.findByUserId(buyer.getId()).orElseThrow()
                    .getStarsBalance()).isZero();
        }

        @Test
        @DisplayName("Nofaol paket sotib olinmaydi")
        void inactivePackageIsRejected() {
            User buyer = buyerWithMoney("100000.00");
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);
            pack.setActive(false);
            packageRepo.save(pack);

            assertThatThrownBy(() -> purchaseService.buy(buyer, pack.getId(),
                    FundingSource.INTERNAL_BALANCE))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Bloklangan foydalanuvchi sotib ololmaydi")
        void blockedUserCannotBuy() {
            User buyer = buyerWithMoney("100000.00");
            userAdminService.setBlocked(null, buyer.getId(), true, "sinov");
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);

            assertThatThrownBy(() -> purchaseService.buy(buyer, pack.getId(),
                    FundingSource.INTERNAL_BALANCE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("bloklangan");
        }

        @Test
        @DisplayName("Anonim sotib ololmaydi")
        void anonymousCannotBuy() {
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);

            assertThatThrownBy(() -> purchaseService.buy(null, pack.getId(),
                    FundingSource.INTERNAL_BALANCE))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("To'lov usuli tanlanmasa rad etiladi")
        void missingFundingSourceIsRejected() {
            User buyer = buyerWithMoney("100000.00");
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);

            assertThatThrownBy(() -> purchaseService.buy(buyer, pack.getId(), null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private CurrencyPackage pack(CurrencyKind kind, long amount, BigDecimal price) {
        return packageRepo.save(CurrencyPackage.builder()
                .kind(kind)
                .amount(amount)
                .price(price)
                .active(true)
                .sortOrder(SEQ.incrementAndGet())
                .build());
    }

    private User buyerWithMoney(String money) {
        Role r = roleRepo.findByName(UserRoles.ROLE_USER);
        if (r == null) {
            int nextId = roleRepo.findAll().stream()
                    .mapToInt(Role::getId).max().orElse(0) + 1;
            r = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99890" + (9200000 + SEQ.incrementAndGet()));
        u.setPassword("x");
        u.setName("Xaridor " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(r)));
        u = userRepo.save(u);

        balanceRepo.save(UserBalance.builder()
                .user(u)
                .moneyBalance(new BigDecimal(money))
                .starsBalance(0L)
                .coinBalance(0L)
                .build());
        return u;
    }
}
