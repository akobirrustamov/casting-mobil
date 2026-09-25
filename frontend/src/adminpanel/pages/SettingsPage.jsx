import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { PageHeader, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';
import SessionsCard from './SessionsCard';
import MobilePaymentsCard, { MOBILE_PAYMENTS_KEY } from './MobilePaymentsCard';

// Ro'yxat tartibi: mantiqiy guruhlar (narx → kurs → daromad → limit → bosh sahifa).
const ORDER = [
  'pricing.episode.default', 'pricing.premiere.default',
  'currency.coin.rate', 'currency.star.rate',
  'revenue.creator.percent',
  'account.device.limit', 'account.device.limit.web',
  'homepage.creators.ranking',
];
// Faqat belgilangan qiymatlarni qabul qiladigan sozlamalar — input o'rniga select.
const OPTIONS = { 'homepage.creators.ranking': ['MANUAL', 'STARS'] };

const rank = (key) => {
  const i = ORDER.indexOf(key);
  return i === -1 ? ORDER.length : i;
};

/**
 * Platforma sozlamalari: narxlar, kurslar, limitlar.
 *
 * Bu qiymatlar kodda emas — o'zgarish darhol kuchga kiradi (§23, §36, §40).
 */
export default function SettingsPage() {
  const { t } = usePanelI18n();
  const { can, atLeast } = useAuth();
  const { data, error, loading, reload } = useApi(() => adminApi.settings(), []);
  const [drafts, setDrafts] = useState({});
  const [saving, setSaving] = useState(null);
  const [saveError, setSaveError] = useState(null);

  const canEdit = can('SETTINGS_EDIT');

  // Tarjima bo'lmasa (yangi kalit) — backenddagi kalit/tavsifga qaytamiz.
  const tr = (key, fallback) => {
    const v = t(key);
    return v === key ? fallback : v;
  };

  const save = async (key) => {
    setSaving(key);
    setSaveError(null);
    try {
      await adminApi.updateSetting(key, drafts[key]);
      setDrafts((d) => {
        const next = { ...d };
        delete next[key];
        return next;
      });
      reload();
    } catch (err) {
      setSaveError(err);
    } finally {
      setSaving(null);
    }
  };

  return (
    <>
      <PageHeader title={t('st.title')} subtitle={t('st.subtitle')} />
      <p className="uz-muted mb-4 text-sm">{t('st.hint')}</p>

      {atLeast('SUPER_ADMIN') && data && (
        <MobilePaymentsCard
          value={data.find((s) => s.key === MOBILE_PAYMENTS_KEY)?.value}
          onChanged={reload}
        />
      )}

      {saveError && (
        <div role="alert" className="mb-4 px-4 py-3"
             style={{ borderRadius: 'var(--p-radius)', background: 'var(--danger-soft)',
                      border: '1px solid var(--danger-border)', color: 'var(--p-danger)', fontSize: 13 }}>
          {saveError.message}
        </div>
      )}

      <div className="uz-card overflow-hidden">
        {loading ? <LoadingState /> :
         error ? <ErrorState error={error} onRetry={reload} /> :
         !data?.length ? <EmptyState icon="⚙️" /> : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  <th>{t('st.key')}</th>
                  <th style={{ width: 200 }}>{t('st.value')}</th>
                  <th>{t('st.description')}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.filter((s) => s.key !== MOBILE_PAYMENTS_KEY).sort((a, b) => rank(a.key) - rank(b.key) || a.key.localeCompare(b.key)).map((s) => {
                  const draft = drafts[s.key];
                  const dirty = draft !== undefined && draft !== s.value;
                  return (
                    <tr key={s.key}>
                      <td>
                        <div style={{ fontWeight: 600, fontSize: 13 }}>{tr(`st.k.${s.key}`, s.key)}</div>
                        <div className="uz-mono uz-muted" style={{ fontSize: 11, marginTop: 2 }}>{s.key}</div>
                      </td>
                      <td>
                        {OPTIONS[s.key] ? (
                          <select className="uz-input" style={{ minHeight: 36 }}
                                  value={draft ?? s.value} disabled={!canEdit}
                                  onChange={(e) => setDrafts({ ...drafts, [s.key]: e.target.value })}>
                            {OPTIONS[s.key].map((o) => (
                              <option key={o} value={o}>{tr(`st.opt.${o}`, o)}</option>
                            ))}
                          </select>
                        ) : (
                          <input className="uz-input uz-mono" style={{ minHeight: 36 }}
                                 value={draft ?? s.value} disabled={!canEdit}
                                 onChange={(e) => setDrafts({ ...drafts, [s.key]: e.target.value })} />
                        )}
                      </td>
                      <td className="uz-muted" style={{ fontSize: 12 }}>{tr(`st.d.${s.key}`, s.description)}</td>
                      <td style={{ textAlign: 'right' }}>
                        {canEdit && dirty && (
                          <button type="button" className="uz-btn uz-btn-primary"
                                  style={{ minHeight: 32, padding: '0 14px', fontSize: 12 }}
                                  disabled={saving === s.key}
                                  onClick={() => save(s.key)}>
                            {saving === s.key ? t('common.saving') : t('common.save')}
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </TableWrap>
        )}
      </div>

      {atLeast('SUPER_ADMIN') && <SessionsCard />}
    </>
  );
}
