package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.VideoProcessingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bitta video uchun transcoding ishi.
 *
 * <h2>⚠️ Holat FAQAT shu yerda</h2>
 * {@code MediaAsset} da {@code processingStatus} maydoni ATAYLAB yo'q.
 * Bo'lganda holat ikki joyda yashardi va birinchi nosozlikdayoq
 * ajralardi: ish {@code FAILED} bo'lib, media {@code TRANSCODING} da
 * qolib ketardi.
 *
 * {@code MediaAsset} faqat NATIJANI saqlaydi — {@code hlsMasterKey}
 * bor bo'lsa HLS tayyor.
 *
 * <h2>Har media uchun BITTA ish</h2>
 * Qayta urinish yangi qator yaratmaydi, mavjudini yangilaydi va
 * {@link #attempts} ni oshiradi. Shu sababli «bu medianing hozirgi
 * holati nima» degan savol oddiy {@code join} bilan javob topadi.
 *
 * Har urinishga alohida qator bo'lsa, kutubxona sahifasidagi 40 ta
 * media uchun 40 marta «eng oxirgi qatorni top» kerak bo'lardi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_transcoding_job", indexes = {
        @Index(name = "idx_transcoding_job_queue", columnList = "status,created_at")
})
public class TranscodingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Qaysi media uchun.
     *
     * ⚠️ {@code LAZY} — worker navbatni o'qiyotganda medialarni ham
     * tortib olishi shart emas. Kerak bo'lganda alohida yuklanadi.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", unique = true)
    private MediaAsset media;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private VideoProcessingStatus status = VideoProcessingStatus.QUEUED;

    /**
     * 0..100 — faqat KO'RSATISH uchun.
     *
     * ⚠️ Mantiq bunga tayanmaydi. {@code ffmpeg} progressi taxminiy va
     * u 100 ga yetmasdan ham tugashi mumkin; «100 bo'ldi, demak tayyor»
     * degan qoida jimgina noto'g'ri ishlardi. Tayyorlikni faqat
     * {@code status} aytadi.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer progress = 0;

    /** Nechanchi urinish. Chegaradan oshsa qayta olinmaydi. */
    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    /**
     * Yiqilish sababi — admin panel uchun.
     *
     * ⚠️ Faqat holatni ko'rsatish adminni logga qarashga majbur
     * qilardi, logga esa uning kirishi yo'q.
     */
    @Column(length = 2000)
    private String error;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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

    /**
     * Bosqichni belgilaydi.
     *
     * ⚠️ Xato matni faqat {@code FAILED} da saqlanadi va boshqa har
     * qanday o'tishda TOZALANADI. Aks holda qayta urinish
     * muvaffaqiyatli tugagach ham eski xato ko'rinib turardi va admin
     * uni yangi nosozlik deb o'ylardi.
     */
    public void moveTo(VideoProcessingStatus next, String failure) {
        this.status = next;
        this.error = next == VideoProcessingStatus.FAILED ? trim(failure) : null;

        if (next == VideoProcessingStatus.PROBING && startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (next.isFinished()) {
            finishedAt = LocalDateTime.now();
        }
        if (next == VideoProcessingStatus.READY) {
            progress = 100;
        }
    }

    /**
     * Xato matni ustun chegarasidan oshmasin.
     *
     * ⚠️ {@code ffmpeg} bir necha ming qatorlik chiqish beradi.
     * Kesilmasa yozuv umuman saqlanmasdi va ish holati JIMGINA
     * yangilanmay qolardi — worker esa uni qayta olardi.
     */
    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }
}
