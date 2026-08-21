import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import LocaleTabs from '../components/LocaleTabs';
import Modal from '../components/Modal';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, TableWrap } from '../components/Ui';
import { LOCALES, toBackendLocale, usePanelI18n } from '../i18n';

/**
 * Bosh sahifa bo'limlari.
 *
 * Mobil ilova bosh sahifasi klientda qotirilmaydi — u shu ro'yxatdan quriladi.
 * Shu sababli bo'limni yoqish/o'chirish oddiy toggle bilan bo'ladi.
 */
export default function HomepagePage() {
  const { t, locale } = usePanelI18n();
  const { can } = useAuth();
  const bl = toBackendLocale(locale);

  const { data, error, loading, reload } = useApi(() => adminApi.homepageSections(), []);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(null);
  const [formLocale, setFormLocale] = useState('UZ');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);

  const canEdit = can('HOMEPAGE_EDIT');

  const titleOf = (s) =>
    s.translations?.[bl]?.title || s.translations?.UZ?.title || s.type;

  const open = (section) => {
    setEditing(section);
    setFormLocale('UZ');
    setSaveError(null);
    setForm({
      enabled: section.enabled !== false,
      sortOrder: section.sortOrder ?? 0,
      itemLimit: section.itemLimit ?? '',
      translations: { UZ: {}, RU: {}, EN: {}, ...(section.translations || {}) },
    });
  };

  /** Toggle ro'yxatning o'zida — modal ochmasdan. */
  const toggle = async (section) => {
    try {
      await adminApi.updateHomepageSection(section.id, {
        enabled: !section.enabled,
        sortOrder: section.sortOrder,
        itemLimit: section.itemLimit,
        translations: section.translations,
      });
      reload();
    } catch (err) {
      setSaveError(err);
    }
  };

  const save = async () => {
    setSaving(true);
    setSaveError(null);
    try {
      await adminApi.updateHomepageSection(editing.id, {
        enabled: form.enabled,
        sortOrder: Number(form.sortOrder) || 0,
        itemLimit: form.itemLimit === '' ? null : Number(form.itemLimit),
        translations: form.translations,
      });
      setEditing(null);
      reload();
    } catch (err) {
      setSaveError(err);
    } finally {
      setSaving(false);
    }
  };

  const setTr = (value) =>
    setForm((p) => ({
      ...p,
      translations: { ...p.translations, [formLocale]: { title: value } },
    }));

  return (
    <>
      <PageHeader title={t('hp.title')} subtitle={t('hp.subtitle')} />

      <p className="uz-muted mb-4 text-sm">{t('hp.hint')}</p>

      {saveError && (
        <div role="alert" className="mb-4 px-4 py-3"
             style={{ borderRadius: 'var(--p-radius)', background: 'rgba(248,113,113,.10)',
                      border: '1px solid rgba(248,113,113,.35)', color: 'var(--p-danger)', fontSize: 13 }}>
          {saveError.message}
        </div>
      )}

      <div className="uz-card overflow-hidden">
        {loading ? <LoadingState /> :
         error ? <ErrorState error={error} onRetry={reload} /> :
         !data?.length ? <EmptyState icon="▦" /> : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  <th>{t('common.sortOrder')}</th>
                  <th>{t('hp.section')}</th>
                  {LOCALES.map((l) => <th key={l}>{l.toUpperCase()}</th>)}
                  <th>{t('hp.itemLimit')}</th>
                  <th>{t('hp.enabled')}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.map((s) => (
                  <tr key={s.id} style={{ opacity: s.enabled ? 1 : 0.5 }}>
                    <td className="uz-mono uz-muted">{s.sortOrder}</td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{titleOf(s)}</div>
                      <div className="uz-muted" style={{ fontSize: 11 }}>{s.type}</div>
                    </td>
                    {LOCALES.map((l) => {
                      const v = s.translations?.[toBackendLocale(l)]?.title;
                      return (
                        <td key={l} style={{ fontSize: 13 }}>
                          {v || <span style={{ color: 'var(--p-danger)' }}>—</span>}
                        </td>
                      );
                    })}
                    <td className="uz-mono uz-muted">{s.itemLimit ?? '—'}</td>
                    <td>
                      <Badge tone={s.enabled ? 'published' : 'draft'}>
                        {s.enabled ? t('common.active') : t('common.inactive')}
                      </Badge>
                    </td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                      {canEdit && (
                        <>
                          <button type="button" className="uz-btn uz-btn-ghost"
                                  style={{ minHeight: 32, padding: '0 12px', fontSize: 12 }}
                                  onClick={() => toggle(s)}>
                            {s.enabled ? '⏻' : '⏼'}
                          </button>
                          <button type="button" className="uz-btn uz-btn-ghost"
                                  style={{ minHeight: 32, padding: '0 12px', fontSize: 12, marginLeft: 8 }}
                                  onClick={() => open(s)}>
                            {t('common.edit')}
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableWrap>
        )}
      </div>

      <Modal
        open={Boolean(editing)}
        title={t('hp.editSection')}
        onClose={() => setEditing(null)}
        width={620}
        footer={
          <>
            {saveError && (
              <span style={{ color: 'var(--p-danger)', fontSize: 13, marginRight: 'auto' }} role="alert">
                {saveError.message}
              </span>
            )}
            <button type="button" className="uz-btn uz-btn-ghost" onClick={() => setEditing(null)}>
              {t('common.cancel')}
            </button>
            <button type="button" className="uz-btn uz-btn-primary" onClick={save} disabled={saving}>
              {saving ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        {form && (
          <>
            <p className="uz-muted mb-4" style={{ fontSize: 12 }}>{editing?.type}</p>

            <LocaleTabs active={formLocale} onChange={setFormLocale}
                        isFilled={(c) => Boolean(form.translations?.[c]?.title?.trim())} />

            <div className="mb-4">
              <label className="uz-label" htmlFor="hp-ti">{t('editor.title')}</label>
              <input id="hp-ti" className="uz-input"
                     value={form.translations?.[formLocale]?.title || ''}
                     onChange={(e) => setTr(e.target.value)} />
            </div>

            <div className="uz-row">
              <div className="uz-col">
                <label className="uz-label" htmlFor="hp-so">{t('common.sortOrder')}</label>
                <input id="hp-so" className="uz-input" type="number" value={form.sortOrder}
                       onChange={(e) => setForm({ ...form, sortOrder: e.target.value })} />
              </div>
              <div className="uz-col">
                <label className="uz-label" htmlFor="hp-il">{t('hp.itemLimit')}</label>
                <input id="hp-il" className="uz-input" type="number" min="1" value={form.itemLimit}
                       onChange={(e) => setForm({ ...form, itemLimit: e.target.value })} />
              </div>
              <div className="uz-col">
                <label className="uz-check">
                  <input type="checkbox" checked={form.enabled}
                         onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />
                  {t('hp.enabled')}
                </label>
              </div>
            </div>
          </>
        )}
      </Modal>
    </>
  );
}
