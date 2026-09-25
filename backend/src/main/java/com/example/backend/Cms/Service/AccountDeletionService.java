package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Cms.Repository.CommentReportRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Repository.UserDeviceRepo;
import com.example.backend.Cms.Repository.NotificationReadRepo;
import com.example.backend.Cms.Repository.UserFavoriteRepo;
import com.example.backend.Cms.Repository.WatchProgressRepo;
import com.example.backend.Entity.User;
import com.example.backend.Repository.RefreshTokenRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Foydalanuvchi o'z hisobini o'chirishi (13.09.2026).
 *
 * <h2>Nima uchun kerak</h2>
 * Google Play'ning «Data deletion» siyosati: ilovada hisob YARATISH mumkin
 * bo'lsa, uni o'chirish ham bo'lishi shart — ilova ichida va ilovasiz,
 * veb-sahifa orqali ({@code /hisobni-ochirish}). Bu shart bajarilmasa ilova
 * do'konga umuman kirmaydi.
 *
 * <h2>⚠️ Nega qator o'chirilmaydi</h2>
 * {@code userRepo.delete(user)} eng to'g'ri yo'l ko'rinadi, lekin `users` ga
 * cascade bilan to'lovlar, obunalar va moliyaviy tarix bog'langan. Odam
 * hisobini o'chirgani — buxgalteriya yozuvlarini yo'q qilish degani emas;
 * maxfiylik siyosatining 4-bo'limi ham aynan shunday va'da beradi:
 * «to'lovlarga tegishli yozuvlar qonunchilik talab qilgan muddat davomida
 * saqlanib qolishi mumkin».
 *
 * Shuning uchun: shaxsiy ma'lumot TOZALANADI, qator esa nomsiz qoladi.
 *
 * <h2>Nima bo'ladi</h2>
 * <pre>
 *   telefon, email, Google sub, ism, rasm → tozalanadi
 *   sessiyalar (refresh token)            → o'chiriladi, kirish uziladi
 *   qurilmalar ro'yxati                   → o'chiriladi
 *   saqlanganlar, ko'rish joyi            → o'chiriladi
 *   yozgan shikoyatlari                   → o'chiriladi
 *   izohlari                              → QOLADI, muallif nomsiz bo'ladi
 *   «yoqdi» lari                          → QOLADI (ommaviy hisoblagich)
 *   to'lovlar, obunalar                   → QOLADI (qonun)
 * </pre>
 *
 * <h2>⚠️ Izohlar nega qoladi</h2>
 * Izoh — suhbatning bir qismi: uni o'chirish qolganlarning javoblarini
 * ma'nosiz qoldiradi. Google ham buni talab qilmaydi, unga SHAXSIY
 * ma'lumot muhim. Muallifning ismi tozalangandan keyin izoh ostida
 * «O'chirilgan foydalanuvchi» chiqadi — matn qoladi, odam qolmaydi.
 *
 * <h2>⚠️ Telefon bo'shaydi — bu ATAYLAB</h2>
 * Raqam {@code null} ga o'tadi, ya'ni o'sha raqam bilan qaytadan
 * ro'yxatdan o'tish mumkin. Aks holda odam hisobini o'chirib, ilovaga
 * boshqa hech qachon kira olmasdi — bu o'chirish emas, umrbod blok
 * bo'lardi.
 *
 * <h2>⚠️ Access token 15 daqiqa yashaydi</h2>
 * Refresh tokenlar o'chiriladi, ya'ni sessiyani uzaytirib bo'lmaydi. Lekin
 * qo'ldagi access token o'z muddatigacha ({@code app.jwt.access-token-ms},
 * sukut bo'yicha 15 daqiqa) ishlaydi. Bu xavf emas: o'sha token egasi —
 * o'chirishni so'ragan odamning o'zi, va ortida allaqachon nomsiz hisob
 * turadi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    /**
     * O'chirilgan hisob nima deb ko'rinadi.
     *
     * ⚠️ Bo'sh qoldirib bo'lmaydi: izoh ostida ism umuman bo'lmasa, ilova
     * «Noma'lum foydalanuvchi» deb yozardi — xuddi ism kiritmagan tirik
     * odamdek. Farqi ko'rinib tursin.
     */
    public static final String ANONYMOUS_NAME = "O'chirilgan foydalanuvchi";

    private final UserRepo userRepo;
    private final UserAccountRepo accountRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final UserDeviceRepo deviceRepo;
    private final UserFavoriteRepo favoriteRepo;
    private final NotificationReadRepo notificationReadRepo;
    private final WatchProgressRepo watchProgressRepo;
    private final CommentReportRepo commentReportRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void delete(User actor) {
        UUID id = actor.getId();
        User user = userRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("User", id));

        // 1. Kirishni uzish. Birinchi bo'lishi kerak: keyingi qadamlar
        //    davomida xato chiqsa ham, tranzaksiya butunlay qaytadi —
        //    yarim o'chirilgan hisob qolmaydi.
        refreshTokenRepo.deleteByUserId(id);

        // 2. Shaxsiy ro'yxatlar.
        deviceRepo.deleteByUserId(id);
        favoriteRepo.deleteByUserId(id);
        notificationReadRepo.deleteByUserId(id);
        watchProgressRepo.deleteByUserId(id);
        commentReportRepo.deleteByUserId(id);

        // 3. Shaxsni tozalash.
        user.setPhone(null);
        user.setEmail(null);
        user.setGoogleSub(null);
        user.setName(ANONYMOUS_NAME);
        user.setAvatarUrl(null);
        // ⚠️ Bo'sh parol emas, TASODIFIY: bo'sh hash bilan kirishga
        // urinish mumkin bo'lardi. Bu hashni hech kim bilmaydi.
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPasswordSet(false);
        userRepo.save(user);

        // 4. Hisob holati. Hisob yo'q bo'lsa ham yaratiladi: o'chirilgan
        //    degan yozuv qaysidir joyda turishi kerak.
        UserAccount account = accountRepo.findByUserId(id)
                .orElseGet(() -> UserAccount.builder().user(user).build());
        account.setStatus(UserStatus.DELETED);
        account.setDeletedAt(LocalDateTime.now());
        // Huquqlar ham tugaydi: hisob yo'q — Premium ham yo'q.
        account.setPremiumUntil(null);
        account.setCastingUntil(null);
        accountRepo.save(account);

        // ⚠️ Telefon LOG'ga yozilmaydi — shaxsiy ma'lumotni o'chirib,
        // uni log faylida qoldirish ma'nosiz bo'lardi.
        log.info("Hisob foydalanuvchi so'roviga ko'ra o'chirildi: {}", id);
    }
}
