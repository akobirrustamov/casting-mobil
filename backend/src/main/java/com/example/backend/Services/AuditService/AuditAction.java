package com.example.backend.Services.AuditService;

/**
 * Audit action nomlari (§59).
 *
 * <b>Nega konstanta.</b> Action nomi — jurnalni filtrlaydigan yagona
 * kalit. Chaqiruv joyida xom satr yozilsa, bitta harf xatosi
 * ({@code "CONTNET_UPDATED"}) hech qanday xatolik bermaydi: yozuv
 * saqlanadi, lekin filtr uni hech qachon topmaydi. Hodisa yo'qolgandek
 * ko'rinadi. {@code AuditActionsAreDeclaredTest} shu sababli barcha
 * chaqiruvlarni shu ro'yxatga solishtiradi.
 */
public final class AuditAction {

    private AuditAction() {
    }

    /**
     * ТЗ ADMIN_CREATED va WORKER_CREATED ni alohida ajratadi: «kimga
     * admin huquqi berildi?» — auditning eng muhim savollaridan biri va
     * u filtr bilan javob berishi kerak. Rol faqat {@code afterState}
     * ichida bo'lsa, uni JSON matni bo'ylab qidirishga to'g'ri kelardi.
     */
    public static final String ADMIN_CREATED = "ADMIN_CREATED";
    public static final String WORKER_CREATED = "WORKER_CREATED";
    public static final String STAFF_CREATED = "STAFF_CREATED";
    public static final String STAFF_UPDATED = "STAFF_UPDATED";
    public static final String STAFF_DEACTIVATED = "STAFF_DEACTIVATED";
    public static final String STAFF_PASSWORD_RESET = "STAFF_PASSWORD_RESET";

    public static final String ROLE_CHANGED = "ROLE_CHANGED";
    public static final String PERMISSION_CHANGED = "PERMISSION_CHANGED";

    public static final String CONTENT_CREATED = "CONTENT_CREATED";
    public static final String CONTENT_UPDATED = "CONTENT_UPDATED";
    public static final String CONTENT_PUBLISHED = "CONTENT_PUBLISHED";
    public static final String CONTENT_ARCHIVED = "CONTENT_ARCHIVED";

    public static final String ADVERTISEMENT_CREATED = "ADVERTISEMENT_CREATED";
    public static final String ADVERTISEMENT_UPDATED = "ADVERTISEMENT_UPDATED";

    public static final String PREMIUM_GRANTED = "PREMIUM_GRANTED";
    public static final String PREMIUM_REVOKED = "PREMIUM_REVOKED";

    public static final String TARIFF_CHANGED = "TARIFF_CHANGED";
    public static final String COMMENT_HIDDEN = "COMMENT_HIDDEN";
    public static final String NOTIFICATION_SENT = "NOTIFICATION_SENT";

    public static final String USER_BLOCKED = "USER_BLOCKED";
    public static final String USER_UNBLOCKED = "USER_UNBLOCKED";
    public static final String DEVICE_REVOKED = "DEVICE_REVOKED";

    public static final String ADMIN_LOGIN = "ADMIN_LOGIN";

    public static final String CATEGORY_CREATED = "CATEGORY_CREATED";
    public static final String CATEGORY_UPDATED = "CATEGORY_UPDATED";
    public static final String GENRE_CREATED = "GENRE_CREATED";
    public static final String GENRE_UPDATED = "GENRE_UPDATED";

    /**
     * Kategoriya va janrni o'chirish (ТЗ §16, §17).
     *
     * ⚠️ Bu HAQIQIY o'chirish, arxivlash emas — kategoriya va janrda
     * `deleted_at` yo'q va ular kontentga bog'liqligi tekshirilgandan
     * keyingina o'chiriladi. Shuning uchun jurnalda alohida amal
     * sifatida turadi: «nega bu kategoriya yo'qoldi?» degan savolga
     * javob faqat shu yerdan topiladi.
     */
    public static final String CATEGORY_DELETED = "CATEGORY_DELETED";
    public static final String GENRE_DELETED = "GENRE_DELETED";
    public static final String CREATOR_CREATED = "CREATOR_CREATED";
    public static final String CREATOR_UPDATED = "CREATOR_UPDATED";

    public static final String SEASON_CREATED = "SEASON_CREATED";
    public static final String SEASON_UPDATED = "SEASON_UPDATED";
    public static final String SEASON_DELETED = "SEASON_DELETED";
    public static final String EPISODE_CREATED = "EPISODE_CREATED";
    public static final String EPISODE_UPDATED = "EPISODE_UPDATED";
    public static final String EPISODE_DELETED = "EPISODE_DELETED";

    public static final String ADVERTISEMENT_ARCHIVED = "ADVERTISEMENT_ARCHIVED";
    public static final String PREMIERE_CREATED = "PREMIERE_CREATED";
    public static final String PREMIERE_UPDATED = "PREMIERE_UPDATED";
    public static final String PREMIERE_ARCHIVED = "PREMIERE_ARCHIVED";

    public static final String HOMEPAGE_SECTION_UPDATED = "HOMEPAGE_SECTION_UPDATED";
    public static final String HOMEPAGE_SECTIONS_REORDERED = "HOMEPAGE_SECTIONS_REORDERED";
    public static final String HOMEPAGE_SECTION_ITEMS_UPDATED = "HOMEPAGE_SECTION_ITEMS_UPDATED";

    public static final String NOTIFICATION_CREATED = "NOTIFICATION_CREATED";
    public static final String NOTIFICATION_UPDATED = "NOTIFICATION_UPDATED";
    public static final String NOTIFICATION_CANCELLED = "NOTIFICATION_CANCELLED";

    public static final String PACKAGE_CREATED = "PACKAGE_CREATED";
    public static final String PACKAGE_UPDATED = "PACKAGE_UPDATED";
    public static final String PACKAGE_DELETED = "PACKAGE_DELETED";
    public static final String CURRENCY_PACKAGE_PURCHASED = "CURRENCY_PACKAGE_PURCHASED";

    public static final String SETTING_CHANGED = "SETTING_CHANGED";
}
