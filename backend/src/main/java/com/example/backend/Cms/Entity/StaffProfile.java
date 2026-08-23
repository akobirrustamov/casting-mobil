package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.StaffStatus;
import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Xodimga oid holat va metadata.
 *
 * Eski {@code users} jadvali muzlatilgan, shuning uchun bu ma'lumot alohida
 * saqlanadi. Batafsil: {@code V10__staff_profile.sql}.
 */
@Entity
@Table(name = "cms_staff_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StaffStatus status;

    /** Kim yaratgan. {@code null} = tizim (AutoRun yoki seeder). */
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Oxirgi muvaffaqiyatli kirish. {@code null} = hali kirmagan. */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "status_changed_by")
    private UUID statusChangedBy;

    @Column(name = "status_reason", length = 500)
    private String statusReason;
}
