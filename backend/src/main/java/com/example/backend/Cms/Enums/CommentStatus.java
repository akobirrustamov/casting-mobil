package com.example.backend.Cms.Enums;

/**
 * Izoh holati.
 *
 * O'chirish HARD DELETE emas: shikoyat tarixini va moderator qarorini
 * saqlab qolish kerak (§58).
 */
public enum CommentStatus {

    /** Hammaga ko'rinadi. */
    VISIBLE,

    /** Moderator yashirgan. Muallif ko'rishi mumkin, boshqalar yo'q. */
    HIDDEN,

    /** O'chirilgan deb belgilangan. Ko'rinmaydi, lekin yozuv saqlanadi. */
    DELETED
}
