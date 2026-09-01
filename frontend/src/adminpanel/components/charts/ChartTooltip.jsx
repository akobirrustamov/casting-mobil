import { usePanelI18n } from '../../i18n';

/**
 * Grafiklar uchun yagona tooltip.
 *
 * <h2>⚠️ Nega u umuman kerak</h2>
 * Eski grafikda tooltip YO'Q edi. Grafik faqat shaklni ko'rsatardi:
 * «o'sish bor» degan taassurot berardi, lekin «14-avgustda nechta
 * bo'lgan» degan savolga javob bermasdi. Aynan shu savol esa admin
 * uchun asosiy — u sonni bilishi kerak, egri chiziqni emas.
 *
 * <h2>⚠️ Matn SERIYA RANGIDA emas</h2>
 * Yozuv har doim oddiy matn rangida qoladi, yonidagi kichik nishon esa
 * qaysi qatorligini bildiradi. Rangli matn qorong'i fonda o'qilishi
 * qiyin va u kontrast tekshiruvidan o'tmaydi — nishon esa o'tadi,
 * chunki u shakl, harf emas.
 */
export default function ChartTooltip({ active, payload, label, formatValue }) {
  const { t } = usePanelI18n();

  if (!active || !payload || payload.length === 0) {
    return null;
  }

  const format = formatValue || ((v) => Number(v).toLocaleString());

  return (
    <div
      className="uz-card"
      style={{
        padding: '10px 12px',
        // ⚠️ Kartochkadan bir daraja yuqori yuza: tooltip grafik
        // USTIDA turadi va bir xil fonda u shunchaki «yopishib»
        // ko'rinardi.
        background: 'var(--p-surface-2)',
        border: '1px solid var(--p-border)',
        boxShadow: 'var(--p-shadow)',
        fontSize: 12,
        // Sichqoncha tooltipning o'ziga tegib, uni miltillatmasin.
        pointerEvents: 'none',
      }}
      role="tooltip"
    >
      <div className="uz-muted mb-2" style={{ fontSize: 11 }}>{label}</div>

      {payload.map((row) => (
        <div key={row.dataKey} className="flex items-center gap-2" style={{ marginTop: 4 }}>
          <span
            aria-hidden="true"
            style={{
              width: 8,
              height: 8,
              borderRadius: 2,
              background: row.color,
              flexShrink: 0,
            }}
          />
          <span style={{ color: 'var(--p-text)' }}>{row.name}</span>
          <span className="uz-mono" style={{ marginLeft: 'auto', color: 'var(--p-text)' }}>
            {format(row.value)}
          </span>
        </div>
      ))}

      {payload.length === 0 && <span className="uz-muted">{t('rp.noData')}</span>}
    </div>
  );
}
