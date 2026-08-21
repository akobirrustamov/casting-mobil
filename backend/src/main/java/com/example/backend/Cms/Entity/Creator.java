package com.example.backend.Cms.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ijodkor: aktyor, rejissyor, model, prodyuser va h.k.
 *
 * Kasb bu yerda saqlanMAYDI - bitta odam bir kinoda aktyor, boshqasida rejissyor
 * bo'lishi mumkin. Kasb {@link ContentCredit} da, kontent bilan bog'liq holda turadi.
 *
 * Ism uch tilda: kirill/lotin/inglizcha transliteratsiya bir xil emas
 * ("Дилноза" / "Dilnoza"), shuning uchun tarjima qilinadi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_creator", indexes = {
        @Index(name = "idx_creator_slug", columnList = "slug", unique = true),
        @Index(name = "idx_creator_featured", columnList = "featured,sort_order"),
        @Index(name = "idx_creator_active", columnList = "active")
})
public class Creator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_media_id")
    private MediaAsset photo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_media_id")
    private MediaAsset cover;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Bosh sahifadagi "Mashhur ijodkorlar" bo'limi uchun. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** Yuborilgan Stars soni - donat reytingi uchun. Denormalizatsiya, tez o'qish uchun. */
    @Column(name = "stars_received", nullable = false)
    @Builder.Default
    private Long starsReceived = 0L;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CreatorTranslation> translations = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Oxirgi o'zgarish vaqti (ТЗ §24).
     *
     * Ijodkor profili tez-tez tahrirlanadi — ism, surat, biografiya,
     * «mashhur» bejagi. Busiz «qaysi profillar eskirgan» degan savolga
     * javob berish uchun audit jurnalini titish kerak bo'lardi.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addTranslation(CreatorTranslation translation) {
        translation.setCreator(this);
        this.translations.add(translation);
    }
}
