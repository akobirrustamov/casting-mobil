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
}
