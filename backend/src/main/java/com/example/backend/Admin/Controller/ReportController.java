package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.Dto.ContentStatisticsDto;
import com.example.backend.Cms.Entity.ContentDailyStatistic;
import com.example.backend.Cms.Repository.AdDailyStatisticRepo;
import com.example.backend.Cms.Repository.ContentDailyStatisticRepo;
import com.example.backend.Cms.Repository.AdvertisementRepo;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Service.AnalyticsService;
import com.example.backend.Enums.Permission;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Hisobotlar (§45–47).
 *
 * Barcha ko'rsatkichlar KUNLIK JAMLANMADAN olinadi — xom hodisalar ustida
 * hech qanday {@code COUNT(*)} yo'q.
 *
 * ⚠️ Ma'lumot bo'lmasa nol qaytariladi, SOXTA raqam yasalmaydi (§45).
 *
 * <h2>Unikal sonlar semantikasi — muhim</h2>
 * {@code uniqueImpressions}, {@code uniqueClicks}, {@code uniqueViewers} —
 * bu <b>KUNLIK unikal sonlarning davr bo'yicha YIG'INDISI</b>, butun davr
 * bo'yicha {@code COUNT(DISTINCT)} EMAS.
 *
 * Ya'ni bitta foydalanuvchi 30 kun davomida har kuni kirsa, u 30 marta
 * sanaladi. Butun davr bo'yicha haqiqiy unikal sonni olish uchun xom
 * hodisalarni skanerlash kerak bo'lardi — bu esa aynan biz qochayotgan
 * qimmat operatsiya.
 *
 * Bu kunlik jamlanma yondashuvining tabiiy cheklovi va sanoatda odatiy
 * holat. Hisobotda shu tushuntirish ko'rsatiladi, aks holda raqam
 * chalg'ituvchi bo'lardi.
 */
