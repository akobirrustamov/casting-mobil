package com.example.backend.Admin.Dto;

import lombok.Builder;
import lombok.Data;

/**
 * Tarifning bitta tildagi matnlari (ТЗ §36).
 *
 * <h2>Nima uchun alohida DTO</h2>
 * Ilgari bu yerda umumiy {@code TranslationDto} ishlatilardi. Unda uchta
 * maydon bor: {@code title}, {@code shortDescription}, {@code description}.
 * Tarifga esa TO'RTTASI kerak — nom, bejak, imkoniyatlar va tavsif.
 *
 * Natijada ТЗ dagi {@code description} va {@code features} bitta katakka
 * qo'shib yuborilgan edi: DTO'ning {@code description} maydoni
 * {@code features} ustuniga yozilardi.
 */
@Data
@Builder
public class TariffTextDto {

    /** Tarif nomi: «1 oy», «Yillik». */
    private String name;

    /** Bejak: «ENG FOYDALI TARIF». Ixtiyoriy. */
    private String badge;

    /** Nasriy izoh: «Oilaviy tomosha uchun eng ommabop tarif». */
    private String description;

    /** Nima kirishi — har bir qator alohida, klient ro'yxat qilib chizadi. */
    private String features;
}
