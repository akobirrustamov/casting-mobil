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
@Table(name = "cms_homepage_section_translation", uniqueConstraints =
        @UniqueConstraint(name = "uk_homepage_locale", columnNames = {"section_id", "locale"}))
public class HomepageSectionTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id")
    private HomepageSection section;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Locale locale;

    /** Bo'lim sarlavhasi — «Yangi premyeralar», «Mashhur ijodkorlar». */
    @Column(nullable = false, length = 255)
    private String title;
}
