package com.example.backend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Muhim admin amallarining o'zgarmas tarixi (§59).
 *
 * ⚠️ Parol, token, to'lov credential'i bu yerga YOZILMAYDI.
 * ⚠️ Oddiy Admin bu jadvalni tozalay olmaydi — o'chirish endpointi berilmaydi.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_actor", columnList = "actor_id"),
                @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
                @Index(name = "idx_audit_created", columnList = "created_at")
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private UUID actorId;

    /** Amal paytidagi rol — keyin rol o'zgarsa ham tarix to'g'ri qoladi. */
    @Column(name = "actor_role", length = 32)
    private String actorRole;

    /** ADMIN_CREATED, CONTENT_PUBLISHED, PREMIUM_GRANTED ... */
    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    /**
     * ⚠️ {@code @Lob} ATAYLAB ishlatilmaydi: PostgreSQL'da u {@code oid} —
     * large object yaratadi, bu alohida oqim bilan ishlashni talab qiladi va
     * oddiy JSON matni uchun ortiqcha. {@code text} to'g'ri tur.
     */
    @Column(name = "before_state", columnDefinition = "text")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "text")
    private String afterState;

    @Column(length = 64)
    private String ip;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
