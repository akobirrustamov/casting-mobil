package com.example.backend.Cms.Enums;

/**
 * Paket qanday to'lanadi (ТЗ §44).
 *
 * ТЗ ikki yo'lni sanaydi. Ular bir-biridan tubdan farq qiladi:
 * birinchisi bizning ichimizda, ikkinchisi tashqi provayderga bog'liq.
 */
public enum FundingSource {

    /**
     * Foydalanuvchining ichki hisobi (so'mda).
     *
     * Bu yo'l BUGUN ishlaydi: pul allaqachon hisobda, tashqi provayder
     * kerak emas.
     */
    INTERNAL_BALANCE,

    /**
     * Tashqi to'lov tizimi.
     *
     * ⚠️ Provayder ulanmagan — bu yo'l 503 qaytaradi. Soxta
     * «to'landi» javobi berilmaydi.
     */
    PAYMENT_SYSTEM
}
