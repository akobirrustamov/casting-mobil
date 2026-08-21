package com.example.backend.Cms.Enums;

/**
 * Klient yuboradigan hodisa turlari (ТЗ §46, §74).
 */
public enum AnalyticsEventType {

    /** Reklama ko'rsatildi. */
    AD_IMPRESSION,

    /** Reklama bosildi. */
    AD_CLICK,

    /** Kontent kartochkasi ochildi. */
    CONTENT_VIEW,

    /** Video ijro etila boshladi. */
    CONTENT_PLAY,

    /** Video oxirigacha ko'rildi. */
    CONTENT_COMPLETE,

    /** Bildirishnoma ochildi. */
    NOTIFICATION_OPEN,

    /**
     * Bildirishnoma ichidagi havola bosildi (ТЗ §33).
     *
     * OCHISH dan farq qiladi: odam xabarni ochib, havolani bosmasligi
     * mumkin. Ikkalasi bitta hodisa deb sanalsa, «clicked» ko'rsatkichi
     * «opened» ning nusxasi bo'lib qolardi.
     */
    NOTIFICATION_CLICK;

    /** Reklama bilan bog'liqmi — agregatda qaysi jadvalga tushishini hal qiladi. */
    public boolean isAdEvent() {
        return this == AD_IMPRESSION || this == AD_CLICK;
    }

    public boolean isContentEvent() {
        return this == CONTENT_VIEW || this == CONTENT_PLAY || this == CONTENT_COMPLETE;
    }
}
