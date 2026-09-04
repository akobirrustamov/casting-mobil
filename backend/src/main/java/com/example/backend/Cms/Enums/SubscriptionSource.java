package com.example.backend.Cms.Enums;

/**
 * Obuna qayerdan kelgan.
 *
 * Admin sovg'a qilgan obunani xariddan ajratish shart: hisobotda daromad
 * sifatida ko'rsatilmasligi kerak.
 */
public enum SubscriptionSource {

    /** Foydalanuvchi sotib olgan. */
    PURCHASE,

    /** Admin sovg'a qilgan (§38). */
    ADMIN_GIFT,

    /**
     * Promokod orqali olingan.
     *
     * ⚠️ Daromad EMAS — {@code paidAmount} bo'sh qoladi va hisobotlar
     * ({@code SubscriptionRepo.totalPaidAmount} va h.k.) uni
     * {@code ADMIN_GIFT} kabi chetlab o'tadi: ular {@code paidAmount is
     * not null} bo'yicha filtrlaydi, manba nomi bo'yicha emas.
     */
    PROMO
}
