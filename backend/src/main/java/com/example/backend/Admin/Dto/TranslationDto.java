package com.example.backend.Admin.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bitta til uchun matnlar to'plami.
 *
 * Admin panel uch tilni birdan tahrirlaydi, shuning uchun ro'yxat va detal
 * endpointlari tarjimalarni til kodi bo'yicha map ko'rinishida qaytaradi:
 * {@code {"UZ": {...}, "RU": {...}, "EN": {...}}}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationDto {

    private String title;
    private String shortDescription;
    private String description;

    public static TranslationDto ofTitle(String title) {
        return TranslationDto.builder().title(title).build();
    }
}
