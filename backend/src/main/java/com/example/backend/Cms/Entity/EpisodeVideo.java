package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.Locale;
import jakarta.persistence.*;
import lombok.*;

/**
 * Qismning bitta video qismi.
 *
 * Nega alohida jadval: ba'zi qismlar bitta katta video emas, bir nechta
 * segmentdan iborat (1-qism: part 1, part 2, part 3). {@code episode.videoUrl}
 * kabi yagona maydon buni ifodalay olmaydi.
 *
 * {@code locale} - dublyaj tili. null bo'lsa barcha tillar uchun umumiy
 * (masalan subtitrsiz original).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_episode_video", indexes = {
        @Index(name = "idx_episode_video", columnList = "episode_id,locale,part_number")
})
public class EpisodeVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "episode_id")
    private Episode episode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id")
    private MediaAsset media;

    /** Dublyaj tili. null = barcha tillar uchun. */
    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Locale locale;

    /** 1, 2, 3 ... - ketma-ketlikdagi o'rni. */
    @Column(name = "part_number", nullable = false)
    @Builder.Default
    private Integer partNumber = 1;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
