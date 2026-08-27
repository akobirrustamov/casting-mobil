package com.example.backend.Cms.Service.Video;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.TranscodingJob;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.VideoProcessingStatus;
import com.example.backend.Cms.Repository.TranscodingJobRepo;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transcoding navbatini boshqaradi.
 *
 * <h2>Vazifasi</h2>
 * Ishni yaratish, navbatdan olish, holatni yangilash va qayta urinish.
 * FFmpeg ning o'zi bu yerda emas — u {@code HlsTranscodingService} da.
 * Sabab: navbat mantig'i tranzaksiya bilan ishlaydi va tez tugaydi,
 * transcoding esa o'nlab daqiqa davom etadi. Ularni bitta tranzaksiyaga
 * qo'yish baza ulanishini shuncha vaqt band qilardi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodingJobService {

    private final TranscodingJobRepo jobRepo;

    /**
     * Nechta marta qayta urinamiz.
     *
     * ⚠️ Cheksiz bo'lsa buzuq fayl navbatni abadiy band qilardi:
     * har safar yiqilib, qayta olinardi va boshqa videolar hech
     * qachon navbatga yetib bormasdi.
     */
    @Value("${app.video.max-attempts:3}")
    private int maxAttempts;

    // ------------------------------------------------------------ yaratish

    /**
     * Yuklangan video uchun ish qo'shadi.
     *
     * ⚠️ Faqat VIDEO uchun. Rasm va hujjatga transcoding kerak emas va
     * ular uchun ish yaratilsa navbat bekorga to'lardi.
     *
     * Takroriy chaqiruv xavfsiz: ish allaqachon bo'lsa yangisi
     * yaratilmaydi (har media uchun bitta, {@code unique(media_id)}).
     */
    @Transactional
    public Optional<TranscodingJob> enqueue(MediaAsset media) {
        if (media == null || media.getType() != MediaType.VIDEO) {
            return Optional.empty();
        }

        Optional<TranscodingJob> existing = jobRepo.findByMediaId(media.getId());
        if (existing.isPresent()) {
            return existing;
        }

        TranscodingJob job = jobRepo.save(TranscodingJob.builder()
                .media(media)
                .status(VideoProcessingStatus.QUEUED)
                .build());

        log.info("Transcoding navbatga qo'shildi: media={}", media.getId());
        return Optional.of(job);
    }

    // ------------------------------------------------------------- navbat

    /**
     * Navbatdan keyingi ishni oladi va uni darhol band qiladi.
     *
     * <h2>⚠️ Nega alohida tranzaksiya</h2>
     * {@code REQUIRES_NEW} — ish olinishi va band qilinishi worker
     * uzoq ishlashidan OLDIN yozilishi kerak. Bitta tranzaksiyada
     * bo'lsa qulf transcoding tugagunicha ushlab turilardi (o'nlab
     * daqiqa) va boshqa hech kim navbatga qaray olmasdi.
     *
     * @return band qilingan ish yoki bo'sh, agar navbat quruq bo'lsa
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<TranscodingJob> claimNext() {
        List<TranscodingJob> next = jobRepo.lockNextPending(
                VideoProcessingStatus.QUEUED, PageRequest.of(0, 1));

        if (next.isEmpty()) {
            return Optional.empty();
        }

        TranscodingJob job = next.get(0);
        job.setAttempts(job.getAttempts() + 1);
        job.moveTo(VideoProcessingStatus.PROBING, null);
        return Optional.of(jobRepo.save(job));
    }

    // ------------------------------------------------------------- holat

    /** Bosqichni yangilaydi. Progress tegilmaydi. */
    @Transactional
    public void moveTo(Long jobId, VideoProcessingStatus status) {
        jobRepo.findById(jobId).ifPresent(job -> {
            job.moveTo(status, null);
            jobRepo.save(job);
        });
    }

    /**
     * Progressni yangilaydi.
     *
     * ⚠️ Alohida metod va ATAYLAB holatni o'zgartirmaydi: progress
     * transcoding davomida o'nlab marta yoziladi va har safar holat
     * o'tishini takrorlash keraksiz yozuv bo'lardi.
     */
    @Transactional
    public void updateProgress(Long jobId, int percent) {
        int bounded = Math.max(0, Math.min(100, percent));
        jobRepo.findById(jobId).ifPresent(job -> {
            // 100 ni faqat READY qo'yadi — aks holda «progress 100,
            // lekin hali TRANSCODING» degan chalkash holat chiqardi.
            job.setProgress(job.getStatus() == VideoProcessingStatus.READY ? 100 : Math.min(99, bounded));
            jobRepo.save(job);
        });
    }

    /**
     * Ish yiqildi.
     *
     * Urinishlar chegaradan oshmagan bo'lsa ish NAVBATGA QAYTADI —
     * vaqtinchalik nosozliklar (tarmoq, disk) o'z-o'zidan tuzalishi
     * mumkin.
     */
    @Transactional
    public void fail(Long jobId, String reason) {
        jobRepo.findById(jobId).ifPresent(job -> {
            if (job.getAttempts() < maxAttempts) {
                log.warn("Transcoding yiqildi, navbatga qaytarildi ({}/{}): media={}",
                        job.getAttempts(), maxAttempts, mediaIdOf(job));
                job.moveTo(VideoProcessingStatus.QUEUED, null);
                // ⚠️ Sabab QUEUED da ham saqlanadi: keyingi urinish ham
                // yiqilsa admin oldingi xatoni ko'ra oladi. `moveTo`
                // uni tozalagani uchun qo'lda qaytariladi.
                job.setError(reason == null ? null
                        : "Oldingi urinish: " + shorten(reason));
            } else {
                log.error("Transcoding butunlay yiqildi: media={} sabab={}",
                        mediaIdOf(job), shorten(reason));
                job.moveTo(VideoProcessingStatus.FAILED, reason);
            }
            jobRepo.save(job);
        });
    }

    /**
     * Admin qayta urinishni so'radi.
     *
     * ⚠️ Urinishlar hisobi NOLGA tushiriladi. Aks holda uch marta
     * yiqilgan ish qayta urinishda darhol yana {@code FAILED} bo'lardi
     * va tugma foydasiz ko'rinardi.
     */
    @Transactional
    public TranscodingJob retry(Long mediaId) {
        TranscodingJob job = jobRepo.findByMediaId(mediaId)
                .orElseThrow(() -> BusinessException.notFound("Transcoding ishi", mediaId));

        if (!job.getStatus().isFinished()) {
            throw BusinessException.validation(
                    "Ish hali tugamagan: " + job.getStatus());
        }

        job.setAttempts(0);
        job.setProgress(0);
        job.setStartedAt(null);
        job.setFinishedAt(null);
        job.moveTo(VideoProcessingStatus.QUEUED, null);

        log.info("Transcoding qayta urinish: media={}", mediaId);
        return jobRepo.save(job);
    }

    // ------------------------------------------------------------- o'qish

    public Optional<TranscodingJob> forMedia(Long mediaId) {
        return jobRepo.findByMediaId(mediaId);
    }

    /**
     * Sahifadagi barcha medialar uchun ishlar — BIR so'rovda.
     *
     * ⚠️ Har media uchun alohida so'rov N+1 bo'lardi: kutubxona 40 ta
     * element ko'rsatadi.
     */
    @Transactional(readOnly = true)
    public Map<Long, TranscodingJob> forMediaIds(List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Map.of();
        }
        return jobRepo.findAllByMediaIdIn(mediaIds).stream()
                .collect(Collectors.toMap(job -> job.getMedia().getId(), Function.identity()));
    }

    // --------------------------------------------------------- ichki qism

    /** Log uchun. Lazy bog'liqlikni ochmaydigan xavfsiz variant. */
    private Long mediaIdOf(TranscodingJob job) {
        return job.getMedia() == null ? null : job.getMedia().getId();
    }

    /** Log uchun qisqartirilgan sabab — ffmpeg ming qatorlik chiqish beradi. */
    private String shorten(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() <= 300 ? reason : reason.substring(0, 300) + "…";
    }
}
