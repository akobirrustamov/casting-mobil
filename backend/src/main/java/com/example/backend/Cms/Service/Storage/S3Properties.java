package com.example.backend.Cms.Service.Storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Timeweb S3 sozlamalari.
 *
 * <h2>Nega Timeweb uchun alohida SDK kerak emas</h2>
 * Timeweb Object Storage — Amazon S3 bilan MOS. Farqi faqat manzilda:
 * endpoint boshqa, imzo va protokol bir xil. Shuning uchun AWS SDK
 * o'zgarishsiz ishlaydi, faqat {@code endpointOverride} beriladi.
 *
 * <h2>⚠️ Maxfiy qiymatlar</h2>
 * {@code accessKey} va {@code secretKey} manba kodda YOZILMAYDI va
 * standart qiymati YO'Q. Ular environment orqali beriladi:
 * {@code S3_ACCESS_KEY}, {@code S3_SECRET_KEY}.
 *
 * Berilmasa — S3 provayderi ishga tushmaydi va tizim lokal diskda
 * ishlashda davom etadi (qarang {@code app.storage.provider}). Bu
 * ataylab: yarim sozlangan S3 «ishlayapti» deb ko'rinib, keyin
 * yuklashda yiqilishdan ko'ra, umuman yoqilmagani yaxshiroq.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3Properties {

    /** Masalan {@code https://s3.twcstorage.ru}. */
    private String endpoint;

    /** Timeweb uchun odatda {@code ru-1}. */
    private String region = "ru-1";

    private String bucket;

    private String accessKey;

    private String secretKey;

    /**
     * Presigned havolaning amal qilish muddati.
     *
     * ⚠️ Qisqa bo'lsa 20 GB lik faylni yuklashga ulgurmaydi, uzun bo'lsa
     * qo'lga tushgan havola uzoq ishlaydi. 6 soat — 20 GB ni sekin
     * kanalda ham qamraydi va bir ish kunidan qisqa.
     */
    private Duration uploadUrlTtl = Duration.ofHours(6);

    /**
     * Sozlamalar to'liq berilganmi.
     *
     * Bo'sh satr ham «berilmagan» hisoblanadi: environment o'zgaruvchisi
     * mavjud, lekin qiymatsiz bo'lishi odatiy hol va u jimgina
     * autentifikatsiya xatosiga olib borardi.
     */
    public boolean isConfigured() {
        return notBlank(endpoint) && notBlank(bucket)
                && notBlank(accessKey) && notBlank(secretKey);
    }

    /**
     * Qaysi maydonlar yetishmayapti.
     *
     * ⚠️ Faqat NOMLAR qaytariladi. Qiymatlarni qaytarish yarim yozilgan
     * kalitni logga tushirardi.
     */
    public java.util.List<String> missingFields() {
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (!notBlank(endpoint)) missing.add("app.storage.s3.endpoint");
        if (!notBlank(bucket)) missing.add("app.storage.s3.bucket");
        if (!notBlank(accessKey)) missing.add("app.storage.s3.access-key");
        if (!notBlank(secretKey)) missing.add("app.storage.s3.secret-key");
        return missing;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
