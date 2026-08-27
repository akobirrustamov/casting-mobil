package com.example.backend.Cms.Service.Storage;

import java.util.Map;

/**
 * Kengaytma → MIME turi.
 *
 * <h2>Nega o'z jadvali</h2>
 * S3 ga yuklashda {@code Content-Type} ANIQ berilishi kerak. Berilmasa
 * S3 {@code binary/octet-stream} qo'yadi va CDN ham shuni qaytaradi —
 * natijada brauzer videoni o'ynatish o'rniga YUKLAB oladi, HLS pleyer
 * esa playlistni umuman tanimaydi.
 *
 * Spring'ning {@code MediaTypeFactory} si HLS formatlarini bilmaydi:
 * {@code .m3u8} va {@code .m4s} uning jadvalida yo'q. Shuning uchun
 * bu yerda alohida jadval.
 */
public final class MediaContentTypes {

    private static final String DEFAULT = "application/octet-stream";

    private static final Map<String, String> BY_EXTENSION = Map.ofEntries(
            // --- rasm
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("gif", "image/gif"),
            Map.entry("svg", "image/svg+xml"),

            // --- video
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("webm", "video/webm"),
            Map.entry("m4v", "video/x-m4v"),
            Map.entry("mkv", "video/x-matroska"),
            Map.entry("avi", "video/x-msvideo"),

            // --- HLS
            //
            // ⚠️ `.m3u8` uchun `application/vnd.apple.mpegurl` — bu Apple
            // ro'yxatdan o'tkazgan rasmiy tur. `application/x-mpegURL` ham
            // uchraydi, lekin u eski norasmiy variant va ba'zi CDN'lar uni
            // kesh qilmaydi.
            Map.entry("m3u8", "application/vnd.apple.mpegurl"),
            // fMP4 segment.
            Map.entry("m4s", "video/iso.segment"),
            // MPEG-TS segment — eski HLS oqimlari uchun.
            Map.entry("ts", "video/mp2t"),

            Map.entry("pdf", "application/pdf"));

    private MediaContentTypes() {
    }

    /** Nomdan yoki kalitdan MIME turini aniqlaydi. Noma'lum bo'lsa — oktet oqimi. */
    public static String of(String filenameOrKey) {
        return BY_EXTENSION.getOrDefault(StorageKeys.extensionOf(filenameOrKey), DEFAULT);
    }
}
