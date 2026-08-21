package com.example.backend.Cms.Enums;

/**
 * Platforma tillari. Kontentning har bir matni shu uch tilda bo'ladi.
 *
 * Kod bo'yicha saqlanadi (STRING), shuning uchun kelajakda yangi til qo'shish
 * migration talab qilmaydi.
 */
public enum Locale {

    /** O'zbekcha - asosiy til. Lotin yozuvi (kodda ham shunday). */
    UZ,

    /** Ruscha. */
    RU,

    /** Inglizcha. */
    EN;

    /** Sukut bo'yicha til. Tarjima topilmasa shunga qaytiladi. */
    public static final Locale DEFAULT = UZ;

    public static Locale fromCode(String code) {
        if (code == null) {
            return DEFAULT;
        }
        for (Locale l : values()) {
            if (l.name().equalsIgnoreCase(code.trim())) {
                return l;
            }
        }
        return DEFAULT;
    }
}
