package com.example.backend.Cms.Service;

import com.example.backend.Cms.Enums.Locale;

import java.util.List;
import java.util.function.Function;

/**
 * Tarjima qatorlaridan kerakli tildagisini tanlash qoidasi.
 *
 * <h2>Nima uchun alohida sinf</h2>
 * Bu qoida {@code HomeFeedService} ichida yopiq metod bo'lib turardi va
 * u yerda to'g'ri ishlardi. Tariflar ilovaga ochilganda o'sha qoida
 * ikkinchi marta kerak bo'ldi — ya'ni uni nusxalash kerak edi.
 *
 * Ikki nusxa bir kun ajralib ketardi: masalan biri «tarjima yo'q bo'lsa
 * bo'sh qaytarsin» ga o'zgartirilar, ikkinchisi eskicha qolardi. Va bu
 * hech qanday xatoga o'xshamasdi — shunchaki bir sahifada nom bor,
 * boshqasida yo'q.
 *
 * <h2>Qoida</h2>
 * <ol>
 *   <li>so'ralgan til bor bo'lsa — o'sha;</li>
 *   <li>bo'lmasa — o'zbekchasi ({@code Locale.DEFAULT});</li>
 *   <li>u ham bo'lmasa — birinchi mavjud qator.</li>
 * </ol>
 *
 * ⚠️ Uchinchi qadam ataylab: nashr paytida uchala til majburiy
 * ({@code TranslationRules}), lekin eski yozuvlarda kamchilik bo'lishi
 * mumkin. Bo'sh nom qaytarish odamga bo'sh katak ko'rsatardi — chet
 * tildagi nom esa hech bo'lmasa nimadir aytadi.
 */
public final class TranslationPicker {

    private TranslationPicker() {
    }

    /**
     * @param rows     tarjima qatorlari; {@code null} yoki bo'sh bo'lishi mumkin
     * @param lang     so'ralgan til
     * @param localeOf qatordan tilni oladigan funksiya
     * @return mos qator yoki {@code null}
     */
    public static <T> T pick(List<T> rows, Locale lang, Function<T, Locale> localeOf) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        T fallback = null;
        for (T row : rows) {
            Locale rowLocale = localeOf.apply(row);
            if (rowLocale == lang) {
                return row;
            }
            if (rowLocale == Locale.DEFAULT) {
                fallback = row;
            }
        }
        return fallback != null ? fallback : rows.get(0);
    }

    /**
     * Bir maydonni tanlab olish — qator kerak bo'lmaganda.
     *
     * @return matn yoki {@code null}
     */
    public static <T> String pickValue(List<T> rows, Locale lang,
                                       Function<T, Locale> localeOf,
                                       Function<T, String> valueOf) {
        T row = pick(rows, lang, localeOf);
        return row == null ? null : valueOf.apply(row);
    }
}
