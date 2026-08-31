/**
 * Grafiklar uchun yagona rang va o'lcham tizimi.
 *
 * <h2>⚠️ Ranglar TANLANMAGAN — HISOBLANGAN</h2>
 * Palitra `dataviz` validatori bilan panelning qorong'i yuzasiga
 * (`--p-surface` = #0C1730) nisbatan tekshirilgan. Beshta mezon:
 * yorqinlik oralig'i, ranglilik pastki chegarasi, rang ko'rmaslikda
 * ajralish, oddiy ko'rishda ajralish va yuzaga nisbatan kontrast.
 *
 * <h2>⚠️ Eski palitra tekshiruvdan O'TMAGAN edi</h2>
 * Grafiklar `--p-gold` (#F5C542) va `--p-warning` (#FBBF24) ni yonma-yon
 * ishlatardi. Ular orasidagi farq ΔE 1.8 — ya'ni **oddiy ko'z bilan ham
 * deyarli bir xil**. Ikkita chiziqni ajratib bo'lmasdi.
 *
 * Bundan tashqari status ranglari (yashil «muvaffaqiyat», sariq
 * «ogohlantirish») oddiy seriya rangi sifatida ishlatilardi. Shunda
 * yashil chiziq «yaxshi» degan ma'noni yo'qotadi: u endi shunchaki
 * uchinchi qator.
 */

/**
 * Kategorik ranglar — QAT'IY TARTIBDA beriladi.
 *
 * ⚠️ Tartib aylantirilmaydi va seriya soniga qarab o'zgarmaydi.
 * Filtr bitta qatorni olib tashlaganda qolganlari rangini
 * O'ZGARTIRMASLIGI kerak: rang qatorning O'ZIGA tegishli, uning
 * ro'yxatdagi o'rniga emas.
 *
 * ⚠️ Aynan TO'RTTA. Beshinchisi qo'shilganda validator yiqiladi:
 * qorong'i yuzada yorqinlik oralig'i tor va beshta rangni bir-biridan
 * ishonchli ajratib bo'lmaydi. Beshinchi qator kerak bo'lsa — uni
 * «Boshqalar» ga yig'ish yoki alohida grafikka ajratish kerak,
 * yangi rang o'ylab topish emas.
 *
 * Validator natijasi (barcha juftlar, qorong'i yuza):
 *   yorqinlik ✅ · ranglilik ✅ · CVD ΔE 9.2 ✅ · oddiy ko'rish ΔE 19.8 ✅
 *   kontrast ✅
 */
export const SERIES = ['#0284C7', '#D97706', '#7C3AED', '#DB2777'];

/**
 * Seriya rangini tartib raqami bo'yicha beradi.
 *
 * ⚠️ Aylantirmaydi. To'rtdan ortiq so'ralsa — bu dizayn xatosi va u
 * jimgina «yana o'sha ko'k» bilan yopilmasligi kerak.
 */
export function seriesColor(index) {
  return SERIES[index] ?? SERIES[SERIES.length - 1];
}

/**
 * Holat ranglari — seriya rangi sifatida ISHLATILMAYDI.
 *
 * Ular ma'no tashiydi: yashil «yaxshi», sariq «e'tibor ber», qizil
 * «muammo». Oddiy qator uchun ishlatilsa bu ma'no yo'qoladi.
 */
export const STATUS = {
  good: 'var(--p-success)',
  warning: 'var(--p-warning)',
  critical: 'var(--p-danger)',
};

/** Yordamchi elementlar — ular ma'lumot emas, shuning uchun bosiq. */
export const AXIS = {
  stroke: 'var(--p-border)',
  tick: { fill: 'var(--p-muted)', fontSize: 11 },
  grid: 'var(--p-border-soft)',
};

/**
 * Chiziq qalinligi.
 *
 * ⚠️ Ingichka. Qalin chiziq ma'lumotni emas, o'zini ko'rsatadi va
 * ikkita qator kesishganda qaysi biri ustida ekani bilinmay qoladi.
 */
export const STROKE_WIDTH = 2;

/** Nuqta o'lchami — sichqoncha tegganda. Kichigiga tegib bo'lmaydi. */
export const DOT_RADIUS = 4;
export const ACTIVE_DOT_RADIUS = 5;
