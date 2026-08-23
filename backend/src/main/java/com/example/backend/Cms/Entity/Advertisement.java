package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.AdAudience;
import com.example.backend.Cms.Enums.PublicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bosh sahifadagi banner.
 *
 * Ikki vazifani bajaradi ({@link AdAudience}):
 *   ADVERTISEMENT      — tijorat reklamasi, faqat obunasi yo'q userlarga;
 *   ADMIN_ANNOUNCEMENT — admin e'loni, hammaga.
 *
 * Tugma ham, havola ham IXTIYORIY: {@code buttonEnabled = false} bo'lsa tugma
 * umuman chizilmaydi, {@code link.linkType = NONE} bo'lsa banner bosilmaydi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_advertisement", indexes = {
        @Index(name = "idx_ad_status", columnList = "status,sort_order"),
        @Index(name = "idx_ad_window", columnList = "start_at,end_at"),
        @Index(name = "idx_ad_audience", columnList = "audience")
})
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ichki nom — adminlar ro'yxatda taniydi. Foydalanuvchiga ko'rinmaydi. */
    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_media_id")
    private MediaAsset image;

    /** Tor ekran uchun alohida rasm. Bo'lmasa asosiysi ishlatiladi. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mobile_image_media_id")
    private MediaAsset mobileImage;

    @Column(name = "button_enabled", nullable = false)
    @Builder.Default
    private Boolean buttonEnabled = false;

    @Embedded
    @Builder.Default
    private InternalLink link = new InternalLink();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private AdAudience audience = AdAudience.ADVERTISEMENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private PublicationStatus status = PublicationStatus.DRAFT;

    /** Ko'rsatish oynasi. null — cheklovsiz. */
    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AdvertisementTranslation> translations = new ArrayList<>();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Oxirgi o'zgarish vaqti (ТЗ §27).
     *
     * Banner tez-tez tahrirlanadi — rasm, matn, ko'rsatish oynasi, tugma.
     * Busiz «qaysi bannerlar eskirgan» degan savolga javob berish uchun
     * audit jurnalini titish kerak bo'lardi.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        updatedAt = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void addTranslation(AdvertisementTranslation t) {
        t.setAdvertisement(this);
        this.translations.add(t);
    }

    /** Hozir ko'rsatilishi kerakmi: nashr qilingan va vaqt oynasi ichida. */
    public boolean isLiveAt(LocalDateTime moment) {
        if (status != PublicationStatus.PUBLISHED) {
            return false;
        }
        if (startAt != null && moment.isBefore(startAt)) {
            return false;
        }
        return endAt == null || !moment.isAfter(endAt);
    }

    /** Tahrirlashda avtomatik yangilanadi — qo'lda o'rnatish esdan chiqmasin. */
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
