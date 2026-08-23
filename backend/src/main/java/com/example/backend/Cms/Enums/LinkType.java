package com.example.backend.Cms.Enums;

/**
 * Havola turi. Reklama, premyera va (kelajakda) bildirishnoma uchun umumiy.
 */
public enum LinkType {

    /** Havola yo'q — element bosilmaydi. */
    NONE,

    /** Tashqi sayt. {@code linkUrl} to'ldiriladi. */
    EXTERNAL,

    /** Ilova ichidagi ekran. {@code internalTargetType} va {@code internalTargetId}. */
    INTERNAL
}
