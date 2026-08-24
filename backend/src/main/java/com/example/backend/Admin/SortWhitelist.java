package com.example.backend.Admin;

import com.example.backend.exceptions.BusinessException;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Klient so'ragan saralashni XAVFSIZ tarzda qabul qiladi (ТЗ §95).
 *
 * <h2>Nega oq ro'yxat</h2>
 * Klientga istalgan maydon bo'yicha saralashga ruxsat berish uch xil
 * muammoni ochadi:
 *
 * <ul>
 *   <li><b>Sekinlik.</b> Indekssiz ustun bo'yicha saralash butun
 *       jadvalni skanerlaydi. Yuz ming qatorli kontent jadvalida bu
 *       har bir sahifa uchun bir necha soniya (§66).</li>
 *   <li><b>Xato.</b> Mavjud bo'lmagan maydon nomi Hibernate'da
 *       istisnoga olib keladi va klient 500 oladi — hech kim
 *       kutmagan javob.</li>
 *   <li><b>Ichki tuzilish.</b> Sinov yo'li bilan qaysi maydonlar
 *       borligini aniqlash mumkin bo'lardi.</li>
 * </ul>
 *
 * Shuning uchun har bir endpoint O'ZI ruxsat berilgan ustunlarni
 * sanaydi. Ro'yxatda yo'q nom — 422, 500 emas: bu klient xatosi va
 * xabar aniq bo'lishi kerak.
 *
 * <h2>Foydalanish</h2>
 * <pre>
 * private static final SortWhitelist SORT = SortWhitelist.of("createdAt")
 *         .add("title", "translations.title")
 *         .add("views", "viewCount");
 *
 * Sort sort = SORT.resolve(sortField, direction);
 * </pre>
 */
public final class SortWhitelist {

    private final Map<String, String> allowed = new LinkedHashMap<>();
    private final String defaultKey;

    private SortWhitelist(String defaultKey) {
        this.defaultKey = defaultKey;
    }

    /**
     * @param defaultKey ustun kaliti — klient hech narsa so'ramaganda
     *                   ishlatiladi va u ham ro'yxatga qo'shiladi
     */
    public static SortWhitelist of(String defaultKey) {
        SortWhitelist w = new SortWhitelist(defaultKey);
        w.allowed.put(defaultKey, defaultKey);
        return w;
    }

    /** Klient ko'radigan nom → entity maydoni. */
    public SortWhitelist add(String key, String entityField) {
        allowed.put(key, entityField);
        return this;
    }

    public SortWhitelist add(String key) {
        return add(key, key);
    }

    /**
     * @param key klient so'ragan ustun; bo'sh bo'lsa standarti
     * @param dir {@code asc} yoki {@code desc}; boshqasi — kamayish
     *            tartibi, chunki ro'yxatlar odatda yangidan eskiga
     */
    public Sort resolve(String key, String dir) {
        String requested = key == null || key.isBlank() ? defaultKey : key.trim();
        String field = allowed.get(requested);

        if (field == null) {
            throw BusinessException.validation(
                    "Bu ustun bo'yicha saralab bo'lmaydi: " + requested
                            + ". Mumkin: " + String.join(", ", allowed.keySet()));
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(dir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, field);
    }

    /** Panel qaysi ustunlarni ko'rsatishini bilishi uchun. */
    public java.util.Set<String> keys() {
        return allowed.keySet();
    }
}
