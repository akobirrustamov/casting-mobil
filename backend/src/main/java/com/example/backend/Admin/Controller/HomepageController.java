package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.Dto.CreatorDto;
import java.time.LocalDate;
import com.example.backend.Cms.Entity.AdDailyStatistic;
import com.example.backend.Admin.Dto.AdStatisticsDto;
import com.example.backend.Admin.RequirePermission;
import com.example.backend.Admin.Dto.*;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Service.HomepageService;
import com.example.backend.Enums.Permission;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bosh sahifa boshqaruvi: bo'limlar, reklama, premyeralar.
 *
 * Mobil ilova bosh sahifasi klientda qotirilmaydi — u shu yerdagi sozlamalardan
 * quriladi. Shu sababli «bo'limni o'chirib qo'yish» admin uchun oddiy amal.
 */
@RestController
@RequestMapping("/api/v1/app/admin")
@RequiredArgsConstructor
public class HomepageController {

    private final HomepageService homepageService;
    private final com.example.backend.Cms.Repository.AdDailyStatisticRepo adDailyStatisticRepo;
    private final PermissionService permissionService;

    private void require(Permission permission) {
        if (!permissionService.hasPermission(CurrentUser.get(), permission)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
        }
    }

    /** Nashr qilish alohida ruxsat — yaratish huquqi uni o'z ichiga olmaydi. */
    private void requirePublishRights(PublicationStatus status) {
        if (status != null && status.isVisibleToUsers()) {
            require(Permission.CONTENT_PUBLISH);
        }
    }

    // ---------------------------------------------------------------- reklama

    @GetMapping("/advertisements")
    public ResponseEntity<List<AdvertisementDto>> advertisements() {
        require(Permission.ADVERTISEMENT_VIEW);
        return ResponseEntity.ok(homepageService.advertisements()
                .stream().map(AdvertisementDto::from).toList());
    }

