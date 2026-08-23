package com.example.backend.Cms.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bo'laklab yuklash sessiyasi.
 *
 * <h2>Nega kerak</h2>
 * Bitta {@code multipart/form-data} so'rovi bilan epizod videosini yuklab
 * bo'lmaydi: prod chegarasi 50 MB, haqiqiy epizod esa yuz megabaytdan
 * gigabaytgacha. Undan tashqari bitta ulkan so'rov uzilsa — hammasi boshidan
 * boshlanadi.
 *
 * Shuning uchun fayl bo'laklarga bo'linib yuboriladi va uzilishdan keyin
 * DAVOM ETTIRISH mumkin.
 *
 * <h2>Qabul qilingan bo'laklar qayerda</h2>
 * Bazada EMAS — diskda. Sessiya papkasidagi {@code .part} fayllar ro'yxati
 * yagona haqiqat manbai. Sabab: baza va disk bir-biriga mos kelmay qolishi
 * mumkin (yozildi, keyin xato), disk esa o'zini o'zi tekshiradi.
 */
@Entity
@Table(name = "cms_upload_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSession {

    @Id
    @Column(length = 36)
    private String id;

    /** Foydalanuvchi bergan nom — faqat kengaytma uchun ishlatiladi. */
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    /** Yakuniy fayl qaysi papkaga tushadi. */
    @Column(nullable = false, length = 64)
    private String folder;

    /** PENDING · COMPLETED · ABORTED */
    @Column(nullable = false, length = 16)
    private String status;

    /** Sessiyani faqat egasi davom ettira oladi. */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Yig'ilgandan keyin yaratilgan media yozuvi. */
    @Column(name = "media_asset_id")
    private Long mediaAssetId;
}
