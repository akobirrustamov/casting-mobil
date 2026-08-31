/**
 * Grafiklar uchun yagona rang va o'lcham tizimi.
 *
 * <h2>⚠️ Rang QIYMATLARI bu yerda emas</h2>
 * Ular `theme/panel.css` da, `--chart-1…4` da. Loyiha qoidasi (§50):
 * gamma bitta faylda yoziladi, aks holda uni almashtirganda panel
 * yarmi yangi rangda, yarmi eskisida qolardi.
 *
 * `DesignTokensTest` buni qo'riqlaydi va grafiklar birinchi
 * yozilganda darhol ushladi — palitra shu faylga hex ko'rinishida
 * yozilgan edi.
 *
 * <h2>Nega palitra umuman almashtirildi</h2>
 * Eskisi tekshiruvdan o'tmagan edi: ikkita rang bir-biridan deyarli
 * farq qilmasdi va status ranglari oddiy seriya rangi sifatida
 * ishlatilardi — shunda yashil chiziq «yaxshi» degan ma'noni
 * yo'qotadi.
 *
 * Batafsil sabab, hisoblangan qiymatlar va validator natijasi —
 * `theme/panel.css` dagi `--chart-1` izohida.
 */

/**
 * Kategorik ranglar — QAT'IY TARTIBDA beriladi.
 *
 * ⚠️ Qiymatlar bu yerda EMAS, `theme/panel.css` da.
 *
 * Loyihada qoida bor (ТЗ §50): rang faqat bitta faylda yoziladi.
 * Aks holda gammani almashtirganda panel yarmi yangi rangda, yarmi
 * eskisida qolardi. `DesignTokensTest` buni qo'riqlaydi — va bu kod
 * birinchi yozilganda uni darhol ushladi.
 *
 * Hisoblangan qiymatlar va validator natijasi o'sha CSS faylida,
 * `--chart-1…4` izohida turibdi.
 *
 * ⚠️ Tartib aylantirilmaydi va seriya soniga qarab o'zgarmaydi.
 * Filtr bitta qatorni olib tashlaganda qolganlari rangini
 * O'ZGARTIRMASLIGI kerak: rang qatorning O'ZIGA tegishli, uning
 * ro'yxatdagi o'rniga emas.
 *
 * ⚠️ Aynan TO'RTTA — sabab CSS izohida.
 */
export const SERIES = [
  'var(--p-chart-1)',
  'var(--p-chart-2)',
  'var(--p-chart-3)',
  'var(--p-chart-4)',
];

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
