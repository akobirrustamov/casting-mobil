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
@Table(name = "cms_premiere_translation", uniqueConstraints =
        @UniqueConstraint(name = "uk_premiere_locale", columnNames = {"premiere_id", "locale"}))
public class PremiereTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "premiere_id")
    private Premiere premiere;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Locale locale;

    @Column(nullable = false, length = 255)
    private String title;

    /** «Tez kunda» kabi qisqa yozuv. */
    @Column(length = 255)
    private String subtitle;

    @Column(length = 2000)
    private String description;

    @Column(name = "button_text", length = 64)
    private String buttonText;
}
