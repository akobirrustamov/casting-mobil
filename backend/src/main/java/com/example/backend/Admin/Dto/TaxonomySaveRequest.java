package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.Locale;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** Kategoriya va janr uchun umumiy so'rov - tuzilishi bir xil. */
@Data
public class TaxonomySaveRequest {

    /** Bo'sh bo'lsa UZ nomidan yasaladi. */
    @Size(max = 128)
    private String slug;

    private Integer sortOrder = 0;

    private Boolean active = true;

    /** Faqat kategoriya uchun. */
    private Long iconMediaId;

    @NotNull(message = "Nomlar kiritilmagan")
    private Map<Locale, TranslationDto> translations = new LinkedHashMap<>();
}
