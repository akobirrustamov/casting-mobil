package com.example.backend.Admin.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Bildirishnoma hisoboti (ТЗ §33).
 *
 * <h2>Nima uchun har bir ko'rsatkichda «mavjudmi» bayrog'i bor</h2>
 * ТЗ: «Mavjud infrastructure qaysi metricni real berishi mumkinligini
 * aniqlab ishlat. Real ma'lumot bo'lmasa fake statistic yaratma.»
 *
 * Ba'zi ko'rsatkichlarni biz O'ZIMIZ bilamiz (yuborildi, xato — bu bizning
 * yozuvimiz). Ba'zilari klientdan keladi (ochildi, bosildi — analitika
 * hodisasi). {@code delivered} esa FAQAT push provayderi kvitansiyasidan
 * kelishi mumkin, u hozir ulanmagan.
 *
 * Agar {@code delivered} nol deb ko'rsatilsa, admin «hech kimga yetib
 * bormadi» deb o'ylardi — bu YOLG'ON. Shuning uchun raqam emas, «o'lchanmaydi»
 * degan holat va uning sababi qaytariladi.
 */
@Data
@Builder
public class NotificationReportDto {

    private Long notificationId;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private String failureReason;

    /** Bizning yozuvimiz: xabar yuborishga urinildimi va natija qanday. */
    private Metric sent;
    private Metric failed;

    /** Klient hodisalari: analitika endpointidan keladi. */
    private Metric opened;
    private Metric clicked;

    /** Provayder kvitansiyasi kerak — hozir mavjud emas. */
    private Metric delivered;

    /**
     * Bitta ko'rsatkich.
     *
     * @param available o'lchanadimi. {@code false} bo'lsa {@code value} null
     *                  bo'ladi — nol EMAS, chunki nol «bo'lmadi» degani,
     *                  bilmaslik esa boshqa narsa.
     */
    @Data
    @Builder
    public static class Metric {
        private Boolean available;
        private Long value;
        private Long unique;
        /** O'lchanmasa — nima uchun. Admin sababni ko'rsin. */
        private String unavailableReason;

        public static Metric of(long value) {
            return Metric.builder().available(true).value(value).build();
        }

        public static Metric of(long value, long unique) {
            return Metric.builder().available(true).value(value).unique(unique).build();
        }

        public static Metric unavailable(String reason) {
            return Metric.builder().available(false).unavailableReason(reason).build();
        }
    }
}
