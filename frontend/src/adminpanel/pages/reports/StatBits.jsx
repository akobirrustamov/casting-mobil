import { usePanelI18n } from '../../i18n';
import { count } from '../../utils/format';

/**
 * Hisobot oynalarining umumiy kichik qismlari (BOSQICH F5).
 *
 * Uchala hisobot (reklama, kontent, bildirishnoma) bir xil ko'rinadi —
 * kartochkalar qatori, davr tanlash va kunlik jadval. Ularni har
 * oynada qayta yozish uchtasini asta-sekin bir-biridan farq qilib
 * ketishga olib borardi (§72).
 */

/** Davr tanlash — uchala hisobotda bir xil. */
export const STAT_PERIODS = [7, 30, 90];

export function PeriodPicker({ days, onChange, disabled }) {
  const { t } = usePanelI18n();
  return (
    <div className="flex gap-2 flex-wrap" role="group" aria-label={t('dash.period')}>
      {STAT_PERIODS.map((p) => (
        <button
          key={p}
          type="button"
          className={`uz-chip ${days === p ? 'selected' : ''}`}
          aria-pressed={days === p}
          disabled={disabled}
          onClick={() => onChange(p)}
        >
          {t('dash.days', { n: p })}
        </button>
      ))}
    </div>
  );
}

/**
 * Bitta ko'rsatkich.
 *
 * ⚠️ `value === null` va `value === 0` ATAYLAB har xil ko'rinadi.
 * Nol — «bo'lmadi», bo'sh katak esa «o'lchanmaydi». Ikkalasini bir xil
 * ko'rsatish admin uchun butunlay boshqa xulosaga olib borardi (§45).
 */
export function StatTile({ label, value, suffix, accent, note, unique }) {
  const { t } = usePanelI18n();
  const missing = value === null || value === undefined;
  return (
    <div className="uz-card p-4">
      <div className="uz-muted" style={{ fontSize: 12, fontWeight: 600 }}>{label}</div>
      {missing ? (
        <>
          <div style={{ fontSize: 20, fontWeight: 700, marginTop: 8, color: 'var(--p-disabled)' }}>
            —
          </div>
          <div className="uz-muted" style={{ fontSize: 11, marginTop: 4 }}>
            {t('stat.notMeasured')}
          </div>
        </>
      ) : (
        <div className="uz-mono" style={{
          fontSize: 26, fontWeight: 700, marginTop: 6, color: accent || 'var(--p-text)',
        }}>
          {typeof value === 'number' ? count(value) : value}
          {suffix && <span style={{ fontSize: 14, marginLeft: 4 }}>{suffix}</span>}
        </div>
      )}
      {!missing && unique != null && (
        <div className="uz-muted uz-mono" style={{ fontSize: 11, marginTop: 4 }}>
          {count(unique)} {t('stat.unique')}
        </div>
      )}
      {note && (
        <div className="uz-muted" style={{ fontSize: 11, marginTop: 6, lineHeight: 1.5 }}>
          {note}
        </div>
      )}
    </div>
  );
}

/** Davr sarlavhasi: qaysi sanadan qaysi sanagacha. */
export function RangeLine({ from, to }) {
  const { t } = usePanelI18n();
  if (!from || !to) return null;
  return (
    <p className="uz-muted uz-mono" style={{ fontSize: 12, margin: '0 0 12px' }}>
      {t('stat.range', { from, to })}
    </p>
  );
}
