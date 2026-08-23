package com.example.backend.Cms.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Kontent kategoriyasi - mobil ilovaning bosh menyusida chiqadi.
 *
 * Kategoriya {@code ContentType} bilan bir xil narsa EMAS:
 * type = MINI_SERIES (texnik tuzilish), category = "Drama" (mavzu).
 *
 * Nomi va tavsifi uch tilda - {@link CategoryTranslation}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_category", indexes = {
        @Index(name = "idx_category_slug", columnList = "slug", unique = true),
        @Index(name = "idx_category_active", columnList = "active,sort_order")
})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** URL uchun barqaror identifikator. Tarjima qilinmaydi. */
    @Column(nullable = false, unique = true, length = 128)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icon_media_id")
    private MediaAsset icon;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CategoryTranslation> translations = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Tarjimani qo'shishning yagona to'g'ri yo'li - ikki tomonlama bog'lanish saqlanadi. */
    public void addTranslation(CategoryTranslation translation) {
        translation.setCategory(this);
        this.translations.add(translation);
    }
}
