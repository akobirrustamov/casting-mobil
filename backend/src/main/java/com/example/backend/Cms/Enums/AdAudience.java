package com.example.backend.Cms.Enums;

/**
 * Banner kimga ko'rinadi.
 *
 * Buyurtmachi talabi: reklama FAQAT faol tarifi yo'q foydalanuvchilarga
 * ko'rinadi (Premium — «reklamasiz tomosha»), admin xabarlari esa hammaga.
 */
public enum AdAudience {

    /** Tijorat reklamasi — faqat faol obunasi YO'Q foydalanuvchilarga. */
    ADVERTISEMENT,

    /** Admin e'loni — barcha foydalanuvchilarga, obunadan qat'i nazar. */
    ADMIN_ANNOUNCEMENT
}
