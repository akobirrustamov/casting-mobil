import { usePanelI18n } from '../i18n';

/**
 * Kunlik dinamika grafigi — sof inline SVG.
 *
 * Grafik kutubxonasi qo'shilmadi: bitta oddiy chiziqli grafik uchun yangi
 * bog'liqlik ortiqcha (§70). Bir nechta turdagi grafik kerak bo'lganda
 * qayta ko'rib chiqiladi.
 *
 * <h2>Chiziqlar tashqaridan beriladi</h2>
 * Ilgari `views / plays / completes` uchtaligi komponentga QOTIRILGAN
 * edi. Dashboard esa butunlay boshqa qatorlarni chizadi — ro'yxatdan
 * o'tish, obuna daromadi, donatlar. Qotirilgan holda har biri uchun
 * yangi komponent yozilardi va ular asta-sekin bir-biridan farq qilib
 * ketardi. Endi qatorlar `series` prop'i orqali keladi, standart qiymat
 * esa hisobotlar sahifasi uchun eskisicha qoladi.
 */
export default function TrendChart({ points, series, height = 180, formatValue }) {
  const { t } = usePanelI18n();

  // ⚠️ Bo'sh ma'lumot — bo'sh holat, soxta grafik EMAS (§45).
  // Nol qiymatli chiziq chizish «hech kim ko'rmagan» degan ma'noni
  // berardi, aslida esa ma'lumot umuman yo'q.
  if (!points || points.length === 0) {
    return <p className="uz-muted" style={{ fontSize: 13 }}>{t('rp.noData')}</p>;
  }

  const lines = series && series.length ? series : [
    { key: 'views', color: 'var(--p-accent)', label: t('rp.views') },
    { key: 'plays', color: 'var(--p-primary)', label: t('rp.plays') },
    { key: 'completes', color: 'var(--p-success)', label: t('rp.completes') },
  ];

  const W = 1000;
  const H = height;
  const PAD = 8;

  const max = Math.max(1, ...points.flatMap((p) => lines.map((s) => Number(p[s.key]) || 0)));
  const stepX = points.length > 1 ? (W - PAD * 2) / (points.length - 1) : 0;

  const pathOf = (key) =>
    points
      .map((p, i) => {
        const x = PAD + i * stepX;
        const y = H - PAD - ((Number(p[key]) || 0) / max) * (H - PAD * 2);
        return `${i === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
      })
      .join(' ');

  const top = formatValue ? formatValue(max) : max.toLocaleString();

  return (
    <div>
      <div className="flex gap-4 flex-wrap mb-3">
        {lines.map((s) => (
          <span key={s.key} className="flex items-center gap-2" style={{ fontSize: 12 }}>
            <span style={{ width: 10, height: 3, background: s.color, borderRadius: 2 }} />
            <span className="uz-muted">{s.label}</span>
          </span>
        ))}
      </div>

      <svg
        viewBox={`0 0 ${W} ${H}`}
        preserveAspectRatio="none"
        style={{ width: '100%', height, display: 'block' }}
        role="img"
        aria-label={t('rp.chart')}
      >
        {/* Yordamchi to'r */}
        {[0, 0.25, 0.5, 0.75, 1].map((f) => (
          <line key={f} x1={PAD} x2={W - PAD}
                y1={PAD + f * (H - PAD * 2)} y2={PAD + f * (H - PAD * 2)}
                stroke="var(--p-border-soft)" strokeWidth="1" />
        ))}

        {lines.map((s) => (
          <path key={s.key} d={pathOf(s.key)} fill="none"
                stroke={s.color} strokeWidth="2"
                strokeLinejoin="round" strokeLinecap="round"
                vectorEffect="non-scaling-stroke" />
        ))}
      </svg>

      <div className="flex justify-between uz-muted mt-1" style={{ fontSize: 11 }}>
        <span>{points[0].day}</span>
        {/* Eng yuqori qiymat ko'rsatilmasa, grafikning balandligi hech
            narsani anglatmasdi: ikkita bir xil ko'rinishdagi chiziq
            10 ta va 10 000 ta hodisani bildirishi mumkin edi. */}
        <span className="uz-mono">{t('rp.max')}: {top}</span>
        <span>{points[points.length - 1].day}</span>
      </div>
    </div>
  );
}
