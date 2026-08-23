package com.example.backend.Config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Ilova vaqt mintaqasi (ТЗ §68).
 *
 * <h2>Muammo</h2>
 * Loyihada 310 dan ortiq joyda {@code LocalDateTime} ishlatiladi va u
 * vaqt mintaqasini SAQLAMAYDI. {@code LocalDateTime.now()} esa JVM ning
 * mintaqasini oladi. Konteynerlarda sukut bo'yicha UTC bo'ladi, admin
 * esa panelga Toshkent vaqtini kiritadi.
 *
 * Natija: admin «21:00» deb qo'ygan premyera serverda 21:00 UTC deb
 * o'qiladi va Toshkentda 02:00 da chiqadi — besh soat kech. Reklama
 * oynasi, rejalashtirilgan bildirishnoma, kunlik hisobot chegaralari —
 * hammasi shu xatoga uchraydi va hech qanday xato xabari bermaydi.
 *
 * <h2>Nega UTC ga o'tkazilmadi</h2>
 * ТЗ «imkon qadar UTC» deydi. To'liq o'tish {@code Instant} ga 310 ta
 * joyni ko'chirishni va panelning har bir sana maydonini o'girishni
 * talab qilardi — yarim bajarilgan o'tish esa umuman qilmaslikdan
 * yomonroq: bir qism qiymatlar UTC, bir qismi mahalliy bo'lib qolardi
 * va ularni farqlash imkoni yo'q.
 *
 * Odatda UTC talab qilinishining asosiy sababi — yozgi vaqt. O'zbekiston
 * 1996 yildan beri yozgi vaqtga o'tmaydi va UTC+5 da qat'iy turadi,
 * ya'ni takrorlanadigan yoki tushib qoladigan soat YO'Q. Shu sababli
 * mahalliy vaqtni saqlash bu yerda noaniqlik yaratmaydi.
 *
 * Qaror {@code roadmap.md → Important Decisions} da qayd etilgan;
 * ko'p mintaqali kengayish bo'lsa {@code Instant} ga o'tish yo'li ham
 * shu yerda yozilgan.
 */
@Slf4j
@Configuration
public class TimeZoneConfig {

    @Value("${app.timezone:Asia/Tashkent}")
    private String timezone;

    @PostConstruct
    public void applyTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        log.info("Ilova vaqt mintaqasi: {}", timezone);
    }
}
