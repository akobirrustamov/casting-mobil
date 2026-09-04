package com.example.backend.Cms.Service;

/**
 * Ma'lum sozlama kalitlari.
 *
 * Magic string yozmaslik uchun (§103). Yangi kalit qo'shilganda shu yerga
 * yoziladi va {@code defaults()} ga default qiymati beriladi.
 */
public final class SettingKeys {

    private SettingKeys() {
    }

    /** Bitta qismning default narxi (so'm). ТЗ: 3 000. */
    public static final String EPISODE_PRICE = "pricing.episode.default";

    /** Butun premyerani sotib olish narxi (so'm). ТЗ: 15 000. */
    public static final String PREMIERE_PRICE = "pricing.premiere.default";

    /** 1 STAR necha so'm. Buyurtmachi kursni aytmagan — 0 qoldirilgan. */
    public static final String STAR_RATE = "currency.star.rate";

    /** 1 COIN necha so'm. Kurs aytilmagan. */
    public static final String COIN_RATE = "currency.coin.rate";

    /**
     * Bitta hisobdan nechta MOBIL qurilma. Buyurtmachi: 2.
     *
     * ⚠️ Brauzer bu hisobga KIRMAYDI — {@link #DEVICE_LIMIT_WEB} ga qarang.
     */
    public static final String DEVICE_LIMIT = "account.device.limit";

    /**
     * Bitta hisobdan nechta BRAUZER.
     *
     * <h2>Nima uchun alohida</h2>
     * Buyurtmachi qarori: veb alohida sanalsin. Aks holda kompyuterda
     * saytni ochish telefonni chiqarib yuborardi — odam esa hech qanday
     * «qurilma» qo'shmagandek his qilardi: u shunchaki brauzerga kirdi.
     *
     * Telefon — odam olib yuradigan narsa; brauzer esa ish stolida,
     * uyda, do'stinikida bo'lishi mumkin. Ularni bitta hisobga qo'shish
     * ikki xil narsani bitta o'lchov bilan o'lchash bo'lardi.
     */
    public static final String DEVICE_LIMIT_WEB = "account.device.limit.web";

    /** Ijodkorga tushadigan ulush foizi. ТЗ: 50. */
    public static final String REVENUE_SHARE_PERCENT = "revenue.creator.percent";

    /**
     * «Mashhur ijodkorlar» bo'limi tartibi: {@code MANUAL} yoki {@code STARS}.
     *
     * ТЗ §25: bugun qo'lda, ertaga analitika asosida. Sozlama orqali
     * almashtiriladi — kod o'zgarmaydi.
     */
    public static final String CREATOR_RANKING = "homepage.creators.ranking";

    /**
     * Boshlang'ich qiymatlar. Kalit bazada bo'lmasa shu ishlatiladi va
     * birinchi so'rovda yoziladi.
     *
     * ⚠️ STAR_RATE va COIN_RATE 0: buyurtmachi kursni hali aytmagan
     * (roadmap.md §8, 1-savol). 0 — «sozlanmagan» degani, soxta qiymat emas.
     */
    public static String[][] defaults() {
        return new String[][]{
                {EPISODE_PRICE, "3000", "Bitta qism narxi (so'm)"},
                {PREMIERE_PRICE, "15000", "Butun premyera narxi (so'm)"},
                {STAR_RATE, "0", "1 STAR necha so'm. 0 = kurs hali belgilanmagan"},
                {COIN_RATE, "0", "1 COIN necha so'm. 0 = kurs hali belgilanmagan"},
                {DEVICE_LIMIT, "2", "Bitta hisobdan maksimum MOBIL qurilma soni"},
                {DEVICE_LIMIT_WEB, "2", "Bitta hisobdan maksimum brauzer soni"},
                {REVENUE_SHARE_PERCENT, "50", "Ijodkorga tushadigan ulush (%)"},
                {CREATOR_RANKING, "MANUAL",
                        "Mashhur ijodkorlar tartibi: MANUAL (admin tanlaydi) yoki STARS"},
        };
    }

