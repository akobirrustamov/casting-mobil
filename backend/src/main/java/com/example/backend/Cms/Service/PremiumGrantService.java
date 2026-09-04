package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.Subscription;
import com.example.backend.Cms.Entity.Tariff;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.SubscriptionSource;
import com.example.backend.Cms.Repository.SubscriptionRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

/**
 * Premium muddatini UZAYTIRISH — bitta qoida, bitta joy.
 *
 * <h2>Nima uchun alohida xizmat</h2>
 * «Mavjud obuna ustiga qo'shiladi, boshidan boshlanmaydi» qoidasi
 * {@code UserAdminService.grantPremium} ichida turardi. Promokod paydo
 * bo'lganda aynan shu qoida ikkinchi marta kerak bo'ldi — va uni
 * nusxalash ikki nusxaning ajralib ketishiga yo'l ochardi: masalan admin
 * sovg'asi ustiga qo'shar, promokod esa boshidan boshlab, odamning
 * to'lagan kunlarini «yeb» qo'yardi.
 *
 * Endi admin sovg'asi ham, promokod ham SHU metod orqali o'tadi. Kelajakda
 * to'lov ham shu yerdan o'tadi ({@code PURCHASE}) — uch manba, bitta
 * arifmetika.
 *
 * <h2>Ikki yozuv, bitta tranzaksiya</h2>
 * {@code cms_user_account.premium_until} — «hozir Premium ochiqmi» degan
 * tez savol uchun ({@code AccessService}). {@code cms_subscription} —
 * o'zgarmas tarix: kim, qachon, qaysi manbadan, qancha to'lab. Ikkalasi
 * birga yoziladi, yarmi yozilib qolmaydi.
 */
@Service
@RequiredArgsConstructor
public class PremiumGrantService {

    private final UserRepo userRepo;
    private final UserAccountRepo accountRepo;
    private final SubscriptionRepo subscriptionRepo;

    /**
     * Muddatni uzaytiradi va tarix yozadi.
     *
     * @param period    qancha: {@code Period.ofMonths(3)} (tarif, admin) yoki
     *                  {@code Period.ofDays(30)} (promokod)
     * @param source    qayerdan — hisobot uchun; {@code PURCHASE} dan
     *                  boshqasida {@code paidAmount} bo'sh qoladi
     * @param tariff    bog'langan tarif yoki {@code null}
     * @param grantedBy admin bo'lsa — kim; foydalanuvchining o'zi bo'lsa {@code null}
     */
    @Transactional
    public Grant extend(User user, Period period, SubscriptionSource source,
                        Tariff tariff, UUID grantedBy) {
        if (period == null || period.isZero() || period.isNegative()) {
            throw BusinessException.validation("Muddat noldan katta bo'lishi kerak");
        }

        UserAccount account = accountOf(user.getId());
        LocalDateTime now = LocalDateTime.now();

        // ⚠️ Faol obuna bo'lsa, undan davom etadi. Aks holda odam to'lagan
        // kunlarini yo'qotardi: 20 kuni qolgan odam 30 kunlik promokod
        // kiritsa, 50 emas, 30 kun qolardi.
        LocalDateTime from = account.hasActivePremium() ? account.getPremiumUntil() : now;
        LocalDateTime until = from.plus(period);

        Subscription subscription = subscriptionRepo.save(Subscription.builder()
                .user(user)
                .tariff(tariff)
                .startAt(now)
                .endAt(until)
                .source(source)
                .paidAmount(null)
                .grantedBy(grantedBy)
                .build());

        account.setPremiumUntil(until);
        return new Grant(subscription, accountRepo.save(account));
    }

    /**
     * Casting bo'limiga kirishni uzaytiradi.
     *
     * <h2>Nima uchun bu yerda, {@code extend} dan alohida</h2>
     * Casting huquqi obuna EMAS: u {@code cms_subscription} ga
     * yozilmaydi. Aks holda «faol obunachilar» soni casting kodlari
     * hisobiga shishib ketardi va daromad hisobotidagi obunachi soni
     * bilan mos kelmasdi.
     *
     * Qolgan arifmetika bir xil: mavjud muddat ustiga qo'shiladi.
     *
     * @return endi qachongacha ochiq
     */
    @Transactional
    public LocalDateTime extendCasting(User user, Period period) {
        if (period == null || period.isZero() || period.isNegative()) {
            throw BusinessException.validation("Muddat noldan katta bo'lishi kerak");
        }

        UserAccount account = accountOf(user.getId());
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime from = account.hasActiveCastingAccess()
                ? account.getCastingUntil() : now;
        LocalDateTime until = from.plus(period);

        account.setCastingUntil(until);
        return accountRepo.save(account).getCastingUntil();
    }

    /**
     * Hisob yozuvi — bo'lmasa yaratiladi.
     *
     * Eski foydalanuvchilar {@code cms_user_account} paydo bo'lishidan oldin
     * yaratilgan; ularda yozuv yo'q va bu xato emas.
     */
    @Transactional
    public UserAccount accountOf(UUID userId) {
        return accountRepo.findByUserId(userId).orElseGet(() -> {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> BusinessException.notFound("User", userId));
            return accountRepo.save(UserAccount.builder().user(user).build());
        });
    }

    /** Nima yozildi: tarix qatori va yangilangan hisob. */
    public record Grant(Subscription subscription, UserAccount account) {

        public LocalDateTime until() {
            return account.getPremiumUntil();
        }
    }
}
