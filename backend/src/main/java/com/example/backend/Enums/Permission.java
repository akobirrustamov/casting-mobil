package com.example.backend.Enums;

/**
 * WORKER uchun fine-grained ruxsatlar.
 *
 * ADMIN, SUPER_ADMIN va HYPER_ADMIN bu ro'yxatga bog'liq emas — ular rol
 * darajasida to'liq huquqqa ega. Permission faqat WORKER uchun ma'noga ega:
 * Admin/SuperAdmin Worker yaratganda unga aynan qaysi amallar ruxsat etilishini
 * tanlaydi.
 *
 * Yangi qiymat qo'shilganda DB'ga migration shart emas — STRING sifatida saqlanadi.
 */
public enum Permission {

    CONTENT_VIEW,
    CONTENT_CREATE,
    CONTENT_EDIT,
    CONTENT_DELETE,
    CONTENT_PUBLISH,

    CATEGORY_VIEW,
    CATEGORY_CREATE,
    CATEGORY_EDIT,
    CATEGORY_DELETE,

    GENRE_VIEW,
    GENRE_CREATE,
    GENRE_EDIT,
    GENRE_DELETE,

    CREATOR_VIEW,
    CREATOR_CREATE,
    CREATOR_EDIT,

    MEDIA_VIEW,
    MEDIA_UPLOAD,
    MEDIA_DELETE,

    ADVERTISEMENT_VIEW,
    ADVERTISEMENT_CREATE,
    ADVERTISEMENT_EDIT,
    ADVERTISEMENT_DELETE,

    PREMIERE_VIEW,
    PREMIERE_CREATE,
    PREMIERE_EDIT,
    PREMIERE_DELETE,

    HOMEPAGE_VIEW,
    HOMEPAGE_EDIT,

    NOTIFICATION_VIEW,
    NOTIFICATION_CREATE,
    NOTIFICATION_SEND,

    COMMENT_VIEW,
    COMMENT_MODERATE,

    USER_VIEW,
    USER_BLOCK,

    /** Premium sovg'a qilish va tortib olish (ТЗ §38). */
    USER_PREMIUM_MANAGE,

    /** Qurilmalarni ko'rish va chiqarib yuborish. */
    USER_DEVICE_MANAGE,

    TARIFF_VIEW,
    TARIFF_EDIT,

    /**
     * Obunalar ro'yxati (ТЗ §71, §107).
     *
     * ⚠️ {@code TARIFF_VIEW} dan alohida: tarif narxi ommaviy ma'lumot,
     * obuna ro'yxati esa KIM qancha to'laganini ko'rsatadi. Ikkalasini
     * bitta ruxsatga qo'shish tarif narxini ko'rishi kerak bo'lgan
     * xodimga foydalanuvchilarning to'lov tarixini ham ochib berardi.
     */
    SUBSCRIPTION_VIEW,

    DONATION_VIEW,
    DONATION_PACKAGE_EDIT,

    SETTINGS_VIEW,
    SETTINGS_EDIT,

    REPORT_VIEW
}
