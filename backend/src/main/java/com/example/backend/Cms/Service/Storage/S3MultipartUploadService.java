package com.example.backend.Cms.Service.Storage;

import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.util.Comparator;
import java.util.List;

/**
 * S3 multipart yuklash — imzolangan havolalar orqali.
 *
 * <h2>Nega fayl Spring Boot orqali o'tmaydi</h2>
 * 10 GB lik videoni server orqali proksilash uni butun yuklash davomida
 * band qiladi, tarmoq trafigini ikki barobarlashtiradi (kirish + chiqish)
 * va hech qanday foyda bermaydi: server baytlar bilan hech nima
 * qilmaydi, faqat uzatadi.
 *
 * Shuning uchun server FAQAT:
 * <ul>
 *   <li>ruxsatni tekshiradi;</li>
 *   <li>obyekt kalitini yasaydi;</li>
 *   <li>har bir bo'lak uchun imzolangan havola beradi;</li>
 *   <li>yakunda S3 dan nima kelganini SO'RAB, yig'ishni buyuradi.</li>
 * </ul>
 *
 * Baytlar brauzerdan to'g'ridan-to'g'ri S3 ga ketadi.
 *
 * <h2>⚠️ Bo'laklar ro'yxati bazada saqlanmaydi</h2>
 * Yagona haqiqat manbai — S3 ning o'zi ({@code ListParts}). Klient
 * ETag'larni qaytarib yuborishi ham mumkin edi, lekin unda:
 * <ul>
 *   <li>yolg'on ma'lumot yuborish imkoni paydo bo'lardi;</li>
 *   <li>uzilishdan keyin klient ularni unutgan bo'lardi va davom
 *       ettirish ishlamasdi.</li>
 * </ul>
 *
 * Bu {@code ChunkedUploadService} dagi qaror bilan bir xil: u ham
 * bo'laklarni bazada emas, diskda sanaydi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class S3MultipartUploadService {

    /**
     * Bo'lak o'lchami.
     *
     * ⚠️ S3 talabi: oxirgisidan tashqari har bir bo'lak kamida 5 MB.
     * 10 MB tanlandi — 20 GB lik fayl uchun 2048 ta bo'lak beradi
     * (S3 chegarasi 10 000), 5 MB da esa 4096 ta bo'lardi va har
     * biri uchun alohida imzolangan havola kerak bo'lardi.
     */
    private static final int PART_SIZE = 10 * 1024 * 1024;

    /** S3 ning qattiq chegarasi. */
    private static final int MAX_PARTS = 10_000;

    private final S3Client s3;
    private final S3Presigner presigner;
    private final S3Properties properties;

    // ---------------------------------------------------------- boshlash

    /**
     * Multipart yuklashni ochadi va S3 identifikatorini qaytaradi.
     *
     * ⚠️ {@code Content-Type} shu yerda o'rnatiladi. Keyin uni
     * o'zgartirib bo'lmaydi: bo'laklar allaqachon o'sha sarlavha bilan
     * imzolanadi va CDN ham o'shani qaytaradi.
     */
    public String begin(String storageKey, String originalFilename) {
        return s3.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey(storageKey))
                .contentType(MediaContentTypes.of(originalFilename))
                .build()).uploadId();
    }

    /** Fayl hajmiga qarab nechta bo'lak kerakligini hisoblaydi. */
    public int partCount(long sizeBytes) {
        long parts = (sizeBytes + PART_SIZE - 1) / PART_SIZE;
        if (parts > MAX_PARTS) {
            throw BusinessException.validation(
                    "Fayl juda katta: " + MAX_PARTS + " tadan ko'p bo'lak kerak bo'ladi");
        }
        // Bo'sh fayl ham bitta bo'lak talab qiladi.
        return (int) Math.max(1, parts);
    }

    public int partSize() {
        return PART_SIZE;
    }

    // ------------------------------------------------------------ havola

    /**
     * Bitta bo'lak uchun imzolangan {@code PUT} havolasi.
     *
     * ⚠️ Havola LOGGA yozilmaydi — u imzo bilan birga o'sha bo'lakka
     * to'liq yozish huquqini beradi.
     *
     * @param partNumber 1 dan boshlanadi (S3 talabi, 0 emas)
     */
    public String presignPart(String storageKey, String s3UploadId, int partNumber) {
        if (partNumber < 1 || partNumber > MAX_PARTS) {
            throw BusinessException.validation("Bo'lak raqami noto'g'ri: " + partNumber);
        }

        UploadPartRequest request = UploadPartRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey(storageKey))
                .uploadId(s3UploadId)
                .partNumber(partNumber)
                .build();

        return presigner.presignUploadPart(UploadPartPresignRequest.builder()
                        .signatureDuration(properties.getUploadUrlTtl())
                        .uploadPartRequest(request)
                        .build())
                .url()
                .toString();
    }

    // ------------------------------------------------------------ holat

    /**
     * S3 ga YETIB KELGAN bo'laklar raqamlari.
     *
     * Uzilishdan keyin klient shu ro'yxatni so'raydi va faqat
     * yetishmaganini qayta yuboradi.
     */
    public List<Integer> receivedParts(String storageKey, String s3UploadId) {
        return listParts(storageKey, s3UploadId).stream()
                .map(Part::partNumber)
                .sorted()
                .toList();
    }

    // ----------------------------------------------------------- yakunlash

    /**
     * Bo'laklarni yig'adi.
     *
     * ⚠️ ETag'lar S3 dan SO'RALADI, klientdan qabul qilinmaydi. Klient
     * bergan ro'yxatga ishonish ikki xavf tug'dirardi: yolg'on ma'lumot
     * va uzilishdan keyin ro'yxatning yo'qolishi.
     *
     * @return yig'ilgan obyektning haqiqiy hajmi
     */
    public long complete(String storageKey, String s3UploadId, int expectedParts) {
        List<Part> parts = listParts(storageKey, s3UploadId);

        if (parts.size() != expectedParts) {
            throw BusinessException.validation(
                    "Bo'laklar to'liq emas: " + parts.size() + " / " + expectedParts);
        }

        List<CompletedPart> completed = parts.stream()
                .sorted(Comparator.comparing(Part::partNumber))
                .map(p -> CompletedPart.builder()
                        .partNumber(p.partNumber())
                        .eTag(p.eTag())
                        .build())
                .toList();

        try {
            s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey(storageKey))
                    .uploadId(s3UploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completed).build())
                    .build());
        } catch (S3Exception e) {
            log.error("S3 multipart yig'ilmadi: {}", storageKey, e);
            throw new BusinessException("STORAGE_ERROR", "Fayl yig'ilmadi",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // ⚠️ Yig'ilgandan KEYIN tekshiriladi: `completeMultipartUpload`
        // 200 qaytarib, tanasida xato berishi mumkin (S3 ning ma'lum
        // xatti-harakati). HEAD esa obyekt haqiqatan borligini aytadi.
        return headSize(storageKey);
    }

    /**
     * Tugallanmagan yuklashni bekor qiladi.
     *
     * ⚠️ Bu MAJBURIY. Bekor qilinmagan multipart bo'laklari bucketda
     * qoladi, ro'yxatda KO'RINMAYDI va ular uchun pul olinaveradi.
     */
    public void abort(String storageKey, String s3UploadId) {
        if (s3UploadId == null || s3UploadId.isBlank()) {
            return;
        }
        try {
            s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey(storageKey))
                    .uploadId(s3UploadId)
                    .build());
        } catch (NoSuchUploadException e) {
            // Allaqachon bekor qilingan yoki yig'ilgan — tuzatish kerak emas.
            log.debug("Multipart topilmadi (allaqachon yopilgan): {}", storageKey);
        } catch (S3Exception e) {
            log.warn("Multipart bekor qilinmadi: {}", storageKey, e);
        }
    }

    // -------------------------------------------------------- ichki qism

    private List<Part> listParts(String storageKey, String s3UploadId) {
        try {
            // ⚠️ `listPartsPaginator` — oddiy `listParts` bir marta atigi
            // 1000 ta bo'lak qaytaradi. 20 GB lik fayl uchun ular 2048 ta
            // va ro'yxat jimgina yarmida kesilardi.
            return s3.listPartsPaginator(ListPartsRequest.builder()
                            .bucket(properties.getBucket())
                            .key(objectKey(storageKey))
                            .uploadId(s3UploadId)
                            .build())
                    .parts()
                    .stream()
                    .toList();
        } catch (NoSuchUploadException e) {
            throw BusinessException.notFound("Yuklash sessiyasi", s3UploadId);
        } catch (S3Exception e) {
            log.error("S3 bo'laklar ro'yxati olinmadi: {}", storageKey, e);
            throw new BusinessException("STORAGE_ERROR", "Bo'laklar ro'yxati olinmadi",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private long headSize(String storageKey) {
        try {
            return s3.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey(storageKey))
                    .build()).contentLength();
        } catch (NoSuchKeyException e) {
            // Yig'ish "muvaffaqiyatli" tugadi-yu obyekt yo'q — bu jimgina
            // buzilish bo'lardi: media yozuvi yaratilib, fayl bo'lmasdi.
            log.error("Yig'ilgan obyekt topilmadi: {}", storageKey);
            throw new BusinessException("STORAGE_ERROR", "Yig'ilgan fayl topilmadi",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Boshidagi {@code /} olib tashlanadi — S3 uni bo'sh papka deb qabul qiladi. */
    private String objectKey(String storageKey) {
        String key = storageKey == null ? "" : storageKey;
        return key.startsWith("/") ? key.substring(1) : key;
    }
}
