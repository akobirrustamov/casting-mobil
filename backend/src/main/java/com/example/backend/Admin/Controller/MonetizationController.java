package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.RequirePermission;
import com.example.backend.Admin.Dto.CurrencyPackageDto;
import com.example.backend.Admin.Dto.CurrencyPackageSaveRequest;
import com.example.backend.Admin.Dto.PlatformSettingDto;
import com.example.backend.Admin.Dto.TariffSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.CurrencyPackage;
import com.example.backend.Cms.Entity.PlatformSetting;
import com.example.backend.Cms.Entity.Tariff;
import com.example.backend.Cms.Entity.TariffTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.DonationRepo;
import com.example.backend.Cms.Service.MonetizationService;
import com.example.backend.Cms.Service.SettingsService;
import com.example.backend.Enums.Permission;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tariflar, valyuta paketlari, donat hisoboti va platforma sozlamalari (PHASE 7).
 *
 * Narxlar va kurslar kodda qotirilmaydi — hammasi shu yerdan boshqariladi.
 */
@RestController
@RequestMapping("/api/v1/app/admin")
@RequiredArgsConstructor
public class MonetizationController {

    private final MonetizationService monetizationService;
    private final SettingsService settingsService;
    private final PermissionService permissionService;

    private void require(Permission permission) {
        if (!permissionService.hasPermission(CurrentUser.get(), permission)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
        }
    }

    // ---------------------------------------------------------------- tarif

    @GetMapping("/tariffs")
    public ResponseEntity<List<TariffDto>> tariffs() {
        require(Permission.TARIFF_VIEW);
        return ResponseEntity.ok(monetizationService.tariffs()
                .stream().map(TariffDto::from).toList());
    }

    @PostMapping("/tariffs")
    @RequirePermission(Permission.TARIFF_EDIT)
    public ResponseEntity<TariffDto> createTariff(@Valid @RequestBody TariffSaveRequest request) {
        require(Permission.TARIFF_EDIT);
        return ResponseEntity.status(HttpStatus.CREATED).body(TariffDto.from(
                monetizationService.saveTariff(CurrentUser.get(), null, request)));
    }

    @PutMapping("/tariffs/{id}")
    @RequirePermission(Permission.TARIFF_EDIT)
    public ResponseEntity<TariffDto> updateTariff(@PathVariable Long id,
                                                  @Valid @RequestBody TariffSaveRequest request) {
        require(Permission.TARIFF_EDIT);
        return ResponseEntity.ok(TariffDto.from(
                monetizationService.saveTariff(CurrentUser.get(), id, request)));
    }

    // ------------------------------------------------------- valyuta paketi

    @GetMapping("/currency-packages")
    public ResponseEntity<List<CurrencyPackageDto>> packages() {
        require(Permission.DONATION_VIEW);
        return ResponseEntity.ok(monetizationService.packages().stream()
                .map(CurrencyPackageDto::from).toList());
    }

    /**
     * ⚠️ Ilgari bu yerda {@code @RequestBody CurrencyPackage} turgan edi —
     * entity to'g'ridan-to'g'ri va HECH QANDAY tekshiruvsiz. {@code kind}
     * bo'sh yuborilsa xato faqat bazada chiqardi va panelda «500 Internal
     * Server Error» ko'rinardi: admin nimani to'ldirmaganini bilmasdi.
     */
    @PostMapping("/currency-packages")
    public ResponseEntity<CurrencyPackageDto> createPackage(
            @Valid @RequestBody CurrencyPackageSaveRequest body) {
        require(Permission.DONATION_PACKAGE_EDIT);
        return ResponseEntity.status(HttpStatus.CREATED).body(CurrencyPackageDto.from(
                monetizationService.savePackage(CurrentUser.get(), null, body)));
    }

    @PutMapping("/currency-packages/{id}")
    public ResponseEntity<CurrencyPackageDto> updatePackage(
            @PathVariable Long id, @Valid @RequestBody CurrencyPackageSaveRequest body) {
        require(Permission.DONATION_PACKAGE_EDIT);
        return ResponseEntity.ok(CurrencyPackageDto.from(
                monetizationService.savePackage(CurrentUser.get(), id, body)));
    }

    @DeleteMapping("/currency-packages/{id}")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        require(Permission.DONATION_PACKAGE_EDIT);
        monetizationService.deletePackage(CurrentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- donat

    @GetMapping("/donations/top")
    public ResponseEntity<DonationReport> topDonations(
            @RequestParam(defaultValue = "20") int limit) {
        require(Permission.DONATION_VIEW);

        List<DonationRepo.TargetTotal> rows = monetizationService.topDonationTargets(limit);
        List<TopTarget> top = rows.stream()
                .map(r -> TopTarget.builder()
                        .targetType(r.getTargetType().name())
                        .targetId(r.getTargetId())
                        .kind(r.getKind().name())
                        .total(r.getTotal())
                        .transactions(r.getTransactions())
                        .build())
                .toList();

        return ResponseEntity.ok(DonationReport.builder()
                .totalTransactions(monetizationService.donationCount())
                .top(top)
                .build());
    }

    // ------------------------------------------------------------ sozlamalar

    @GetMapping("/settings")
    public ResponseEntity<List<PlatformSettingDto>> settings() {
        require(Permission.SETTINGS_VIEW);
        return ResponseEntity.ok(settingsService.all().stream()
                .map(PlatformSettingDto::from).toList());
    }

    @PutMapping("/settings/{key}")
    public ResponseEntity<PlatformSettingDto> updateSetting(@PathVariable String key,
                                                            @RequestBody SettingValue body) {
        require(Permission.SETTINGS_EDIT);
        return ResponseEntity.ok(PlatformSettingDto.from(
                settingsService.update(CurrentUser.get(), key, body.getValue())));
    }

    // ------------------------------------------------------------------ DTO

    @Data
    public static class SettingValue {
        private String value;
    }

    @Data
    @Builder
    public static class TariffDto {
        private Long id;
        private String code;
        private Integer durationMonths;
        private BigDecimal price;
        private BigDecimal monthlyPrice;
        private String currency;
        private Boolean active;
        private Boolean highlighted;
        private Integer sortOrder;
        private Map<Locale, TranslationDto> translations;

        static TariffDto from(Tariff t) {
            Map<Locale, TranslationDto> tr = new LinkedHashMap<>();
            for (TariffTranslation x : t.getTranslations()) {
                tr.put(x.getLocale(), TranslationDto.builder()
                        .title(x.getName())
                        .shortDescription(x.getBadge())
                        .description(x.getFeatures())
                        .build());
            }
            return TariffDto.builder()
                    .id(t.getId())
                    .code(t.getCode())
                    .durationMonths(t.getDurationMonths())
                    .price(t.getPrice())
                    .monthlyPrice(t.monthlyPrice())
                    .currency(t.getCurrency())
                    .active(t.getActive())
                    .highlighted(t.getHighlighted())
                    .sortOrder(t.getSortOrder())
                    .translations(tr)
                    .build();
        }
    }

    @Data
    @Builder
    public static class TopTarget {
        private String targetType;
        private Long targetId;
        private String kind;
        private Long total;
        private Long transactions;
    }

    @Data
    @Builder
    public static class DonationReport {
        private Long totalTransactions;
        private List<TopTarget> top;
    }
}
