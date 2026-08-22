package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.Locale;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tarif saqlash (ТЗ §36).
 *
 * Matnlar {@link TariffTextDto} da: nom · bejak · tavsif · imkoniyatlar.
 * Ilgari umumiy {@code TranslationDto} ishlatilardi va unda uchta maydon
 * bo'lgani uchun ТЗ dagi {@code description} bilan {@code features} bitta
 * katakka qo'shib yuborilgan edi.
 */
@Data
public class TariffSaveRequest {

    /** Faqat yaratishda. Tahrirlashda e'tiborga olinmaydi. */
    private String code;

    @NotNull(message = "Muddat kiritilmagan")
    @Min(value = 1, message = "Muddat kamida 1 oy bo'lishi kerak")
    private Integer durationMonths;

    @NotNull(message = "Narx kiritilmagan")
    private BigDecimal price;

    private String currency = "UZS";
    private Boolean active = true;
    private Boolean highlighted = false;
    private Integer sortOrder = 0;

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
    private Map<Locale, TariffTextDto> translations = new LinkedHashMap<>();
}
