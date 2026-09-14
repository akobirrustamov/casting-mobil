package com.example.backend.Cms.Enums;

/**
 * Izohga shikoyat sababi.
 *
 * Ro'yxat QISQA ataylab: mobil ilovada u tugmalar bo'lib chiqadi, va
 * o'nta variant orasidan tanlash o'rniga odam shikoyat qilishdan
 * butunlay voz kechadi. Moderator uchun ham farq yo'q — u baribir
 * izohning o'zini o'qiydi.
 */
public enum CommentReportReason {

    /** Reklama, havolalar, takroriy matn. */
    SPAM,

    /** Haqorat, tahdid, kamsitish. */
    INSULT,

    /** Kattalar uchun yoki nomaqbul mazmun. */
    ADULT,

    /** Yuqoridagilarga tushmaydi — moderator o'zi qaraydi. */
    OTHER
}
