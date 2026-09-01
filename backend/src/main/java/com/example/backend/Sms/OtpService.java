package com.example.backend.Sms;

import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telefon orqali ro'yxatdan o'tish/kirish uchun SMS-kod (OTP).
 *
 * <h2>Nega xotirada, DB'da emas</h2>
 * Kod bir necha daqiqa yashaydi va faqat bitta tasdiqlash uchun kerak —
 * xuddi shu sababga ko'ra {@code LoginAttemptService} va {@code RateLimiter}
 * ham xotirada ishlaydi. ⚠️ Bir nechta server nusxasi ishga tushirilsa, har
 * biri o'z xotirasini yuritadi — gorizontal masshtablashda Redis kerak
 * bo'ladi (mavjud ikkala servis bilan bir xil cheklov).
 *
 * <h2>Kod nega hash qilinadi</h2>
 * Xotiradagi jarayon xatosi yoki log chiqishi orqali ochiq kodning sizib
 * ketish ehtimolini kamaytiradi — BCrypt shu maqsadda allaqachon bor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 4;

    /**
     * ⚠️ Aniq tur emas, INTERFEYS. Prod'da bu {@link EskizSmsClient},
     * lokal profilda {@link LoggingSmsClient} — bu servisning mantiqi
     * ikkalasida ham bir xil ishlaydi.
     */
    private final SmsClient smsClient;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.otp.ttl-seconds:180}")
    private int ttlSeconds;

    /** Bir xil raqamga qayta SMS yuborishdan oldin kutish - 2 daqiqa. */
    @Value("${app.otp.resend-cooldown-seconds:120}")
    private int resendCooldownSeconds;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    /** {@code %s} — 4 xonali kod. UzCasting kabinetida tasdiqlangan shablon matni. */
    @Value("${app.otp.message-template:UzCasting platformasida ro'yxatdan o'tish uchun tasdiqlash kod: %s}")
    private String messageTemplate;

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    /** @return kod amal qilish muddati (soniya) — javobda ilovaga ko'rsatish uchun. */
    public int send(String rawPhone) {
        String phone = normalize(rawPhone);
        Instant now = Instant.now();

        Entry existing = codes.get(phone);
        if (existing != null) {
            Instant cooldownEnds = existing.sentAt.plusSeconds(resendCooldownSeconds);
            if (cooldownEnds.isAfter(now)) {
                long wait = Duration.between(now, cooldownEnds).getSeconds() + 1;
                throw new BusinessException("OTP_COOLDOWN",
                        "Kod allaqachon yuborilgan, " + wait + " soniyadan keyin qayta so'rang",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
        }

        String code = generateCode();
        try {
            smsClient.send(phone, String.format(messageTemplate, code));
        } catch (IllegalStateException e) {
            // Sozlanmagan - mijozning aybi emas, xizmat vaqtincha yo'q.
            throw new BusinessException("SMS_NOT_CONFIGURED", e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RestClientException e) {
            log.error("Eskiz SMS yuborilmadi: {}", phone, e);
            throw new BusinessException("SMS_SEND_FAILED",
                    "SMS yuborilmadi, birozdan keyin urinib ko'ring", HttpStatus.BAD_GATEWAY);
        }

        codes.put(phone, new Entry(passwordEncoder.encode(code), now, now.plusSeconds(ttlSeconds)));
        return ttlSeconds;
    }

    /** @throws BusinessException kod noto'g'ri, muddati o'tgan yoki urinishlar tugagan bo'lsa */
    public void verify(String rawPhone, String code) {
        String phone = normalize(rawPhone);
        Entry entry = codes.get(phone);

        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            codes.remove(phone);
            throw new BusinessException("OTP_EXPIRED",
                    "Kod muddati tugagan, qaytadan so'rang", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        synchronized (entry) {
            if (entry.attempts >= maxAttempts) {
                codes.remove(phone);
                throw new BusinessException("OTP_LOCKED",
                        "Urinishlar soni tugadi, qaytadan so'rang", HttpStatus.TOO_MANY_REQUESTS);
            }

            if (code == null || !passwordEncoder.matches(code, entry.codeHash)) {
                entry.attempts++;
                throw new BusinessException("OTP_INVALID", "Kod noto'g'ri",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        codes.remove(phone);
    }

    /** +998 formatidagi telefon raqamni Eskiz kutgan "998XXXXXXXXX" ko'rinishiga keltiradi. */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.validation("Telefon raqam kiritilmagan");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 9) {
            digits = "998" + digits;
        }
        if (!digits.startsWith("998") || digits.length() != 12) {
            throw BusinessException.validation("Telefon raqam noto'g'ri: +998XXXXXXXXX kutilmoqda");
        }
        return digits;
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        return String.format("%0" + CODE_LENGTH + "d", RANDOM.nextInt(bound));
    }

    private static final class Entry {
        private final String codeHash;
        private final Instant sentAt;
        private final Instant expiresAt;
        private int attempts;

        Entry(String codeHash, Instant sentAt, Instant expiresAt) {
            this.codeHash = codeHash;
            this.sentAt = sentAt;
            this.expiresAt = expiresAt;
        }
    }
}
