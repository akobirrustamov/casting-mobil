package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.MediaRole;
import jakarta.persistence.*;
import lombok.*;

/**
 * Kontent va media fayl orasidagi bog'lanish.
 *
 * <b>Tilga bog'liq media.</b> {@code locale} maydoni ixtiyoriy:
 * <ul>
 *   <li>{@code null} - bu fayl BARCHA tillar uchun (default);</li>
 *   <li>{@code RU} - faqat rus tilida ko'rsatiladi.</li>
 * </ul>
 * Tanlash qoidasi: avval aniq til uchun fayl qidiriladi, topilmasa
 * {@code locale = null} bo'lgani olinadi. Ya'ni har bir til uchun alohida
 * afisha yuklash MUMKIN, lekin MAJBURIY emas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_content_media", indexes = {
        @Index(name = "idx_content_media_lookup", columnList = "content_id,role,locale")
})
public class ContentMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id")
    private MediaAsset media;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MediaRole role;

    /** null = barcha tillar uchun umumiy. */
    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Locale locale;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
