package com.example.backend.Cms.Enums;

/**
 * Media faylning kontentdagi vazifasi.
 *
 * Bir kontentda bir nechta rasm bo'lishi mumkin (GALLERY), va har bir rol
 * uchun til bo'yicha alohida fayl yuklash mumkin - masalan ruscha afisha.
 */
public enum MediaRole {
    POSTER,
    COVER,
    THUMBNAIL,
    /**
     * Kontentning ASOSIY videosi (ТЗ §22, Step 2 — «videos»).
     *
     * <h2>Nima uchun kerak</h2>
     * SINGLE tuzilmadagi kontentda (film, qisqa metraj, klip, shou) qism
     * bo'lmaydi — demak {@code EpisodeVideo} ham yo'q. Bu rol qo'shilgunga
     * qadar filmning asosiy videosini saqlaydigan joy UMUMAN yo'q edi va
     * filmni tomosha qilib bo'lmasdi.
     *
     * <h2>Bir nechta segment</h2>
     * ТЗ §19: «Ba'zi KINOLAR yoki epizodlar bitta katta video emas, bir
     * nechta video segmentdan iborat bo'lishi mumkin.» Shuning uchun bitta
     * kontentda bir nechta VIDEO qatori bo'lishi mumkin —
     * {@code sortOrder} segment tartibini, {@code locale} esa dublyaj
     * tilini beradi ({@code null} = barcha tillar uchun).
     *
     * ⚠️ TRAILER va TEASER dan FARQ QILADI: ular reklama roliklari, bu esa
     * kontentning o'zi. Entitlement faqat shunga qo'llanadi.
     */
    VIDEO,
    TRAILER,
    TEASER,
    GALLERY
}
