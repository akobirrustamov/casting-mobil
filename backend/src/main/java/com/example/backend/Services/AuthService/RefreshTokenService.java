package com.example.backend.Services.AuthService;

import com.example.backend.Entity.RefreshToken;
import com.example.backend.Entity.User;
import com.example.backend.Repository.RefreshTokenRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh tokenlarni berish, aylantirish va bekor qilish (§61).
 *
 * <h2>Rotatsiya</h2>
 * Har yangilashda eski token bekor qilinadi va yangisi beriladi. Ya'ni
 * bitta refresh token faqat BIR MARTA ishlaydi. Nusxa ko'chirilgan
 * token ikkinchi marta kelganda darhol bilinadi.
 *
 * <h2>O'g'rilikni aniqlash</h2>
 * Allaqachon bekor qilingan token qayta kelsa — ikki ehtimol bor:
 * yo tarmoq uzilib klient qayta urindi, yo tokenning nusxasi birovda.
 * Ikkinchisini birinchisidan ajratib bo'lmaydi, shuning uchun
 * XAVFSIZ tomonni tanlaymiz: foydalanuvchining barcha sessiyalari
 * bekor qilinadi va u qaytadan kiradi. Noqulaylik — bir marta parol
 * terish; muqobili — o'g'ri cheksiz kirib turishi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepo repo;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-token-ms:86400000}")
    private long refreshTokenMs;

    /** Yangi refresh token beradi va uni ro'yxatga yozadi. */
    @Transactional
    public String issue(User user, HttpServletRequest request) {
        UUID jti = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        repo.save(RefreshToken.builder()
                .id(jti)
                .userId(user.getId())
                .createdAt(now)
                .expiresAt(now.plusNanos(refreshTokenMs * 1_000_000))
                .userAgent(trim(header(request, "User-Agent"), 512))
                .ip(trim(clientIp(request), 64))
                .build());

        return jwtService.generateJwtRefreshToken(user, jti);
    }

    /**
     * Tokenni tekshiradi va uni bekor qilib, o'rniga yangisini beradi.
     *
     * @return bekor qilingan tokenning egasi — chaqiruvchi yangi juftlik
     *         yasashi uchun
     */
    /**
     * ⚠️ {@code noRollbackFor}: o'g'rilik aniqlanganda biz avval butun
     * zanjirni bekor qilamiz, keyin xato tashlaymiz. Oddiy sozlamada
     * xato tranzaksiyani ORQAGA QAYTARADI va bekor qilish ham bekor
     * bo'ladi — ya'ni himoya ishlagandek ko'rinib, aslida hech narsa
     * qilmasdi. Testda aynan shu tutildi.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public UUID rotate(String refreshToken, HttpServletRequest request) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw unauthorized("Refresh token yuborilmadi");
        }
        if (!jwtService.validateToken(refreshToken)) {
            throw unauthorized("Refresh token yaroqsiz yoki muddati o'tgan");
        }
        // ⚠️ Access token bilan yangilash mumkin bo'lmasin: aks holda
        // o'g'irlangan access token cheksiz yangilanib turardi.
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw unauthorized("Bu token yangilash uchun emas");
        }

        UUID jti = jwtService.jtiOf(refreshToken);
        if (jti == null) {
            throw unauthorized("Refresh token eski formatda - qaytadan kiring");
        }

        Optional<RefreshToken> stored = repo.findById(jti);
        if (stored.isEmpty()) {
            throw unauthorized("Refresh token bekor qilingan");
        }

        RefreshToken row = stored.get();
        LocalDateTime now = LocalDateTime.now();

        if (row.getRevokedAt() != null) {
            // Bekor qilingan token qayta ishlatildi - nusxasi birovda
            // bo'lishi mumkin. Butun zanjir yopiladi.
            log.warn("Bekor qilingan refresh token qayta ishlatildi: userId={}", row.getUserId());
            repo.revokeAllForUser(row.getUserId(), now);
            throw unauthorized("Sessiya xavfsizlik sababli yopildi - qaytadan kiring");
        }
        if (!row.getExpiresAt().isAfter(now)) {
            throw unauthorized("Refresh token muddati o'tgan");
        }

        row.setRevokedAt(now);
        repo.save(row);
        return row.getUserId();
    }

    /** Rotatsiyada yangi tokenni eskisiga bog'laydi. */
    @Transactional
    public void linkReplacement(String oldToken, String newToken) {
        UUID oldJti = jwtService.jtiOf(oldToken);
        UUID newJti = jwtService.jtiOf(newToken);
        if (oldJti == null || newJti == null) {
            return;
        }
        repo.findById(oldJti).ifPresent(row -> {
            row.setReplacedBy(newJti);
            repo.save(row);
        });
    }

    /** Chiqish — token darhol kuchini yo'qotadi. */
    @Transactional
    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()
                || !jwtService.validateToken(refreshToken)) {
            // Chiqish hech qachon xato bermaydi: klient baribir
            // tokenini o'chiradi, xato faqat chalkashtirardi.
            return;
        }
        UUID jti = jwtService.jtiOf(refreshToken);
        if (jti == null) {
            return;
        }
        repo.findById(jti).ifPresent(row -> {
            if (row.getRevokedAt() == null) {
                row.setRevokedAt(LocalDateTime.now());
                repo.save(row);
            }
        });
    }

    /** Barcha qurilmalardan chiqarish (bloklash, parol o'zgarishi). */
    @Transactional
    public int revokeAll(UUID userId) {
        return repo.revokeAllForUser(userId, LocalDateTime.now());
    }

    /**
     * Muddati o'tgan yozuvlarni tozalaydi.
     *
     * <h2>⚠️ Nima uchun bu ZARUR bo'lib qoldi</h2>
     * {@code deleteExpired} so'rovi allaqachon yozilgan edi, lekin uni
     * HECH KIM chaqirmasdi. Ilgari bu sezilmasdi: jadvalga faqat admin
     * panelga kirishda yozilardi.
     *
     * Mobil ilova yangilash oqimiga ulangach hisob butunlay o'zgardi.
     * Faol foydalanuvchi har 15 daqiqada tokenini yangilaydi, ya'ni
     * sutkasiga ~96 qator. 3000 foydalanuvchida bu kuniga ~288 000
     * qator — va ularning hech biri o'chirilmasdi.
     *
     * <h2>Nima o'chiriladi</h2>
     * Faqat muddati o'tganlari. Ular baribir ishlamaydi: {@code rotate}
     * muddatni tekshiradi va rad etadi.
     *
     * ⚠️ Bekor qilingan, lekin muddati o'tmagan yozuvlar QOLADI —
     * aynan ular o'g'rilikni aniqlash uchun kerak. Ularni erta
     * o'chirish nusxa ko'chirilgan tokenni «notanish» qilib qo'yardi
     * va zanjir yopilmasdi.
     */
    @Scheduled(cron = "${app.auth.refresh-cleanup-cron:0 15 3 * * *}")
    @Transactional
    public void cleanUpExpired() {
        int removed = repo.deleteExpired(LocalDateTime.now());
        if (removed > 0) {
            log.info("Muddati o'tgan {} ta refresh token o'chirildi", removed);
        }
    }

    private BusinessException unauthorized(String message) {
        return new BusinessException("INVALID_REFRESH_TOKEN", message, HttpStatus.UNAUTHORIZED);
    }

    private String header(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
