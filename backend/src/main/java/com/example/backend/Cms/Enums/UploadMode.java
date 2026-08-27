package com.example.backend.Cms.Enums;

/**
 * Bo'laklar qayerga tushadi.
 *
 * <h2>Nega ikki rejim</h2>
 * {@code CHUNKED} — mavjud oqim: har bir bo'lak Spring Boot orqali
 * o'tib, diskka yoziladi, keyin yig'iladi. U ishlaydi, lekin katta
 * fayl uchun server orqali gigabaytlab trafik o'tkazadi.
 *
 * {@code S3_MULTIPART} — bo'laklar brauzerdan TO'G'RIDAN-TO'G'RI S3 ga
 * ketadi. Spring Boot faqat ruxsat beradi va imzolangan havola yasaydi;
 * fayl uning yonidan ham o'tmaydi.
 *
 * ⚠️ Rejim SESSIYA boshlanganda tanlanadi va o'zgarmaydi. Yarim yo'lda
 * almashtirish allaqachon yuborilgan bo'laklarni yaroqsiz qilardi.
 */
public enum UploadMode {

    /** Bo'laklar server diskiga. Sukut — mavjud xatti-harakat. */
    CHUNKED,

    /** Bo'laklar to'g'ridan-to'g'ri S3 ga. */
    S3_MULTIPART
}
