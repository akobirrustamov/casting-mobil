package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
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

    private void require(Permission permission) {
        if (!permissionService.hasPermission(CurrentUser.get(), permission)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
        }
    }

    private Pageable page(int p, int size, Sort sort) {
        return PageRequest.of(Math.max(0, p), Math.min(Math.max(1, size), 100), sort);
    }

    // ---------------------------------------------------------------- izohlar

    @GetMapping("/comments")
    public ResponseEntity<PageResponse<CommentDto>> comments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) CommentStatus status,
            @RequestParam(required = false) Long contentId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean reportedOnly) {

        require(Permission.COMMENT_VIEW);
        var result = moderationService.comments(status, contentId, q, reportedOnly,
                page(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(PageResponse.of(result, CommentDto::from));
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        require(Permission.NOTIFICATION_VIEW);
        var result = notificationService.list(page(page, size, Sort.unsorted()));
        return ResponseEntity.ok(PageResponse.of(result, NotificationDto::from));
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

    @PostMapping("/notifications/{id}/cancel")
    public ResponseEntity<Void> cancelNotification(@PathVariable Long id) {
        require(Permission.NOTIFICATION_CREATE);
        notificationService.cancel(CurrentUser.get(), id);
        return ResponseEntity.noContent().build();
    }
}
