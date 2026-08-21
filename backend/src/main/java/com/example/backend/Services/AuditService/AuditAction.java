package com.example.backend.Services.AuditService;

/**
 * Audit action nomlari. Magic string yozmaslik uchun (§103).
 */
public final class AuditAction {

    private AuditAction() {
    }

    public static final String STAFF_CREATED = "STAFF_CREATED";
    public static final String STAFF_UPDATED = "STAFF_UPDATED";
    public static final String STAFF_DEACTIVATED = "STAFF_DEACTIVATED";
    public static final String STAFF_PASSWORD_RESET = "STAFF_PASSWORD_RESET";

    public static final String ROLE_CHANGED = "ROLE_CHANGED";
    public static final String PERMISSION_CHANGED = "PERMISSION_CHANGED";

    public static final String CONTENT_CREATED = "CONTENT_CREATED";
    public static final String CONTENT_UPDATED = "CONTENT_UPDATED";
    public static final String CONTENT_PUBLISHED = "CONTENT_PUBLISHED";
    public static final String CONTENT_ARCHIVED = "CONTENT_ARCHIVED";

    public static final String ADVERTISEMENT_CREATED = "ADVERTISEMENT_CREATED";
    public static final String ADVERTISEMENT_UPDATED = "ADVERTISEMENT_UPDATED";

    public static final String PREMIUM_GRANTED = "PREMIUM_GRANTED";
    public static final String PREMIUM_REVOKED = "PREMIUM_REVOKED";

    public static final String TARIFF_CHANGED = "TARIFF_CHANGED";
    public static final String COMMENT_HIDDEN = "COMMENT_HIDDEN";
    public static final String NOTIFICATION_SENT = "NOTIFICATION_SENT";

    public static final String USER_BLOCKED = "USER_BLOCKED";
    public static final String USER_UNBLOCKED = "USER_UNBLOCKED";
    public static final String DEVICE_REVOKED = "DEVICE_REVOKED";
}
