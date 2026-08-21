package com.example.backend.Cms.Enums;

/**
 * Xodim hisobining holati.
 *
 * <h2>Nega BLOCKED va INACTIVE alohida</h2>
 * Ular turli maqsadga xizmat qiladi va aralashtirilsa ma'no yo'qoladi:
 *
 * <ul>
 *   <li>{@link #BLOCKED} — VAQTINCHA to'xtatish. Masalan tergov davomida
 *       yoki shubhali faoliyat aniqlanganda. Odatda qaytariladi.</li>
 *   <li>{@link #INACTIVE} — xodim endi ishlamaydi. Bu <b>hard delete
 *       o'rnida</b>: yozuv qoladi, chunki audit jurnalidagi amallar
 *       kimga tegishli ekani ma'lum bo'lib turishi kerak.</li>
 * </ul>
 *
 * Ikkalasida ham tizimga kirish yopiladi.
 */
public enum StaffStatus {

    ACTIVE,

    /** Faolsizlantirilgan — hard delete o'rnida. */
    INACTIVE,

    /** Vaqtincha bloklangan. */
    BLOCKED;

    /** Shu holatda tizimga kirish va ishlash mumkinmi. */
    public boolean canWork() {
        return this == ACTIVE;
    }
}
