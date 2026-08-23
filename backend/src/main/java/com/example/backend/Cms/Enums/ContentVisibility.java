package com.example.backend.Cms.Enums;

/**
 * Kontent qanchalik topiladi (ТЗ §15).
 *
 * <h2>Nega {@code status} dan alohida</h2>
 * Ular ikki BOSHQA savolga javob beradi va ularni bitta maydonga
 * qo'shib bo'lmaydi:
 *
 * <ul>
 *   <li>{@code status} — kontent hayot siklining qaysi bosqichida:
 *       qoralamami, ko'rikdami, nashr qilinganmi, arxivdami;</li>
 *   <li>{@code visibility} — nashr qilingan kontent KIMGA topiladi:
 *       katalogda ko'rinadimi yoki faqat to'g'ridan-to'g'ri havola bilan.</li>
 * </ul>
 *
 * Misol: premyeradan oldin nashr qilingan ({@code PUBLISHED}) film
 * {@code UNLISTED} bo'lishi mumkin — havola bilan ko'rish mumkin, lekin
 * katalogda hali chiqmaydi.
 */
public enum ContentVisibility {

    /** Katalogda va qidiruvda ko'rinadi. */
    PUBLIC,

    /**
     * Katalogda va qidiruvda YO'Q, lekin to'g'ridan-to'g'ri havola bilan
     * ochiladi. Premyeradan oldingi ko'rik yoki cheklangan tarqatish uchun.
     */
    UNLISTED,

    /**
     * Faqat panel xodimlari uchun. Oddiy foydalanuvchi havola bilan ham
     * ochа olmaydi — tayyorlanayotgan kontentni tekshirish uchun.
     */
    PRIVATE;

    /** Oddiy foydalanuvchi havola orqali ochа oladimi. */
    public boolean reachableByLink() {
        return this != PRIVATE;
    }

    /** Katalog va qidiruvda chiqadimi. */
    public boolean listedInCatalog() {
        return this == PUBLIC;
    }
}