    /**
     * Sozlama qiymatining turi — YOZISHDA tekshirish uchun.
     *
     * <h2>Nima uchun kerak</h2>
     * Ilgari qiymat hech qanday tekshiruvsiz saqlanardi. Admin
     * {@code currency.coin.rate} ga «abc» yozsa — saqlanardi, panelda
     * ko'rinardi, u kursni belgiladim deb o'ylardi. Aslida esa
     * {@code getMoney} uni o'qiy olmay 0 qaytarardi va paketlar jimgina
     * sotib olinmaydigan bo'lib qolardi.
     *
     * ⚠️ Narx sozlamalarida bu ancha xavfli: {@code pricing.episode.default}
     * ga xato yozilsa, qism narxi 0 ga aylanadi — ya'ni <b>pullik kontent
     * bepul bo'lib qoladi</b>. Xato esa yozilgan paytda emas, kimdir
     * pulsiz tomosha qilganda bilinadi.
     *
     * Shuning uchun tekshiruv YOZISH paytida turadi: admin xatoni darhol
     * ko'radi.
     */
    public enum ValueType {
        /** Musbat pul qiymati. Nol ham mumkin — «belgilanmagan» degani. */
        MONEY,
        /** Butun son. */
        INTEGER,
        /** Ro'yxatdagi qiymatlardan biri. */
        ENUM,
        /** Ixtiyoriy matn. */
        TEXT
    }

    /**
     * Kalit qanday turdagi qiymat kutadi.
     *
     * Noma'lum kalit uchun {@code TEXT} — u baribir {@link #defaultValue}
     * tekshiruvidan o'tmaydi.
     */
    public static ValueType typeOf(String key) {
        if (key == null) {
            return ValueType.TEXT;
        }
        return switch (key) {
            case EPISODE_PRICE, PREMIERE_PRICE, STAR_RATE, COIN_RATE -> ValueType.MONEY;
            case DEVICE_LIMIT, DEVICE_LIMIT_WEB, REVENUE_SHARE_PERCENT -> ValueType.INTEGER;
            case CREATOR_RANKING -> ValueType.ENUM;
            default -> ValueType.TEXT;
        };
    }

    /** {@code ENUM} turidagi kalit uchun ruxsat etilgan qiymatlar. */
    public static java.util.List<String> allowedValues(String key) {
        if (CREATOR_RANKING.equals(key)) {
            return java.util.List.of("MANUAL", "STARS");
        }
        return java.util.List.of();
    }

    /**
     * Kalitning E'LON QILINGAN standart qiymati.
     *
     * <h2>Nima uchun kerak</h2>
     * Ilgari {@code SettingsService.getMoney} bazada satr topilmasa
     * {@code "0"} qaytarardi. Sozlamalar esa faqat admin sozlamalar
     * sahifasini ochganda yozilardi — ya'ni yangi o'rnatishda jadval bo'sh
     * bo'lib, <b>qism narxi 0 so'm</b> ko'rinardi.
     *
     * Endi zaxira qiymat shu yerdan olinadi: kod nima e'lon qilgan bo'lsa,
     * baza satri yo'q bo'lsa ham o'sha ishlaydi. Baza satri esa faqat
     * USTIDAN YOZADI.
     *
     * @return standart qiymat, kalit noma'lum bo'lsa {@code null}
     */
    public static String defaultValue(String key) {
        for (String[] d : defaults()) {
            if (d[0].equals(key)) {
                return d[1];
            }
        }
        return null;
    }

    /** Kalitning e'lon qilingan tavsifi. Noma'lum bo'lsa {@code null}. */
    public static String descriptionOf(String key) {
        for (String[] d : defaults()) {
            if (d[0].equals(key)) {
                return d[2];
            }
        }
        return null;
    }
}
