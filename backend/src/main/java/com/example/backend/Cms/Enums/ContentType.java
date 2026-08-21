package com.example.backend.Cms.Enums;

/**
 * Kontent turi. Kategoriya bilan ARALASHTIRILMAYDI:
 * type = MINI_SERIES, category = "Drama", genre = "Romance".
 */
public enum ContentType {
    SHORT_FILM,
    MOVIE,
    MINI_SERIES,
    SERIES,
    PODCAST,
    SHOW,
    INTERVIEW,
    /** Jonli efir yozuvi. Doskadagi "Streamlar" bo'limi. */
    STREAM,
    /** Qisqa klip. Doskadagi "Kliplar" bo'limi. */
    CLIP,
    OTHER
}
