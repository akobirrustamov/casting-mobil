package com.example.backend.Cms;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.VideoProcessingStatus;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Repository.TranscodingJobRepo;
import com.example.backend.Cms.Service.Video.TranscodingJobService;
import com.example.backend.Admin.TestStaffFactory;
import com.example.backend.Enums.Permission;
import com.example.backend.support.CapturingStatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Transcoding holati admin panel API'sida.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * <ul>
 *   <li>video BO'LMAGAN media uchun {@code transcoding} maydoni
 *       umuman bo'lmaydi — «ish yo'q» va «ish yiqilgan» boshqa narsa;</li>
 *   <li>ro'yxat ishlarni BITTA so'rovda oladi — 40 elementli sahifada
 *       N+1 bo'lardi;</li>
 *   <li>qayta urinish {@code MEDIA_UPLOAD} talab qiladi;</li>
 *   <li>ishlab turgan ishni qayta urinib bo'lmaydi.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
class MediaTranscodingApiTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private MockMvc mockMvc;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private TranscodingJobRepo jobRepo;
    @Autowired private TranscodingJobService jobService;
    @Autowired private TestStaffFactory staff;

    private String token;
    private String tokenWithoutUpload;

    @BeforeEach
    void seed() {
        jobRepo.deleteAll();
        token = staff.token("+99890000" + (400 + SEQ.incrementAndGet()),
                EnumSet.of(Permission.MEDIA_VIEW, Permission.MEDIA_UPLOAD));
        tokenWithoutUpload = staff.token("+99890000" + (500 + SEQ.incrementAndGet()),
                EnumSet.of(Permission.MEDIA_VIEW));
    }

    private MediaAsset media(MediaType type) {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/m-" + SEQ.incrementAndGet() + ".mp4")
                .originalFilename("kino.mp4")
                .type(type)
                .mimeType(type == MediaType.VIDEO ? "video/mp4" : "image/png")
                .sizeBytes(1024L)
                .status(MediaStatus.READY)
                .createdBy(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Nested
    @DisplayName("Ro'yxat")
    class Listing {

        /**
         * ⚠️ Har media uchun alohida so'rov 40 elementli sahifada
         * 40 ta ortiqcha murojaat bo'lardi — klassik N+1.
         *
         * Bu test AYNI SO'ROVLAR SONINI o'lchaydi: media soni
         * o'zgarganda so'rovlar soni o'zgarmasligi kerak.
         */
        /**
         * ⚠️ So'rovlar soni MEDIA SONIGA bog'liq bo'lmasligi kerak.
         *
         * Aynan shu tekshiriladi, «bitta so'rov» emas: `library`
         * so'rovining o'zi ham `cms_transcoding_job` ni eslatadi
         * (`FAILED` filtri uchun `exists` kichik so'rovi). Qat'iy
         * «hasSize(1)» tekshiruvi shu sababdan yolg'on yiqilardi va
         * u N+1 haqida hech narsa aytmasdi.
         *
         * Bu variant esa aynan masalani o'lchaydi: media soni uch
         * barobar oshsa, so'rovlar soni O'ZGARMASLIGI kerak.
         */
        @Test
        @DisplayName("So'rovlar soni media soniga BOG'LIQ EMAS — N+1 yo'q")
        void queryCountDoesNotGrowWithMediaCount() throws Exception {
            int withFour = jobQueriesForListing(4);
            int withTwelve = jobQueriesForListing(8);   // jami 12 ta

            assertThat(withTwelve)
                    .as("media 4 → 12 ga o'sdi, so'rovlar %d → %d — N+1",
                            withFour, withTwelve)
                    .isEqualTo(withFour);
        }

        /** Berilgancha media qo'shib, ro'yxatni so'raydi va so'rovlarni sanaydi. */
        private int jobQueriesForListing(int extraMedia) throws Exception {
            for (int i = 0; i < extraMedia; i++) {
                jobService.enqueue(media(MediaType.VIDEO));
            }

            CapturingStatementInspector.clear();

            mockMvc.perform(get("/api/v1/app/admin/media?size=40")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // ⚠️ Yagona ishonchli dalil — yuborilgan SQL'ning O'ZI.
            // Statistika hisoblagichlari yolg'on tinchlik berardi: agar
            // yozuvlar o'sha tranzaksiyada yaratilgan bo'lsa, ular
            // kontekstda turadi va "yuklash" hisoblanmaydi.
            return CapturingStatementInspector.selectsFrom("cms_transcoding_job").size();
        }

        @Test
        @DisplayName("Video uchun `transcoding` obyekti keladi")
        void videoHasTranscodingBlock() throws Exception {
            MediaAsset video = media(MediaType.VIDEO);
            jobService.enqueue(video);

            mockMvc.perform(get("/api/v1/app/admin/media/" + video.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transcoding.status").value("QUEUED"))
                    .andExpect(jsonPath("$.transcoding.progress").value(0))
                    .andExpect(jsonPath("$.transcoding.attempts").value(0))
                    // Navbatdagi ishni qayta urinib bo'lmaydi.
                    .andExpect(jsonPath("$.transcoding.retryable").value(false));
        }

        /**
         * ⚠️ «Ish yo'q» va «ish yiqilgan» BOSHQA narsa.
         *
         * Rasm uchun bo'sh obyekt qaytarilsa, panel unga holat
         * nishonini chizardi va admin «rasm qayta ishlanmoqda» degan
         * ma'nosiz holatni ko'rardi.
         */
        @Test
        @DisplayName("Rasm uchun `transcoding` maydoni UMUMAN yo'q")
        void imageHasNoTranscodingBlock() throws Exception {
            MediaAsset image = media(MediaType.IMAGE);

            mockMvc.perform(get("/api/v1/app/admin/media/" + image.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transcoding").doesNotExist());
        }

        @Test
        @DisplayName("Ishi yo'q eski video uchun ham maydon yo'q")
        void legacyVideoWithoutJob() throws Exception {
            // Transcoding joriy qilinishidan OLDIN yuklangan fayl.
            MediaAsset old = media(MediaType.VIDEO);

            mockMvc.perform(get("/api/v1/app/admin/media/" + old.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transcoding").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Qayta urinish")
    class Retry {

        @Test
        @DisplayName("Yiqilgan ish navbatga QAYTADI va hisob nolga tushadi")
        void failedJobCanBeRetried() throws Exception {
            MediaAsset video = media(MediaType.VIDEO);
            jobService.enqueue(video);

            // Uch marta yiqitamiz → FAILED.
            for (int i = 0; i < 3; i++) {
                jobService.fail(jobService.claimNext().orElseThrow().getId(), "ffmpeg xatosi");
            }

            mockMvc.perform(post("/api/v1/app/admin/media/" + video.getId() + "/retry-transcoding")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transcoding.status").value("QUEUED"))
                    .andExpect(jsonPath("$.transcoding.attempts").value(0))
                    // Eski xato tozalanadi — aks holda admin uni yangi
                    // nosozlik deb o'ylardi.
                    .andExpect(jsonPath("$.transcoding.error").doesNotExist());
        }

        /**
         * ⚠️ Ishlab turgan ishni navbatga qaytarish IKKITA FFmpeg ni
         * bitta media ustida ishlatardi: ular bir xil vaqtinchalik
         * papkaga yozib, bir-birining faylini buzardi.
         */
        @Test
        @DisplayName("Ishlab turgan ishni qayta urinib BO'LMAYDI")
        void runningJobCannotBeRetried() throws Exception {
            MediaAsset video = media(MediaType.VIDEO);
            jobService.enqueue(video);
            jobService.claimNext();   // PROBING

            mockMvc.perform(post("/api/v1/app/admin/media/" + video.getId() + "/retry-transcoding")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Ruxsatsiz xodim qayta urina OLMAYDI")
        void workerWithoutPermissionIsDenied() throws Exception {
            MediaAsset video = media(MediaType.VIDEO);
            jobService.enqueue(video);
            jobService.fail(jobService.claimNext().orElseThrow().getId(), "xato");

            mockMvc.perform(post("/api/v1/app/admin/media/" + video.getId() + "/retry-transcoding")
                            .header("Authorization", "Bearer " + tokenWithoutUpload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Autentifikatsiyasiz rad etiladi")
        void anonymousIsDenied() throws Exception {
            MediaAsset video = media(MediaType.VIDEO);

            mockMvc.perform(post("/api/v1/app/admin/media/" + video.getId() + "/retry-transcoding"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Navbat holati")
    class Queue {

        @Test
        @DisplayName("Navbatdagi va yiqilgan ishlar sanaladi")
        void countsAreReported() throws Exception {
            jobService.enqueue(media(MediaType.VIDEO));
            jobService.enqueue(media(MediaType.VIDEO));

            MediaAsset broken = media(MediaType.VIDEO);
            jobService.enqueue(broken);
            for (int i = 0; i < 3; i++) {
                jobService.fail(jobService.claimNext().orElseThrow().getId(), "xato");
            }

            mockMvc.perform(get("/api/v1/app/admin/media/transcoding-queue")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.failed").value(1))
                    // Ikkitasi hali navbatda.
                    .andExpect(jsonPath("$.queued").value(2))
                    .andExpect(jsonPath("$.active").value(true));
        }

        /**
         * ⚠️ Panel shu bayroq bo'yicha davriy yangilashni TO'XTATADI.
         * Doimiy so'rov ochiq turgan panel serverga bekorga yuk
         * berardi.
         */
        @Test
        @DisplayName("Ish qolmasa `active` FALSE bo'ladi")
        void inactiveWhenQueueIsEmpty() throws Exception {
            mockMvc.perform(get("/api/v1/app/admin/media/transcoding-queue")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.queued").value(0))
                    .andExpect(jsonPath("$.running").value(0))
                    .andExpect(jsonPath("$.active").value(false));
        }
    }
}
