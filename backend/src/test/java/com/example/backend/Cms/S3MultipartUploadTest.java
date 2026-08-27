package com.example.backend.Cms;

import com.example.backend.Cms.Entity.UploadSession;
import com.example.backend.Cms.Enums.UploadMode;
import com.example.backend.Cms.Service.ChunkedUploadService;
import com.example.backend.Cms.Service.Storage.S3MultipartUploadService;
import com.example.backend.Cms.Service.Storage.S3Properties;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * S3 multipart yuklash mantig'i.
 *
 * ⚠️ HAQIQIY S3 ga chiqilmaydi va hisob ma'lumotlari ishlatilmaydi
 * (§30). {@code S3Client} va {@code S3Presigner} — mock. Tekshiriladigan
 * narsa S3 ning o'zi emas, BIZNING hisob-kitobimiz: bo'laklar soni,
 * raqamlarni aylantirish va chegaralar.
 */
class S3MultipartUploadTest {

    private static final long MB = 1024L * 1024L;
    private static final long GB = 1024L * MB;

    private S3Client s3;
    private S3Presigner presigner;
    private S3MultipartUploadService service;

    @BeforeEach
    void setUp() {
        s3 = mock(S3Client.class);
        presigner = mock(S3Presigner.class);

        S3Properties properties = new S3Properties();
        properties.setEndpoint("https://s3.example.invalid");
        properties.setBucket("test-bucket");
        properties.setAccessKey("KEY");
        properties.setSecretKey("SECRET");
        properties.setUploadUrlTtl(Duration.ofHours(6));

        service = new S3MultipartUploadService(s3, presigner, properties);
    }

    @Nested
    @DisplayName("Bo'laklar soni")
    class PartCount {

        @Test
        @DisplayName("Fayl bo'lak o'lchamiga bo'linmasa oxirgi bo'lak QO'SHILADI")
        void remainderGetsItsOwnPart() {
            // 25 MB, bo'lak 10 MB → 10 + 10 + 5
            assertThat(service.partCount(25 * MB)).isEqualTo(3);
            // Aynan chegarada — ortiqcha bo'lak yasalmaydi.
            assertThat(service.partCount(20 * MB)).isEqualTo(2);
        }

        @Test
        @DisplayName("Bo'sh fayl ham BITTA bo'lak talab qiladi")
        void emptyFileStillNeedsOnePart() {
            // ⚠️ 0 qaytarilsa S3 «bo'laksiz multipart» deb rad etardi.
            assertThat(service.partCount(0)).isEqualTo(1);
            assertThat(service.partCount(1)).isEqualTo(1);
        }

        @Test
        @DisplayName("20 GB S3 chegarasiga SIG'ADI")
        void twentyGigabytesFits() {
            // Talab qilingan eng katta hajm (roadmap §8). 10 MB lik
            // bo'lakda 2048 ta chiqadi — S3 chegarasi 10 000.
            assertThat(service.partCount(20 * GB)).isEqualTo(2048);
        }

        @Test
        @DisplayName("Chegaradan oshgan fayl BOSHIDA rad etiladi")
        void tooLargeIsRejectedUpFront() {
            // ⚠️ Rad javobi ENG BOSHIDA berilishi kerak. Aks holda
            // admin gigabaytlab ma'lumot yuborib bo'lgach, yig'ish
            // paytida xato olardi.
            assertThatThrownBy(() -> service.partCount(200 * GB))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("10000");
        }
    }

    @Nested
    @DisplayName("Imzolangan havola")
    class Presigning {

        @BeforeEach
        void stubPresigner() throws Exception {
            PresignedUploadPartRequest presigned = mock(PresignedUploadPartRequest.class);
            when(presigned.url()).thenReturn(new URL("https://s3.example.invalid/part"));
            when(presigner.presignUploadPart(any(UploadPartPresignRequest.class)))
                    .thenReturn(presigned);
        }

        @Test
        @DisplayName("Berilgan raqam S3 so'roviga o'zgarishsiz o'tadi")
        void partNumberReachesS3() {
            service.presignPart("/content/a.mp4", "UPLOAD-1", 7);

            ArgumentCaptor<UploadPartPresignRequest> captor =
                    ArgumentCaptor.forClass(UploadPartPresignRequest.class);
            org.mockito.Mockito.verify(presigner).presignUploadPart(captor.capture());

            assertThat(captor.getValue().uploadPartRequest().partNumber()).isEqualTo(7);
        }

