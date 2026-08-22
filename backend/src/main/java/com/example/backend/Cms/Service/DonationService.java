package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.DonationTransaction;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Entity.UserBalance;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Repository.CreatorRepo;
import com.example.backend.Cms.Repository.DonationRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Repository.UserBalanceRepo;
import com.example.backend.Entity.User;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Donat yuborish (ТЗ §39).
 *
 * <h2>Nima uchun kerak edi</h2>
 * Ma'lumot modeli ({@code DonationTransaction}, {@code UserBalance}) va
 * admin hisoboti (§42) bor edi, lekin foydalanuvchi donat YUBORADIGAN
 * yo'l yo'q edi. Ya'ni jadvallar faqat dev ma'lumotlari bilan to'lardi.
 *
 * <h2>Ikki valyuta</h2>
 * {@code STARS} va {@code COIN} — mustaqil hisoblar. Ular bir-biriga
 * ALMASHTIRILMAYDI: kurslari admin panelida alohida belgilanadi (§40, §41)
 * va hozircha 0.
 *
 * <h2>Nishon</h2>
 * Ijodkor yoki kontent. ТЗ «actor» va «actress» ni ham sanaydi, lekin
 * ular ALOHIDA nishon emas: §24 ga ko'ra bitta odam bir kinoda aktyor,
 * boshqasida rejissyor bo'lishi mumkin — ya'ni kasb ROLdir, shaxs esa
 * {@code Creator}. Ularni alohida nishon qilish bir odamning donatini
 * ikki joyga bo'lib yuborardi.
 */
@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepo donationRepo;
    private final UserBalanceRepo balanceRepo;
    private final UserAccountRepo accountRepo;
    private final CreatorRepo creatorRepo;
    private final ContentRepo contentRepo;

    /**
     * Donat yuboradi va balansdan yechadi.
     *
     * <h2>Nima uchun bitta tranzaksiyada</h2>
     * Balansdan yechish va yozuvni saqlash ajralib qolsa, birinchisi
     * bajarilib ikkinchisi yiqilganda pul yo'qolardi — foydalanuvchi
     * hisobidan yechilgan, hech kimga yetib bormagan.
     *
     * @throws BusinessException balans yetmasa, nishon topilmasa yoki
     *                           foydalanuvchi bloklangan bo'lsa
     */
    @Transactional
    public DonationTransaction donate(User sender, DonationTargetType targetType,
                                      Long targetId, CurrencyKind kind, Long amount) {
        if (sender == null) {
            throw BusinessException.accessDenied("Donat yuborish uchun tizimga kiring");
        }
        if (kind == null) {
            throw BusinessException.validation("Valyuta tanlanmagan");
        }
        if (targetType == null || targetId == null) {
            throw BusinessException.validation("Kimni qo'llab-quvvatlash kerakligi ko'rsatilmagan");
        }
        // ⚠️ Nol va manfiy alohida tekshiriladi: manfiy miqdor balansni
        // OSHIRIB yuborardi, ya'ni pul yaratish usuli bo'lardi.
        if (amount == null || amount <= 0) {
            throw BusinessException.validation("Miqdor noldan katta bo'lishi kerak");
        }

        UserAccount account = accountRepo.findByUserId(sender.getId()).orElse(null);
        if (account != null && account.getStatus() == UserStatus.BLOCKED) {
            throw BusinessException.accessDenied("Hisobingiz bloklangan");
        }

        requireTargetExists(targetType, targetId);

        UserBalance balance = balanceRepo.findByUserId(sender.getId())
                .orElseThrow(() -> notEnough(kind));

        long available = kind == CurrencyKind.STARS
                ? nz(balance.getStarsBalance()) : nz(balance.getCoinBalance());
        if (available < amount) {
            throw notEnough(kind);
        }

        if (kind == CurrencyKind.STARS) {
            balance.setStarsBalance(available - amount);
        } else {
            balance.setCoinBalance(available - amount);
        }

        try {
            balanceRepo.saveAndFlush(balance);
        } catch (ObjectOptimisticLockingFailureException e) {
            // ⚠️ Ikki donat bir vaqtda yuborilsa, ikkalasi ham eski
            // balansni o'qib, ikkalasi ham yechishi mumkin edi — ya'ni
            // 100 ta yulduz bilan 200 ta donat qilish. @Version buni
            // to'xtatadi; foydalanuvchiga tushunarli xabar beriladi.
            throw new BusinessException("BALANCE_CONFLICT",
                    "Balans shu payt o'zgardi. Qayta urinib ko'ring",
                    HttpStatus.CONFLICT);
        }

        // ⚠️ O'ZGARMAS yozuv: moliyaviy tarix tahrirlanmaydi va
        // o'chirilmaydi (§42).
        return donationRepo.save(DonationTransaction.builder()
                .sender(sender)
                .targetType(targetType)
                .targetId(targetId)
                .kind(kind)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Nishon HAQIQATDAN mavjudmi.
     *
     * Tekshirilmasa, mavjud bo'lmagan ijodkorga yuborilgan donat
     * balansdan yechilib, hisobotda hech qayerda ko'rinmasdi — pul
     * yo'qolardi.
     */
    private void requireTargetExists(DonationTargetType type, Long id) {
        boolean exists = switch (type) {
            case CREATOR -> creatorRepo.findById(id)
                    .map(c -> !Boolean.FALSE.equals(c.getActive()))
                    .orElse(false);
            // Nashr qilinmagan kontentni qo'llab-quvvatlab bo'lmaydi —
            // foydalanuvchi uni ko'rmagan ham.
            case CONTENT -> contentRepo.findById(id)
                    .map(c -> c.getDeletedAt() == null
                            && c.getStatus() == PublicationStatus.PUBLISHED)
                    .orElse(false);
        };
        if (!exists) {
            throw BusinessException.validation(
                    "Qo'llab-quvvatlash uchun nishon topilmadi: " + type + " #" + id);
        }
    }

    private BusinessException notEnough(CurrencyKind kind) {
        return new BusinessException("INSUFFICIENT_BALANCE",
                (kind == CurrencyKind.STARS ? "Yulduzlar" : "Tangalar") + " yetarli emas",
                HttpStatus.PAYMENT_REQUIRED);
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }
}
