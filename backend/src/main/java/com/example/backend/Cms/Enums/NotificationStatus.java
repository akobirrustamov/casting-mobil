package com.example.backend.Cms.Enums;

/**
 * Bildirishnoma yuborish holati.
 *
 * ⚠️ SENT holati FAQAT haqiqiy provayder (FCM) tasdiqlagandan keyin qo'yiladi.
 * Provayder sozlanmagan bo'lsa soxta muvaffaqiyat yozilmaydi (§32).
 */
public enum NotificationStatus {
    DRAFT,
    SCHEDULED,
    SENDING,
    SENT,
    FAILED,
    CANCELLED
}
