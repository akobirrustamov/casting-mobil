package com.example.backend.Cms;

import com.example.backend.Cms.Service.Storage.MediaContentTypes;
import com.example.backend.Cms.Service.Storage.MediaContentTypes.PreviewKind;
import com.example.backend.Cms.Service.Storage.S3Properties;
import com.example.backend.Cms.Service.Storage.StoragePreviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Yetim faylni panelda ko'rish.
 *
 * <h2>Nima uchun kerak</h2>
 * Yetim ro'yxatida faqat kalit turadi ({@code content/2ac6ed2b-….mp4}) —
 * nomlar UUID, chunki ular server tomonida yasaladi. «O'chirish
 * xavfsiz» degan yozuvga ishonish uchun admin faylni ko'rishi kerak.
 *
 * <h2>⚠️ Bu yerda haqiqiy S3 YO'Q va kerak ham emas</h2>
 * Imzolash — sof hisoblash: kalit, muddat va so'rov parametrlaridan
 * HMAC olinadi. Tarmoq ulanishi kerak emas, shuning uchun test soxta
 * hisob ma'lumotlari bilan ishlaydi va CI da tashqi xizmatga
 * bog'lanmaydi. Haqiqiy kalitlar testga HECH QACHON yozilmaydi.
 */
class StoragePreviewTest {

    /**
     * Ko'rsatish turi — tugma chiqadimi yoki yo'qmi, shu hal qiladi.
     *
     * <h2>⚠️ Nima jim buziladi</h2>
     * Ikki tomonga xato bo'lishi mumkin va ikkalasi ham skrinshotda
     * to'g'ri ko'rinadi:
     *
     * 1. Rasm yoki video OTHER deb belgilansa — tugma umuman chiqmaydi
     *    va admin faylni ko'ra olmaydi. Sahifa buzuq ko'rinmaydi.
     * 2. HLS bo'lagi VIDEO deb belgilansa — tugma chiqadi, bosiladi,
     *    pleyer ochiladi va hech narsa o'ynamaydi. Admin «video buzuq»
     *    degan XATO xulosaga keladi.
     */
    @Nested
    @DisplayName("Ko'rsatish turi")
    class Turi {

        @Test
        @DisplayName("rasmlar — IMAGE")
        void rasmlar() {
            for (String key : new String[] {
                    "cms-dev/afisha.jpg", "content/a.jpeg", "content/b.png",
                    "content/c.webp", "content/d.gif" }) {
                assertThat(MediaContentTypes.previewKind(key))
                        .as(key)
                        .isEqualTo(PreviewKind.IMAGE);
            }
        }

        @Test
        @DisplayName("videolar — VIDEO")
        void videolar() {
            for (String key : new String[] {
                    "content/film.mp4", "content/a.mov", "content/b.webm",
                    "content/c.m4v", "content/d.mkv", "content/e.avi" }) {
                assertThat(MediaContentTypes.previewKind(key))
                        .as(key)
                        .isEqualTo(PreviewKind.VIDEO);
            }
        }

        @Test
        @DisplayName("⚠️ HLS bo'laklari VIDEO EMAS — ular o'zicha o'ynamaydi")
        void hlsBolaklari() {
            for (String key : new String[] {
                    "videos/146/hls/480p/seg-1.m4s",
                    "videos/146/hls/480p/seg-1.ts",
                    "videos/146/hls/master.m3u8" }) {
                assertThat(MediaContentTypes.previewKind(key))
                        .as(key)
                        .isEqualTo(PreviewKind.OTHER);
            }
        }

        @Test
        @DisplayName("qolgan hammasi — OTHER")
        void qolganlari() {
            for (String key : new String[] {
                    "arxiv/a.zip", "hujjat/b.pdf", "kalitsiz-fayl", "" }) {
                assertThat(MediaContentTypes.previewKind(key))
                        .as(key)
                        .isEqualTo(PreviewKind.OTHER);
            }
        }
    }

