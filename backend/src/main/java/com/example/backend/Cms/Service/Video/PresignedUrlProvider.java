package com.example.backend.Cms.Service.Video;

import com.example.backend.Cms.Service.Storage.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * S3 presigned havolalar.
 *
 * <h2>⚠️ Nega kesh kerak — o'lchangan sabab</h2>
 * S3 imzosi {@code X-Amz-Date} ni o'z ichiga oladi va u imzolash
 * VAQTI. MinIO bilan tekshirildi: bir soniya farq bilan yasalgan
 * ikkita havola BOSHQA satr beradi.
 *
 * Ya'ni har foydalanuvchi o'z havolasini olardi. CDN uchun bu boshqa
 * manzil degani: 3000 kishi bitta filmni ko'rsa, kesh umuman
 * ishlamaydi va butun trafik omborga tushadi.
 *
 * <h2>Yechim: vaqt oynasi bo'yicha kesh</h2>
 * Havola bir marta yasaladi va oyna tugagunicha HAMMAGA bir xil
 * qaytariladi. Shunda CDN uni bir marta keshlaydi.
 *
 * Oyna tugagach havola ham amal qilishdan to'xtaydi — ya'ni sizib
 * chiqqan manzil ko'pi bilan bir oyna davomida ishlaydi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class PresignedUrlProvider implements SignedUrlProvider {

    private final S3Presigner presigner;
    private final S3Properties properties;

    /**
     * Havola qancha yashaydi.
     *
     * ⚠️ Ikki tomonlama tanlov:
     * <ul>
     *   <li>qisqa bo'lsa — uzun filmni ko'rayotgan odam o'rtasida
     *       uzilib qoladi, chunki playlistdagi havolalar eskiradi;</li>
     *   <li>uzun bo'lsa — sizib chiqqan havola shuncha vaqt ishlaydi.</li>
     * </ul>
     *
     * 4 soat — eng uzun filmdan ham uzoq, lekin bir kundan qisqa.
     */
    @Value("${app.video.signed-url-ttl:4h}")
    private Duration ttl;

    /**
     * Kesh oynasi.
     *
     * ⚠️ TTL dan sezilarli KICHIK bo'lishi kerak. Aks holda oyna
     * oxirida berilgan havola deyarli darhol eskirardi va uzun
     * filmning oxirgi segmentlari ochilmasdi.
     */
    @Value("${app.video.signed-url-window:1h}")
    private Duration window;

    /**
     * Oyna → (kalit → havola).
     *
     * ⚠️ Chegaralanmagan {@code Map} xotirani yeb qo'yardi: har
     * segment uchun bitta yozuv, ikki soatlik filmda 1200 ta.
     * Eski oyna kelganda butun boshli tozalanadi — bu eng oddiy va
     * ishonchli yo'l.
     */
    private final Map<Long, Map<String, String>> cache = new ConcurrentHashMap<>();

    @Override
    public boolean isAvailable() {
        return presigner != null && properties.isConfigured();
    }

    @Override
    public String sign(String storageKey) {
        long slot = Instant.now().getEpochSecond() / Math.max(1, window.toSeconds());

        // Eski oynalar tozalanadi. Bitta oyna qoladi — o'sha payt
        // ishlatilayotgani.
        cache.keySet().removeIf(existing -> existing < slot);

        return cache
                .computeIfAbsent(slot, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(storageKey, this::presign);
    }

    private String presign(String storageKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey(storageKey))
                .build();

        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .getObjectRequest(request)
                        .build())
                .url()
                .toString();
    }

    /** Boshidagi {@code /} — S3 uni bo'sh nomli papka deb qabul qiladi. */
    private String objectKey(String storageKey) {
        String key = storageKey == null ? "" : storageKey;
        return key.startsWith("/") ? key.substring(1) : key;
    }
}
