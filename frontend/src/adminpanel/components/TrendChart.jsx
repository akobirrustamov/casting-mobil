import { usePanelI18n } from '../i18n';

/**
 * Kunlik dinamika grafigi — sof inline SVG.
 *
 * Grafik kutubxonasi qo'shilmadi: bitta oddiy chiziqli grafik uchun yangi
 * bog'liqlik ortiqcha (§70). Bir nechta turdagi grafik kerak bo'lganda
 * qayta ko'rib chiqiladi.
 */
export default function TrendChart({ points, height = 180 }) {
  const { t } = usePanelI18n();

  if (!points || points.length === 0) {
    return <p className="uz-muted" style={{ fontSize: 13 }}>{t('rp.noData')}</p>;
  }

  const W = 1000;
  const H = height;
  const PAD = 8;

  const series = [
    { key: 'views', color: 'var(--p-accent)', label: t('rp.views') },
    { key: 'plays', color: 'var(--p-primary)', label: t('rp.plays') },
    { key: 'completes', color: 'var(--p-success)', label: t('rp.completes') },
  ];

  const max = Math.max(1, ...points.flatMap((p) => series.map((s) => Number(p[s.key]) || 0)));
  const stepX = points.length > 1 ? (W - PAD * 2) / (points.length - 1) : 0;

  const pathOf = (key) =>
    points
      .map((p, i) => {
        const x = PAD + i * stepX;
        const y = H - PAD - ((Number(p[key]) || 0) / max) * (H - PAD * 2);
        return `${i === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
      })
      .join(' ');

  return (
    <div>
      <div className="flex gap-4 flex-wrap mb-3">
        {series.map((s) => (
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

        {series.map((s) => (
          <path key={s.key} d={pathOf(s.key)} fill="none"
                stroke={s.color} strokeWidth="2"
                strokeLinejoin="round" strokeLinecap="round"
                vectorEffect="non-scaling-stroke" />
        ))}
      </svg>

      <div className="flex justify-between uz-muted mt-1" style={{ fontSize: 11 }}>
        <span>{points[0].day}</span>
        <span>{points[points.length - 1].day}</span>
      </div>
    </div>
  );
}
