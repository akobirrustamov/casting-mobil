package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.CommentStatus;
import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kontent yoki qismga yozilgan izoh.
 *
 * Izohni foydalanuvchi mobil ilovadan yozadi; admin panel faqat moderatsiya
 * qiladi — yashirish, tiklash, o'chirilgan deb belgilash.
 *
 * Hard delete yo'q: shikoyat tarixi va moderator qarori saqlanadi (§58).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_comment", indexes = {
        @Index(name = "idx_comment_content", columnList = "content_id,created_at"),
        @Index(name = "idx_comment_status", columnList = "status,created_at"),
        @Index(name = "idx_comment_author", columnList = "author_id"),
        @Index(name = "idx_comment_reports", columnList = "reports_count")
})
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    /** Izoh aynan qaysi qismga yozilgan. null — butun kontentga. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id")
    private Episode episode;

    @Column(nullable = false, length = 2000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private CommentStatus status = CommentStatus.VISIBLE;

    /** Nechta foydalanuvchi shikoyat qilgan — moderator ustuvorlik uchun ko'radi. */
    @Column(name = "reports_count", nullable = false)
    @Builder.Default
    private Integer reportsCount = 0;

    /** Kim moderatsiya qildi va qachon — javobgarlik izlanadigan bo'lsin. */
    @Column(name = "moderated_by")
    private UUID moderatedBy;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
