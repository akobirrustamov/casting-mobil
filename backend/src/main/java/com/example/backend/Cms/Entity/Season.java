package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.PublicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serial fasli. Faqat {@code StructureType.SEASONAL} kontentda bo'ladi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_season",
        uniqueConstraints = @UniqueConstraint(name = "uk_season_number",
                columnNames = {"content_id", "season_number"}),
        indexes = @Index(name = "idx_season_content", columnList = "content_id,sort_order"))
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_media_id")
    private MediaAsset poster;

    @Column(name = "premiere_date")
    private LocalDateTime premiereDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private PublicationStatus status = PublicationStatus.DRAFT;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SeasonTranslation> translations = new ArrayList<>();

    public void addTranslation(SeasonTranslation t) {
        t.setSeason(this);
        this.translations.add(t);
    }
}
