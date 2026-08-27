package com.example.backend.Cms.Enums;

/**
 * Transcoding ishining bosqichi.
 *
 * <h2>Nega {@code MediaStatus} kengaytirilmadi</h2>
 * {@code MediaStatus} ({@code READY} / {@code ARCHIVED}) — bu faylning
 * KUTUBXONADAGI holati: u tanlash uchun taklif qilinadimi yoki yo'q.
 * Transcoding esa boshqa o'q: arxivlangan video ham transcoding
 * qilingan bo'lishi mumkin, yangi yuklangani esa hali navbatda
 * turgan bo'lishi mumkin.
 *
 * Ikkalasini bitta enumga qo'shish «{@code ARCHIVED} bo'lsa
 * transcoding qaysi bosqichda?» degan savolga javobsiz qoldirardi.
 *
 * <h2>Bosqichlar tartibi</h2>
 * <pre>
 *   QUEUED → PROBING → TRANSCODING → UPLOADING → READY
 *                 ↓          ↓            ↓
 *               FAILED    FAILED       FAILED
 * </pre>
 *
 * ⚠️ {@code READY} FAQAT hamma narsa omborga yuklangandan keyin
 * qo'yiladi. Qisman yuklangan HLS o'ynatilganda o'rtasida uzilardi va
 * buni foydalanuvchi «video buzuq» deb tushunardi.
 */
public enum VideoProcessingStatus {

    /** Navbatda. Worker hali olmagan. */
    QUEUED,

    /** {@code ffprobe} — o'lcham, davomiylik, kodeklar aniqlanmoqda. */
    PROBING,

    /** {@code ffmpeg} — sifat variantlari yaratilmoqda. Eng uzun bosqich. */
    TRANSCODING,

    /** HLS omborga yuklanmoqda. */
    UPLOADING,

    /** Tayyor. {@code hlsMasterKey} to'ldirilgan. */
    READY,

    /** Yiqildi. Sabab {@code error} maydonida. */
    FAILED;

    /** Worker olishi mumkin bo'lgan holatlar. */
    public boolean isPending() {
        return this == QUEUED;
    }

    /** Ish tugagan — endi o'zgarmaydi (qayta urinishdan tashqari). */
    public boolean isFinished() {
        return this == READY || this == FAILED;
    }
}
