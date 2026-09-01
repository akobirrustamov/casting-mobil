package com.example.backend.Sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Lokal stend uchun SMS o'rniga LOG.
 *
 * <h2>Nega bor</h2>
 * Telefon orqali kirish/ro'yxatdan o'tish — ilovaga kirishning yagona
 * yo'li, lekin u Eskiz kabinetiga tayanadi. Lokalda kabinet yo'q, ya'ni
 * usiz kirish ekranidan nari o'tib bo'lmasdi va ichki ekranlarni
 * tekshirib ko'rishning iloji yo'q edi.
 *
 * Kod konsolga chiqadi:
 * <pre>
 * ============================================================
 *  LOKAL OTP  ·  +998901234567  ·  KOD: 4821
 * ============================================================
 * </pre>
 *
 * <h2>⚠️ Nega bu prod'ga uchib ketmaydi</h2>
 * Bean {@code @Profile("local")} bilan qulflangan, {@link EskizSmsClient}
 * esa {@code @Profile("!local")}. Ya'ni bir vaqtda faqat bittasi mavjud
 * bo'ladi va tanlov tasodifiy xossaga emas, profilga bog'liq.
 *
 * {@code local} profili esa serverda ishlatib bo'lmaydi: u bazani
 * XOTIRADAGI H2 ga o'tkazadi va sinov ma'lumotlarini seed qiladi
 * (application-local.properties). Ya'ni uni tasodifan yoqib qo'yish
 * jimgina emas — birinchi so'rovdayoq ma'lum bo'ladi.
 *
 * Qo'shimcha ravishda ishga tushishda ogohlantirish yoziladi, chunki
 * "SMS ketdi" degan taassurot noto'g'ri bo'lardi.
 */
@Slf4j
@Component
@Profile("local")
public class LoggingSmsClient implements SmsClient {

    @PostConstruct
    void warn() {
        log.warn("⚠️  SMS shlyuzi O'CHIRILGAN (local profili). OTP kodlari "
                + "SMS o'rniga shu logga yoziladi. Serverda bu bean yo'q.");
    }

    /**
     * ⚠️ Kod TO'LIQ yoziladi, yashirilmaydi.
     *
     * {@link OtpService} kodni xotirada faqat BCrypt hash ko'rinishida
     * saqlaydi — ya'ni uni boshqa hech qayerdan o'qib bo'lmaydi. Bu yerda
     * niqoblansa, lokal kirish ham ishlamasdi va butun bean ma'nosiz
     * bo'lardi.
     */
    @Override
    public void send(String phone, String message) {
        log.info("""

                ============================================================
                 LOKAL OTP  ·  {}
                 {}
                ============================================================
                """, phone, message);
    }
}
