package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class SeasonSaveRequest {

    @NotNull(message = "Fasl raqami kiritilmagan")
    @Min(value = 1, message = "Fasl raqami 1 dan kichik bo'lishi mumkin emas")
    private Integer seasonNumber;

    private Long posterMediaId;
    private LocalDateTime premiereDate;
    private PublicationStatus status = PublicationStatus.DRAFT;
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
}
