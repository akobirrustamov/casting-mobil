import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { PageHeader, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';

/**
 * Platforma sozlamalari: narxlar, kurslar, limitlar.
 *
 * Bu qiymatlar kodda emas — o'zgarish darhol kuchga kiradi (§23, §36, §40).
 */
export default function SettingsPage() {
  const { t } = usePanelI18n();
  const { can } = useAuth();
  const { data, error, loading, reload } = useApi(() => adminApi.settings(), []);
  const [drafts, setDrafts] = useState({});
  const [saving, setSaving] = useState(null);
  const [saveError, setSaveError] = useState(null);

  const canEdit = can('SETTINGS_EDIT');

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
                {[...data].sort((a, b) => a.key.localeCompare(b.key)).map((s) => {
                  const draft = drafts[s.key];
                  const dirty = draft !== undefined && draft !== s.value;
                  return (
                    <tr key={s.key}>
                      <td className="uz-mono" style={{ fontSize: 12 }}>{s.key}</td>
                      <td>
                        <input className="uz-input uz-mono" style={{ minHeight: 36 }}
                               value={draft ?? s.value} disabled={!canEdit}
                               onChange={(e) => setDrafts({ ...drafts, [s.key]: e.target.value })} />
                      </td>
                      <td className="uz-muted" style={{ fontSize: 12 }}>{s.description}</td>
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
    </>
  );
}
