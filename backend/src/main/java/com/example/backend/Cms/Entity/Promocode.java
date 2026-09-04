package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.PromocodeGrantType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Promokod — bepul Premium kunlar.
 *
 * <h2>Nima beradi</h2>
 * Buyurtmachi qarori (04.09.2026): faqat bepul kunlar. Chegirma yoki
 * Yulduz emas — bitta tur, bitta qoida. Mavjud obuna bo'lsa, kunlar
 * uning muddati USTIGA qo'shiladi ({@code PremiumGrantService}).
 *
 * <h2>Ikki chegara</h2>
 * <ul>
 *   <li>{@code maxRedemptions} — umumiy: nechta odam ishlatishi
 *       mumkin. {@code null} — cheksiz.</li>
 *   <li>bitta odam — bir marta: {@code PromocodeRedemption} dagi
 *       {@code uk_promocode_user} cheklovi.</li>
 * </ul>
 *
 * <h2>O'chirilmaydi</h2>
 * Admin kodni {@code active = false} qiladi. Ishlatilgan kodni o'chirish
 * {@code cms_promocode_redemption} dagi tarixni yetim qoldirardi va «bu
 * odamga premium qayerdan kelgan» degan savol javobsiz qolardi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_promocode",
        uniqueConstraints = @UniqueConstraint(name = "uk_promocode_code", columnNames = "code"),
        indexes = @Index(name = "idx_promocode_active", columnList = "active,valid_until"))
public class Promocode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kodning o'zi — KATTA harflarda saqlanadi.
     *
     * Odam telefonda «yangi2026» yoki «YANGI2026» deb yozishi mumkin;
     * ikkalasi ham bitta kod. Normalizatsiya {@code PromocodeService}
     * da, bitta joyda.
     */
    @Column(nullable = false, length = 32)
    private String code;

    /**
     * Kod NIMA beradi. Admin yaratishda tanlaydi.
     *
     * ⚠️ Tahrirlashda o'zgartirilmaydi: kod allaqachon tarqatilgan
     * bo'lishi va odamlar undan boshqa narsa kutayotgan bo'lishi mumkin.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type", nullable = false, length = 24)
    @Builder.Default
    private PromocodeGrantType grantType = PromocodeGrantType.PREMIUM_DAYS;

    /**
     * Necha kun beradi.
     *
     * ⚠️ Nomi {@code premiumDays} edi va V34 da to'g'rilandi: casting
     * kodida u Premium bermaydi, ya'ni eski nom yolg'on gapirardi.
     */
    @Column(name = "grant_days", nullable = false)
    private Integer grantDays;

    /** Nechta odam ishlatishi mumkin. {@code null} — cheksiz. */
    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    /** Shu vaqtdan boshlab amal qiladi. {@code null} — darhol. */
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    /** Shu vaqtgacha. {@code null} — muddatsiz. */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Admin izohi: «Instagram aksiyasi, sentyabr». Foydalanuvchi ko'rmaydi. */
    @Column(length = 255)
    private String note;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Sana oynasi ichidami — faollikdan alohida savol. */
    public boolean isWithinWindow(LocalDateTime moment) {
        if (validFrom != null && moment.isBefore(validFrom)) {
            return false;
        }
        return validUntil == null || moment.isBefore(validUntil);
    }
}
