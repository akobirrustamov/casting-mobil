package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Kontent yaratish va tahrirlash so'rovi.
 *
 * Tarjimalar til kodi bo'yicha map: {@code {"UZ": {...}, "RU": {...}, "EN": {...}}}.
 * Kamida UZ tarjimasi majburiy - u sukut bo'yicha til.
 */
@Data
public class ContentSaveRequest {

    /** Bo'sh bo'lsa UZ sarlavhasidan avtomatik yasaladi. */
    @Size(max = 200)
    private String slug;

    @NotNull(message = "Kontent turi tanlanmagan")
    private ContentType contentType;

    @NotNull(message = "Tuzilish turi tanlanmagan")
    private StructureType structureType;

    private ContentOrientation orientation = ContentOrientation.LANDSCAPE;

    private PublicationStatus status = PublicationStatus.DRAFT;

    /** Katalogda ko'rinadimi. Berilmasa PUBLIC. */
    private ContentVisibility visibility = ContentVisibility.PUBLIC;

    /**
     * Asarning asl tili — ISO 639-1 ({@code uz}, {@code ko}…).
     *
     * Tarjima tili EMAS: sarlavha va tavsif baribir uch tilda saqlanadi.
     */
    @Pattern(regexp = "^[a-z]{2}$", message = "Til kodi ISO 639-1 bo'lsin (masalan: uz, ru, ko)")
    private String language;

    private AccessPolicy accessPolicy = AccessPolicy.FREE;

    private BigDecimal premierePrice;

    private Long categoryId;

    private Set<Long> genreIds = new LinkedHashSet<>();

    @Size(max = 8)
    private String ageRating;

    private Integer durationMinutes;

    private LocalDateTime premiereDate;
    private LocalDateTime publicationDate;

    private Boolean featured = false;
    private Boolean popular = false;

    /** Uch til. UZ majburiy, qolganlari ixtiyoriy (lekin tavsiya etiladi). */
    /**
     * ⚠️ {@code @NotEmpty}, {@code @NotNull} EMAS.
     *
     * Maydonda standart qiymat bor ({@code new LinkedHashMap<>()}),
     * shuning uchun u HECH QACHON null bo'lmaydi va {@code @NotNull}
     * hech qachon ishlamasdi. Annotatsiya himoya qilayotgandek
     * ko'rinardi, aslida o'lik edi: bo'sh tana bemalol o'tib,
     * xato servisda — maydon nomisiz — chiqardi.
     */
    @NotEmpty(message = "Tarjimalar kiritilmagan")
    private Map<Locale, TranslationDto> translations = new LinkedHashMap<>();

    /** To'liq almashtiriladi: yuborilmagan media o'chiriladi. */
    private List<MediaLink> media = new ArrayList<>();

    /** To'liq almashtiriladi. */
    private List<CreditLink> credits = new ArrayList<>();

    /** Optimistic locking uchun. Tahrirlashda majburiy. */
    private Long version;

    @Data
    public static class MediaLink {
        @NotNull
        private MediaRole role;
        /** null = barcha tillar uchun umumiy. */
        private Locale locale;
        @NotNull
        private Long mediaId;
        private Integer sortOrder = 0;
    }

    @Data
    public static class CreditLink {
        @NotNull
        private Long creatorId;
        @NotNull
        private CreatorProfession profession;
        private String characterName;
        private Integer sortOrder = 0;
    }
}
