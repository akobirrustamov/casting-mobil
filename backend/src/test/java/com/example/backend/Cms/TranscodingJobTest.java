package com.example.backend.Cms;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.TranscodingJob;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.VideoProcessingStatus;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Repository.TranscodingJobRepo;
import com.example.backend.Cms.Service.Video.TranscodingJobService;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Transcoding navbati: yaratish, olish, yiqilish, qayta urinish.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * <ul>
 *   <li>navbatga faqat VIDEO tushadi — rasm uchun ish yaratilsa navbat
 *       bekorga to'lardi;</li>
 *   <li>vaqtinchalik nosozlikdan keyin ish NAVBATGA QAYTADI, lekin
 *       cheksiz emas — buzuq fayl navbatni abadiy band qilmasin;</li>
 *   <li>qayta urinishda hisob NOLGA tushadi, aks holda tugma foydasiz
 *       bo'lardi;</li>
 *   <li>xato matni muvaffaqiyatdan keyin TOZALANADI.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.video.max-attempts=3")
class TranscodingJobTest {

    @Autowired
    private TranscodingJobService jobService;

    @Autowired
    private TranscodingJobRepo jobRepo;

    @Autowired
    private MediaAssetRepo mediaAssetRepo;

    /**
     * ⚠️ Navbat HAR TEST oldidan tozalanadi.
     *
     * Baza test YURISHI boshida bir marta quriladi ({@code TestDatabaseReset}),
     * har test oldidan emas. {@code claimNext()} esa
     * {@code REQUIRES_NEW} bilan ishlaydi va test tranzaksiyasidan
     * chiqib COMMIT qiladi — ya'ni oldingi testlarning ishlari
     * navbatda qolib, «navbat bo'sh» degan tekshiruvlarni buzardi.
     *
     * Faqat ishlar o'chiriladi, medialar emas: ular boshqa testlarga
     * kerak bo'lishi mumkin.
     */
    @BeforeEach
    void clearQueue() {
        jobRepo.deleteAll();
    }

