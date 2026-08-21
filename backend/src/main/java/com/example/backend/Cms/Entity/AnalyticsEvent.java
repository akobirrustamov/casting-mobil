package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.AnalyticsEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Xom hodisa — klient yuborgan bitta voqea.
 *
 * <b>Bu jadval KATTA bo'ladi.</b> Dashboard undan HECH QACHON to'g'ridan-to'g'ri
 * o'qimaydi (§29, §74): ko'rsatkichlar kunlik agregatlardan olinadi
 * ({@link AdDailyStatistic}, {@link ContentDailyStatistic}).
 *
 * Xom yozuvlar shu sababli saqlanadi:
 *   1. agregatni qayta hisoblash mumkin bo'lsin (formula o'zgarsa);
 *   2. unikal foydalanuvchilarni sanash uchun (agregatda faqat natija turadi).
 *
 * {@code eventDate} ataylab alohida ustun: agregat bo'yicha guruhlash
 * {@code created_at} dan sana ajratmasdan, indeks bilan ishlaydi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_analytics_event", indexes = {
        @Index(name = "idx_event_agg", columnList = "event_date,type,processed"),
        @Index(name = "idx_event_target", columnList = "type,target_id,event_date"),
        @Index(name = "idx_event_created", columnList = "created_at")
})
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AnalyticsEventType type;

    /** Reklama, kontent yoki bildirishnoma id'si. */
    @Column(name = "target_id")
    private Long targetId;

    /** Qism darajasidagi hodisalar uchun. */
    @Column(name = "episode_id")
    private Long episodeId;

    /** Kim — unikal sanash uchun. Anonim bo'lsa null. */
    @Column(name = "user_id")
    private UUID userId;

    /**
     * Anonim foydalanuvchini unikal sanash uchun klient identifikatori.
     * Shaxsni aniqlamaydi — faqat bir qurilmani ikkinchisidan ajratadi.
     */
    @Column(name = "device_key", length = 128)
    private String deviceKey;

    /** Guruhlash kaliti. created_at dan ajratilmaydi — indeks samarali bo'lsin. */
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    /** Agregatga qo'shilganmi. Qayta hisoblashda false ga qaytariladi. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (eventDate == null) {
            eventDate = now.toLocalDate();
        }
    }
}
