package com.example.backend.Cms.Enums;

/**
 * Ijodkorning kontentdagi roli.
 *
 * Bitta ijodkor bir kinoda aktyor, boshqasida rejissyor bo'lishi mumkin -
 * shuning uchun bu Creator entity'sida emas, ContentCredit'da saqlanadi.
 */
public enum CreatorProfession {
    ACTOR,
    ACTRESS,
    DIRECTOR,
    MODEL,
    PRODUCER,
    SCREENWRITER,
    OPERATOR,
    HOST,
    CREATOR,
    OTHER
}
