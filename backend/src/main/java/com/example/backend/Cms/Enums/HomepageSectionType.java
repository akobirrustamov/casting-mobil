package com.example.backend.Cms.Enums;

/**
 * Mobil ilova bosh sahifasidagi bo'lim turi.
 *
 * Bosh sahifa klientda QOTIRILMAYDI — u shu ro'yxatdan quriladi, shuning uchun
 * admin bo'limlarni yoqishi, o'chirishi va tartibini o'zgartirishi mumkin.
 */
public enum HomepageSectionType {

    /** Yuqoridagi reklama karuseli. */
    ADVERTISEMENT_CAROUSEL,

    /** «Yangi premyeralar». */
    NEW_PREMIERES,

    /** Kategoriyalar plitkalari. */
    CATEGORIES,

    /**
     * «Mini seriallar» (ТЗ §31).
     *
     * Kontent turi MINI_SERIES ga mos qator. Kategoriya emas — kontent
     * turi bo'yicha yig'iladi (§13: tur va kategoriya bir xil narsa emas).
     */
    MINI_SERIES,

    /** Tik formatdagi seriallar (Reels uslubi). */
    REELS_SERIES,

    PODCASTS,
    SHOWS,
    STREAMS,
    CLIPS,

    /** Tanlangan (featured) kontent. */
    FEATURED_CONTENT,

    /** Mashhur (popular) kontent. */
    POPULAR_CONTENT,

    /**
     * «Mashhur ijodkorlar».
     * Buyurtmachi talabi: bu bo'lim eng pastda turishi kerak.
     */
    POPULAR_CREATORS,

    /** Admin qo'lda yig'gan qator. */
    CUSTOM_ROW
}
