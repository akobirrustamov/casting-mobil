package com.example.backend.Cms.Service.Video;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.TranscodingJob;
import com.example.backend.Cms.Enums.VideoProcessingStatus;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Repository.TranscodingJobRepo;
import com.example.backend.Cms.Service.StorageService;
import com.example.backend.Cms.Service.Storage.StorageKeys;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Transcoding ishlarini bajaradi.
 *
 * <h2>Zanjir</h2>
 * <pre>
 *   navbatdan ol → ombordan yuklab ol → ffprobe → profil tanla
 *   → ffmpeg → HLS'ni omborga yukla → READY
 *                                      ↓ har qanday xatoda
 *                                    FAILED / navbatga qaytish
 *   va HAR QANDAY holatda → vaqtinchalik fayllarni o'chir
 * </pre>
 *
 * <h2>⚠️ Nega {@code @Scheduled}, navbat brokeri emas</h2>
 * Loyihada RabbitMQ ham, Redis ham yo'q va Docker ham yo'q. Yangi
 * infratuzilma qo'shish yangi nosozlik nuqtasi degani. Baza jadvali
 * esa qayta ishga tushirishdan omon qoladi, holatni tabiiy saqlaydi
 * va admin panel uni oddiy so'rov bilan ko'ra oladi.
 *
 * Kelajakda broker kerak bo'lsa, u faqat {@code claimNext} o'rnini
 * egallaydi — bu klassning qolgan mantig'i o'zgarmaydi.
 */
