package com.example.backend.Enums;

/**
 * DB'da saqlanadigan rol nomlari (jadval: role).
 *
 * ⚠️ Bu enum boshqa loyihadan (universitet tizimi) meros qolgan. Ishlatilmaydigan
 * qiymatlar O'CHIRILMAYDI: production DB'da ular bo'yicha satrlar bor va enum'dan
 * olib tashlash mavjud ma'lumotni o'qib bo'lmaydigan qiladi.
 *
 * Biznes ierarxiyasi uchun {@link PlatformRole} ishlatiladi,
 * o'girish — {@link com.example.backend.Security.RoleMapper}.
 */
public enum UserRoles {

    // --- UZCASTING platformasi rollari ---

    /** → PlatformRole.HYPER_ADMIN. Tarixiy nom, o'zgartirilmaydi (mavjud hisob bor). */
    ROLE_GIPERSUPERADMIN,

    /** → PlatformRole.SUPER_ADMIN */
    ROLE_SUPERADMIN,

    /** → PlatformRole.ADMIN */
    ROLE_ADMIN,

    /** → PlatformRole.WORKER. UZCASTING admin paneli uchun yangi. */
    ROLE_WORKER,

    /** → PlatformRole.USER. Mobil ilova foydalanuvchisi. */
    ROLE_USER,

    // --- Meros qolgan rollar. Ishlatilmaydi, lekin o'chirilmaydi. ---

    /** @deprecated universitet loyihasidan qolgan, UZCASTING'da ishlatilmaydi. */
    @Deprecated
    ROLE_REKTOR,

    /** @deprecated universitet loyihasidan qolgan, UZCASTING'da ishlatilmaydi. */
    @Deprecated
    ROLE_STUDENT,

    /** @deprecated universitet loyihasidan qolgan, UZCASTING'da ishlatilmaydi. */
    @Deprecated
    ROLE_TEACHER,

    /** @deprecated universitet loyihasidan qolgan, UZCASTING'da ishlatilmaydi. */
    @Deprecated
    ROLE_DEKAN
}