@RestController
@RequestMapping("/api/v1/app/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AnalyticsService analyticsService;
    private final com.example.backend.Cms.Repository.ContentDailyStatisticRepo contentStatRepo;
    private final AdvertisementRepo advertisementRepo;
    private final ContentRepo contentRepo;
    private final PermissionService permissionService;

    private void require(Permission permission) {
        if (!permissionService.hasPermission(CurrentUser.get(), permission)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
        }
    }

    /**
     * Davr chegaralari.
     *
     * Nomlangan oraliqlar (§47): today, yesterday, last7, last30.
     * Aniq sanalar berilsa — o'shalar ishlatiladi.
     */
    private LocalDate[] range(String period, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        if (from != null && to != null) {
            if (to.isBefore(from)) {
                throw BusinessException.validation("Tugash sanasi boshlanishdan oldin bo'lishi mumkin emas");
            }
            return new LocalDate[]{from, to};
        }
        return switch (period == null ? "last30" : period) {
            case "today" -> new LocalDate[]{today, today};
            case "yesterday" -> new LocalDate[]{today.minusDays(1), today.minusDays(1)};
            case "last7" -> new LocalDate[]{today.minusDays(6), today};
            default -> new LocalDate[]{today.minusDays(29), today};
        };
    }

    /**
     * Bitta kontentning statistikasi (ТЗ §46).
     *
     * <h2>Nima uchun alohida endpoint</h2>
     * Umumiy hisobotda faqat top-10 chiqadi. 200 ta filmi bor admin
     * 150-chisining raqamlarini umuman ko'ra olmasdi. Reklamada bu
     * bo'shliq §29 da tuzatilgan, kontentda esa qolib ketgan edi.
     *
     * <h2>Nima uchun tez</h2>
     * Ma'lumot kunlik jamlanmadan olinadi — millionlab xom hodisa
     * skanerlanmaydi.
     */
    @GetMapping("/content/{id}/statistics")
    public ResponseEntity<ContentStatisticsDto> contentStatistics(
            @PathVariable Long id,
            @RequestParam(required = false) Integer days) {

        require(Permission.REPORT_VIEW);
        int window = (days == null || days <= 0) ? 30 : Math.min(days, 365);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(window - 1L);

        List<ContentDailyStatistic> daily = contentStatRepo
                .findAllByContentIdAndStatDateBetweenOrderByStatDateAsc(id, from, to);

        long views = daily.stream().mapToLong(d -> z(d.getViews())).sum();
        long plays = daily.stream().mapToLong(d -> z(d.getPlays())).sum();
        long completes = daily.stream().mapToLong(d -> z(d.getCompletes())).sum();
        long uniques = daily.stream().mapToLong(d -> z(d.getUniqueViewers())).sum();

        return ResponseEntity.ok(ContentStatisticsDto.builder()
                .contentId(id)
                .from(from)
                .to(to)
                .views(views)
                .plays(plays)
                .completes(completes)
                .uniqueViewers(uniques)
                // ⚠️ Bo'luvchi nol bo'lsa nol qaytariladi — bo'linish
                // xatosi butun hisobotni yiqitardi.
                .playRate(percent(plays, views))
                .completionRate(percent(completes, plays))
                .daily(daily.stream().map(d -> ContentStatisticsDto.DayRow.builder()
                        .date(d.getStatDate())
                        .views(z(d.getViews()))
                        .plays(z(d.getPlays()))
                        .completes(z(d.getCompletes()))
                        .uniqueViewers(z(d.getUniqueViewers()))
                        .completionRate(d.completionRate())
                        .build()).toList())
                .build());
    }

    private static long z(Long value) {
        return value == null ? 0L : value;
    }

    /** Foizda, ikki xonagacha. Bo'luvchi nol bo'lsa — nol. */
    private static double percent(long part, long whole) {
        return whole == 0 ? 0d : Math.round(part * 10000.0 / whole) / 100.0;
    }

    @GetMapping("/overview")
    public ResponseEntity<ReportOverview> overview(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {

        require(Permission.REPORT_VIEW);
        LocalDate[] r = range(period, from, to);

        var series = analyticsService.dailySeries(r[0], r[1]);
        var adTotals = analyticsService.adTotals(r[0], r[1]);
        var contentTotals = analyticsService.contentTotals(r[0], r[1]);

        long views = series.stream().mapToLong(p -> nz(p.getViews())).sum();
        long plays = series.stream().mapToLong(p -> nz(p.getPlays())).sum();
        long completes = series.stream().mapToLong(p -> nz(p.getCompletes())).sum();
        long impressions = adTotals.stream().mapToLong(a -> nz(a.getImpressions())).sum();
        long clicks = adTotals.stream().mapToLong(a -> nz(a.getClicks())).sum();

        // Nomlarni bitta so'rovda olamiz — har qator uchun alohida so'rov emas (§66)
        Map<Long, String> adNames = advertisementRepo.findAllByOrderBySortOrderAscIdAsc()
                .stream().collect(Collectors.toMap(a -> a.getId(), a -> a.getName(), (a, b) -> a));
        Map<Long, String> contentNames = contentRepo.findAll()
                .stream().collect(Collectors.toMap(c -> c.getId(), c -> c.getSlug(), (a, b) -> a));

        return ResponseEntity.ok(ReportOverview.builder()
                .from(r[0]).to(r[1])
                .totalViews(views).totalPlays(plays).totalCompletes(completes)
                .completionRate(plays == 0 ? 0d : completes * 100d / plays)
                .adImpressions(impressions).adClicks(clicks)
                .adCtr(impressions == 0 ? 0d : clicks * 100d / impressions)
                .pendingEvents(analyticsService.pendingEvents())
                .series(series.stream().map(p -> DailyPoint.builder()
                        .day(p.getDay()).views(nz(p.getViews()))
                        .plays(nz(p.getPlays())).completes(nz(p.getCompletes()))
                        .build()).toList())
                .topContent(contentTotals.stream().limit(10).map(c -> ContentRow.builder()
                        .contentId(c.getContentId())
                        .slug(contentNames.getOrDefault(c.getContentId(), "#" + c.getContentId()))
                        .views(nz(c.getViews())).plays(nz(c.getPlays()))
                        .completes(nz(c.getCompletes())).uniqueViewers(nz(c.getUniqueViewers()))
                        .build()).toList())
                .topAds(adTotals.stream().limit(10).map(a -> AdRow.builder()
                        .advertisementId(a.getAdvertisementId())
                        .name(adNames.getOrDefault(a.getAdvertisementId(), "#" + a.getAdvertisementId()))
                        .impressions(nz(a.getImpressions())).clicks(nz(a.getClicks()))
                        .uniqueImpressions(nz(a.getUniqueImpressions()))
                        .uniqueClicks(nz(a.getUniqueClicks()))
                        .ctr(nz(a.getImpressions()) == 0 ? 0d
                                : nz(a.getClicks()) * 100d / nz(a.getImpressions()))
                        .build()).toList())
                .build());
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    @Data
    @Builder
    public static class ReportOverview {
        private LocalDate from;
        private LocalDate to;
        private Long totalViews;
        private Long totalPlays;
        private Long totalCompletes;
        private Double completionRate;
        private Long adImpressions;
        private Long adClicks;
        private Double adCtr;
        /** Hali jamlanmaga qo'shilmagan hodisalar — ko'rsatkich kechikishi ko'rinsin. */
        private Long pendingEvents;
        private List<DailyPoint> series;
        private List<ContentRow> topContent;
        private List<AdRow> topAds;
    }

    @Data
    @Builder
    public static class DailyPoint {
        private LocalDate day;
        private Long views;
        private Long plays;
        private Long completes;
    }

    @Data
    @Builder
    public static class ContentRow {
        private Long contentId;
        private String slug;
        private Long views;
        private Long plays;
        private Long completes;
        private Long uniqueViewers;
    }

    @Data
    @Builder
    public static class AdRow {
        private Long advertisementId;
        private String name;
        private Long impressions;
        private Long clicks;
        private Long uniqueImpressions;
        private Long uniqueClicks;
        private Double ctr;
    }
}
