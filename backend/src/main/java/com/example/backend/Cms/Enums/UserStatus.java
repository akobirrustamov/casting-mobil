package com.example.backend.Cms.Enums;

/**
 * Mobil foydalanuvchi hisobining holati.
 */
public enum UserStatus {

    ACTIVE,

    /** Admin bloklagan: kira oladi, lekin tomosha va izoh yopiq. */
    BLOCKED,

    /**
     * Foydalanuvchi hisobini O'ZI o'chirgan (13.09.2026, Google Play talabi).
     *
     * ⚠️ {@link #BLOCKED} dan farqi tamoman boshqa: bloklash — jazo, uni
     * admin qo'yadi va qaytarib olishi mumkin. Bu esa odamning o'z qarori,
     * va ortga yo'l yo'q — shaxsiy ma'lumot allaqachon tozalangan.
     *
     * Hisob qatori saqlanadi, chunki unga to'lovlar va obunalar
     * bog'langan; ular qonun talab qilgan muddat davomida turishi kerak.
     */
    DELETED
}
