package com.example.backend.Cms.Service.Storage;

import com.example.backend.exceptions.BusinessException;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Saqlash kalitlari va ruxsat etilgan kengaytmalar — YAGONA manba.
 *
 * <h2>Nega alohida klass</h2>
 * Ilgari bu mantiq {@code LocalStorageService} ichida yopiq turardi.
 * Ikkinchi implementatsiya (S3) paydo bo'lganda uni nusxalash kerak
 * bo'lardi — ya'ni XAVFSIZLIK qoidasi (kengaytma oq ro'yxati va yo'l
 * himoyasi) ikki joyda yashardi va birinchi o'zgarishdayoq ajralardi.
 *
 * Nusxalangan xavfsizlik tekshiruvi — tekshiruvsizlikdan yomonroq:
 * u himoya bor degan taassurot beradi.
 *
 * ⚠️ Ro'yxatni o'zgartirsangiz {@code UploadFormatContractTest} ni
 * ishga tushiring — u panel taklif qiladigan formatlar bilan
 * solishtiradi.
 */
public final class StorageKeys {

    /** Ruxsat etilgan kengaytmalar. Boshqasi qabul qilinmaydi. */
    private static final Set<String> ALLOWED = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "svg",
            // ⚠️ `mkv` va `avi` — ARXIV formatlari. Ular qabul qilinadi,
            // lekin brauzer va mobil pleyer ularni to'g'ridan-to'g'ri
            // O'YNATA OLMAYDI (batafsil: MediaType.VIDEO_EXTENSIONS).
            "mp4", "mov", "webm", "m4v", "mkv", "avi",
            "pdf");

    private StorageKeys() {
    }

    public static boolean accepts(String originalFilename) {
        return ALLOWED.contains(extensionOf(originalFilename));
    }

    /**
     * Yangi saqlash kaliti: {@code /{folder}/{uuid}.{ext}}.
     *
     * ⚠️ Nom BUTUNLAY server tomonida yasaladi — foydalanuvchi nomidan
     * faqat kengaytma olinadi. Shu sababli {@code ../../etc/passwd}
     * kabi yo'l hosil bo'lishi mumkin emas.
     */
    public static String newKey(String originalFilename, String folder) {
        String extension = extensionOf(originalFilename);
        if (!ALLOWED.contains(extension)) {
            throw BusinessException.validation(
                    "Bu turdagi fayl qabul qilinmaydi: " + extension);
        }
        return "/" + safeFolder(folder) + "/" + UUID.randomUUID() + "." + extension;
    }

    /** Papka nomidan harf, raqam, tire va pastki chiziqdan boshqasi olib tashlanadi. */
    public static String safeFolder(String folder) {
        String cleaned = folder == null ? "" : folder.replaceAll("[^a-zA-Z0-9_-]", "");
        return cleaned.isBlank() ? "misc" : cleaned;
    }

    public static String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
