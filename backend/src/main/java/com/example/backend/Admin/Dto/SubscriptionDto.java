package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Subscription;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.SubscriptionSource;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Obuna qatori (ТЗ §71, §107).
 *
 * ⚠️ Faqat ro'yxat uchun kerakli maydonlar. Entity qaytarilsa
 * foydalanuvchining paroli, roli va boshqa bog'lanishlari ham birga
 * ketardi (§65).
 */
@Data
@Builder
public class SubscriptionDto {

    private Long id;
    private UUID userId;
    private String userName;
    private String userPhone;
    private Long tariffId;
    private String tariffName;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private SubscriptionSource source;

    /**
     * To'langan summa.
     *
     * ⚠️ {@code ADMIN_GIFT} obunalarida {@code null} — nol emas.
     * Nol «bepul sotildi» degani, {@code null} esa «sotilmagan».
     * Bu farq hisobotda muhim (§45).
     */
    private BigDecimal paidAmount;

    private LocalDateTime revokedAt;

    /** Hozir amal qiladimi — panelda rangli belgi uchun. */
    private boolean active;

    public static SubscriptionDto from(Subscription s) {
        LocalDateTime now = LocalDateTime.now();
        return SubscriptionDto.builder()
                .id(s.getId())
                .userId(s.getUser() == null ? null : s.getUser().getId())
                .userName(s.getUser() == null ? null : s.getUser().getName())
                .userPhone(s.getUser() == null ? null : s.getUser().getPhone())
                .tariffId(s.getTariff() == null ? null : s.getTariff().getId())
                .tariffName(tariffName(s))
                .startAt(s.getStartAt())
                .endAt(s.getEndAt())
                .source(s.getSource())
                .paidAmount(s.getPaidAmount())
                .revokedAt(s.getRevokedAt())
                .active(s.getRevokedAt() == null
                        && s.getEndAt() != null && s.getEndAt().isAfter(now))
                .build();
    }

    /** Tarif nomi — tarjimalardan birinchi to'ldirilgani. */
    private static String tariffName(Subscription s) {
        if (s.getTariff() == null || s.getTariff().getTranslations() == null) {
            return null;
        }
        return s.getTariff().getTranslations().stream()
                .sorted((a, b) -> a.getLocale() == Locale.UZ ? -1 : 1)
                .map(t -> t.getName())
                .filter(n -> n != null && !n.isBlank())
                .findFirst().orElse(null);
    }
}
