package com.example.backend.Cms.Service.Video;

/**
 * Video qayta ishlashda yuz bergan nosozlik.
 *
 * <h2>Nega {@code BusinessException} emas</h2>
 * {@code BusinessException} — bu KLIENTGA yuboriladigan javob: unda
 * HTTP holati va foydalanuvchi ko'radigan xabar bor.
 *
 * Transcoding esa FON ishida bajariladi va u yerda klient umuman
 * yo'q. Uning xatosi HTTP javobga aylanmaydi — u ish yozuviga
 * yoziladi va admin panelda ko'rinadi.
 *
 * ⚠️ Ikkalasini aralashtirish jimgina noto'g'ri xatti-harakat
 * berardi: fon ishidagi nosozlik tasodifan 422 bo'lib, butunlay
 * boshqa so'rovning javobiga aralashib ketishi mumkin edi.
 */
public class VideoProcessingException extends RuntimeException {

    public VideoProcessingException(String message) {
        super(message);
    }

    public VideoProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
