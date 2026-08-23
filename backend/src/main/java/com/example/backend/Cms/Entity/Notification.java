package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.NotificationAudience;
import com.example.backend.Cms.Enums.NotificationStatus;
import com.example.backend.Cms.Enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Push-bildirishnoma.
 *
 * ⚠️ Yuborish HALI ULANMAGAN. FCM provayderi sozlanmagan, shuning uchun
 * "yuborildi" holati qo'yilmaydi va soxta statistika ko'rsatilmaydi (§32, §33).
 * Admin bildirishnoma yaratishi va rejalashtirishi mumkin; haqiqiy yuborish
 * provayder ulangandan keyin ishlaydi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_notification", indexes = {
        @Index(name = "idx_notification_status", columnList = "status,scheduled_at"),
        @Index(name = "idx_notification_type", columnList = "type")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private NotificationType type = NotificationType.APP_NOTIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private NotificationAudience audience = NotificationAudience.ALL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_media_id")
    private MediaAsset image;

    @Embedded
    @Builder.Default
    private InternalLink link = new InternalLink();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.DRAFT;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    /** Haqiqatda yuborilgan payt. Provayder tasdiqlamaguncha null. */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** Yuborish yiqilsa sababi — soxta muvaffaqiyat o'rniga aniq xato. */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<NotificationTranslation> translations = new ArrayList<>();

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

    public void addTranslation(NotificationTranslation t) {
        t.setNotification(this);
        this.translations.add(t);
    }
}
