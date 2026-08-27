package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Markazlashtirilgan media kutubxonasining bitta yozuvi.
 *
 * Mavjud {@code Attachment} entity'si o'rnini bosmaydi: u casting moduli uchun
 * ishlaydi va unda atigi 4 maydon bor (id, prefix, name, isWebShow) - mime, o'lcham,
 * davomiylik, kenglik/balandlik yo'q. Attachment'ni kengaytirish bot oqimini
 * regressiyaga uchratardi, shuning uchun yangi entity (roadmap.md -> D10).
 *
 * Fayl qayerda turishi {@code storageKey} orqali aniqlanadi, provayder nomi
 * biznes logikaga qotirilmaydi (StorageService abstraksiyasi).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "media_asset", indexes = {
        @Index(name = "idx_media_type", columnList = "type"),
        @Index(name = "idx_media_created", columnList = "created_at")
})
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Storage ichidagi yo'l/kalit. Provayderga bog'liq emas. */
    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    /** Foydalanuvchi yuklagan asl nom. Fayl tizimida ISHLATILMAYDI (path traversal). */
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MediaType type;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** Video/audio uchun sekundlarda. */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    private Integer width;
    private Integer height;

    /**
     * Fayl holati — {@code READY} yoki {@code ARCHIVED}.
     *
     * ⚠️ Ilgari oddiy {@code String} edi va doim {@code "READY"} yozilardi.
     * Arxivlash qo'shilgach holat mantiqqa ta'sir qila boshladi, matn esa
     * xato yozuvga yo'l qo'yardi ({@code "Ready"}, {@code "ready"}) va
     * ularni hech narsa ushlamasdi.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private MediaStatus status = MediaStatus.READY;

    // ------------------------------------------------- HLS (V28)

    /**
     * HLS master playlist kaliti. {@code null} = HLS yo'q.
     *
     * ⚠️ To'liq URL EMAS, aynan KALIT. CDN domeni sozlamadan olinadi
     * va runtime'da qo'shiladi — shunda domen almashtirish bitta
     * sozlama o'zgarishi bo'ladi, ming qatorli {@code UPDATE} emas.
     *
     * ⚠️ Transcoding HOLATI bu yerda yo'q va bu ataylab: u
     * {@code TranscodingJob} da yashaydi. Ikki joyda bo'lsa ular
     * ajralib ketardi.
     */
    @Column(name = "hls_master_key", length = 512)
    private String hlsMasterKey;

    /** {@code ffprobe} aniqlagan video kodeki — masalan {@code h264}. */
    @Column(name = "video_codec", length = 32)
    private String videoCodec;

    /** {@code ffprobe} aniqlagan audio kodeki — masalan {@code aac}. */
    @Column(name = "audio_codec", length = 32)
    private String audioCodec;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
