package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.SubscriptionSource;
import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Premium obuna yozuvi.
 *
 * O'zgarmas tarix: obuna tugasa ham yozuv o'chirilmaydi (§42, §58) —
 * hisobot va nizoli holatlar uchun kerak.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_subscription", indexes = {
        @Index(name = "idx_subscription_user", columnList = "user_id,end_at"),
        @Index(name = "idx_subscription_source", columnList = "source")
})
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_id")
    private Tariff tariff;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private SubscriptionSource source = SubscriptionSource.PURCHASE;

    /**
     * To'langan summa. ADMIN_GIFT uchun null — bu daromad EMAS va hisobotda
     * shunday hisoblanishi kerak.
     */
    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount;

    /** Admin sovg'a qilgan bo'lsa — kim. */
    @Column(name = "granted_by")
    private UUID grantedBy;

    /** Muddatidan oldin tortib olingan bo'lsa. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isActiveAt(LocalDateTime moment) {
        return revokedAt == null && !moment.isBefore(startAt) && moment.isBefore(endAt);
    }
}
