package com.example.backend.Cms.Service.Storage;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;

/**
 * Ombordagi faylni panelda KO'RISH uchun qisqa muddatli havola.
 *
 * <h2>Nima uchun imzolangan havola, ilova orqali oqizish emas</h2>
 * Yetim fayl ro'yxatida yalang'och kalit turadi
 * ({@code content/2ac6ed2b-….mp4}) va u adminga hech narsa aytmaydi.
 * «O'chirish xavfsiz» degan yozuvga ishonish uchun admin faylni
 * KO'RISHI kerak.
 *
 * ⚠️ Panel access tokenni XOTIRADA saqlaydi va uni `Authorization`
 * sarlavhasida yuboradi. `<img src>` va `<video src>` esa sarlavha
 * qo'sha olmaydi — ya'ni «shunchaki endpoint manzilini qo'yish»
 * ishlamaydi: brauzer tokensiz so'rov yuborib 401 olardi.
 *
 * Faylni ilova orqali oqizish ham to'g'ri yechim emas: video bir necha
 * gigabayt bo'lishi mumkin va u butun tomosha davomida ilovaning
 * oqimini va xotirasini band qilardi. Imzolangan havola bilan brauzer
 * to'g'ridan-to'g'ri S3 dan oladi — `Range` so'rovlari bilan, ya'ni
 * videoni o'rtasidan ham ochish mumkin.
 *
 * <h2>⚠️ Havolani olgan har kim faylni ko'ra oladi</h2>
 * Imzo muddati tugaguncha (sukut bo'yicha 10 daqiqa) havola kalitsiz
 * ishlaydi. Shuning uchun u qisqa muddatli va faqat
 * {@code MEDIA_DELETE} ruxsati bor adminga beriladi.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class StoragePreviewService {

    private final S3Presigner presigner;
    private final S3Properties properties;

    /**
     * Havola qancha yashaydi.
     *
     * ⚠️ Qisqa bo'lgani yaxshi, lekin video uchun juda qisqa bo'lmasin:
     * muddat tomosha o'rtasida tugasa, pleyer keyingi `Range` so'rovida
     * 403 olib «video buzuq» degandek to'xtab qolardi.
     */
    @Value("${app.storage.preview-ttl-seconds:600}")
    private long ttlSeconds;

    public Preview of(String rawKey) {
        String key = objectKey(rawKey);
        String contentType = MediaContentTypes.of(key);
        Duration ttl = Duration.ofSeconds(ttlSeconds);

        // ⚠️ Turni S3 dagi saqlangan qiymatdan EMAS, kengaytmadan
        // olamiz va javobda majburan belgilaymiz. Eski fayllar
        // `application/octet-stream` bilan yuklangan bo'lishi mumkin —
        // o'shanda brauzer videoni ochish o'rniga yuklab olardi.
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .responseContentType(contentType)
                // Ko'rsatish, yuklab olish emas.
                .responseContentDisposition("inline")
                .build();

        String url = presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .getObjectRequest(get)
                        .build())
                .url()
                .toString();

        return new Preview(key, url, contentType, MediaContentTypes.previewKind(key),
                Instant.now().plus(ttl));
    }

    /** Boshidagi {@code /} — hisobotda bor, S3 kalitida yo'q. */
    private String objectKey(String storageKey) {
        String key = storageKey == null ? "" : storageKey;
        return key.startsWith("/") ? key.substring(1) : key;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class Preview {
        private String key;
        private String url;
        private String contentType;
        private MediaContentTypes.PreviewKind kind;
        private Instant expiresAt;
    }
}
