package com.example.backend.Services.AuditService;

import com.example.backend.Entity.User;

/**
 * Muhim amallarni audit jurnaliga yozadi (§59).
 *
 * Yozish hech qachon asosiy amalni buzmasligi kerak: audit yiqilsa,
 * xato faqat logga tushadi va biznes tranzaksiyasi davom etadi.
 */
public interface AuditService {

    /** Oddiy yozuv — entity holatisiz. */
    void log(User actor, String action);

    /** Entity bilan bog'liq yozuv. */
    void log(User actor, String action, String entityType, Object entityId);

    /** To'liq yozuv: nima o'zgardi. before/after JSON qilib saqlanadi. */
    void log(User actor, String action, String entityType, Object entityId,
             Object before, Object after);
}
