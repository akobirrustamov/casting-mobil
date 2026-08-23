package com.example.backend.Entity;

import com.example.backend.Enums.Permission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WORKER'ga berilgan bitta ruxsat.
 *
 * Faqat WORKER uchun ma'noga ega: ADMIN va undan yuqori rollar rol darajasida
 * to'liq huquqqa ega va bu jadvalga qaralmaydi.
 *
 * Nega alohida jadval, enum ichida emas: ruxsatlar runtime'da o'zgaradi —
 * Admin/SuperAdmin Worker yaratayotganda ularni tanlaydi.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "user_permission",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_permission",
                columnNames = {"user_id", "permission"}
        )
)
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private Permission permission;

    /** Kim bergan — audit uchun. */
    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @PrePersist
    void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }
}