    private MediaAsset media(MediaType type) {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/content/" + UUID.randomUUID() + ".mp4")
                .originalFilename("kino.mp4")
                .type(type)
                .mimeType("video/mp4")
                .sizeBytes(1000L)
                .status(MediaStatus.READY)
                .createdBy(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Nested
    @DisplayName("Navbatga qo'shish")
    class Enqueue {

        @Test
        @DisplayName("Video navbatga tushadi")
        void videoIsQueued() {
            MediaAsset asset = media(MediaType.VIDEO);

            TranscodingJob job = jobService.enqueue(asset).orElseThrow();

            assertThat(job.getStatus()).isEqualTo(VideoProcessingStatus.QUEUED);
            assertThat(job.getAttempts()).isZero();
            assertThat(job.getProgress()).isZero();
        }

        @Test
        @DisplayName("Rasm va hujjat uchun ish YARATILMAYDI")
        void nonVideoIsIgnored() {
            // ⚠️ Aks holda har bir yuklangan afisha navbatni bekorga
            // to'ldirardi va worker ularni birma-bir «transcoding»
            // qilishga urinardi.
            assertThat(jobService.enqueue(media(MediaType.IMAGE))).isEmpty();
            assertThat(jobService.enqueue(media(MediaType.DOCUMENT))).isEmpty();
            assertThat(jobService.enqueue(null)).isEmpty();
        }

        @Test
        @DisplayName("Takroriy chaqiruv IKKINCHI ish yaratmaydi")
        void enqueueIsIdempotent() {
            MediaAsset asset = media(MediaType.VIDEO);

            Long first = jobService.enqueue(asset).orElseThrow().getId();
            Long second = jobService.enqueue(asset).orElseThrow().getId();

            assertThat(second).isEqualTo(first);
        }
    }

    @Nested
    @DisplayName("Navbatdan olish")
    class Claim {

        @Test
        @DisplayName("Olingan ish darhol BAND bo'ladi va urinish sanaladi")
        void claimMarksInProgress() {
            jobService.enqueue(media(MediaType.VIDEO));

            TranscodingJob claimed = jobService.claimNext().orElseThrow();

            // ⚠️ Holat darhol o'zgarishi SHART: aks holda keyingi
            // so'rovchi ayni ishni qayta olardi.
            assertThat(claimed.getStatus()).isEqualTo(VideoProcessingStatus.PROBING);
            assertThat(claimed.getAttempts()).isEqualTo(1);
            assertThat(claimed.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("Ayni ish IKKI marta olinmaydi")
        void sameJobIsNotClaimedTwice() {
            jobService.enqueue(media(MediaType.VIDEO));

            Long first = jobService.claimNext().orElseThrow().getId();
            // Navbatda boshqa ish yo'q — ikkinchi chaqiruv bo'sh qaytadi.
            assertThat(jobService.claimNext()).isEmpty();
            assertThat(first).isNotNull();
        }

        @Test
        @DisplayName("Navbat bo'sh bo'lsa hech narsa qaytmaydi")
        void emptyQueueReturnsNothing() {
            assertThat(jobService.claimNext()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Yiqilish va qayta urinish")
    class Failure {

        @Test
        @DisplayName("Chegaraga yetmagan urinish NAVBATGA QAYTADI")
        void retriableFailureGoesBackToQueue() {
            jobService.enqueue(media(MediaType.VIDEO));
            TranscodingJob job = jobService.claimNext().orElseThrow();

            jobService.fail(job.getId(), "tarmoq uzildi");

            TranscodingJob after = jobRepo.findById(job.getId()).orElseThrow();
            // Vaqtinchalik nosozlik o'z-o'zidan tuzalishi mumkin.
            assertThat(after.getStatus()).isEqualTo(VideoProcessingStatus.QUEUED);
            // Sabab saqlanadi — keyingisi ham yiqilsa admin ko'ra oladi.
            assertThat(after.getError()).contains("tarmoq uzildi");
        }

        @Test
        @DisplayName("Chegaradan oshgach FAILED va navbatga qaytmaydi")
        void exhaustedAttemptsFailPermanently() {
            jobService.enqueue(media(MediaType.VIDEO));

            // ⚠️ Cheksiz qayta urinish buzuq faylni navbatda abadiy
            // aylantirardi va boshqa videolar hech qachon yetib
            // bormasdi.
            for (int i = 0; i < 3; i++) {
                TranscodingJob job = jobService.claimNext().orElseThrow();
                jobService.fail(job.getId(), "ffmpeg xatosi");
            }

            assertThat(jobService.claimNext()).isEmpty();

            TranscodingJob finished = jobRepo.findAll().get(0);
            assertThat(finished.getStatus()).isEqualTo(VideoProcessingStatus.FAILED);
            assertThat(finished.getError()).contains("ffmpeg xatosi");
            assertThat(finished.getFinishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Qayta urinish hisobni NOLGA tushiradi")
        void retryResetsAttempts() {
            jobService.enqueue(media(MediaType.VIDEO));
            for (int i = 0; i < 3; i++) {
                jobService.fail(jobService.claimNext().orElseThrow().getId(), "xato");
            }
            TranscodingJob failed = jobRepo.findAll().get(0);

            jobService.retry(failed.getMedia().getId());

            TranscodingJob after = jobRepo.findById(failed.getId()).orElseThrow();
            // ⚠️ Hisob tushirilmasa qayta urinish darhol yana FAILED
            // bo'lardi va tugma foydasiz ko'rinardi.
            assertThat(after.getAttempts()).isZero();
            assertThat(after.getStatus()).isEqualTo(VideoProcessingStatus.QUEUED);
            assertThat(after.getError()).isNull();
            assertThat(after.getFinishedAt()).isNull();
            // Va u haqiqatan qayta olinadi.
            assertThat(jobService.claimNext()).isPresent();
        }

        @Test
        @DisplayName("Tugamagan ishni qayta urinib bo'lmaydi")
        void cannotRetryRunningJob() {
            jobService.enqueue(media(MediaType.VIDEO));
            TranscodingJob job = jobService.claimNext().orElseThrow();

            assertThatThrownBy(() -> jobService.retry(job.getMedia().getId()))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Holat o'tishlari")
    class Transitions {

        @Test
        @DisplayName("READY xato matnini TOZALAYDI")
        void successClearsPreviousError() {
            jobService.enqueue(media(MediaType.VIDEO));
            TranscodingJob job = jobService.claimNext().orElseThrow();
            jobService.fail(job.getId(), "birinchi urinish yiqildi");

            TranscodingJob retried = jobService.claimNext().orElseThrow();
            jobService.moveTo(retried.getId(), VideoProcessingStatus.READY);

            TranscodingJob after = jobRepo.findById(job.getId()).orElseThrow();
            // ⚠️ Tozalanmasa admin muvaffaqiyatli videoda eski xatoni
            // ko'rib, uni yangi nosozlik deb o'ylardi.
            assertThat(after.getError()).isNull();
            assertThat(after.getProgress()).isEqualTo(100);
        }

        @Test
        @DisplayName("Progress 100 ga FAQAT READY da yetadi")
        void progressNeverReachesHundredBeforeReady() {
            jobService.enqueue(media(MediaType.VIDEO));
            TranscodingJob job = jobService.claimNext().orElseThrow();

            jobService.updateProgress(job.getId(), 100);

            // ⚠️ «Progress 100, lekin hali TRANSCODING» — admin uchun
            // chalkash holat. Tayyorlikni faqat `status` aytadi.
            assertThat(jobRepo.findById(job.getId()).orElseThrow().getProgress())
                    .isEqualTo(99);
        }

        @Test
        @DisplayName("Progress chegaradan chiqmaydi")
        void progressIsBounded() {
            jobService.enqueue(media(MediaType.VIDEO));
            TranscodingJob job = jobService.claimNext().orElseThrow();

            jobService.updateProgress(job.getId(), -5);
            assertThat(jobRepo.findById(job.getId()).orElseThrow().getProgress()).isZero();

            jobService.updateProgress(job.getId(), 500);
            assertThat(jobRepo.findById(job.getId()).orElseThrow().getProgress()).isEqualTo(99);
        }
    }

    @Nested
    @DisplayName("Sahifa uchun ishlar")
    class BatchLookup {

        @Test
        @DisplayName("Bir necha media uchun ishlar BIR so'rovda olinadi")
        void jobsAreLoadedInOneQuery() {
            MediaAsset first = media(MediaType.VIDEO);
            MediaAsset second = media(MediaType.VIDEO);
            jobService.enqueue(first);
            jobService.enqueue(second);

            var jobs = jobService.forMediaIds(List.of(first.getId(), second.getId()));

            assertThat(jobs).containsOnlyKeys(first.getId(), second.getId());
        }

        @Test
        @DisplayName("Bo'sh ro'yxat uchun so'rov umuman yuborilmaydi")
        void emptyListShortCircuits() {
            assertThat(jobService.forMediaIds(List.of())).isEmpty();
            assertThat(jobService.forMediaIds(null)).isEmpty();
        }
    }
}
