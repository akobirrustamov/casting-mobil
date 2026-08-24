package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.Dto.AppUserDto;
import com.example.backend.Admin.Dto.PageResponse;
import com.example.backend.Cms.Entity.UserDevice;
import com.example.backend.Cms.Repository.UserDeviceRepo;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Enums.Permission;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mobil foydalanuvchilarni boshqarish (PHASE 7).
 *
 * ⚠️ Bu yerda XODIMLAR emas — ular {@code /staff} da. USER admin panelga
 * kira olmaydi, lekin admin uni ko'radi va boshqaradi.
 */
@RestController
@RequestMapping("/api/v1/app/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final UserRepo userRepo;
    private final UserDeviceRepo deviceRepo;
    private final PermissionService permissionService;

    private void require(Permission permission) {
        if (!permissionService.hasPermission(CurrentUser.get(), permission)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
        }
    }

    /**
     * Ilova foydalanuvchilari ro'yxati (ТЗ §35).
     *
     * <h2>Nima uchun sahifalash</h2>
     * Ilgari butun jadval xotiraga tortilib, chegara Java'da qo'llanardi.
     * 100 000 ta foydalanuvchida panelni ochish har safar 100 000 satr
     * degani edi.
     */
    /**
     * Foydalanuvchi ro'yxati saralanadigan ustunlar (§95).
     *
     * ⚠️ Balans va premium bu yerda YO'Q: ular boshqa jadvallarda
     * (`UserBalance`, `UserAccount`) va ular bo'yicha saralash `join`
     * talab qiladi. Hisobi yo'q foydalanuvchi esa natijadan tushib
     * qolardi — bu jimgina ma'lumot yo'qotish bo'lardi.
     */
    private static final com.example.backend.Admin.SortWhitelist USER_SORT =
            com.example.backend.Admin.SortWhitelist.of("createdAt")
                    .add("name")
                    .add("phone");

    @GetMapping
    public ResponseEntity<PageResponse<AppUserDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {

        require(Permission.USER_VIEW);
        int safeSize = Math.min(Math.max(size, 1), 200);
        var result = userAdminService.searchPage(q,
                org.springframework.data.domain.PageRequest.of(
                        Math.max(page, 0), safeSize, USER_SORT.resolve(sort, dir)));

        return ResponseEntity.ok(PageResponse.of(result, row -> AppUserDto.from(
                row.user(), row.account(), row.balance(), row.activeDevices())));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AppUserDto> detail(@PathVariable UUID userId) {
        require(Permission.USER_VIEW);
        var user = userRepo.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));
        int devices = deviceRepo.findAllByUserIdAndActiveTrueOrderByLastActiveAtAsc(userId).size();
        return ResponseEntity.ok(AppUserDto.from(user,
                userAdminService.accountOf(userId), userAdminService.balanceOf(userId), devices));
    }

    // ---------------------------------------------------------------- bloklash

    @PostMapping("/{userId}/block")
    public ResponseEntity<AppUserDto> block(@PathVariable UUID userId,
                                            @RequestBody(required = false) BlockRequest request) {
        require(Permission.USER_BLOCK);
        var account = userAdminService.setBlocked(CurrentUser.get(), userId, true,
                request == null ? null : request.getReason());
        return ResponseEntity.ok(AppUserDto.from(account.getUser(), account,
                userAdminService.balanceOf(userId), 0));
    }

    @PostMapping("/{userId}/unblock")
    public ResponseEntity<AppUserDto> unblock(@PathVariable UUID userId) {
        require(Permission.USER_BLOCK);
        var account = userAdminService.setBlocked(CurrentUser.get(), userId, false, null);
        return ResponseEntity.ok(AppUserDto.from(account.getUser(), account,
                userAdminService.balanceOf(userId), 0));
    }

    // ----------------------------------------------------------------- premium

    /** Premium sovg'a qilish. Mavjud obuna ustiga qo'shiladi (§38). */
    @PostMapping("/{userId}/premium")
    public ResponseEntity<AppUserDto> grantPremium(@PathVariable UUID userId,
                                                   @RequestBody PremiumRequest request) {
        require(Permission.USER_PREMIUM_MANAGE);
        var account = userAdminService.grantPremium(CurrentUser.get(), userId,
                request.getMonths(), request.getTariffId());
        return ResponseEntity.ok(AppUserDto.from(account.getUser(), account,
                userAdminService.balanceOf(userId), 0));
    }

    @DeleteMapping("/{userId}/premium")
    public ResponseEntity<AppUserDto> revokePremium(@PathVariable UUID userId) {
        require(Permission.USER_PREMIUM_MANAGE);
        var account = userAdminService.revokePremium(CurrentUser.get(), userId);
        return ResponseEntity.ok(AppUserDto.from(account.getUser(), account,
                userAdminService.balanceOf(userId), 0));
    }

    // ---------------------------------------------------------------- qurilma

    @GetMapping("/{userId}/devices")
    public ResponseEntity<List<DeviceDto>> devices(@PathVariable UUID userId) {
        require(Permission.USER_DEVICE_MANAGE);
        return ResponseEntity.ok(userAdminService.devices(userId)
                .stream().map(DeviceDto::from).toList());
    }

    @DeleteMapping("/{userId}/devices/{deviceRowId}")
    public ResponseEntity<Void> revokeDevice(@PathVariable UUID userId,
                                             @PathVariable Long deviceRowId) {
        require(Permission.USER_DEVICE_MANAGE);
        userAdminService.revokeDevice(CurrentUser.get(), userId, deviceRowId);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class BlockRequest {
        private String reason;
    }

    @Data
    public static class PremiumRequest {
        private Integer months;
        private Long tariffId;
    }

    @Data
    @Builder
    public static class DeviceDto {
        private Long id;
        private String deviceId;
        private String deviceName;
        private String platform;
        private Boolean active;
        private LocalDateTime lastActiveAt;
        private LocalDateTime createdAt;

        static DeviceDto from(UserDevice d) {
            return DeviceDto.builder()
                    .id(d.getId())
                    .deviceId(d.getDeviceId())
                    .deviceName(d.getDeviceName())
                    .platform(d.getPlatform())
                    .active(d.getActive())
                    .lastActiveAt(d.getLastActiveAt())
                    .createdAt(d.getCreatedAt())
                    .build();
        }
    }
}
