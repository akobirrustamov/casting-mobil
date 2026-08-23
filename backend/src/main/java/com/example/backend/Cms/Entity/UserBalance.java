package com.example.backend.Cms.Entity;

import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Foydalanuvchi balansi: pul, Stars va Coin.
 *
 * Buyurtmachi: «Ikkala donat balansi User Profilida aks etirilishi kerak»
 * va donatni balansdan yechish mumkin bo'lishi kerak.
 *
 * Pul {@code BigDecimal} da — hech qachon {@code double} emas (§36).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_user_balance", indexes =
        @Index(name = "idx_balance_user", columnList = "user_id", unique = true))
public class UserBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    /** Hisobdagi pul (so'm). */
    @Column(name = "money_balance", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal moneyBalance = BigDecimal.ZERO;

    @Column(name = "stars_balance", nullable = false)
    @Builder.Default
    private Long starsBalance = 0L;

    @Column(name = "coin_balance", nullable = false)
    @Builder.Default
    private Long coinBalance = 0L;

    /** Bir vaqtda ikki tranzaksiya balansni buzmasligi uchun. */
    @Version
    private Long version;
}
