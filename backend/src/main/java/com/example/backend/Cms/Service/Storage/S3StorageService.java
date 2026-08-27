package com.example.backend.Cms.Service.Storage;

import com.example.backend.Cms.Service.StorageService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Timeweb S3 da saqlash.
 *
 * <h2>Bu bin TO'G'RIDAN-TO'G'RI ishlatilmaydi</h2>
 * {@code LocalStorageService} O'CHIRILMAYDI: mavjud fayllar lokal diskda
 * yotibdi va ochilishda davom etishi kerak. Qaysi omborga borishni
 * {@link RoutingStorageService} hal qiladi — aynan u {@code @Primary}.
 *
 * Bu yerga faqat YANGI fayllar yoziladi.
 *
 * <h2>Kalit shakli lokal bilan BIR XIL</h2>
 * {@code /{folder}/{uuid}.{ext}} — {@link StorageKeys} orqali. Shu
 * sababli bazadagi {@code storage_key} qiymatlari ikkala omborda ham
 * bir xil ma'noni bildiradi va migratsiya paytida qayta yozish kerak
 * emas.
 */
/*
 * ⚠️ `@ConditionalOnBean` ISHLATILMAYDI.
 *
 * U faqat avtokonfiguratsiya klasslari uchun ishonchli: oddiy
 * `@Service` da shart komponent SKANERLASH TARTIBIDA baholanadi va
 * kerakli bin hali ro'yxatga olinmagan bo'lishi mumkin. Aynan shu
 * sodir bo'ldi — bin yaratilib, bog'liqligi topilmadi va butun
 * kontekst ko'tarilmadi (213 test yiqildi).
 *
 * Xususiyat sharti esa tartibga bog'liq emas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    /**
     * Multipart bo'lagining o'lchami.
     *
     * S3 chegarasi — eng kichik bo'lak 5 MB (oxirgisidan tashqari).
     * Bu qiymat AYNI PAYTDA RAM'da turadigan bufer hajmi, shuning uchun
     * uni kattalashtirish xotira sarfini oshiradi.
     */
    private static final int PART_SIZE = 5 * 1024 * 1024;

    private final S3Client s3;
    private final S3Properties properties;

    // ------------------------------------------------------------- yozish

    /**
     * ⚠️ Bu yerda oqim EMAS, {@code PutObject} ishlatiladi: multipart
     * fayl o'z hajmini biladi va bitta so'rov uch marta kam tarmoq
     * murojaati demakdir.
     */
    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.validation("Fayl bo'sh");
        }
        String key = StorageKeys.newKey(file.getOriginalFilename(), folder);
        try (InputStream in = file.getInputStream()) {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(objectKey(key))
                            .contentType(contentTypeFor(key))
                            .build(),
                    RequestBody.fromInputStream(in, file.getSize()));
        } catch (IOException | S3Exception e) {
            log.error("S3 ga saqlanmadi: {}", key, e);
            throw storageError();
        }
        return key;
    }

    /**
     * Hajmi NOMA'LUM oqimni saqlaydi.
     *
     * <h2>Nega multipart, oddiy PutObject emas</h2>
     * {@code PutObject} uchun {@code Content-Length} oldindan kerak.
     * Bu metodga esa hajm berilmaydi ({@code StorageService} imzosi),
     * ya'ni yagona yo'l — butun faylni RAM yoki diskka buferlash.
     *
     * 10 GB lik video uchun bu qabul qilib bo'lmaydi. Multipart esa bir
     * vaqtda faqat BITTA bo'lakni ({@value #PART_SIZE} bayt) xotirada
     * ushlaydi.
     *
     * ⚠️ Xato bo'lsa {@code abortMultipartUpload} chaqiriladi: tugallanmagan
     * multipart bo'laklari S3 da qoladi va ular uchun PUL olinadi.
     */
    @Override
    public String store(InputStream in, String originalFilename, String folder) {
        String key = StorageKeys.newKey(originalFilename, folder);
        String objectKey = objectKey(key);

        String uploadId = s3.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentTypeFor(key))
                .build()).uploadId();

        List<CompletedPart> parts = new ArrayList<>();
        try (InputStream stream = in) {
            byte[] buffer = new byte[PART_SIZE];
            int partNumber = 1;

            while (true) {
                int filled = readFully(stream, buffer);
                // Bo'sh fayl ham saqlanishi kerak: birinchi bo'lak 0 bayt
                // bo'lsa ham yuboriladi, aks holda S3 "bo'laksiz multipart"
                // deb rad etardi.
                if (filled == 0 && partNumber > 1) {
                    break;
                }

                String etag = s3.uploadPart(
                        UploadPartRequest.builder()
                                .bucket(properties.getBucket())
                                .key(objectKey)
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .build(),
                        RequestBody.fromBytes(java.util.Arrays.copyOf(buffer, filled))).eTag();

                parts.add(CompletedPart.builder().partNumber(partNumber).eTag(etag).build());
                partNumber++;

                if (filled < PART_SIZE) {
                    break;
                }
            }

            s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                    .build());

        } catch (IOException | RuntimeException e) {
            abortQuietly(objectKey, uploadId);
            log.error("S3 multipart saqlanmadi: {}", key, e);
            throw e instanceof BusinessException business ? business : storageError();
        }
        return key;
    }

    // ------------------------------------------------------------- o'qish

    @Override
    public boolean accepts(String originalFilename) {
        return StorageKeys.accepts(originalFilename);
    }

    @Override
    public Resource load(String storageKey) {
        long length = sizeOrThrow(storageKey);
        return new S3Resource(s3, properties.getBucket(), objectKey(storageKey), length);
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            head(storageKey);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            // 404 dan boshqa xato "yo'q" degani EMAS — tarmoq nosozligini
            // fayl yo'qligi deb talqin qilish o'chirishga olib borishi mumkin.
            log.warn("S3 mavjudlik tekshiruvi muvaffaqiyatsiz: {}", storageKey, e);
            throw storageError();
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey(storageKey))
                    .build());
        } catch (S3Exception e) {
            // Fayl topilmasa xato EMAS — lokal implementatsiyadagi bilan
            // bir xil xatti-harakat.
            log.warn("S3 dan o'chirilmadi: {}", storageKey, e);
        }
    }

    // -------------------------------------------------------- ichki qism

    private long sizeOrThrow(String storageKey) {
        try {
            return head(storageKey).contentLength();
        } catch (NoSuchKeyException e) {
            throw BusinessException.notFound("Media", storageKey);
        } catch (S3Exception e) {
            log.error("S3 metadata o'qilmadi: {}", storageKey, e);
            throw storageError();
        }
    }

    private software.amazon.awssdk.services.s3.model.HeadObjectResponse head(String storageKey) {
        return s3.headObject(HeadObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey(storageKey))
                .build());
    }

    /**
     * Bazadagi kalitni S3 obyekt kalitiga aylantiradi.
     *
     * ⚠️ Boshidagi {@code /} OLIB TASHLANADI. S3 uni oddiy belgi deb
     * qabul qiladi va {@code //content/x.mp4} kabi bo'sh nomli papka
     * hosil bo'lardi — brauzerda ochilardi, lekin konsolda topib
     * bo'lmasdi.
     */
    private String objectKey(String storageKey) {
        String key = storageKey == null ? "" : storageKey;
        return key.startsWith("/") ? key.substring(1) : key;
    }

    /** Kengaytmaga qarab MIME. CDN ham shu qiymatni qaytaradi. */
    private String contentTypeFor(String key) {
        return MediaContentTypes.of(key);
    }

    /**
     * Oqimdan buferni TO'LDIRGUNCHA o'qiydi.
     *
     * ⚠️ Bitta {@code read()} chaqiruvi buferni to'ldirishi SHART EMAS —
     * u xohlagancha kam bayt qaytarishi mumkin. Shusiz bo'laklar 5 MB
     * dan kichik chiqib, S3 ularni rad etardi.
     */
    private int readFully(InputStream in, byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int read = in.read(buffer, total, buffer.length - total);
            if (read < 0) {
                break;
            }
            total += read;
        }
        return total;
    }

    private void abortQuietly(String objectKey, String uploadId) {
        try {
            s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .uploadId(uploadId)
                    .build());
        } catch (S3Exception e) {
            log.warn("Multipart bekor qilinmadi: {}", objectKey, e);
        }
    }

    private BusinessException storageError() {
        return new BusinessException("STORAGE_ERROR", "Fayl saqlanmadi",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
