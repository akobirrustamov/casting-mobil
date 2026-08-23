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
 * <h2>Beshta ko'rsatkich — bu QABUL QILUVCHILAR bo'yicha voronka</h2>
 * <pre>
 *   sent → delivered → opened → clicked
 *        ↘ failed
 * </pre>
 * Ya'ni har biri ODAMLAR sonini bildiradi, xabarning o'z holatini emas.
 *
 * <h2>Nima uchun {@code sent} ham «o'lchanmaydi»</h2>
 * Ilgari bu yerda {@code sent = 1} qaytarilardi: «bu xabarning holati
 * SENT». Bu voronkani ma'nosiz qilardi — 1 kishiga yuborilgan xabarni
 * 250 kishi ochgan bo'lib chiqardi. Bu ham soxta statistika, faqat
 * nozikroq turi: raqam o'ylab topilmagan, lekin u BOSHQA narsani
 * o'lchaydi va qo'shni ustunlar bilan solishtirib bo'lmaydi.
 *
 * Qabul qiluvchilar sonini bilish uchun har bir odam bo'yicha yozuv kerak
 * ({@code notification_delivery}), u esa push provayderi ulangandan keyin
 * paydo bo'ladi. Xabarning O'Z holati esa {@link #status} va
 * {@link #failureReason} da — o'z joyida turadi.
 *
 * <h2>Nima uchun nol emas, {@code null}</h2>
 * Nol «bo'lmadi» degani. Bilmaslik esa boshqa narsa. {@code delivered = 0}
 * ko'rsatilsa admin «hech kimga yetib bormadi» deb o'ylardi va butunlay
 * boshqa muammoni qidirardi.
 */
@Data
@Builder
public class NotificationReportDto {

    private Long notificationId;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private String failureReason;

    /**
     * Auditoriya HAJMI — nechta foydalanuvchi bu xabarni olishi kerak.
     *
     * ⚠️ Bu «yuborildi» EMAS. Bu nishon auditoriyasining hozirgi hajmi.
     * Usiz {@code opened} ni umuman talqin qilib bo'lmaydi: 250 ta ochilish
     * ko'p ham, oz ham bo'lishi mumkin — auditoriya 300 kishimi yoki
     * 300 000 kishimi, bilinmaydi.
     */
    private Metric audienceSize;

    /**
     * Yuborilgan QABUL QILUVCHILAR soni.
     *
     * ⚠️ Bu «bu xabar yuborildimi» degan 0/1 EMAS — u {@link #status} da
     * turadi. Bu yerda odamlar soni kerak, uni esa faqat push provayderi
     * bera oladi.
     */
    private Metric sent;

    /** Yuborib bo'lmagan qabul qiluvchilar soni — provayderdan keladi. */
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
