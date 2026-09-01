package com.example.backend.Cms.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Katalog kategoriyasi — mobil ilovaning bosh sahifasidagi qator
 * («Drama», ostida kartochkalar).
 *
 * <h2>Nima uchun bosh sahifa feedi yetmadi</h2>
 * {@code GET /api/v1/app/home} dagi {@code CATEGORIES} bo'limi faqat
 * NOMLARNI beradi — plitka chiziladi, lekin ichida nima borligi noma'lum.
 * Kontent qatorlari esa ({@code PODCASTS}, {@code SHOWS}, {@code MINI_SERIES})
 * kontent TURI bo'yicha yig'iladi, kategoriya bo'yicha emas (§13: tur va
 * kategoriya — har xil o'q). Ya'ni «Drama» qatorini feeddan yig'ib
 * bo'lmasdi: kartochkada kategoriya maydoni ham yo'q.
 *
 * <h2>Ikki shakl, bitta DTO</h2>
 * Ro'yxat endpointi ({@code /catalog/categories}) {@link #items} ni BO'SH
 * qaytaradi — u faqat nomlar va sonlar uchun. To'liq qator esa
 * ({@code /catalog/categories/{id}}) shu maydonni to'ldiradi.
 *
 * {@link #total} ayni {@link #items} qoidasi bo'yicha sanaladi — ya'ni
 * «Drama (7)» yozuvi ochilganda 7 ta kontent chiqishiga kafolat beradi.
 */
@Data
@Builder
public class CatalogCategoryDto {

    private Long id;

    /** Tarjima qilinmaydigan barqaror identifikator. */
    private String slug;

    /** So'ralgan tildagi nom. Tarjima yo'q bo'lsa — o'zbekchasi. */
    private String name;

    private Long iconMediaId;

    /**
     * Kategoriyadagi ko'rinadigan kontent soni — sahifadan QAT'I NAZAR.
     * Klient shu son bo'yicha «Barchasi ›» ni ko'rsatadi.
     */
    private Integer total;

    /** Nechanchi sahifa qaytdi (0 dan). Ro'yxat endpointida — {@code null}. */
    private Integer page;

    /** Sahifa hajmi. Ro'yxat endpointida — {@code null}. */
    private Integer size;

    /**
     * Yana sahifa bormi.
     *
     * Klient buni {@code total} dan o'zi ham hisoblay olardi, lekin unda
     * «oxirimi?» degan qaror ikki joyda yashardi. Serverning o'zi aytgani
     * aniqroq: ro'yxat oxiriga yetganda ortiqcha so'rov yuborilmaydi.
     */
    private Boolean hasMore;

    /**
     * SHU SAHIFADAGI kartochkalar. Ro'yxat endpointida bo'sh.
     *
     * Bosh sahifadagi kartochka bilan BIR XIL shakl
     * ({@link HomeFeedDto.ContentCard}) — klientda alohida komponent
     * yozilmasin, «Drama» qatori «Podkastlar» qatoriday ko'rinsin.
     */
    @Builder.Default
    private List<HomeFeedDto.ContentCard> items = List.of();
}
