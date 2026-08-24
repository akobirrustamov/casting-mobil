import { usePanelI18n } from '../../i18n';
import { ALL_PERMISSIONS, PERMISSION_GROUPS } from './permissionGroups';

/**
 * WORKER ruxsatlarini tanlash (ТЗ §12, BOSQICH F1).
 *
 * <h2>Nega ruxsat nomi tarjima qilinmaydi</h2>
 * `CONTENT_PUBLISH` — bu backenddagi enum qiymatining O'ZI. U audit
 * jurnalida, xato xabarlarida va API javoblarida aynan shu ko'rinishda
 * chiqadi. Uni panelda «Kontentni nashr qilish» deb ko'rsatib,
 * jurnalda `CONTENT_PUBLISH` qoldirish admin uchun ikkita alohida
 * lug'at yaratardi va «qaysi ruxsat yetishmayapti?» degan savolga
 * javob topishni qiyinlashtirardi. Guruh sarlavhalari esa tarjima
 * qilinadi — ular navigatsiya uchun, identifikator emas.
 */
export default function PermissionPicker({ value, onChange, disabled = false }) {
  const { t } = usePanelI18n();
  const selected = new Set(value || []);

  const toggle = (permission) => {
    const next = new Set(selected);
    if (next.has(permission)) {
      next.delete(permission);
    } else {
      next.add(permission);
    }
    onChange(Array.from(next));
  };

  const toggleGroup = (group) => {
    const next = new Set(selected);
    const allOn = group.permissions.every((p) => next.has(p));
    group.permissions.forEach((p) => (allOn ? next.delete(p) : next.add(p)));
    onChange(Array.from(next));
  };

  return (
    <div>
      <div className="flex items-center justify-between gap-3 flex-wrap mb-3">
        <span className="uz-muted" style={{ fontSize: 12 }}>
          {t('staff.permissionsSelected', { n: selected.size })}
        </span>
        <div className="flex gap-2">
          <button
            type="button"
            className="uz-btn uz-btn-ghost"
            style={{ minHeight: 30, padding: '0 10px', fontSize: 12 }}
            disabled={disabled}
            onClick={() => onChange([...ALL_PERMISSIONS])}
          >
            {t('staff.selectAll')}
          </button>
          <button
            type="button"
            className="uz-btn uz-btn-ghost"
            style={{ minHeight: 30, padding: '0 10px', fontSize: 12 }}
            disabled={disabled}
            onClick={() => onChange([])}
          >
            {t('staff.clearAll')}
          </button>
        </div>
      </div>

      <div
        className="grid gap-3"
        style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))' }}
      >
        {PERMISSION_GROUPS.map((group) => {
          const on = group.permissions.filter((p) => selected.has(p)).length;
          return (
            <fieldset
              key={group.key}
              style={{
                border: '1px solid var(--p-border-soft)',
                borderRadius: 'var(--p-radius)',
                padding: '10px 12px 12px',
                minWidth: 0,
              }}
            >
              <legend
                className="flex items-center gap-2"
                style={{ fontSize: 12, fontWeight: 700, padding: '0 6px' }}
              >
                <button
                  type="button"
                  className="uz-btn uz-btn-ghost"
                  style={{ minHeight: 24, padding: '0 8px', fontSize: 11 }}
                  disabled={disabled}
                  onClick={() => toggleGroup(group)}
                >
                  {t(`staff.permGroup.${group.key}`)} · {on}/{group.permissions.length}
                </button>
              </legend>

              {group.permissions.map((permission) => (
                <label
                  key={permission}
                  className="uz-check"
                  style={{ fontSize: 12, marginTop: 4 }}
                >
                  <input
                    type="checkbox"
                    checked={selected.has(permission)}
                    disabled={disabled}
                    onChange={() => toggle(permission)}
                  />
                  <span className="uz-mono">{permission}</span>
                </label>
              ))}
            </fieldset>
          );
        })}
      </div>
    </div>
  );
}
