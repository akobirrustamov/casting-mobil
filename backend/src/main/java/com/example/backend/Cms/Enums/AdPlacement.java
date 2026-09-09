package com.example.backend.Cms.Enums;

/**
 * Banner QAYERDA ko'rinadi.
 *
 * Buyurtmachi (09.09.2026) «Majburiy reklama» maketini yubordi: butun
 * ekranni yopadigan kartochka, yopish tugmasi bilan. Shu paytgacha
 * bannerlar faqat bosh sahifadagi karuselda ko'rinardi — ya'ni ularni
 * ko'rmasdan o'tib ketish mumkin edi.
 *
 * ⚠️ Bitta banner IKKALA joyda chiqmaydi: to'liq ekranga qo'yilgani
 * karuselga tushmaydi. Aks holda odam bitta reklamani ketma-ket ikki
 * marta ko'rardi va buni buzilish deb o'qirdi.
 *
 * ⚠️ Bu «kimga ko'rinadi» EMAS. Kimga — bu {@link AdAudience}: tijorat
 * reklamasi faqat obunasizlarga, admin e'loni hammaga. Ikkala qoida
 * birga ishlaydi: to'liq ekranli tijorat reklamasi obunachiga
 * ko'rsatilmaydi.
 */
public enum AdPlacement {

    /** Bosh sahifadagi karusel — sukut bo'yicha, eski xatti-harakat. */
    FEED,

    /** Butun ekranni yopadigan kartochka. */
    INTERSTITIAL
}