    @PostMapping("/advertisements")
    @RequirePermission(Permission.ADVERTISEMENT_CREATE)
    public ResponseEntity<AdvertisementDto> createAdvertisement(
            @Valid @RequestBody AdvertisementSaveRequest request) {
        require(Permission.ADVERTISEMENT_CREATE);
        requirePublishRights(request.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(AdvertisementDto.from(
                homepageService.saveAdvertisement(CurrentUser.get(), null, request)));
    }

    @PutMapping("/advertisements/{id}")
    @RequirePermission(Permission.ADVERTISEMENT_EDIT)
    public ResponseEntity<AdvertisementDto> updateAdvertisement(
            @PathVariable Long id, @Valid @RequestBody AdvertisementSaveRequest request) {
        require(Permission.ADVERTISEMENT_EDIT);
        requirePublishRights(request.getStatus());
        return ResponseEntity.ok(AdvertisementDto.from(
                homepageService.saveAdvertisement(CurrentUser.get(), id, request)));
    }

    @DeleteMapping("/advertisements/{id}")
    public ResponseEntity<Void> deleteAdvertisement(@PathVariable Long id) {
        require(Permission.ADVERTISEMENT_DELETE);
        homepageService.deleteAdvertisement(CurrentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    // --------------------------------------------------------------- premyera

    @GetMapping("/premieres")
    public ResponseEntity<List<PremiereDto>> premieres() {
        require(Permission.PREMIERE_VIEW);
        return ResponseEntity.ok(homepageService.premieres()
                .stream().map(PremiereDto::from).toList());
    }

    @PostMapping("/premieres")
    @RequirePermission(Permission.PREMIERE_CREATE)
    public ResponseEntity<PremiereDto> createPremiere(@Valid @RequestBody PremiereSaveRequest request) {
        require(Permission.PREMIERE_CREATE);
        requirePublishRights(request.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(PremiereDto.from(
                homepageService.savePremiere(CurrentUser.get(), null, request)));
    }

    @PutMapping("/premieres/{id}")
    @RequirePermission(Permission.PREMIERE_EDIT)
    public ResponseEntity<PremiereDto> updatePremiere(
            @PathVariable Long id, @Valid @RequestBody PremiereSaveRequest request) {
        require(Permission.PREMIERE_EDIT);
        requirePublishRights(request.getStatus());
        return ResponseEntity.ok(PremiereDto.from(
                homepageService.savePremiere(CurrentUser.get(), id, request)));
    }

    @DeleteMapping("/premieres/{id}")
    public ResponseEntity<Void> deletePremiere(@PathVariable Long id) {
        require(Permission.PREMIERE_DELETE);
        homepageService.deletePremiere(CurrentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------ bosh sahifa

    @GetMapping("/homepage/sections")
    public ResponseEntity<List<HomepageSectionDto>> sections() {
        require(Permission.HOMEPAGE_VIEW);
        return ResponseEntity.ok(homepageService.sections()
                .stream().map(HomepageSectionDto::from).toList());
    }

    /**
     * «Mashhur ijodkorlar» bo'limiga tushadigan ijodkorlar (ТЗ §25).
     *
     * Admin bu yerda bo'lim AMALDA qanday ko'rinishini ko'radi — ya'ni
     * {@code featured} bayrog'i va tartibi to'g'ri qo'yilganini tekshira
     * oladi. Ilgari bo'limni ko'rish imkoni yo'q edi: sozlama satri bor,
     * mazmuni esa noma'lum.
     *
     * Tartib {@code homepage.creators.ranking} sozlamasiga bo'ysunadi.
     */
    @GetMapping("/homepage/creators")
    @RequirePermission(Permission.HOMEPAGE_VIEW)
    public ResponseEntity<List<CreatorDto>> homepageCreators(
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(homepageService.featuredCreators(limit)
                .stream().map(CreatorDto::from).toList());
    }

    /**
     * Bitta reklamaning statistikasi (ТЗ §29).
     *
     * <h2>Nima uchun alohida endpoint</h2>
     * Umumiy hisobotda faqat TOP-10 banner chiqadi. 30 ta banneri bor
     * admin 25-chisining natijasini umuman ko'ra olmasdi — ТЗ esa «har bir
     * reklama uchun» deydi.
     *
     * <h2>Nima uchun tez</h2>
     * Ma'lumot kunlik jamlanmadan ({@code cms_ad_daily_statistic}) olinadi.
     * Millionlab xom hodisa ustida {@code COUNT(*)} qilinmaydi — u
     * agregatsiya vaqtida bir marta hisoblanadi.
     *
     * @param days nechа kun orqaga; standart 30
     */
    @GetMapping("/advertisements/{id}/statistics")
    @RequirePermission(Permission.ADVERTISEMENT_VIEW)
    public ResponseEntity<AdStatisticsDto> advertisementStatistics(
            @PathVariable Long id,
            @RequestParam(required = false) Integer days) {

        int window = (days == null || days <= 0) ? 30 : Math.min(days, 365);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(window - 1L);

        List<AdDailyStatistic> daily = adDailyStatisticRepo
                .findAllByAdvertisementIdAndStatDateBetweenOrderByStatDateAsc(id, from, to);

        long impressions = daily.stream().mapToLong(d -> nz(d.getImpressions())).sum();
        long clicks = daily.stream().mapToLong(d -> nz(d.getClicks())).sum();
        long uniqueImpressions = daily.stream().mapToLong(d -> nz(d.getUniqueImpressions())).sum();
        long uniqueClicks = daily.stream().mapToLong(d -> nz(d.getUniqueClicks())).sum();

        return ResponseEntity.ok(AdStatisticsDto.builder()
                .advertisementId(id)
                .from(from)
                .to(to)
                .impressions(impressions)
                .clicks(clicks)
                // ⚠️ Bu kunlik unikallar YIG'INDISI, davr bo'yicha distinct EMAS
                // (D25). Bir odam ikki kun ko'rsa - ikki marta sanaladi.
                // Davr bo'yicha aniq distinct xom hodisalarni talab qilardi.
                .uniqueImpressions(uniqueImpressions)
                .uniqueClicks(uniqueClicks)
                .ctr(impressions == 0 ? 0d
                        : Math.round(clicks * 10000.0 / impressions) / 100.0)
                .daily(daily.stream().map(d -> AdStatisticsDto.DayRow.builder()
                        .date(d.getStatDate())
                        .impressions(nz(d.getImpressions()))
                        .clicks(nz(d.getClicks()))
                        .uniqueImpressions(nz(d.getUniqueImpressions()))
                        .uniqueClicks(nz(d.getUniqueClicks()))
                        .ctr(d.ctr())
                        .build()).toList())
                .build());
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * Qatorga kiradigan kontent (ТЗ §31 — «Custom content rows»).
     *
     * Bo'limning O'ZI ilgari ham bor edi, lekin unga qaysi kontent
     * kirishini saqlaydigan joy yo'q edi — ya'ni «Maxsus qator» ni yoqish
     * mumkin, to'ldirish esa mumkin emas edi.
     */
    @GetMapping("/homepage/sections/{id}/items")
    @RequirePermission(Permission.HOMEPAGE_VIEW)
    public ResponseEntity<List<ContentListDto>> sectionItems(@PathVariable Long id) {
        return ResponseEntity.ok(homepageService.sectionItems(id).stream()
                .map(i -> ContentListDto.from(i.getContent()))
                .toList());
    }

    @PutMapping("/homepage/sections/{id}/items")
    @RequirePermission(Permission.HOMEPAGE_EDIT)
    public ResponseEntity<List<ContentListDto>> replaceSectionItems(
            @PathVariable Long id, @Valid @RequestBody SectionItemsRequest request) {

        return ResponseEntity.ok(homepageService
                .replaceSectionItems(CurrentUser.get(), id, request.getContentIds()).stream()
                .map(i -> ContentListDto.from(i.getContent()))
                .toList());
    }

    @PutMapping("/homepage/sections/{id}")
    @RequirePermission(Permission.HOMEPAGE_EDIT)
    public ResponseEntity<HomepageSectionDto> updateSection(
            @PathVariable Long id, @Valid @RequestBody HomepageSectionSaveRequest request) {
        require(Permission.HOMEPAGE_EDIT);
        return ResponseEntity.ok(HomepageSectionDto.from(
                homepageService.saveSection(CurrentUser.get(), id, request)));
    }
}
