/**
 * Kontent muharriri uchun ro'yxatlar (ТЗ §53).
 *
 * ⚠️ Bular BITTA joyda: ilgari ular muharrirning o'zida edi va bo'limlar
 * ajratilganda har biriga nusxa ko'chirilishi kerak bo'lardi. Nusxa esa
 * vaqt o'tib asl nusxadan chetga chiqadi — masalan yangi kontent turi
 * qo'shilganda bittasida paydo bo'lib, ikkinchisida yo'q bo'lardi.
 *
 * ⚠️ Qiymatlar backend enum'lari bilan MOS bo'lishi shart.
 */
export const TYPES = ['MOVIE', 'SERIES', 'MINI_SERIES', 'SHORT_FILM', 'PODCAST', 'SHOW', 'INTERVIEW', 'STREAM', 'CLIP', 'OTHER'];

export const STRUCTURES = ['SINGLE', 'EPISODIC', 'SEASONAL'];

export const ORIENTATIONS = ['LANDSCAPE', 'VERTICAL'];

export const STATUSES = ['DRAFT', 'IN_REVIEW', 'SCHEDULED', 'PUBLISHED', 'ARCHIVED'];

export const POLICIES = ['FREE', 'PREMIUM_ONLY', 'PURCHASE_ONLY', 'PREMIUM_OR_PURCHASE'];

export const PROFESSIONS = ['ACTOR', 'ACTRESS', 'DIRECTOR', 'MODEL', 'PRODUCER', 'SCREENWRITER', 'OPERATOR', 'HOST', 'CREATOR', 'OTHER'];

