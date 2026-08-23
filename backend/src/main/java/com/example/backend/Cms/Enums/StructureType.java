package com.example.backend.Cms.Enums;

/**
 * Kontentning ichki tuzilishi. Uch holat ham qo'llab-quvvatlanadi.
 */
public enum StructureType {

    /** Bitta qismlik: film, qisqa metraj, bitta podkast epizodi. */
    SINGLE,

    /** Faslsiz, bir nechta qism. Mini-serial. Episode.seasonId = null. */
    EPISODIC,

    /** Fasllarga bo'lingan serial. Season -> Episode. */
    SEASONAL
}
