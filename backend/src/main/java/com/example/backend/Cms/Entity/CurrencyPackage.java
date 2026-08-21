package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.CurrencyKind;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Stars yoki Coin paketi.
 *
 * ТЗ: paketlar 10 / 50 / 100 / 500 / 1 000, lekin «1 yulduz = X so'm» kursi
 * admin panel orqali boshqarilishi kerak. Shuning uchun har bir paketning
 * narxi alohida saqlanadi — chegirmali paket ham qilish mumkin.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_currency_package", indexes =
        @Index(name = "idx_package_kind", columnList = "kind,active,sort_order"))
public class CurrencyPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CurrencyKind kind;

    /** Nechta birlik beriladi. */
    @Column(nullable = false)
    private Long amount;

    /** Narxi (so'm). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
