package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.PublicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Qism.
 *
 * {@code season} NULL bo'lishi mumkin - faslsiz mini-seriallarda
 * ({@code StructureType.EPISODIC}) qismlar to'g'ridan-to'g'ri kontentga tegishli.
 *
 * <b>Video bitta maydon emas.</b> Bir qism bir nechta video qismdan iborat
 * bo'lishi mumkin ({@link EpisodeVideo}) - shuning uchun {@code videoUrl} yo'q.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_episode", indexes = {
        @Index(name = "idx_episode_content", columnList = "content_id,sort_order"),
        @Index(name = "idx_episode_season", columnList = "season_id,episode_number"),
        @Index(name = "idx_episode_status", columnList = "status")
})
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    /** null - faslsiz mini-serial. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_media_id")
    private MediaAsset thumbnail;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "premiere_date")
    private LocalDateTime premiereDate;

    @Column(name = "publication_date")
    private LocalDateTime publicationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private PublicationStatus status = PublicationStatus.DRAFT;

    /**
     * Kontent siyosatini bekor qiladi. null - kontentnikidan meros olinadi.
     * Masalan: serial PREMIUM_OR_PURCHASE, lekin 1-qism FREE (reklama uchun).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_policy", length = 32)
    private AccessPolicy accessPolicyOverride;

    /** Shu qismni alohida sotib olish narxi. null - sozlamalardagi default. */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "episode", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EpisodeTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "episode", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EpisodeVideo> videos = new ArrayList<>();

    @Version
    private Long version;

    public void addTranslation(EpisodeTranslation t) {
        t.setEpisode(this);
        this.translations.add(t);
    }

    public void addVideo(EpisodeVideo v) {
        v.setEpisode(this);
        this.videos.add(v);
    }

    /** Amaldagi kirish siyosati: o'zinikini, bo'lmasa kontentnikini beradi. */
    public AccessPolicy effectiveAccessPolicy() {
        if (accessPolicyOverride != null) {
            return accessPolicyOverride;
        }
        return content == null ? AccessPolicy.FREE : content.getAccessPolicy();
    }
}
