package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.Dto.PageResponse;
import com.example.backend.Entity.AuditLog;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.AuditLogRepo;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit jurnali (§59).
 *
 * <b>Faqat o'qish.</b> O'chirish yoki tahrirlash endpointi ATAYLAB yo'q —
 * jurnal o'zgarmas bo'lishi kerak, aks holda uning ma'nosi qolmaydi.
 *
 * Faqat ADMIN va undan yuqori rollar ko'radi.
 */
@RestController
@RequestMapping("/api/v1/app/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepo auditLogRepo;
    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<PageResponse<AuditLogDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId) {

        PlatformRole role = permissionService.roleOf(CurrentUser.get());
        if (role == null || !role.isAtLeast(PlatformRole.ADMIN)) {
            throw BusinessException.accessDenied("Audit jurnali uchun ruxsat yo'q");
        }

        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 200),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        var result = actorId != null
                ? auditLogRepo.findAllByActorIdOrderByCreatedAtDesc(actorId, pageable)
                : (entityType != null && entityId != null
                    ? auditLogRepo.findAllByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                            entityType, entityId, pageable)
                    : (action != null
                        ? auditLogRepo.findAllByActionOrderByCreatedAtDesc(action, pageable)
                        : auditLogRepo.findAll(pageable)));

        return ResponseEntity.ok(PageResponse.of(result, AuditLogDto::from));
    }

    @Data
    @Builder
    public static class AuditLogDto {
        private Long id;
        private UUID actorId;
        private String actorRole;
        private String action;
        private String entityType;
        private String entityId;
        private String beforeState;
        private String afterState;
        private String ip;
        private LocalDateTime createdAt;

        static AuditLogDto from(AuditLog a) {
            return AuditLogDto.builder()
                    .id(a.getId())
                    .actorId(a.getActorId())
                    .actorRole(a.getActorRole())
                    .action(a.getAction())
                    .entityType(a.getEntityType())
                    .entityId(a.getEntityId())
                    .beforeState(a.getBeforeState())
                    .afterState(a.getAfterState())
                    .ip(a.getIp())
                    .createdAt(a.getCreatedAt())
                    .build();
        }
    }
}
