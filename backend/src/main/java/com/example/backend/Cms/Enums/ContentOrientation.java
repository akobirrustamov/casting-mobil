package com.example.backend.Cms.Enums;

/**
 * Video kadr yo'nalishi. Buyurtmachi talabi: kontent ikki xil ko'rinishda
 * joylanadi - YouTube uslubida (yonlama) va Reels uslubida (tik).
 *
 * Bosh sahifada alohida bo'limlarga ajratish uchun kerak.
 */
public enum ContentOrientation {

    /** 16:9 - odatiy gorizontal video. */
    LANDSCAPE,

    /** 9:16 - vertikal, Reels/Shorts uslubi. */
    VERTICAL
}
