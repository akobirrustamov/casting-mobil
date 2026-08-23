package com.example.backend.Cms.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Premium tarifi.
 *
 * ⚠️ Narxlar KODDA QOTIRILMAYDI (§36) — admin panel orqali o'zgartiriladi.
 * Boshlang'ich qiymatlar (24 000 / 49 999 / 99 000 / 159 900) migratsiyada
 * seed sifatida qo'yiladi, keyin admin ixtiyorida.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_tariff", indexes =
        @Index(name = "idx_tariff_active", columnList = "active,sort_order"))
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Barqaror identifikator: m1, m3, m6, y1. */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 8)
    @Builder.Default
    private String currency = "UZS";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** «ENG FOYDALI TARIF» belgisi. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean highlighted = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "tariff", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TariffTranslation> translations = new ArrayList<>();

    public void addTranslation(TariffTranslation t) {
        t.setTariff(this);
        this.translations.add(t);
    }

    /** Oyiga qancha tushishi — «oyiga atigi 13 325 so'm» yozuvi uchun. */
    public BigDecimal monthlyPrice() {
        if (durationMonths == null || durationMonths <= 0 || price == null) {
            return null;
        }
        return price.divide(BigDecimal.valueOf(durationMonths), 0, java.math.RoundingMode.HALF_UP);
    }
}
