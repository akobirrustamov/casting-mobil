package com.example.backend.Cms.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Sarlavhadan URL uchun yaroqli slug yasaydi.
 *
 * Kirill ham qo'llab-quvvatlanadi: ruscha sarlavhadan slug yasalsa,
 * u lotin harflariga o'giriladi (Normalizer kirillni tashlab yuboradi).
 */
public final class SlugGenerator {

    private SlugGenerator() {
    }

    private static final Map<Character, String> CYRILLIC = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"),
            Map.entry('г', "g"), Map.entry('д', "d"), Map.entry('е', "e"),
            Map.entry('ё', "yo"), Map.entry('ж', "j"), Map.entry('з', "z"),
            Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
            Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"),
            Map.entry('о', "o"), Map.entry('п', "p"), Map.entry('р', "r"),
            Map.entry('с', "s"), Map.entry('т', "t"), Map.entry('у', "u"),
            Map.entry('ф', "f"), Map.entry('х', "h"), Map.entry('ц', "ts"),
            Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "sch"),
            Map.entry('ъ', ""), Map.entry('ы', "y"), Map.entry('ь', ""),
            Map.entry('э', "e"), Map.entry('ю', "yu"), Map.entry('я', "ya"));

    /** O'zbek lotinidagi apostroflar ham tozalanadi: o'/g' -> o/g. */
    public static String slugify(String source) {
        if (source == null || source.isBlank()) {
            return "";
        }
        String lower = source.toLowerCase(Locale.ROOT);

        StringBuilder transliterated = new StringBuilder(lower.length());
        for (char c : lower.toCharArray()) {
            String mapped = CYRILLIC.get(c);
            transliterated.append(mapped != null ? mapped : c);
        }

        String normalized = Normalizer.normalize(transliterated.toString(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                // O'zbek lotinidagi apostrof (o', g') va turli tirnoq belgilari
                // TASHLAB YUBORILADI, chiziqchaga aylantirilmaydi:
                // "Qo'shiq" -> "qoshiq", "qo-shiq" emas.
                .replaceAll("['\u2018\u2019\u02BC\u02BB`\u00B4]", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    /**
     * Band bo'lmagan slug qaytaradi: band bo'lsa oxiriga -2, -3 ... qo'shadi.
     *
     * @param isTaken slug bandligini tekshiruvchi funksiya
     */
    public static String unique(String source, String fallback, Predicate<String> isTaken) {
        String base = slugify(source);
        if (base.isEmpty()) {
            base = slugify(fallback);
        }
        if (base.isEmpty()) {
            base = "item";
        }
        if (!isTaken.test(base)) {
            return base;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = base + "-" + i;
            if (!isTaken.test(candidate)) {
                return candidate;
            }
        }
        // 1000 ta urinishdan keyin ham band bo'lsa - bu deyarli imkonsiz,
        // lekin cheksiz sikldan ko'ra aniq xato yaxshiroq.
        throw new IllegalStateException("Slug uchun bo'sh o'rin topilmadi: " + base);
    }
}
