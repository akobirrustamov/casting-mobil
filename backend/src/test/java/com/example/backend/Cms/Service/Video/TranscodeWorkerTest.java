package com.example.backend.Cms.Service.Video;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.TranscodingJob;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.VideoProcessingStatus;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Repository.TranscodingJobRepo;
import com.example.backend.Cms.Service.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Transcoding worker — butun zanjir.
 *
 * <h2>⚠️ FFmpeg va S3 mock qilingan</h2>
 * Ular bu mashinada yo'q (§30). Tekshiriladigan narsa ularning ishi
 * emas, BIZNING zanjirimiz: bosqichlar tartibi, xatolarni ushlash va
 * eng muhimi — vaqtinchalik fayllarni TOZALASH.
 */
class TranscodeWorkerTest {

    private TranscodingJobRepo jobRepo;
    private TranscodingJobService jobs;
    private MediaAssetRepo mediaAssetRepo;
    private StorageService storage;
    private VideoProbeService probe;
    private HlsTranscodingService transcoding;
    private HlsUploadService upload;

    private TranscodeWorker worker;
    private Path tempRoot;
    private MediaAsset media;
    private TranscodingJob job;

    @BeforeEach
    void setUp() throws Exception {
        tempRoot = Files.createTempDirectory("worker-test");

        jobRepo = mock(TranscodingJobRepo.class);
        jobs = mock(TranscodingJobService.class);
        mediaAssetRepo = mock(MediaAssetRepo.class);
        storage = mock(StorageService.class);
        probe = mock(VideoProbeService.class);
        transcoding = mock(HlsTranscodingService.class);
        upload = mock(HlsUploadService.class);

        media = MediaAsset.builder()
                .id(7L)
                .storageKey("/content/abc.mp4")
                .originalFilename("kino.mp4")
                .type(MediaType.VIDEO)
                .mimeType("video/mp4")
                .sizeBytes(1000L)
                .status(MediaStatus.READY)
                .createdBy(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();

        job = TranscodingJob.builder()
                .id(1L)
                .media(media)
                .status(VideoProcessingStatus.PROBING)
                .build();

        when(mediaAssetRepo.findById(7L)).thenReturn(Optional.of(media));
        when(mediaAssetRepo.save(any(MediaAsset.class))).thenAnswer(i -> i.getArgument(0));
        // Manba bo'sh bo'lmasligi kerak — worker buni tekshiradi.
        when(storage.load("/content/abc.mp4"))
                .thenReturn(new ByteArrayResource("video-bytes".getBytes()));
        when(probe.probe(any(Path.class)))
                .thenReturn(new VideoMetadata(1920, 1080, 3600, 25.0, "h264", "aac", 5_000_000L));
        when(transcoding.transcode(any(), any(), any(), any(), any()))
                .thenAnswer(i -> ((Path) i.getArgument(1)).resolve("master.m3u8"));
        when(upload.upload(any(Path.class), eq(7L)))
                .thenReturn("/videos/7/hls/master.m3u8");

        worker = new TranscodeWorker(jobs, jobRepo, mediaAssetRepo, storage, probe,
                new VideoProfileSelector(new VideoTranscodingProperties()),
                transcoding, upload, 1, tempRoot.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (Files.isDirectory(tempRoot)) {
            try (Stream<Path> walk = Files.walk(tempRoot)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                        // test tozalashi — ahamiyatsiz
                    }
                });
            }
        }
    }

    private boolean workDirExists() {
        return Files.exists(tempRoot.resolve("7"));
    }

    @Nested
    @DisplayName("Muvaffaqiyatli oqim")
    class HappyPath {

        @Test
        @DisplayName("Bosqichlar TARTIB bilan o'tadi va READY bilan tugaydi")
        void stagesRunInOrder() {
            worker.process(job);

            var order = org.mockito.Mockito.inOrder(jobs);
            order.verify(jobs).moveTo(1L, VideoProcessingStatus.TRANSCODING);
            order.verify(jobs).moveTo(1L, VideoProcessingStatus.UPLOADING);
            order.verify(jobs).moveTo(1L, VideoProcessingStatus.READY);
            verify(jobs, never()).fail(anyLong(), any());
        }

        @Test
        @DisplayName("ffprobe natijasi MEDIA yozuviga saqlanadi")
        void metadataIsPersisted() {
            worker.process(job);

            // Bu maydonlar allaqachon mavjud edi, lekin ularni
            // o'lchaydigan narsa yo'q edi va ular doim null turardi.
            assertThat(media.getWidth()).isEqualTo(1920);
            assertThat(media.getHeight()).isEqualTo(1080);
            assertThat(media.getDurationSeconds()).isEqualTo(3600);
            assertThat(media.getVideoCodec()).isEqualTo("h264");
            assertThat(media.getAudioCodec()).isEqualTo("aac");
        }

        /**
         * ⚠️ `hlsMasterKey` — bu «video tayyor» belgisi.
         *
         * Yuklashdan OLDIN yozilsa, pleyer mavjud bo'lmagan fayllarni
         * so'rardi: foydalanuvchi videoni ochib, darhol xato ko'rardi.
         */
        @Test
        @DisplayName("hlsMasterKey FAQAT yuklash tugagach yoziladi")
        void masterKeyIsWrittenAfterUpload() {
            worker.process(job);

            var order = org.mockito.Mockito.inOrder(upload, jobs);
            order.verify(upload).upload(any(Path.class), eq(7L));
            order.verify(jobs).moveTo(1L, VideoProcessingStatus.READY);

            assertThat(media.getHlsMasterKey()).isEqualTo("/videos/7/hls/master.m3u8");
        }

        @Test
        @DisplayName("Vaqtinchalik fayllar TOZALANADI")
        void tempFilesAreRemoved() {
            worker.process(job);

            // Bitta ikki soatlik film uchun manba + uchta variant
            // o'nlab gigabayt egallaydi — va bu har yuklashda
            // takrorlanardi.
            assertThat(workDirExists()).isFalse();
        }
    }

    @Nested
    @DisplayName("Yiqilish")
    class Failures {

        @Test
        @DisplayName("ffprobe yiqilsa ish FAILED ga o'tadi")
        void probeFailureIsReported() {
            when(probe.probe(any(Path.class)))
                    .thenThrow(new VideoProcessingException("ffprobe topilmadi"));

            worker.process(job);

            verify(jobs).fail(1L, "ffprobe topilmadi");
            verify(jobs, never()).moveTo(1L, VideoProcessingStatus.READY);
        }

        @Test
        @DisplayName("FFmpeg yiqilsa ham vaqtinchalik fayllar TOZALANADI")
        void tempFilesAreRemovedOnFailure() {
            // ⚠️ `doThrow(...).when(...)` shakli — `when(mock.method(…))`
            // qayta stub qilishda OLDINGI javobni chaqiradi va u null
            // argumentlar bilan yiqilardi.
            doThrow(new VideoProcessingException("FFmpeg xatosi"))
                    .when(transcoding).transcode(any(), any(), any(), any(), any());

            worker.process(job);

            // ⚠️ Eng muhim tekshiruv. Xatoda tozalanmasa disk asta-sekin
            // to'lardi va buni hech kim sezmasdi — har yiqilgan
            // transcoding o'z axlatini qoldirardi.
            assertThat(workDirExists()).isFalse();
            verify(jobs).fail(1L, "FFmpeg xatosi");
        }

        @Test
        @DisplayName("Yuklash yiqilsa hlsMasterKey YOZILMAYDI")
        void masterKeyStaysNullWhenUploadFails() {
            doThrow(new VideoProcessingException("S3 javob bermadi"))
                    .when(upload).upload(any(Path.class), anyLong());

            worker.process(job);

            // Yozilsa video READY bo'lmasa ham «tayyor» ko'rinardi.
            assertThat(media.getHlsMasterKey()).isNull();
            verify(jobs, never()).moveTo(1L, VideoProcessingStatus.READY);
        }

        @Test
        @DisplayName("Bo'sh manba fayl ANIQ xato beradi")
        void emptySourceIsRejected() {
            when(storage.load("/content/abc.mp4"))
                    .thenReturn(new ByteArrayResource(new byte[0]));

            worker.process(job);

            verify(jobs).fail(eq(1L), org.mockito.ArgumentMatchers.contains("bo'sh"));
            assertThat(workDirExists()).isFalse();
        }

        @Test
        @DisplayName("Media topilmasa zanjir boshlanmaydi")
        void missingMediaStopsEarly() {
            when(mediaAssetRepo.findById(7L)).thenReturn(Optional.empty());

            worker.process(job);

            verify(probe, never()).probe(any());
            verify(jobs).fail(eq(1L), org.mockito.ArgumentMatchers.contains("Media topilmadi"));
        }
    }

    @Nested
    @DisplayName("Navbat")
    class Queue {

        @Test
        @DisplayName("Navbat bo'sh bo'lsa hech narsa qilinmaydi")
        void emptyQueueDoesNothing() {
            when(jobs.claimNext()).thenReturn(Optional.empty());

            worker.pollQueue();

            verify(probe, never()).probe(any());
        }

        /**
         * ⚠️ Bo'sh joy bo'lmasa navbatga UMUMAN qaralmaydi.
         *
         * Ish olinib, keyin bajarilmay qolsa u `PROBING` holatida
         * muzlab qolardi: uni hech kim olmaydi (navbat faqat `QUEUED`
         * ni ko'radi) va hech kim tugatmaydi.
         */
        @Test
        @DisplayName("Bo'sh joy bo'lmasa ish OLINMAYDI")
        void noSlotMeansNoClaim() throws Exception {
            // Yagona joyni band qilamiz.
            var slotsField = TranscodeWorker.class.getDeclaredField("slots");
            slotsField.setAccessible(true);
            ((java.util.concurrent.Semaphore) slotsField.get(worker)).acquire();

            worker.pollQueue();

            verify(jobs, never()).claimNext();
        }

        @Test
        @DisplayName("Navbat xatosi joyni QAYTARADI")
        void slotIsReleasedOnQueueError() throws Exception {
            when(jobs.claimNext()).thenThrow(new RuntimeException("baza yo'q"));

            worker.pollQueue();

            var slotsField = TranscodeWorker.class.getDeclaredField("slots");
            slotsField.setAccessible(true);
            var semaphore = (java.util.concurrent.Semaphore) slotsField.get(worker);
            // Qaytarilmasa worker abadiy to'xtab qolardi — birinchi
            // baza uzilishidan keyin navbat hech qachon qayta
            // ishlamasdi.
            assertThat(semaphore.availablePermits()).isEqualTo(1);
        }
    }
}
