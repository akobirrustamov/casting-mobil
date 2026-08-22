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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Maxfiy deb hisoblanadigan maydon nomlari (§59).
     *
     * Solishtirish kichik harfda va «ichida bor» tamoyili bilan:
     * {@code passwordHash}, {@code newPassword}, {@code refreshToken},
     * {@code apiKey} — hammasi tushadi.
     */
    private static final List<String> SECRET_HINTS = List.of(
            "password", "token", "secret", "apikey", "api_key",
            "credential", "authorization", "otp", "pin");

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(redact(value));
        } catch (Exception e) {
            log.warn("Audit uchun JSON'ga o'girib bo'lmadi: {}", value.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Maxfiy qiymatlarni jurnalga tushishdan oldin o'chiradi.
     *
     * <b>Nega kerak.</b> {@code toJson} istalgan obyektni seriyalashtiradi.
     * Bugun barcha chaqiruv joylari qo'lda tanlangan {@code Map.of(...)}
     * uzatadi, lekin ertaga kimdir butun so'rov DTO'sini uzatishi mumkin —
     * u yerda esa {@code password} bor. Audit jadvali odatda uzoq
     * saqlanadi va ko'p odam o'qiydi, ya'ni parol eng noqulay joyga
     * tushadi. ТЗ buni to'g'ridan-to'g'ri taqiqlaydi.
     *
     * <b>Nega o'chirilmaydi, balki belgilanadi.</b> Maydonni butunlay
     * tashlab yuborsak, «parol o'zgardimi?» degan savolga javob
     * qolmasdi. {@code ***} esa voqeani ko'rsatadi, qiymatni emas.
     */
    private Object redact(Object value) {
        Object tree = objectMapper.convertValue(value, Object.class);
        return redactNode(tree);
    }

    private Object redactNode(Object node) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> clean = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                clean.put(key, isSecret(key) ? "***" : redactNode(e.getValue()));
            }
            return clean;
        }
        if (node instanceof List<?> list) {
            return list.stream().map(this::redactNode).toList();
        }
        return node;
    }

    private boolean isSecret(String key) {
        String lower = key.toLowerCase();
        return SECRET_HINTS.stream().anyMatch(lower::contains);
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
