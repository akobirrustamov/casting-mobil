package com.example.backend.Cms.Enums;

/**
 * «Mashhur ijodkorlar» bo'limi qanday tartiblanadi (ТЗ §25).
 *
 * <h2>Nega enum, oddiy bayroq emas</h2>
 * ТЗ: «Hozir manual featured/sort imkoniyati yetarli, ammo arxitektura
 * analytics rankingga mos bo'lsin.» Ya'ni bugun qo'lda, ertaga avtomatik.
 *
 * Tanlov {@code cms_platform_setting} da saqlanadi va uni admin
 * o'zgartiradi — kod qayta yozilmaydi, deploy kutilmaydi. Yangi strategiya
 * qo'shish uchun shu enumga qiymat va {@code HomepageService} ga bitta
 * {@code case} qo'shiladi.
 */
public enum CreatorRanking {

    /**
     * Admin tanlagan tartib: {@code featured} bayrog'i + {@code sortOrder}.
     * Hozirgi standart.
     */
    MANUAL,

    /**
     * Olingan Stars soni bo'yicha — ko'pi yuqorida.
     *
     * ⚠️ Bu tartib donat oqimi {@code Creator.starsReceived} ni
     * yangilaganda MA'NOGA EGA bo'ladi. Hozir u faqat dev ma'lumotida
     * to'ldirilgan, ya'ni prodda hamma qiymat 0 bo'lib, tartib tasodifiy
     * chiqadi. Shuning uchun tanlanganda ogohlantirish yoziladi.
     */
    STARS
}
