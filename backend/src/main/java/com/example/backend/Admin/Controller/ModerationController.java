package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.Dto.NotificationReportDto;
import com.example.backend.Admin.RequirePermission;
import com.example.backend.Admin.Dto.CommentDto;
import com.example.backend.Admin.Dto.NotificationDto;
import com.example.backend.Admin.Dto.NotificationSaveRequest;
import com.example.backend.Admin.Dto.PageResponse;
import com.example.backend.Cms.Enums.CommentStatus;
import com.example.backend.Cms.Enums.NotificationStatus;
import com.example.backend.Cms.Service.ModerationService;
import com.example.backend.Cms.Service.NotificationAdminService;
import com.example.backend.Enums.Permission;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Izohlar moderatsiyasi va bildirishnomalar (PHASE 6).
 */
@RestController
@RequestMapping("/api/v1/app/admin")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;
    private final NotificationAdminService notificationService;
    private final PermissionService permissionService;
    private final com.example.backend.Cms.Repository.UserAccountRepo userAccountRepo;

    private void require(Permission permission) {
        if (!permissionService.hasPermission(CurrentUser.get(), permission)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
        }
    }

    private Pageable page(int p, int size, Sort sort) {
        return PageRequest.of(Math.max(0, p), Math.min(Math.max(1, size), 100), sort);
    }

    // ---------------------------------------------------------------- izohlar

    /**
     * Izohlar saralanadigan ustunlar (§95).
     *
     * ⚠️ Shikoyatlar soni bo'yicha saralash moderator uchun eng
     * kerakli tartib: ko'p shikoyat qilingan izoh birinchi navbatda
     * ko'riladi.
     */
    private static final com.example.backend.Admin.SortWhitelist COMMENT_SORT =
            com.example.backend.Admin.SortWhitelist.of("createdAt")
                    .add("reports", "reportsCount");

    /**
     * Bildirishnomalar saralanadigan ustunlar (§95).
     *
     * ⚠️ Ilgari tartib umuman yo'q edi ({@code Sort.unsorted()}) —
     * ya'ni ro'yxat bazaning ixtiyoriga qolgan va sahifalar orasida
     * takrorlanishi mumkin edi.
     */
    private static final com.example.backend.Admin.SortWhitelist NOTIFICATION_SORT =
            com.example.backend.Admin.SortWhitelist.of("createdAt")
                    .add("scheduledAt")
                    .add("status");

    @GetMapping("/comments")
    public ResponseEntity<PageResponse<CommentDto>> comments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) CommentStatus status,
            @RequestParam(required = false) Long contentId,
            @RequestParam(required = false) java.util.UUID userId,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            java.time.LocalDateTime from,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            java.time.LocalDateTime to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean reportedOnly,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {

        require(Permission.COMMENT_VIEW);
        // ⚠️ Filtrlar BIRGA ishlaydi. Ilgari ular bir-birini inkor qilardi
        // va tanlangan filtrlardan biri jimgina e'tiborsiz qolardi (§34).
        var result = moderationService.comments(status, contentId, userId, from, to,
                q, reportedOnly,
                page(page, size, COMMENT_SORT.resolve(sort, dir)));
        // ⚠️ Telefon raqami — shaxsiy ma'lumot va u boshqa vazifaga
        // tegishli. Izohni moderatsiya qilish uchun muallifning ismi va
        // ID'si yetarli. Aks holda faqat COMMENT_VIEW ruxsati berilgan
        // xodim butun foydalanuvchi bazasining telefonlarini ko'rardi.
        boolean canSeeUsers = permissionService.hasPermission(
                CurrentUser.get(), Permission.USER_VIEW);

        return ResponseEntity.ok(PageResponse.of(result,
                c -> CommentDto.from(c, canSeeUsers)));
    }

    /**
     * Izoh holatini o'zgartirish: yashirish, tiklash, o'chirilgan deb belgilash.
     * Hard delete yo'q — moderator qarori saqlanadi.
     */
    @PutMapping("/comments/{id}/status/{status}")
    public ResponseEntity<CommentDto> changeCommentStatus(@PathVariable Long id,
                                                          @PathVariable CommentStatus status) {
        require(Permission.COMMENT_MODERATE);
        return ResponseEntity.ok(CommentDto.from(
                moderationService.changeStatus(CurrentUser.get(), id, status)));
    }

    // --------------------------------------------------------- bildirishnoma

    @GetMapping("/notifications")
    public ResponseEntity<PageResponse<NotificationDto>> notifications(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {
        require(Permission.NOTIFICATION_VIEW);
        var result = notificationService.list(q,
                page(page, size, NOTIFICATION_SORT.resolve(sort, dir)));
        return ResponseEntity.ok(PageResponse.of(result, NotificationDto::from));
    }

    /**
     * Auditoriya tillar bo'yicha (mobil 3 tilli talabi).
     *
     * <h2>Nega kerak</h2>
     * Bildirishnomada uchala tarjima ham majburiy, lekin admin ularni
     * qanchalik puxta yozishini bilishi kerak: RU matnini shosha-pisha
     * yozgan bo'lsa, necha kishi o'sha matnni o'qishini ko'rsin.
     *
     * ⚠️ Hisobi yo'q foydalanuvchilar bu ro'yxatga KIRMAYDI — ular
     * hali ilovani ochmagan va til tanlamagan. Ularni UZ ga qo'shish
     * taxminni fakt sifatida ko'rsatish bo'lardi (§45).
     */
    @GetMapping("/notifications/audience")
    public ResponseEntity<java.util.List<LanguageAudience>> audienceByLanguage() {
        require(Permission.NOTIFICATION_VIEW);

        var rows = userAccountRepo.countByLanguage().stream()
                .filter(r -> r.getLanguage() != null)
                .map(r -> new LanguageAudience(r.getLanguage(), r.getTotal()))
                .toList();

        return ResponseEntity.ok(rows);
    }

    /** Bitta til va undagi foydalanuvchilar soni. */
    public record LanguageAudience(com.example.backend.Cms.Enums.Locale language,
                                   long users) {
    }

    @PostMapping("/notifications")
    @RequirePermission(Permission.NOTIFICATION_CREATE)
    public ResponseEntity<NotificationDto> createNotification(
            @Valid @RequestBody NotificationSaveRequest request) {
        require(Permission.NOTIFICATION_CREATE);
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationDto.from(
                notificationService.save(CurrentUser.get(), null, request)));
    }

    @PutMapping("/notifications/{id}")
    @RequirePermission(Permission.NOTIFICATION_CREATE)
    public ResponseEntity<NotificationDto> updateNotification(
            @PathVariable Long id, @Valid @RequestBody NotificationSaveRequest request) {
        require(Permission.NOTIFICATION_CREATE);
        return ResponseEntity.ok(NotificationDto.from(
                notificationService.save(CurrentUser.get(), id, request)));
    }

    /**
     * Yuborish.
     *
     * Servis natijani SAQLAYDI (urinish izsiz qolmasin), HTTP kodini esa shu
     * yerda hal qilamiz: provayder ulanmagan bo'lsa 503 — soxta muvaffaqiyat
     * qaytarilmaydi (§32, §33).
     */
    @PostMapping("/notifications/{id}/send")
    public ResponseEntity<NotificationDto> sendNotification(@PathVariable Long id) {
        require(Permission.NOTIFICATION_SEND);

        var sent = notificationService.send(CurrentUser.get(), id);
        if (sent.getStatus() != NotificationStatus.SENT) {
            throw new BusinessException("PUSH_PROVIDER_NOT_CONFIGURED",
                    sent.getFailureReason() == null
                            ? NotificationAdminService.PROVIDER_NOT_CONFIGURED
                            : sent.getFailureReason(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return ResponseEntity.ok(NotificationDto.from(sent));
    }

    /**
     * Bildirishnoma hisoboti (ТЗ §33).
     *
     * Qaysi ko'rsatkich real o'lchanishi javobning o'zida ko'rsatiladi:
     * {@code delivered} push provayderi kvitansiyasini talab qiladi va u
     * hozir ulanmagan — nol emas, «o'lchanmaydi» qaytadi.
     */
    @GetMapping("/notifications/{id}/report")
    @RequirePermission(Permission.NOTIFICATION_VIEW)
    public ResponseEntity<NotificationReportDto> notificationReport(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.report(id));
    }

    @PostMapping("/notifications/{id}/cancel")
    public ResponseEntity<Void> cancelNotification(@PathVariable Long id) {
        require(Permission.NOTIFICATION_CREATE);
        notificationService.cancel(CurrentUser.get(), id);
        return ResponseEntity.noContent().build();
    }
}
