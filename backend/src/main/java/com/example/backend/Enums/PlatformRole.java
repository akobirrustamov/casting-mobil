package com.example.backend.Enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * UZCASTING platformasining rol ierarxiyasi.
 *
 * Bu {@link UserRoles} ning o'rnini bosmaydi — u DB'da saqlanadigan texnik enum
 * va unda boshqa loyihadan qolgan qiymatlar ham bor. PlatformRole esa biznes
 * ierarxiyasi: kim kimni yaratadi va kim nimaga kira oladi.
 *
 * O'girish uchun {@link com.example.backend.Security.RoleMapper} ishlatiladi.
 */
public enum PlatformRole {

    /** Platformadagi eng yuqori rol. Barcha modullar, barcha staff. */
    HYPER_ADMIN(100),

    /** Admin va Worker yaratadi. HyperAdmin yarata olmaydi. */
    SUPER_ADMIN(80),

    /** Faqat Worker yaratadi. */
    ADMIN(60),

    /** Hech kimni yarata olmaydi. Huquqlari fine-grained Permission orqali. */
    WORKER(40),

    /** Mobil ilova foydalanuvchisi. Admin panelga KIRA OLMAYDI. */
    USER(10);

    private final int level;

    PlatformRole(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /** Admin panelga kirish huquqi. USER — yo'q. */
    public boolean canAccessAdminPanel() {
        return this != USER;
    }

    /**
     * Ierarxiya bo'yicha ushbu rol berilgan roldan yuqori yoki teng ekanini tekshiradi.
     * WORKER uchun bu yetarli emas — unda Permission ham tekshiriladi.
     */
    public boolean isAtLeast(PlatformRole other) {
        return other != null && this.level >= other.level;
    }

    /**
     * Ushbu rol qaysi rollarni yarata oladi.
     *
     * HYPER_ADMIN o'ziga teng rol yarata OLMAYDI — bu ataylab. Yagona hyper-admin
     * hisobi AutoRun orqali environment'dan yaratiladi, API orqali emas. Aks holda
     * bitta buzilgan hyper-admin hisobi cheksiz ko'paya oladi.
     */
    /**
     * Shu rol qaysi rollarni yarata oladi.
     *
     * <h2>QAROR: HYPER_ADMIN boshqa HYPER_ADMIN yarata OLMAYDI</h2>
     *
     * Qoida butun ierarxiyada bir xil: <b>faqat o'zidan QAT'IY quyi</b>
     * rolni yaratish mumkin. HYPER_ADMIN uchun ham istisno yo'q.
     *
     * <h3>Nega aynan shunday</h3>
     * {@link #canManage} qat'iy taqqoslashdan foydalanadi
     * ({@code this.level > other.level}). Ya'ni ikkita HYPER_ADMIN mavjud
     * bo'lsa, ular <b>bir-birini boshqara olmaydi</b>: na o'chirish, na
     * rolini pasaytirish.
     *
     * Agar HYPER_ADMIN o'ziga teng hisob yaratishi mumkin bo'lsa, bitta
     * o'g'irlangan hisob cheksiz «abadiy» HYPER_ADMIN yarata olardi va
     * ularni <b>hech kim</b> — hatto dastlabki egasi ham — olib tashlay
     * olmasdi. Bir martalik buzilish doimiy va qaytarib bo'lmas nazoratga
     * aylanardi.
     *
     * <h3>Yagona HYPER_ADMIN yo'qolsa nima bo'ladi</h3>
     * Tiklash yo'li ilova ichida emas, <b>serverda</b>:
     * {@code APP_GIPERSUPERADMIN_PHONE} va {@code APP_GIPERSUPERADMIN_PASSWORD}
     * environment o'zgaruvchilari berilib, ilova qayta ishga tushiriladi
     * ({@code AutoRun}). Parol {@code BootstrapPasswordPolicy} talabidan
     * o'tishi shart.
     *
     * Bu ataylab qiyinroq yo'l: u serverga kirish huquqini talab qiladi,
     * ya'ni veb-interfeys orqali huquq oshirib bo'lmaydi.
     *
     * ⚠️ Bu qarorni o'zgartirmoqchi bo'lsangiz, avval {@link #canManage} ni
     * ham ko'rib chiqing — aks holda o'chirib bo'lmaydigan hisoblar paydo
     * bo'ladi.
     */
    public Set<PlatformRole> creatableRoles() {
        switch (this) {
            case HYPER_ADMIN:
                // SUPER_ADMIN va pastrog'i. HYPER_ADMIN ataylab yo'q — yuqoriga qarang.
                return EnumSet.of(SUPER_ADMIN, ADMIN, WORKER);
            case SUPER_ADMIN:
                return EnumSet.of(ADMIN, WORKER);
            case ADMIN:
                return EnumSet.of(WORKER);
            default:
                return EnumSet.noneOf(PlatformRole.class);
        }
    }

    /** Ushbu rol {@code target} rolini yarata oladimi. */
    public boolean canCreate(PlatformRole target) {
        return target != null && creatableRoles().contains(target);
    }

    /**
     * Ushbu rol {@code target} rolidagi hisobni boshqara oladimi (tahrir, bloklash).
     * O'ziga teng yoki yuqori rolni boshqarib bo'lmaydi — privilege escalation oldini oladi.
     */
    public boolean canManage(PlatformRole target) {
        return target != null && this.level > target.level;
    }
}
