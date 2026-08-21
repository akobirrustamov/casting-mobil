package com.example.backend.Cms.Entity;

import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Foydalanuvchi kirgan qurilma.
 *
 * Buyurtmachi talabi: «bitta hisobdan 2 ta dan ortiq qurilmadan kirish mumkin
 * emas» va begona qurilmani chiqarib yuborish imkoniyati bo'lishi kerak.
 *
 * Limit qattiq kodda emas — {@code PlatformSetting} da, chunki u o'zgarishi mumkin.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_user_device",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_device",
                columnNames = {"user_id", "device_id"}),
        indexes = @Index(name = "idx_device_user", columnList = "user_id,active"))
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** Klient generatsiya qiladigan barqaror identifikator. */
    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    /** ios / android / web */
    @Column(length = 32)
    private String platform;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastActiveAt == null) {
            lastActiveAt = now;
        }
    }
}