    /**
     * Imzolangan havola.
     *
     * ⚠️ Havolada uchta narsa bo'lishi SHART va har biri alohida
     * buzilishi mumkin — havola esa ko'rinishidan baribir to'g'ri
     * bo'lib qolardi.
     */
    @Nested
    @DisplayName("Imzolangan havola")
    class Havola {

        private StoragePreviewService xizmat(long ttlSeconds) {
            S3Properties properties = new S3Properties();
            properties.setEndpoint("https://s3.example.invalid");
            properties.setRegion("ru-1");
            properties.setBucket("sinov-bucket");
            // ⚠️ Soxta qiymatlar. Imzolash uchun ular YETARLI: SDK
            // tarmoqqa chiqmaydi, faqat HMAC hisoblaydi.
            properties.setAccessKey("SINOV_KALIT");
            properties.setSecretKey("SINOV_MAXFIY");

            S3Presigner presigner = S3Presigner.builder()
                    .endpointOverride(URI.create(properties.getEndpoint()))
                    .region(Region.of(properties.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    properties.getAccessKey(), properties.getSecretKey())))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .build();

            StoragePreviewService service = new StoragePreviewService(presigner, properties);
            ReflectionTestUtils.setField(service, "ttlSeconds", ttlSeconds);
            return service;
        }

        @Test
        @DisplayName("kalit, imzo va muddat havolada bor")
        void havolaToliq() {
            StoragePreviewService.Preview preview =
                    xizmat(600).of("content/film.mp4");

            assertThat(preview.getUrl()).contains("sinov-bucket/content/film.mp4");
            // Imzosiz havola ochiq bucket'da ishlab ketardi va yopilgan
            // bucket'da jimgina 403 berardi.
            assertThat(preview.getUrl()).contains("X-Amz-Signature=");
            assertThat(preview.getUrl()).contains("X-Amz-Expires=600");
        }

        @Test
        @DisplayName("⚠️ boshidagi `/` olib tashlanadi")
        void boshidagiSlash() {
            // Hisobotda kalit `/cms-dev/x.jpg` ko'rinishida keladi, S3 da
            // esa boshida `/` yo'q. Qoldirilsa S3 bo'sh nomli papka
            // yasab, 404 qaytarardi.
            StoragePreviewService.Preview preview =
                    xizmat(600).of("/cms-dev/afisha.jpg");

            assertThat(preview.getKey()).isEqualTo("cms-dev/afisha.jpg");
            assertThat(preview.getUrl()).doesNotContain("//cms-dev");
        }

        @Test
        @DisplayName("⚠️ MIME majburan belgilanadi — brauzer yuklab olmasin")
        void mimeMajburan() {
            StoragePreviewService.Preview preview =
                    xizmat(600).of("content/film.mp4");

            assertThat(preview.getContentType()).isEqualTo("video/mp4");
            // Eski fayllar `octet-stream` bilan yuklangan bo'lishi
            // mumkin — o'shanda brauzer videoni ochish o'rniga yuklab
            // olardi. Shuning uchun javob turi havolada ustiga yoziladi.
            assertThat(preview.getUrl()).contains("response-content-type");
            assertThat(preview.getUrl()).contains("response-content-disposition");
        }

        @Test
        @DisplayName("muddat kelajakda va sozlamaga bog'liq")
        void muddat() {
            Instant oldin = Instant.now();
            StoragePreviewService.Preview preview = xizmat(60).of("content/a.jpg");

            assertThat(preview.getExpiresAt()).isAfter(oldin);
            assertThat(preview.getUrl()).contains("X-Amz-Expires=60");
        }

        @Test
        @DisplayName("tur havola bilan BIRGA qaytadi")
        void turQaytadi() {
            assertThat(xizmat(600).of("content/film.mp4").getKind())
                    .isEqualTo(PreviewKind.VIDEO);
            assertThat(xizmat(600).of("content/a.png").getKind())
                    .isEqualTo(PreviewKind.IMAGE);
        }
    }
}
