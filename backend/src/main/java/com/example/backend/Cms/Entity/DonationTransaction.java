package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Donat tranzaksiyasi.
 *
 * ⚠️ O'ZGARMAS: hech qachon tahrirlanmaydi va o'chirilmaydi (§42).
 * Moliyaviy tarix — nizo yuzaga kelsa yagona dalil.
 *
 * Har bir donat aniq nishonga bog'lanadi (ijodkor yoki kontent), chunki
 * buyurtmachi «har bir kontent uchun alohida hisoblanadi» deb talab qilgan.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_donation", indexes = {
        @Index(name = "idx_donation_target", columnList = "target_type,target_id,created_at"),
        @Index(name = "idx_donation_sender", columnList = "sender_id,created_at"),
        @Index(name = "idx_donation_created", columnList = "created_at")
})
public class DonationTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private DonationTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CurrencyKind kind;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
