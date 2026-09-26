import { useState } from 'react';
import { adminApi } from '../api/client';
import { usePanelI18n } from '../i18n';

export const MOBILE_PAYMENTS_KEY = 'mobile.payments.visible';

/**
 * Mobil ilovada to'lovga oid bo'limlarni ko'rsatish/yashirish — faqat SUPER_ADMIN.
 *
 * Default yashirin (`false`). Backend ham shu kalitni faqat SUPER_ADMIN va
 * undan yuqoriga o'zgartirishga ruxsat beradi (`MonetizationController`).
 */
export default function MobilePaymentsCard({ value, onChanged }) {
  const { t } = usePanelI18n();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const visible = value === 'true';

  const toggle = async () => {
    setSaving(true);
    setError(null);
    try {
      await adminApi.updateSetting(MOBILE_PAYMENTS_KEY, visible ? 'false' : 'true');
      onChanged?.();
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="uz-card p-5 mb-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div style={{ flex: '1 1 280px' }}>
          <div className="uz-h2 mb-1" style={{ fontSize: 15 }}>{t('mp.title')}</div>
          <p className="uz-muted text-sm">{t('mp.hint')}</p>
        </div>
        <label className="flex items-center gap-3" style={{ cursor: saving ? 'wait' : 'pointer' }}>
          <span className="text-sm" style={{ color: visible ? 'var(--p-success)' : 'var(--p-danger)' }}>
            {visible ? t('mp.visible') : t('mp.hidden')}
          </span>
          <button type="button"
                  role="switch"
                  aria-checked={visible}
                  aria-label={t('mp.title')}
                  disabled={saving}
                  onClick={toggle}
                  style={{
                    position: 'relative',
                    width: 48,
                    height: 26,
                    padding: 0,
                    border: 'none',
                    borderRadius: 13,
                    background: visible ? 'var(--p-success)' : 'var(--p-border)',
                    opacity: saving ? 0.6 : 1,
                    cursor: 'inherit',
                    transition: 'background 0.2s',
                  }}>
            <span style={{
              position: 'absolute',
              top: 3,
              left: visible ? 25 : 3,
              width: 20,
              height: 20,
              borderRadius: '50%',
              background: 'var(--p-knob)',
              boxShadow: 'var(--p-knob-shadow)',
              transition: 'left 0.2s',
            }} />
          </button>
        </label>
      </div>
      {error && (
        <div role="alert" className="mt-4 text-sm" style={{ color: 'var(--p-danger)' }}>
          {error.message}
        </div>
      )}
    </div>
  );
}
