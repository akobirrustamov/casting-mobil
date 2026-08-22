package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Platformadagi asosiy birlik: film, serial, mini-serial, podkast, shou, stream, klip.
 *
 * Uchta tuzilishni ham qo'llab-quvvatlaydi ({@link StructureType}):
 * SINGLE - qismlarsiz; EPISODIC - faslsiz qismlar; SEASONAL - fasl -> qism.
 *
 * Matnlar uch tilda ({@link ContentTranslation}), media esa til bo'yicha
 * ixtiyoriy ({@link ContentMedia#getLocale()}): masalan ruscha afisha alohida
 * yuklanishi mumkin, yuklanmasa umumiy afisha ishlatiladi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_content", indexes = {
        @Index(name = "idx_content_slug", columnList = "slug", unique = true),
        @Index(name = "idx_content_status", columnList = "status"),
        @Index(name = "idx_content_category", columnList = "category_id"),
        @Index(name = "idx_content_publication", columnList = "publication_date"),
        @Index(name = "idx_content_premiere", columnList = "premiere_date"),
        @Index(name = "idx_content_type", columnList = "content_type,orientation"),
        @Index(name = "idx_content_featured", columnList = "featured"),
        @Index(name = "idx_content_popular", columnList = "popular")
})
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 32)
    private ContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "structure_type", nullable = false, length = 16)
    private StructureType structureType;

    /** YouTube uslubi (yonlama) yoki Reels uslubi (tik). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private ContentOrientation orientation = ContentOrientation.LANDSCAPE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private PublicationStatus status = PublicationStatus.DRAFT;

    /**
     * Kontent qanchalik topiladi (ТЗ §15).
     *
     * {@code status} dan FARQLI: u hayot siklini, bu esa topilishini
     * bildiradi. Nashr qilingan film premyeradan oldin {@code UNLISTED}
     * bo'lishi mumkin — havola bilan ochiladi, katalogda chiqmaydi.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private ContentVisibility visibility = ContentVisibility.PUBLIC;

    /**
     * Asarning ASL tili — ISO 639-1 kodi ({@code uz}, {@code ru},
     * {@code ko}, {@code tr}…).
     *
     * ⚠️ Tarjimalar bilan ARALASHTIRILMASIN: {@code ContentTranslation}
     * sarlavha va tavsifni UZ/RU/EN da saqlaydi, bu maydon esa asarning
     * o'zi qaysi tilda suratga olinganini bildiradi. Koreys seriali
     * ruscha tarjimasi bilan ham koreyscha qoladi.
     *
     * Enum emas: dunyo tillari ro'yxati enum'ga sig'maydi va har yangi til
     * migratsiya talab qilardi (D18 bilan bir xil sabab).
     */
    @Column(length = 8)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_policy", nullable = false, length = 32)
    @Builder.Default
    private AccessPolicy accessPolicy = AccessPolicy.FREE;

    /**
     * Butun premyerani sotib olish narxi. Null - sotuvda emas.
     * BigDecimal: pul hech qachon double bilan saqlanmaydi.
     */
    @Column(name = "premiere_price", precision = 12, scale = 2)
    private BigDecimal premierePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cms_content_genre",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    @Builder.Default
    private Set<Genre> genres = new LinkedHashSet<>();

    /** "16+" kabi. */
    @Column(name = "age_rating", length = 8)
    private String ageRating;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "premiere_date")
    private LocalDateTime premiereDate;

    @Column(name = "publication_date")
    private LocalDateTime publicationDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean popular = false;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "stars_received", nullable = false)
    @Builder.Default
    private Long starsReceived = 0L;

    /**
     * ⚠️ {@code @BatchSize} — bosh sahifa uchun (§31).
     *
     * Bosh sahifada 8-10 qator, har birida 20 tagacha film bo'ladi. Bu
     * to'plamlar dangasa (lazy): har bir film uchun alohida so'rov ketsa,
     * bitta sahifa 200 dan ortiq so'rovga aylanardi (N+1).
     *
     * {@code @EntityGraph} bilan fetch join qilib ham bo'lmaydi: bu yerda
     * IKKITA to'plam bor ({@code translations} va {@code media}), ikkalasini
     * birdan fetch join qilish {@code MultipleBagFetchException} beradi.
     *
     * {@code @BatchSize} esa Hibernate'ga «bittalab emas, elliktalab yukla»
     * deydi — natijada har bir to'plam uchun bir nechta so'rov, minglab
     * emas.
     */
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<ContentTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<ContentMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ContentCredit> credits = new ArrayList<>();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Soft delete. Null - o'chirilmagan. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Ikki admin bir vaqtda tahrirlaganda birinchisining o'zgarishi yo'qolmasligi uchun. */
    @Version
    private Long version;

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

    /**
     * Qatorni «o'zgargan» deb belgilaydi (§60).
     *
     * {@code @PreUpdate} bu ishni bajarmaydi: u Hibernate qatorni
     * allaqachon iflos deb topgandan KEYIN ishlaydi. Faqat tarjima
     * o'zgarganda esa kontent qatori toza qoladi va versiya oshmasdi.
     */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addTranslation(ContentTranslation t) {
        t.setContent(this);
        this.translations.add(t);
    }

    public void addMedia(ContentMedia m) {
        m.setContent(this);
        this.media.add(m);
    }

    public void addCredit(ContentCredit c) {
        c.setContent(this);
        this.credits.add(c);
    }
}
