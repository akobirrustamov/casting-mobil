package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Repository.*;
import com.example.backend.Enums.Permission;
import com.example.backend.Cms.Service.AnalyticsService;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.CastingUserRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.RoleMapper;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.Cms.Enums.DonationTargetType;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dashboard uchun YAGONA endpoint.
 *
 * Bosh sahifa ochilganda 20 ta alohida so'rov yuborilmasligi kerak (§73) -
 * barcha ko'rsatkichlar shu yerdan bitta javobda keladi.
 *
 * ⚠️ Hozircha ko'rsatkichlar mavjud jadvallardan hisoblanadi. Reklama, obuna,
 * donat va daromad modullari hali yo'q - ular uchun SOXTA raqam qaytarilmaydi
 * (§45), maydonlar null bo'ladi va frontend "ma'lumot yo'q" holatini ko'rsatadi.
 */
@RestController
@RequestMapping("/api/v1/app/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ContentRepo contentRepo;
    private final EpisodeRepo episodeRepo;
    private final CreatorRepo creatorRepo;
    private final CategoryRepo categoryRepo;
    private final MediaAssetRepo mediaAssetRepo;
    private final UserRepo userRepo;
    private final CastingUserRepo castingUserRepo;
    private final UserAccountRepo userAccountRepo;
    private final SubscriptionRepo subscriptionRepo;
    private final CommentRepo commentRepo;
    private final com.example.backend.Cms.Repository.PurchaseRepo purchaseRepo;
    private final com.example.backend.Cms.Repository.AdvertisementRepo advertisementRepo;
    private final com.example.backend.Cms.Repository.NotificationRepo notificationRepo;
    private final com.example.backend.Cms.Repository.DonationRepo donationRepo;
    private final AnalyticsService analyticsService;
    private final PermissionService permissionService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> summary() {
        if (!permissionService.hasPermission(CurrentUser.get(), Permission.CONTENT_VIEW)) {
            throw BusinessException.accessDenied("Dashboard uchun ruxsat yo'q");
        }

        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(29);

        // Reklama ko'rsatkichlari KUNLIK JAMLANMADAN — xom hodisalar ustida COUNT(*) yo'q
        var adTotals = analyticsService.adTotals(monthAgo, today);
        long impressions = adTotals.stream()
                .mapToLong(a -> a.getImpressions() == null ? 0 : a.getImpressions()).sum();
        long clicks = adTotals.stream()
                .mapToLong(a -> a.getClicks() == null ? 0 : a.getClicks()).sum();

        var contentTotals = analyticsService.contentTotals(monthAgo, today);
        long views = contentTotals.stream()
                .mapToLong(c -> c.getViews() == null ? 0 : c.getViews()).sum();

        // ⚠️ Ilgari bu yerda findAll() + Java filtri turgan edi — ya'ni har
        // bir dashboard ochilishida BUTUN foydalanuvchilar va obunalar
        // jadvali xotiraga tortilardi. Endi sanoq bazada.
        LocalDateTime monthAgoTime = monthAgo.atStartOfDay();
        long appUsers = userRepo.countAppUsers();

        // Daromad: FAQAT haqiqiy xaridlar. ADMIN_GIFT obunalarida paidAmount null
        // va ular bu yerga tushmaydi — sovg'a daromad emas.
        BigDecimal subscriptionRevenue = nz(subscriptionRepo.totalPaidAmount());

        // Eng ko'p ko'rilgan kontent va eng ko'p qo'llab-quvvatlangan
        // ijodkorlar — bo'sh bo'lsa BO'SH RO'YXAT, soxta qator emas.
        List<TopRow> topViewed = contentTotals.stream()
                .limit(5)
                .map(c -> TopRow.builder()
                        .id(c.getContentId())
                        .value(c.getViews() == null ? 0L : c.getViews())
                        .build())
                .toList();

        List<TopRow> topCreators = donationRepo.topTargetsOfType(
                        DonationTargetType.CREATOR, PageRequest.of(0, 5)).stream()
                .map(r -> TopRow.builder()
                        .id(r.getTargetId())
                        .value(r.getTotal())
                        .build())
                .toList();

        return ResponseEntity.ok(DashboardSummary.builder()
                .totalContent(contentRepo.countByDeletedAtIsNull())
                .publishedContent(contentRepo.countByDeletedAtIsNullAndStatus(PublicationStatus.PUBLISHED))
                .draftContent(contentRepo.countByDeletedAtIsNullAndStatus(PublicationStatus.DRAFT))
                .scheduledContent(contentRepo.countByDeletedAtIsNullAndStatus(PublicationStatus.SCHEDULED))
                .totalEpisodes(episodeRepo.count())
                .totalCreators(creatorRepo.count())
                .totalCategories(categoryRepo.count())
                .totalMedia(mediaAssetRepo.count())
                // ⚠️ Ilgari bu yerda userRepo.count() turgan edi — ya'ni
                // BARCHA foydalanuvchilar «xodimlar» deb ko'rsatilardi.
                // 100 000 ta ilova foydalanuvchisi bo'lsa, dashboard
                // «100 000 xodim» deb yozardi.
                .totalStaff(userRepo.countStaff())
                .totalCastingApplications(castingUserRepo.count())
                .totalComments(commentRepo.count())

                .totalUsers(appUsers)
                .activeUsers(userAccountRepo.countByLastActiveAtAfter(monthAgoTime))
                .newUsers(userRepo.countAppUsersCreatedAfter(monthAgoTime))
                .premiumUsers(userAccountRepo.countByPremiumUntilAfter(LocalDateTime.now()))
                .totalSubscriptions(subscriptionRepo.count())
                // ⚠️ Ilgari .longValue() edi — tiyinlar TASHLAB
                // YUBORILARDI. 49 999.50 so'm 49 999 bo'lib ko'rinardi va
                // xato har bir obuna bilan yig'ilardi.
                .subscriptionRevenue(subscriptionRevenue)
                .singlePurchaseRevenue(nz(purchaseRepo.contentPurchaseRevenue()))
                .currencyPackageRevenue(nz(purchaseRepo.currencyPackageRevenue()))
                .totalAds(advertisementRepo.count())
                .totalNotifications(notificationRepo.count())
                .topViewedContent(topViewed)
                .topCreators(topCreators)

                .contentViews30d(views)
                .adImpressions(impressions)
                .adClicks(clicks)
                .adCtr(impressions == 0 ? 0d : clicks * 100d / impressions)
                .pendingEvents(analyticsService.pendingEvents())

                // ⚠️ Donat daromadi PULDA hisoblanmaydi: Stars va Coin kursi
                // (1 STAR = X so'm) buyurtmachi tomonidan hali aytilmagan.
                // Taxminiy kurs bilan pulga o'girish — soxta raqam bo'lardi (§45).
                //
                // Nol qaytarish ham yaramaydi: nol «donat yo'q» degani,
                // bilmaslik esa boshqa narsa. Shuning uchun sabab ham
                // yuboriladi — admin raqam yo'qligini xato deb o'ylamasin.
                .donationRevenue(null)
                .donationRevenueAvailable(false)
                .donationRevenueUnavailableReason(
                        "Donatlar STARS va COIN da hisoblanadi. Ularni so'mga "
                                + "o'girish uchun kurs kerak, u esa hali "
                                + "belgilanmagan (currency.star.rate = 0)")
                .donationsByKind(donationRepo.totalsByKind().stream()
                        .map(k -> TopRow.builder()
                                .name(k.getKind().name())
                                .value(k.getTotal())
                                .build())
                        .toList())
                .build());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Data
    @Builder
    public static class DashboardSummary {
        private Long totalContent;
        private Long publishedContent;
        private Long draftContent;
        private Long scheduledContent;
        private Long totalEpisodes;
        private Long totalCreators;
        private Long totalCategories;
        private Long totalMedia;
        private Long totalStaff;
        private Long totalCastingApplications;
        private Long totalComments;

        private Long totalUsers;
        /** Oxirgi 30 kunda faol bo'lganlar. */
        private Long activeUsers;
        /** Oxirgi 30 kunda ro'yxatdan o'tganlar (V17 dan keyingilar). */
        private Long newUsers;
        private Long premiumUsers;
        private Long totalSubscriptions;
        /** ⚠️ BigDecimal — tiyinlar tashlab yuborilmaydi. */
        private BigDecimal subscriptionRevenue;
        /** Kontent xaridlari (qism, premyera). Paketlar bu yerga kirmaydi. */
        private BigDecimal singlePurchaseRevenue;
        /** Valyuta paketlari — alohida, chunki bu boshqa turdagi daromad. */
        private BigDecimal currencyPackageRevenue;
        private Long totalAds;
        private Long totalNotifications;
        private List<TopRow> topViewedContent;
        private List<TopRow> topCreators;
        /** Donatlar valyuta bo'yicha — so'mga o'girilmaydi. */
        private List<TopRow> donationsByKind;
        /** Donat daromadi so'mda o'lchanadimi. */
        private Boolean donationRevenueAvailable;
        private String donationRevenueUnavailableReason;

        /** So'nggi 30 kun. */
        private Long contentViews30d;
        private Long adImpressions;
        private Long adClicks;
        private Double adCtr;

        /** Hali jamlanmaga qo'shilmagan hodisalar — kechikish ko'rinsin. */
        private Long pendingEvents;

        /** null = kurs sozlanmagan, frontend "ma'lumot yo'q" ko'rsatadi. */
        private Long donationRevenue;
    }

    /** Reyting qatori: nima va qancha. */
    @Data
    @Builder
    public static class TopRow {
        private Long id;
        private String name;
        private Long value;
    }
}
