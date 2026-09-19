package com.example.backend.Cms.Dto;

import com.example.backend.Entity.Attachment;
import com.example.backend.Entity.CastingUser;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * «Mening arizam» — ilovada ko'rinadigan qisqa shakl.
 *
 * ⚠️ Telefon, email, o'lchovlar bu yerda YO'Q: odam ularni o'zi yozgan va
 * ro'yxatda ko'rsatishga hojat yo'q. Javob qanchalik tor bo'lsa, u
 * tasodifan boshqa joyda qayta ishlatilganda shunchalik kam sizadi.
 */
@Data
@Builder
public class MyCastingApplicationDto {

    public enum Status {
        /** {@code status = 0} yoki {@code null} — admin hali ko'rmagan. */
        PENDING,
        /** {@code status = 1}. */
        APPROVED,
        /** {@code status = 2}. */
        REJECTED
    }

    private Integer id;
    private String castingType;
    private String name;
    private Status status;

    /**
     * Narx so'mda. Admin qo'ymaguncha {@code null}.
     *
     * Ustunning o'zidan ({@code casting_user.price}) — eski admin sayti uni
     * {@code /casting-user/price/**} orqali yozadi.
     */
    private Double price;

    /** To'langanmi: eski admin {@code /casting-user/payed/{id}} bilan belgilaydi. */
    private boolean paid;

    /** Saytdagi ochiq katalogda ko'rinadimi. */
    private Boolean isWebShow;

    private LocalDateTime createdAt;

    /** Rasm id'lari — {@code GET /api/v1/file/getFile/{id}} orqali ochiladi. */
    private List<UUID> photos;

    public static MyCastingApplicationDto from(CastingUser c) {
        return MyCastingApplicationDto.builder()
                .id(c.getId())
                .castingType(c.getCastingType())
                .name(c.getName())
                .status(statusOf(c.getStatus()))
                .price(c.getPrice())
                .paid(Integer.valueOf(1).equals(c.getSecondChan()))
                .isWebShow(Boolean.TRUE.equals(c.getIsWebShow()))
                .createdAt(c.getCreatedAt())
                .photos(c.getPhotos() == null ? List.of() : c.getPhotos().stream()
                        .filter(Objects::nonNull)
                        .map(Attachment::getId)
                        .toList())
                .build();
    }

    /**
     * Eski raqamli holat → aniq nom.
     *
     * ⚠️ Noma'lum qiymat PENDING deb ko'rsatiladi: eski admin sayti faqat
     * 0/1/2 yozadi, boshqa raqam paydo bo'lsa odamga «qabul qilindi» yoki
     * «rad etildi» deb yolg'on aytgandan ko'ra «kutilmoqda» xavfsizroq.
     */
    static Status statusOf(Integer raw) {
        if (raw == null) {
            return Status.PENDING;
        }
        return switch (raw) {
            case 1 -> Status.APPROVED;
            case 2 -> Status.REJECTED;
            default -> Status.PENDING;
        };
    }
}