        @Test
        @DisplayName("Bo'lak raqami 0 yoki manfiy bo'lsa rad etiladi")
        void invalidPartNumberRejected() {
            assertThatThrownBy(() -> service.presignPart("/a.mp4", "U", 0))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> service.presignPart("/a.mp4", "U", -1))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> service.presignPart("/a.mp4", "U", 10_001))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Havolaning amal qilish muddati sozlamadan olinadi")
        void ttlComesFromConfiguration() {
            service.presignPart("/content/a.mp4", "UPLOAD-1", 1);

            ArgumentCaptor<UploadPartPresignRequest> captor =
                    ArgumentCaptor.forClass(UploadPartPresignRequest.class);
            org.mockito.Mockito.verify(presigner).presignUploadPart(captor.capture());

            assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofHours(6));
        }
    }

    /**
     * ⚠️ Bu eng xavfli JIMGINA xato bo'lardi.
     *
     * Klient bo'laklarni 0 dan sanaydi (mavjud {@code /chunks/{n}} oqimi
     * shunday), S3 esa 1 dan. Aylantirish {@code ChunkedUploadService}
     * da yashaydi, shuning uchun uni AYNAN O'SHA YERDA sinash kerak.
     *
     * Birinchi urinishda test {@code S3MultipartUploadService} ni
     * sinagan edi va BO'SH chiqdi: 1 berib 1 kutgan. Mutatsiya sinovi
     * shuni ko'rsatdi — aylantirishni olib tashlaganda test o'tishda
     * davom etdi.
     */
    @Nested
    @DisplayName("Klient raqamini S3 raqamiga aylantirish")
    class IndexConversion {

        private S3MultipartUploadService s3Multipart;
        private ChunkedUploadService chunked;

        @BeforeEach
        void setUp() {
            s3Multipart = mock(S3MultipartUploadService.class);
            chunked = new ChunkedUploadService(
                    mock(com.example.backend.Cms.Repository.UploadSessionRepo.class),
                    mock(com.example.backend.Cms.Repository.MediaAssetRepo.class),
                    mock(com.example.backend.Cms.Service.StorageService.class),
                    java.util.Optional.of(s3Multipart),
                    mock(com.example.backend.Cms.Service.Video.TranscodingJobService.class),
                    "/tmp/test-uploads");
        }

        private UploadSession s3Session(int totalChunks) {
            return UploadSession.builder()
                    .id("S1")
                    .originalFilename("kino.mp4")
                    .sizeBytes(100L)
                    .chunkSize(10 * (int) MB)
                    .totalChunks(totalChunks)
                    .folder("content")
                    .status("PENDING")
                    .createdBy(java.util.UUID.randomUUID())
                    .createdAt(java.time.LocalDateTime.now())
                    .uploadMode(UploadMode.S3_MULTIPART)
                    .storageKey("/content/uuid.mp4")
                    .s3UploadId("UPLOAD-1")
                    .build();
        }

        @Test
        @DisplayName("Klientning 0-bo'lagi S3 ga 1-BO'LAK bo'lib ketadi")
        void zeroBecomesOne() {
            chunked.presignedPartUrl(s3Session(3), 0);

            // Aylantirish unutilsa S3 ga 0 ketardi va u BO'LAKNI RAD
            // ETADI — yuklash birinchi qadamdayoq to'xtardi.
            org.mockito.Mockito.verify(s3Multipart)
                    .presignPart("/content/uuid.mp4", "UPLOAD-1", 1);
        }

        @Test
        @DisplayName("Oxirgi bo'lak ham to'g'ri raqam oladi")
        void lastPartIsShiftedToo() {
            chunked.presignedPartUrl(s3Session(3), 2);

            org.mockito.Mockito.verify(s3Multipart)
                    .presignPart("/content/uuid.mp4", "UPLOAD-1", 3);
        }

        @Test
        @DisplayName("Chegaradan tashqaridagi raqam rad etiladi")
        void outOfRangeRejected() {
            assertThatThrownBy(() -> chunked.presignedPartUrl(s3Session(3), 3))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> chunked.presignedPartUrl(s3Session(3), -1))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("S3 rejimida BO'LMAGAN sessiya uchun havola berilmaydi")
        void nonS3SessionRejected() {
            UploadSession local = s3Session(3);
            local.setUploadMode(UploadMode.CHUNKED);

            assertThatThrownBy(() -> chunked.presignedPartUrl(local, 0))
                    .isInstanceOf(BusinessException.class);
        }

        /**
         * ⚠️ Usiz butun maqsad yo'qolardi.
         *
         * Klient eski yo'ldan ({@code PUT .../chunks/{n}}) foydalanishda
         * davom etsa, 10 GB baribir SERVER ORQALI oqardi. Undan ham
         * yomoni — bo'laklar diskka yozilardi, S3 ga esa hech narsa
         * tushmasdi, va yig'ish paytida «bo'laklar to'liq emas»
         * chiqardi, sababi umuman tushunarsiz bo'lgan holda.
         */
        @Test
        @DisplayName("S3 rejimida bo'lak SERVER orqali qabul qilinmaydi")
        void chunkThroughServerRejectedInS3Mode() {
            assertThatThrownBy(() -> chunked.saveChunk(
                    s3Session(3), 0, new java.io.ByteArrayInputStream(new byte[10])))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("S3");
        }
    }

    @Nested
    @DisplayName("Boshlash")
    class Begin {

        @Test
        @DisplayName("Content-Type kengaytmadan aniqlanadi")
        void contentTypeComesFromExtension() {
            when(s3.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                    .thenReturn(CreateMultipartUploadResponse.builder()
                            .uploadId("UPLOAD-1").build());

            service.begin("/content/uuid.mp4", "kino.mp4");

            ArgumentCaptor<CreateMultipartUploadRequest> captor =
                    ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
            org.mockito.Mockito.verify(s3).createMultipartUpload(captor.capture());

            // ⚠️ Berilmasa S3 `binary/octet-stream` qo'yadi va CDN ham
            // shuni qaytaradi — brauzer videoni o'ynatish o'rniga
            // YUKLAB olardi.
            assertThat(captor.getValue().contentType()).isEqualTo("video/mp4");
        }

        @Test
        @DisplayName("Obyekt kalitidan boshidagi `/` OLIB TASHLANADI")
        void leadingSlashIsStripped() {
            when(s3.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                    .thenReturn(CreateMultipartUploadResponse.builder()
                            .uploadId("UPLOAD-1").build());

            service.begin("/content/uuid.mp4", "kino.mp4");

            ArgumentCaptor<CreateMultipartUploadRequest> captor =
                    ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
            org.mockito.Mockito.verify(s3).createMultipartUpload(captor.capture());

            // ⚠️ S3 boshidagi `/` ni oddiy belgi deb qabul qiladi va
            // `//content/x.mp4` kabi BO'SH NOMLI papka hosil bo'lardi:
            // brauzerda ochilardi, lekin konsolda topib bo'lmasdi.
            assertThat(captor.getValue().key()).isEqualTo("content/uuid.mp4");
        }
    }
}
