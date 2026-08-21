package com.example.backend.Cms.Enums;

/**
 * Media faylining holati (ТЗ §26 — «remove/archive»).
 *
 * <h2>Nega o'chirish emas, ARXIVLASH</h2>
 * Fayl 12 xil joydan havola qilinishi mumkin (afisha, qism videosi,
 * reklama rasmi…). Uni o'chirish sahifalarda sinib qolgan rasm va
 * o'ynamaydigan video demakdir.
 *
 * Arxivlash esa xavfsiz: fayl kutubxonada KO'RINMAY QOLADI, ya'ni admin
 * uni yangi kontentga qo'shib yubormaydi — lekin mavjud havolalar
 * ishlashda davom etadi.
 *
 * Butunlay o'chirish ham bor ({@code DELETE /media/{id}}), lekin u faqat
 * hech qayerda ishlatilmagan fayl uchun ruxsat etiladi.
 *
 * <h2>Nega enum, matn emas</h2>
 * Ilgari bu maydon oddiy {@code String} edi va faqat {@code "READY"}
 * qiymati yozilardi. Holat mantiqqa ta'sir qiladigan bo'lgach, matn xato
 * yozuvga yo'l qo'yardi ({@code "Ready"}, {@code "ready"}) va ularni
 * hech narsa ushlamasdi.
 */
public enum MediaStatus {

    /** Ishlatishga tayyor. */
    READY,

    /**
     * Kutubxonada ko'rsatilmaydi, lekin mavjud havolalar ishlaydi.
     * Eskirgan yoki almashtirilgan fayllar uchun.
     */
    ARCHIVED;

    /** Yangi kontentga qo'shish uchun taklif qilinadimi. */
    public boolean selectable() {
        return this == READY;
    }
}