/*
 * ⚠️ Testlarda O'CHIRILADI (`application-test.properties`).
 *
 * Yoqiq qolsa har test konteksti navbatni tekshirib, u yerdagi
 * ishlarni haqiqiy FFmpeg bilan bajarishga urinardi. FFmpeg CI da
 * o'rnatilmagan, ya'ni ishlar bekorga FAILED bo'lardi va testlar
 * bir-biriga aralashardi.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.video", name = "worker-enabled",
        havingValue = "true", matchIfMissing = true)
public class TranscodeWorker {

    private final TranscodingJobService jobs;
    private final TranscodingJobRepo jobRepo;
    private final MediaAssetRepo mediaAssetRepo;
    private final StorageService storageService;
    private final VideoProbeService probeService;
    private final VideoProfileSelector profileSelector;
    private final HlsTranscodingService transcodingService;
    private final HlsUploadService uploadService;

    private final Path tempRoot;
    private final ExecutorService executor;
    private final Semaphore slots;

    /**
     * @param maxConcurrentJobs bir vaqtda nechta video.
     *
     * ⚠️ Sukut qiymat — BITTA. FFmpeg butun protsessorni egallaydi va
     * u API server bilan AYNI mashinada turibdi (Docker yo'q, ajratish
     * yo'q). Ikkitasi API ni sezilarli sekinlashtiradi.
     */
    public TranscodeWorker(TranscodingJobService jobs,
                           TranscodingJobRepo jobRepo,
                           MediaAssetRepo mediaAssetRepo,
                           StorageService storageService,
                           VideoProbeService probeService,
                           VideoProfileSelector profileSelector,
                           HlsTranscodingService transcodingService,
                           HlsUploadService uploadService,
                           @Value("${app.video.max-concurrent-jobs:1}") int maxConcurrentJobs,
                           @Value("${app.video.temp-dir:backend/files/.transcoding}") String tempDir) {
        this.jobs = jobs;
        this.jobRepo = jobRepo;
        this.mediaAssetRepo = mediaAssetRepo;
        this.storageService = storageService;
        this.probeService = probeService;
        this.profileSelector = profileSelector;
        this.transcodingService = transcodingService;
        this.uploadService = uploadService;
        this.tempRoot = Paths.get(tempDir);

        int limit = Math.max(1, maxConcurrentJobs);
        this.slots = new Semaphore(limit);
        this.executor = Executors.newFixedThreadPool(limit, runnable -> {
            Thread thread = new Thread(runnable, "transcode-worker");
            // ⚠️ Demon EMAS: ishlab turgan FFmpeg to'satdan uzilmasin.
            // To'xtatish `@PreDestroy` da boshqariladi.
            thread.setDaemon(false);
            return thread;
        });
    }

    // ------------------------------------------------------ tiklash

    /**
     * Yarim qolgan ishlarni navbatga qaytaradi.
     *
     * <h2>⚠️ Nega kerak</h2>
     * Server transcoding paytida qayta ishga tushsa, ish
     * {@code TRANSCODING} holatida MUZLAB qolardi: uni hech kim
     * olmaydi (navbat faqat {@code QUEUED} ni ko'radi) va hech kim
     * tugatmaydi.
     *
     * Admin panelda u abadiy «bajarilmoqda» bo'lib turardi va bu
     * jimgina nosozlikning eng yomon turi — hech qanday xato yo'q,
     * shunchaki hech qachon tugamaydi.
     */
    @jakarta.annotation.PostConstruct
    @Transactional
    public void requeueInterrupted() {
        List<VideoProcessingStatus> running = List.of(
                VideoProcessingStatus.PROBING,
                VideoProcessingStatus.TRANSCODING,
                VideoProcessingStatus.UPLOADING);

        List<TranscodingJob> stuck = jobRepo.findAll().stream()
                .filter(job -> running.contains(job.getStatus()))
                .toList();

        for (TranscodingJob job : stuck) {
            // ⚠️ `attempts` KAMAYTIRILMAYDI: qayta ishga tushish
            // sababi transcoding'ning o'zi bo'lishi mumkin (xotira
            // tugashi). Cheksiz aylanishga yo'l qo'ymaymiz.
            job.moveTo(VideoProcessingStatus.QUEUED, null);
            jobRepo.save(job);
        }

        if (!stuck.isEmpty()) {
            log.warn("Uzilib qolgan {} ta transcoding ishi navbatga qaytarildi", stuck.size());
        }
    }

    // ------------------------------------------------------ navbat

    /**
     * Navbatni tekshiradi.
     *
     * ⚠️ Ishning O'ZI alohida ipda bajariladi. Rejalashtiruvchining
     * ipida bajarilsa, u o'nlab daqiqa band bo'lardi va boshqa barcha
     * rejalashtirilgan vazifalar ({@code NotificationDispatcher},
     * {@code AnalyticsService}) to'xtab qolardi.
     */
    @Scheduled(fixedDelayString = "${app.video.poll-delay-ms:15000}")
    public void pollQueue() {
        // Bo'sh joy bo'lmasa navbatga umuman qaralmaydi: ish olinib,
        // keyin bajarilmay qolsa u `PROBING` da muzlab qolardi.
        if (!slots.tryAcquire()) {
            return;
        }

        Optional<TranscodingJob> claimed;
        try {
            claimed = jobs.claimNext();
        } catch (RuntimeException e) {
            slots.release();
            log.error("Navbatdan ish olinmadi", e);
            return;
        }

        if (claimed.isEmpty()) {
            slots.release();
            return;
        }

        TranscodingJob job = claimed.get();
        executor.submit(() -> {
            try {
                process(job);
            } finally {
                slots.release();
            }
        });
    }

    // ------------------------------------------------------ bajarish

    /**
     * Bitta ishni boshidan oxirigacha.
     *
     * ⚠️ Bu metod {@code @Transactional} EMAS va bo'lmasligi kerak:
     * u o'nlab daqiqa ishlaydi va baza ulanishini shuncha vaqt band
     * qilardi. Holat o'zgarishlari alohida qisqa tranzaksiyalarda
     * yoziladi ({@code TranscodingJobService}).
     */
    void process(TranscodingJob job) {
        Long jobId = job.getId();
        Long mediaId = job.getMedia().getId();
        Path workDir = tempRoot.resolve(String.valueOf(mediaId));

        try {
            MediaAsset media = mediaAssetRepo.findById(mediaId)
                    .orElseThrow(() -> new VideoProcessingException(
                            "Media topilmadi: " + mediaId));

            Files.createDirectories(workDir);

            // 1. Ombordan yuklab olamiz.
            Path source = download(media, workDir);

            // 2. Tekshiramiz.
            VideoMetadata metadata = probeService.probe(source);
            saveMetadata(media, metadata);

            // 3. Variantlarni tanlaymiz.
            var profiles = profileSelector.select(metadata);

            // 4. Transcoding.
            jobs.moveTo(jobId, VideoProcessingStatus.TRANSCODING);
            Path outputDir = workDir.resolve("out");
            Files.createDirectories(outputDir);
            transcodingService.transcode(source, outputDir, profiles, metadata,
                    percent -> jobs.updateProgress(jobId, percent));

            // 5. Omborga yuklaymiz.
            jobs.moveTo(jobId, VideoProcessingStatus.UPLOADING);
            String masterKey = uploadService.upload(outputDir, mediaId);

            // 6. ⚠️ `hlsMasterKey` FAQAT shu yerda yoziladi — hamma
            // narsa omborga tushgandan KEYIN. Ilgariroq yozilsa
            // pleyer mavjud bo'lmagan fayllarni so'rardi.
            media.setHlsMasterKey(masterKey);
            mediaAssetRepo.save(media);

            jobs.moveTo(jobId, VideoProcessingStatus.READY);
            log.info("Transcoding tugadi: media={} variant={}", mediaId, profiles.size());

        } catch (Exception e) {
            log.error("Transcoding yiqildi: media={}", mediaId, e);
            jobs.fail(jobId, e.getMessage());

        } finally {
            // ⚠️ HAR QANDAY holatda tozalanadi (§16).
            //
            // Tozalanmasa disk to'lardi: bitta ikki soatlik film uchun
            // manba + uchta variant o'nlab gigabayt egallaydi, va bu
            // har yuklashda takrorlanardi.
            deleteRecursively(workDir);
        }
    }

    // --------------------------------------------------------- ichki qism

    /**
     * Manbani omborga qaytib-qaytib murojaat qilmaslik uchun
     * lokal diskka tushiradi.
     *
     * ⚠️ FFmpeg faylga TASODIFIY joydan murojaat qiladi (indeks
     * odatda faylning oxirida). Uni to'g'ridan-to'g'ri S3 dan o'qish
     * har seek uchun yangi HTTP so'rov degani — transcoding bir necha
     * barobar sekinlashardi.
     */
    private Path download(MediaAsset media, Path workDir) throws IOException {
        String extension = StorageKeys.extensionOf(media.getStorageKey());
        Path target = workDir.resolve("source" + (extension.isEmpty() ? "" : "." + extension));

        Resource resource = storageService.load(media.getStorageKey());
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        if (Files.size(target) == 0) {
            throw new VideoProcessingException("Manba fayl bo'sh: " + media.getStorageKey());
        }
        return target;
    }

    /**
     * {@code ffprobe} natijasini media yozuviga yozadi.
     *
     * Bu maydonlar ({@code width}, {@code height},
     * {@code durationSeconds}) allaqachon mavjud edi, lekin ularni
     * o'lchaydigan narsa yo'q edi va ular doim {@code null} turardi.
     */
    @Transactional
    void saveMetadata(MediaAsset media, VideoMetadata metadata) {
        media.setWidth(metadata.width());
        media.setHeight(metadata.height());
        media.setVideoCodec(metadata.videoCodec());
        media.setAudioCodec(metadata.audioCodec());
        if (metadata.durationSeconds() != null) {
            // ⚠️ null bo'lsa mavjud qiymat SAQLANADI: uni admin qo'lda
            // kiritган bo'lishi mumkin va uni o'chirish yo'qotish
            // bo'lardi.
            media.setDurationSeconds(metadata.durationSeconds());
        }
        mediaAssetRepo.save(media);
    }

    /** Papkani ichidagilari bilan o'chiradi. Xato bo'lsa ham yiqilmaydi. */
    private void deleteRecursively(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("O'chirib bo'lmadi: {}", path);
                }
            });
        } catch (IOException e) {
            // Tozalash muvaffaqiyatsizligi ishni yiqitmasligi kerak —
            // u allaqachon tugagan.
            log.warn("Vaqtinchalik papka tozalanmadi: {}", dir, e);
        }
    }

    /**
     * ⚠️ To'xtatishda ishlab turgan FFmpeg tugashiga imkon beriladi.
     *
     * Darhol uzilsa yarim yozilgan HLS papkada qolardi va keyingi
     * urinish uni to'liq deb qabul qilishi mumkin edi.
     */
    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Transcoding to'xtatilmadi — majburiy uziladi");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
