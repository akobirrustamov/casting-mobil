package com.example.backend.Services.AuditService;

import com.example.backend.Entity.AuditLog;
import com.example.backend.Entity.User;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.AuditLogRepo;
import com.example.backend.Security.RoleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    /** IP va User-Agent maydonlari uzunligi — entity'dagi cheklovga mos. */
    private static final int IP_MAX = 64;
    private static final int UA_MAX = 512;

    private final AuditLogRepo auditLogRepo;
    private final ObjectMapper objectMapper;

    @Override
    public void log(User actor, String action) {
        log(actor, action, null, null, null, null);
    }

    @Override
    public void log(User actor, String action, String entityType, Object entityId) {
        log(actor, action, entityType, entityId, null, null);
    }

    @Override
    public void log(User actor, String action, String entityType, Object entityId,
                    Object before, Object after) {
        try {
            PlatformRole role = RoleMapper.highestRole(actor);

            AuditLog entry = AuditLog.builder()
                    .actorId(actor == null ? null : actor.getId())
                    .actorRole(role == null ? null : role.name())
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId == null ? null : String.valueOf(entityId))
                    .beforeState(toJson(before))
                    .afterState(toJson(after))
                    .ip(trim(currentIp(), IP_MAX))
                    .userAgent(trim(currentUserAgent(), UA_MAX))
                    .build();

            auditLogRepo.save(entry);
        } catch (Exception e) {
            // Audit hech qachon asosiy amalni yiqitmasligi kerak.
            log.error("Audit yozib bo'lmadi: action={}, entityType={}", action, entityType, e);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Audit uchun JSON'ga o'girib bo'lmadi: {}", value.getClass().getSimpleName());
            return null;
        }
    }

    private String currentIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Proksi orqasida birinchi manzil — haqiqiy klient.
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    private String currentUserAgent() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader("User-Agent");
    }

    private HttpServletRequest currentRequest() {
        // Fon vazifalaridan chaqirilsa request bo'lmaydi — bu normal holat.
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
