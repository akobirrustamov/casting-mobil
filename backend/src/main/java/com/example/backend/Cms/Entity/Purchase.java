package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.PurchaseType;
import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bir martalik xarid: bitta qism yoki butun premyera.
 *
 * <b>O'zgarmas moliyaviy yozuv</b> — hech qachon o'chirilmaydi (§58, §42).
 * Foydalanuvchi nimaga haq to'laganini isbotlaydigan yagona manba.
 *
 * {@code targetId} turga qarab: EPISODE uchun qism id'si, PREMIERE uchun
 * kontent id'si.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_purchase", indexes = {
        @Index(name = "idx_purchase_lookup", columnList = "user_id,type,target_id"),
        @Index(name = "idx_purchase_user", columnList = "user_id,created_at"),
        @Index(name = "idx_purchase_target", columnList = "type,target_id")
})
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PurchaseType type;

    /** EPISODE → qism id, PREMIERE → kontent id. */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 8)
    @Builder.Default
    private String currency = "UZS";

    /** To'lov tizimidagi tranzaksiya id'si. Hozircha null — provayder ulanmagan. */
    @Column(name = "payment_reference", length = 128)
    private String paymentReference;

    /**
     * Qaytarilgan bo'lsa. ТЗ refund'ni eslatadi, lekin qoidalarni yozmagan
     * (roadmap.md §8, 4-savol) — maydon bor, mantiq yo'q.
     */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Xarid hozir amal qiladimi — qaytarilgan bo'lsa yo'q. */
    public boolean isValid() {
        return refundedAt == null;
    }
}
