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
@Table(name = "cms_tariff_translation", uniqueConstraints =
        @UniqueConstraint(name = "uk_tariff_locale", columnNames = {"tariff_id", "locale"}))
public class TariffTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tariff_id")
    private Tariff tariff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Locale locale;

    @Column(nullable = false, length = 255)
    private String name;

    /** «⭐ ENG FOYDALI TARIF» kabi qo'shimcha yozuv. */
    @Column(length = 255)
    private String badge;

    /** Har bir qator alohida — klient ro'yxat qilib chizadi. */
    @Column(length = 2000)
    private String features;
}
