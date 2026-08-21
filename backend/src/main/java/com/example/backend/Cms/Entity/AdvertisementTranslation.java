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
@Table(name = "cms_advertisement_translation", uniqueConstraints =
        @UniqueConstraint(name = "uk_ad_locale", columnNames = {"advertisement_id", "locale"}))
public class AdvertisementTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advertisement_id")
    private Advertisement advertisement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Locale locale;

    @Column(length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    /** Tugma yozuvi. buttonEnabled = false bo'lsa ishlatilmaydi. */
    @Column(name = "button_text", length = 64)
    private String buttonText;
}
