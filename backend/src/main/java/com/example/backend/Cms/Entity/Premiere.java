package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.PublicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * «Yangi premyeralar» bo'limi kartochkasi.
 *
 * Reklamadan farqi: bu kontent haqidagi e'lon (treyler, tez kunda chiqadi),
 * reklama esa tijorat banneri. Ikkalasi ham bosh sahifada, lekin alohida
 * bo'limlarda va boshqacha ko'rinishda.
 *
 * Havola mexanizmi reklama bilan BIR XIL — {@link InternalLink}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_premiere", indexes = {
        @Index(name = "idx_premiere_status", columnList = "status,sort_order"),
        @Index(name = "idx_premiere_window", columnList = "start_at,end_at")
})
public class Premiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ichki nom — adminlar ro'yxatda taniydi. */
    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_media_id")
    private MediaAsset image;

    /** Treyler yoki qisqa video. Ixtiyoriy. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_media_id")
    private MediaAsset video;

    /** Bog'langan kontent — bo'lsa, kartochka to'g'ridan-to'g'ri unga olib boradi. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private Content content;

    @Column(name = "button_enabled", nullable = false)
    @Builder.Default
    private Boolean buttonEnabled = true;

    @Embedded
    @Builder.Default
    private InternalLink link = new InternalLink();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private PublicationStatus status = PublicationStatus.DRAFT;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "premiere", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PremiereTranslation> translations = new ArrayList<>();

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

    public void addTranslation(PremiereTranslation t) {
        t.setPremiere(this);
        this.translations.add(t);
    }

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
