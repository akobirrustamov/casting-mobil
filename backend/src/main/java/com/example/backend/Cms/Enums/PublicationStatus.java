package com.example.backend.Cms.Enums;

/**
 * Kontent, fasl va epizodning nashr holati.
 */
public enum PublicationStatus {
    DRAFT,
    IN_REVIEW,
    SCHEDULED,
    PUBLISHED,
    ARCHIVED,
    BLOCKED;

    /** Foydalanuvchiga ko'rinadimi. */
    public boolean isVisibleToUsers() {
        return this == PUBLISHED;
    }
}
