package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.Locale;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_season_translation", uniqueConstraints =
        @UniqueConstraint(name = "uk_season_locale", columnNames = {"season_id", "locale"}))
public class SeasonTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id")
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Locale locale;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;
}
