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
@Table(name = "cms_content_translation", uniqueConstraints =
        @UniqueConstraint(name = "uk_content_locale", columnNames = {"content_id", "locale"}))
public class ContentTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Locale locale;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "short_description", length = 1000)
    private String shortDescription;

    @Column(length = 8000)
    private String description;
}
