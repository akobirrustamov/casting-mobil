package com.example.backend.Security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hisob bo'yicha muvaffaqiyatsiz kirish himoyasi (§61).
 *
 * <h2>Nega rate limit yetarli emas</h2>
 * Mavjud {@code RateLimitFilter} IP bo'yicha cheklaydi. Bu bitta
 * manbadan kelayotgan shovqinni to'xtatadi, lekin BITTA HISOBGA
 * qaratilgan hujumni emas: yuzta IP'dan har biri daqiqasiga o'n marta
 * urinsa, IP limiti hech qachon buzilmaydi, hisob esa daqiqasiga ming
 * marta sinaladi.
 *
 * Shuning uchun hisoblash telefon raqami bo'yicha ham yuritiladi.
 *
 * <h2>Nega xotirada</h2>
 * Mavjud {@code RateLimiter} ham xotirada ishlaydi — izchillik uchun
 * shu yondashuv davom ettirildi. ⚠️ Bir nechta server nusxasi
 * ishlatilsa, har biri o'z hisobini yuritadi va samarali chegara
 * nusxalar soniga ko'payadi. Bu holatda Redis kerak bo'ladi.
 */
@Slf4j
@Service
public class LoginAttemptService {

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    @Value("${app.auth.max-failed-attempts:5}")
    private int maxFailed;

    @Value("${app.auth.lockout-minutes:15}")
    private int lockoutMinutes;

    @Value("${app.auth.enabled:true}")
    private boolean enabled;

    /** Hisob bloklangan bo'lsa — qolgan daqiqalar, aks holda 0. */
    public long lockedMinutesLeft(String login) {
        if (!enabled || login == null) {
            return 0;
        }
        Attempts a = attempts.get(key(login));
        if (a == null || a.lockedUntil == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (a.lockedUntil.isAfter(now)) {
            return Math.max(1, java.time.Duration.between(now, a.lockedUntil).toMinutes() + 1);
        }
        // Muddat o'tdi — hisob tozalanadi, aks holda foydalanuvchi
        // keyingi bitta xatodan keyin darhol yana bloklanardi.
        attempts.remove(key(login));
        return 0;
    }

    public void recordFailure(String login) {
        if (!enabled || login == null) {
            return;
        }
        Attempts a = attempts.computeIfAbsent(key(login), k -> new Attempts());
        synchronized (a) {
            a.count++;
            if (a.count >= maxFailed) {
                a.lockedUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
                a.count = 0;
                log.warn("Hisob {} daqiqaga bloklandi: ko'p muvaffaqiyatsiz kirish", lockoutMinutes);
            }
        }
    }

    /** Muvaffaqiyatli kirish hisobni tozalaydi. */
    public void recordSuccess(String login) {
        if (login != null) {
            attempts.remove(key(login));
        }
    }

    private String key(String login) {
        return login.replaceAll("\\s", "").toLowerCase();
    }

    private static final class Attempts {
        private int count;
        private LocalDateTime lockedUntil;
    }
}
