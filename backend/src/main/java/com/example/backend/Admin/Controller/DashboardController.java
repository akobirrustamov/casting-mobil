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

        // Faqat USER rolidagilar — xodimlar hisobga olinmaydi
        long appUsers = userRepo.findAll().stream()
                .filter(u -> {
                    PlatformRole r = RoleMapper.highestRole(u);
                    return r == null || r == PlatformRole.USER;
                })
                .count();

        // Daromad: FAQAT haqiqiy xaridlar. ADMIN_GIFT obunalarida paidAmount null
        // va ular bu yerga tushmaydi — sovg'a daromad emas.
        BigDecimal subscriptionRevenue = subscriptionRepo.findAll().stream()
                .filter(x -> x.getPaidAmount() != null)
                .map(x -> x.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(DashboardSummary.builder()
                .totalContent(contentRepo.countByDeletedAtIsNull())
                .publishedContent(contentRepo.countByDeletedAtIsNullAndStatus(PublicationStatus.PUBLISHED))
                .draftContent(contentRepo.countByDeletedAtIsNullAndStatus(PublicationStatus.DRAFT))
                .scheduledContent(contentRepo.countByDeletedAtIsNullAndStatus(PublicationStatus.SCHEDULED))
                .totalEpisodes(episodeRepo.count())
                .totalCreators(creatorRepo.count())
                .totalCategories(categoryRepo.count())
                .totalMedia(mediaAssetRepo.count())
                .totalStaff(userRepo.count())
                .totalCastingApplications(castingUserRepo.count())
                .totalComments(commentRepo.count())

                .totalUsers(appUsers)
                .premiumUsers(userAccountRepo.countByPremiumUntilAfter(LocalDateTime.now()))
                .subscriptionRevenue(subscriptionRevenue.longValue())

                .contentViews30d(views)
                .adImpressions(impressions)
                .adClicks(clicks)
                .adCtr(impressions == 0 ? 0d : clicks * 100d / impressions)
                .pendingEvents(analyticsService.pendingEvents())

                // ⚠️ Donat daromadi PULDA hisoblanmaydi: Stars va Coin kursi
                // (1 STAR = X so'm) buyurtmachi tomonidan hali aytilmagan.
                // Taxminiy kurs bilan pulga o'girish - soxta raqam bo'lardi (§45).
                .donationRevenue(null)
                .build());
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
        private Long premiumUsers;
        private Long subscriptionRevenue;

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
}
