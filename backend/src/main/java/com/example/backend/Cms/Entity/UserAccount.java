package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mobil foydalanuvchining platformaga oid holati.
 *
 * <b>Nega alohida jadval, `users` ga ustun qo'shish emas:</b> `users` jadvalini
 * sayt, Telegram bot va mobil ilova birgalikda ishlatadi. Unga ustun qo'shish
 * uchtala klientni ham xavf ostiga qo'yadi. Bu yerda esa faqat yangi
 * funksiyalar uchun kerak bo'lgan holat saqlanadi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_user_account", indexes = {
        @Index(name = "idx_account_user", columnList = "user_id", unique = true),
        @Index(name = "idx_account_status", columnList = "status"),
        @Index(name = "idx_account_premium", columnList = "premium_until")
})
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    /**
     * Premium qachongacha amal qiladi. null — obuna yo'q.
     *
     * Denormalizatsiya: har safar Subscription jadvalidan hisoblash o'rniga
     * shu yerda saqlanadi, chunki bu qiymat HAR bir kirish tekshiruvida kerak.
     */
    @Column(name = "premium_until")
    private LocalDateTime premiumUntil;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Hozir faol Premium bormi. */
    public boolean hasActivePremium() {
        return premiumUntil != null && premiumUntil.isAfter(LocalDateTime.now());
    }

    public UUID userId() {
        return user == null ? null : user.getId();
    }
}
