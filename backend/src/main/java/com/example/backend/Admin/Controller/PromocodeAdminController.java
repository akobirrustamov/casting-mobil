package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.RequirePermission;
import com.example.backend.Cms.Entity.Promocode;
import com.example.backend.Cms.Entity.PromocodeRedemption;
import com.example.backend.Cms.Enums.PromocodeGrantType;
import com.example.backend.Cms.Service.PromocodeService;
import com.example.backend.Enums.Permission;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Promokodlar — admin panel.
 *
 * <h2>Ruxsatlar</h2>
 * {@code PROMOCODE_VIEW} — ro'yxat va statistika; {@code PROMOCODE_EDIT} —
 * yaratish va tahrirlash. Tariflardagi kabi ajratilgan: kim nima
 * ko'rishi bilan kim nima o'zgartirishi boshqa savollar.
 *
 * <h2>O'chirish yo'q</h2>
 * Kod to'xtatiladi ({@code active = false}). Ishlatilgan kodni o'chirish
 * «bu odamga premium qayerdan kelgan» degan savolni javobsiz qoldirardi.
 */
@RestController
@RequestMapping("/api/v1/app/admin/promocodes")
@RequiredArgsConstructor
public class PromocodeAdminController {

    private final PromocodeService promocodeService;
    private final PermissionService permissionService;

    private void require(Permission permission) {
        if (!permissionService.hasPermission(CurrentUser.get(), permission)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
        }
    }

    @GetMapping
    public ResponseEntity<List<PromocodeDto>> list() {
        require(Permission.PROMOCODE_VIEW);
        LocalDateTime now = LocalDateTime.now();
        return ResponseEntity.ok(promocodeService.all().stream()
                .map(p -> PromocodeDto.from(p, promocodeService.redemptionCount(p.getId()), now))
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromocodeDto> one(@PathVariable Long id) {
        require(Permission.PROMOCODE_VIEW);
        Promocode p = promocodeService.get(id);
        return ResponseEntity.ok(PromocodeDto.from(p,
                promocodeService.redemptionCount(id), LocalDateTime.now()));
    }

    /** Kim ishlatgan — «bu kod qayerga ketdi» degan savolga. */
    @GetMapping("/{id}/redemptions")
    public ResponseEntity<List<RedemptionDto>> redemptions(@PathVariable Long id) {
        require(Permission.PROMOCODE_VIEW);
        promocodeService.get(id);
        return ResponseEntity.ok(promocodeService.redemptions(id).stream()
                .map(RedemptionDto::from)
                .toList());
    }

    @PostMapping
    @RequirePermission(Permission.PROMOCODE_EDIT)
    public ResponseEntity<PromocodeDto> create(@RequestBody SaveRequest body) {
        require(Permission.PROMOCODE_EDIT);
        Promocode saved = promocodeService.create(CurrentUser.get(), body.toDraft());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PromocodeDto.from(saved, 0, LocalDateTime.now()));
    }

    @PutMapping("/{id}")
    @RequirePermission(Permission.PROMOCODE_EDIT)
    public ResponseEntity<PromocodeDto> update(@PathVariable Long id, @RequestBody SaveRequest body) {
        require(Permission.PROMOCODE_EDIT);
        Promocode saved = promocodeService.update(CurrentUser.get(), id, body.toDraft());
        return ResponseEntity.ok(PromocodeDto.from(saved,
                promocodeService.redemptionCount(id), LocalDateTime.now()));
    }

    // ------------------------------------------------------------------ DTO

    @Data
    public static class SaveRequest {
        /** Bo'sh — generatsiya qilinadi. Tahrirlashda e'tiborga olinmaydi. */
        private String code;

        /**
         * Kod nima beradi. Faqat YARATISHDA o'qiladi.
         *
         * Buyurtmachi: «nima uchun yaratilsa o'shanga ulanib
         * ketaveradigan qilish kerak».
         */
        private PromocodeGrantType grantType;

        private Integer grantDays;
        private Integer maxRedemptions;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;
        private Boolean active;
        private String note;

        PromocodeService.Draft toDraft() {
            return new PromocodeService.Draft(code, grantType, grantDays, maxRedemptions,
                    validFrom, validUntil, active, note);
        }
    }

    @Data
    @Builder
    public static class PromocodeDto {
        private Long id;
        private String code;
        private PromocodeGrantType grantType;
        private int grantDays;
        private Integer maxRedemptions;
        private long redemptions;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;
        private boolean active;
        private String note;
        private UUID createdBy;
        private LocalDateTime createdAt;

        /**
         * Hozir ishlatsa bo'ladimi — panel uchun bitta so'z.
         *
         * {@code ACTIVE}, {@code SCHEDULED} (hali boshlanmagan),
         * {@code EXPIRED}, {@code EXHAUSTED}, {@code DISABLED}. Panel bu
         * hisobni o'zi qilmasin: to'rtta maydonni solishtirish qoidasi
         * ikki joyda bo'lardi.
         */
        private String status;

        static PromocodeDto from(Promocode p, long redemptions, LocalDateTime now) {
            return PromocodeDto.builder()
                    .id(p.getId())
                    .code(p.getCode())
                    .grantType(p.getGrantType())
                    .grantDays(p.getGrantDays())
                    .maxRedemptions(p.getMaxRedemptions())
                    .redemptions(redemptions)
                    .validFrom(p.getValidFrom())
                    .validUntil(p.getValidUntil())
                    .active(Boolean.TRUE.equals(p.getActive()))
                    .note(p.getNote())
                    .createdBy(p.getCreatedBy())
                    .createdAt(p.getCreatedAt())
                    .status(status(p, redemptions, now))
                    .build();
        }

        private static String status(Promocode p, long redemptions, LocalDateTime now) {
            if (!Boolean.TRUE.equals(p.getActive())) {
                return "DISABLED";
            }
            if (p.getValidFrom() != null && now.isBefore(p.getValidFrom())) {
                return "SCHEDULED";
            }
            if (p.getValidUntil() != null && !now.isBefore(p.getValidUntil())) {
                return "EXPIRED";
            }
            if (p.getMaxRedemptions() != null && redemptions >= p.getMaxRedemptions()) {
                return "EXHAUSTED";
            }
            return "ACTIVE";
        }
    }

    @Data
    @Builder
    public static class RedemptionDto {
        private Long id;
        private UUID userId;
        private String userName;
        private String userPhone;
        private LocalDateTime redeemedAt;
        private LocalDateTime grantedUntil;

        static RedemptionDto from(PromocodeRedemption r) {
            return RedemptionDto.builder()
                    .id(r.getId())
                    .userId(r.getUser().getId())
                    .userName(r.getUser().getName())
                    .userPhone(r.getUser().getPhone())
                    .redeemedAt(r.getRedeemedAt())
                    .grantedUntil(r.getGrantedUntil())
                    .build();
        }
    }
}
