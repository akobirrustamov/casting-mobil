package com.example.backend.Cms.Enums;

import java.util.Locale;
import java.util.Set;

public enum MediaType {
    IMAGE,
    VIDEO,
    DOCUMENT;

    /**
     * Video kengaytmalari.
     *
     * ⚠️ `mkv` va `avi` bu ro'yxatda, lekin ular ARXIV formatlari:
     * HTML5 `<video>` ularni o'ynata olmaydi (Chrome ba'zi mkv'larni
     * ochadi, avi'ni esa deyarli hech qaysi brauzer ochmaydi).
     *
     * Ular ataylab qabul qilinadi — admin manba faylni omborga
     * qo'yishi kerak. Lekin TOMOSHA uchun mp4 (H.264/AAC) kerak:
     * shu format barcha brauzer va mobil qurilmalarda ishlaydi.
     * Transkodlash hali yo'q, shuning uchun mkv/avi ni epizod videosi
     * sifatida biriktirish foydalanuvchida qora ekran beradi.
     */
    private static final Set<String> VIDEO_EXTENSIONS =
            Set.of("mp4", "mov", "webm", "m4v", "mkv", "avi");

    /**
     * Brauzer va mobil pleyer TO'G'RIDAN-TO'G'RI o'ynata oladigan
     * formatlar. Qolganlari omborda saqlanadi, lekin tomosha uchun
     * avval mp4 ga o'girilishi kerak.
     */
    private static final Set<String> PLAYABLE_EXTENSIONS =
            Set.of("mp4", "m4v", "webm", "mov");
    private static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "gif", "svg");

    /**
     * Fayl turini aniqlaydi.
     *
     * <h2>Nima uchun MIME yolg'iz yetarli emas</h2>
     * MIME turini BRAUZER beradi va u har doim ham bermaydi. `.m4v`,
     * `.mkv` kabi kengaytmalar uchun ko'p brauzerlarda {@code File.type}
     * bo'sh satr bo'ladi, klient esa uni {@code application/octet-stream}
     * ga aylantiradi.
     *
     * ⚠️ Ilgari tur FAQAT MIME bo'yicha aniqlanardi va oqibati jimgina
     * edi: bunday video {@code DOCUMENT} bo'lib saqlanardi. Fayl
     * muvaffaqiyatli yuklanardi, lekin qism muharriridagi video tanlash
     * oynasi ({@code type=VIDEO}) uni KO'RSATMASDI — admin o'zi
     * yuklagan videoni topa olmasdi.
     *
     * Kengaytma ishonchliroq manba: {@code StorageService} allaqachon
     * faqat ruxsat etilgan kengaytmalarni qabul qiladi, ya'ni bu yerga
     * kelgan nom tekshirilgan bo'ladi.
     */
    public static MediaType detect(String mimeType, String originalFilename) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (mime.startsWith("video")) {
            return VIDEO;
        }
        if (mime.startsWith("image")) {
            return IMAGE;
        }

        String extension = extensionOf(originalFilename);
        if (VIDEO_EXTENSIONS.contains(extension)) {
            return VIDEO;
        }
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return IMAGE;
        }
        return DOCUMENT;
    }

    /**
     * Bu faylni pleyer to'g'ridan-to'g'ri o'ynata oladimi.
     *
     * Panel shu asosda ogohlantiradi: `.mkv` yuklangan bo'lsa fayl
     * omborda turadi, lekin uni epizodga biriktirish foydalanuvchida
     * QORA EKRAN beradi va buni hech kim darhol sezmaydi.
     */
    public static boolean isPlayable(String originalFilename) {
        return PLAYABLE_EXTENSIONS.contains(extensionOf(originalFilename));
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
