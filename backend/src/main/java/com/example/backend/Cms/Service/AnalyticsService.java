package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.AdDailyStatistic;
import com.example.backend.Cms.Entity.AnalyticsEvent;
import com.example.backend.Cms.Entity.ContentDailyStatistic;
import com.example.backend.Cms.Enums.AnalyticsEventType;
import com.example.backend.Cms.Repository.AdDailyStatisticRepo;
import com.example.backend.Cms.Repository.AnalyticsEventRepo;
import com.example.backend.Cms.Repository.ContentDailyStatisticRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Analitika: hodisalarni qabul qilish va kunlik agregatga aylantirish.
 *
 * <h2>Nega ikki bosqich</h2>
 * Klient yuborgan hodisa {@code cms_analytics_event} ga yoziladi — bu jadval
 * millionlab qatorga yetishi mumkin. Dashboard undan HECH QACHON o'qimaydi
 * (§29, §74): fon vazifasi hodisalarni kunlik jamlanmaga aylantiradi va
 * ko'rsatkichlar shundan olinadi.
 *
 * Agar agregat darhol (yozish paytida) yangilanganda, bir xil satrga ko'p
 * yozuv urilib, qulf raqobati yuzaga kelardi. Fon vazifasi bu muammoni
 * yo'q qiladi va unikal sonlarni ham to'g'ri hisoblash imkonini beradi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsEventRepo eventRepo;
    private final AdDailyStatisticRepo adStatRepo;
    private final ContentDailyStatisticRepo contentStatRepo;

    /**
     * Hodisani qabul qilish.
     *
     * Yengil operatsiya: faqat bitta INSERT, hech qanday hisob-kitob yo'q —
     * klient kutib qolmasin.
     */
    @Transactional
    public void record(AnalyticsEventType type, Long targetId, Long episodeId,
                       UUID userId, String deviceKey) {
        eventRepo.save(AnalyticsEvent.builder()
                .type(type)
                .targetId(targetId)
                .episodeId(episodeId)
                .userId(userId)
                .deviceKey(deviceKey)
                .build());
    }

    /**
     * Xom hodisalarni kunlik jamlanmaga qo'shadi.
     *
     * Har 5 daqiqada ishlaydi. Ko'rsatkichlar shu qadar kechikadi — bu
     * admin panel uchun mutlaqo yetarli va bazani tinch qoldiradi.
     *
     * @return agregatlangan hodisalar soni
     */
    @Scheduled(fixedDelayString = "${app.analytics.aggregate-delay-ms:300000}")
    @Transactional
    public int aggregate() {
        List<AnalyticsEventRepo.AggregateRow> rows = eventRepo.aggregateUnprocessed();
        if (rows.isEmpty()) {
            return 0;
        }

        for (AnalyticsEventRepo.AggregateRow row : rows) {
            if (row.getTargetId() == null) {
                continue; // nishonsiz hodisa (masalan NOTIFICATION_OPEN) — jamlanmaga tushmaydi
            }
            if (row.getType().isAdEvent()) {
                applyAdRow(row);
            } else if (row.getType().isContentEvent()) {
                applyContentRow(row);
            }
        }

        int processed = eventRepo.markAllProcessed();
        log.info("Analitika: {} ta hodisa {} ta jamlanma qatoriga qo'shildi",
                processed, rows.size());
        return processed;
    }

    private void applyAdRow(AnalyticsEventRepo.AggregateRow row) {
        AdDailyStatistic stat = adStatRepo
                .findByAdvertisementIdAndStatDate(row.getTargetId(), row.getDay())
                .orElseGet(() -> AdDailyStatistic.builder()
                        .advertisementId(row.getTargetId())
                        .statDate(row.getDay())
                        .build());

        long total = nz(row.getTotal());
        long uniques = nz(row.getUniques());

        // ⚠️ JAMI qo'shiladi, UNIKAL esa QAYTA HISOBLANADI.
        //
        // Agregatsiya har 5 daqiqada ishlaydi va faqat yangi hodisalarni
        // ko'radi. Unikal sanoq ham qo'shib borilsa, bir kunda reklamani
        // uch marta ko'rgan odam 3 ta «unikal» bo'lib hisoblanardi va
        // ko'rsatkich asta-sekin JAMI ga yaqinlashib, ma'nosini yo'qotardi.
        //
        // Shuning uchun unikal butun kun bo'yicha qaytadan sanaladi.
        long dayUniques = eventRepo.countUniquesForDay(
                row.getType(), row.getTargetId(), row.getDay());

        if (row.getType() == AnalyticsEventType.AD_IMPRESSION) {
            stat.setImpressions(nz(stat.getImpressions()) + total);
            stat.setUniqueImpressions(dayUniques);
        } else {
            stat.setClicks(nz(stat.getClicks()) + total);
            stat.setUniqueClicks(dayUniques);
        }
        adStatRepo.save(stat);
    }

    private void applyContentRow(AnalyticsEventRepo.AggregateRow row) {
        ContentDailyStatistic stat = contentStatRepo
                .findByContentIdAndStatDate(row.getTargetId(), row.getDay())
                .orElseGet(() -> ContentDailyStatistic.builder()
                        .contentId(row.getTargetId())
                        .statDate(row.getDay())
                        .build());

        long total = nz(row.getTotal());

        switch (row.getType()) {
            case CONTENT_VIEW -> {
                stat.setViews(nz(stat.getViews()) + total);
                // Reklama bilan bir xil sabab: unikal qo'shilmaydi, qayta
                // hisoblanadi. Aks holda bir soat ko'rgan foydalanuvchi
                // 12 ta «unikal tomoshabin» bo'lib chiqardi.
                stat.setUniqueViewers(eventRepo.countUniquesForDay(
                        row.getType(), row.getTargetId(), row.getDay()));
            }
            case CONTENT_PLAY -> stat.setPlays(nz(stat.getPlays()) + total);
            case CONTENT_COMPLETE -> stat.setCompletes(nz(stat.getCompletes()) + total);
            default -> {
                // boshqa turlar bu yerga tushmaydi
            }
        }
        contentStatRepo.save(stat);
    }

    @Transactional(readOnly = true)
    public long pendingEvents() {
        return eventRepo.countByProcessedFalse();
    }

    /** Davr bo'yicha kunlik qator — grafik uchun. */
    @Transactional(readOnly = true)
    public List<ContentDailyStatisticRepo.DailyPoint> dailySeries(LocalDate from, LocalDate to) {
        return contentStatRepo.dailySeries(from, to);
    }

    @Transactional(readOnly = true)
    public List<AdDailyStatisticRepo.AdTotals> adTotals(LocalDate from, LocalDate to) {
        return adStatRepo.totalsBetween(from, to);
    }

    @Transactional(readOnly = true)
    public List<ContentDailyStatisticRepo.ContentTotals> contentTotals(LocalDate from, LocalDate to) {
        return contentStatRepo.totalsBetween(from, to);
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }
}
