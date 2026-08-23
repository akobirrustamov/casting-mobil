package com.example.backend.Cms.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Kontent bo'yicha kunlik jamlanma.
 *
 * Dashboard va hisobotlar shu yerdan o'qiydi, xom hodisalardan emas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_content_daily_statistic",
        uniqueConstraints = @UniqueConstraint(name = "uk_content_stat_day",
                columnNames = {"content_id", "stat_date"}),
        indexes = @Index(name = "idx_content_stat_date", columnList = "stat_date"))
public class ContentDailyStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(nullable = false)
    @Builder.Default
    private Long views = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long plays = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long completes = 0L;

    @Column(name = "unique_viewers", nullable = false)
    @Builder.Default
    private Long uniqueViewers = 0L;

    /** Necha foiz oxirigacha ko'rildi. */
    public double completionRate() {
        if (plays == null || plays == 0) {
            return 0d;
        }
        return (completes == null ? 0 : completes) * 100d / plays;
    }
}
