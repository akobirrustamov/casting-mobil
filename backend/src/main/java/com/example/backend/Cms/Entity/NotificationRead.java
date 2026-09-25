package com.example.backend.Cms.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * «Bu odam shu xabarni o'qigan» — qator bor bo'lsa o'qilgan.
 *
 * <h2>⚠️ Nega bog'lanish ({@code @ManyToOne}) emas, oddiy ustunlar</h2>
 * Bu jadvaldan faqat «qaysi id'lar o'qilgan» degan savol so'raladi.
 * Odamni yoki xabarni yuklash hech qachon kerak emas — ular
 * allaqachon qo'lda. Chet el kalitlari migratsiyada bor (V43).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_notification_read")
@IdClass(NotificationRead.Key.class)
public class NotificationRead {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private UUID userId;
        private Long notificationId;
    }
}
