package com.example.backend.Admin.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Barcha ro'yxat endpointlari uchun yagona sahifalash formati.
 *
 * Spring'ning Page'i to'g'ridan-to'g'ri qaytarilmaydi: uning JSON tuzilishi
 * versiyaga bog'liq va ortiqcha maydonlarga to'la.
 */
@Data
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /**
     * Xotiradagi ro'yxatdan sahifa kesadi.
     *
     * <h2>Qachon ishlatiladi</h2>
     * Filtrlash allaqachon xotirada bajarilgan bo'lsa — masalan xodimlar
     * ro'yxatida, u yerda eski sxema muzlatilgan va filtr SQL'ga
     * ko'chirilmagan. Bunday holatda sahifani SQL'da kesish uchun butun
     * filtrni ham ko'chirish kerak bo'lardi, foydasi esa yo'q: ro'yxat
     * allaqachon xotirada.
     *
     * ⚠️ Faqat KICHIK ro'yxatlar uchun. Ro'yxat minglarga yetsa —
     * filtr ham, sahifalash ham SQL'ga ko'chirilishi kerak.
     */
    public static <T> PageResponse<T> ofList(List<T> all, int page, int size) {
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());

        int totalPages = all.isEmpty() ? 0
                : (int) Math.ceil((double) all.size() / safeSize);

        return new PageResponse<>(
                all.subList(from, to),
                safePage,
                safeSize,
                all.size(),
                totalPages);
    }
}
