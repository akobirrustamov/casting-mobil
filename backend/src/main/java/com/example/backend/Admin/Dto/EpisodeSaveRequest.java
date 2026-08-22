package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class EpisodeSaveRequest {

    /** Faslsiz mini-serialda null bo'ladi. */
    private Long seasonId;

    @NotNull(message = "Qism raqami kiritilmagan")
    @Min(value = 1, message = "Qism raqami 1 dan kichik bo'lishi mumkin emas")
    private Integer episodeNumber;

    private Long thumbnailMediaId;
    private Integer durationSeconds;
    private LocalDateTime premiereDate;
    private LocalDateTime publicationDate;
    private PublicationStatus status = PublicationStatus.DRAFT;

    /** null - kontent siyosati meros olinadi. */
    private AccessPolicy accessPolicyOverride;

    private BigDecimal price;
    private Integer sortOrder;

    /**
     * ⚠️ {@code @NotEmpty}, {@code @NotNull} EMAS.
     *
     * Maydonda standart qiymat bor ({@code new LinkedHashMap<>()}),
     * shuning uchun u HECH QACHON null bo'lmaydi va {@code @NotNull}
     * hech qachon ishlamasdi. Annotatsiya himoya qilayotgandek
     * ko'rinardi, aslida o'lik edi: bo'sh tana bemalol o'tib,
     * xato servisda — maydon nomisiz — chiqardi.
     */
    @NotEmpty(message = "Nomlar kiritilmagan")
    private Map<Locale, TranslationDto> translations = new LinkedHashMap<>();

    /** To'liq almashtiriladi: yuborilmagan video qismlar o'chiriladi. */
    private List<VideoLink> videos = new ArrayList<>();

    private Long version;

    @Data
    public static class VideoLink {
        @NotNull
        private Long mediaId;
        /** Dublyaj tili. null = barcha tillar uchun. */
        private Locale locale;
        private Integer partNumber = 1;
        private Integer sortOrder = 0;
    }
}
