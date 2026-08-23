package com.example.backend.support;

import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Enums.Locale;

import java.util.Map;

/**
 * Testlar uchun tarjima to'plamlari.
 *
 * <h2>Nega kerak</h2>
 * Buyurtmachi talabi: foydalanuvchiga ko'rinadigan kontent uchala tilda
 * bo'lishi shart ({@code TranslationRules}). Testlarda esa
 * {@code Map.of(Locale.UZ, ...)} yozish oson va u NASHR QILINGAN kontent
 * yaratganda endi rad etiladi.
 *
 * Bu yordamchi ikkisini aniq ajratadi: {@link #all} — haqiqiy holat,
 * {@link #uzOnly} — aynan «tarjima yetishmayapti» holatini sinaydigan
 * testlar uchun.
 */
public final class Translations {

    private Translations() {
    }

    /** Uchala til — nashr qilinadigan kontent uchun. */
    public static Map<Locale, TranslationDto> all(String title) {
        return Map.of(
                Locale.UZ, TranslationDto.ofTitle(title),
                Locale.RU, TranslationDto.ofTitle(title + " (RU)"),
                Locale.EN, TranslationDto.ofTitle(title + " (EN)"));
    }

    /**
     * Faqat o'zbekcha — qoralama uchun yetarli, nashr uchun EMAS.
     *
     * Ataylab shu nom bilan: chaqiruv joyida to'liq emasligi ko'rinib tursin.
     */
    public static Map<Locale, TranslationDto> uzOnly(String title) {
        return Map.of(Locale.UZ, TranslationDto.ofTitle(title));
    }
}
