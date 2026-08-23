package com.example.backend.Cms.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Bitta reklama uchun bitta kunlik jamlanma (§29).
 *
 * Dashboard aynan shu jadvaldan o'qiydi — xom hodisalar ustida
 * {@code COUNT(*)} qilinmaydi.
 *
 * Unikal sonlar agregatlash paytida {@code COUNT(DISTINCT ...)} bilan
 * hisoblanadi: bu bitta kunlik oyna ustida bo'lgani uchun cheklangan ish.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_ad_daily_statistic",
        uniqueConstraints = @UniqueConstraint(name = "uk_ad_stat_day",
                columnNames = {"advertisement_id", "stat_date"}),
        indexes = @Index(name = "idx_ad_stat_date", columnList = "stat_date"))
public class AdDailyStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "advertisement_id", nullable = false)
    private Long advertisementId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(nullable = false)
    @Builder.Default
    private Long impressions = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "unique_impressions", nullable = false)
    @Builder.Default
    private Long uniqueImpressions = 0L;

    @Column(name = "unique_clicks", nullable = false)
    @Builder.Default
    private Long uniqueClicks = 0L;

    /** CTR foizda. Nol ko'rsatishda 0 qaytaradi — nolga bo'linish yo'q. */
    public double ctr() {
        if (impressions == null || impressions == 0) {
            return 0d;
        }
        return (clicks == null ? 0 : clicks) * 100d / impressions;
    }
}
