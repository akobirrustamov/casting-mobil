import { useEffect, useState } from 'react';
import { adminApi } from '../api/client';
import LocaleTabs from '../components/LocaleTabs';
import MediaField from '../components/MediaField';
import Modal from '../components/Modal';
import { usePanelI18n } from '../i18n';

const empty = () => ({
  slug: '',
  photoMediaId: null,
  coverMediaId: null,
  birthDate: '',
  active: true,
  featured: false,
  sortOrder: 0,
  translations: { UZ: {}, RU: {}, EN: {} },
});

export default function CreatorForm({ open, row, onClose, onSaved }) {
  const { t } = usePanelI18n();
  const isEdit = Boolean(row?.id);

  const [locale, setLocale] = useState('UZ');
  const [form, setForm] = useState(empty);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open) return;
    setLocale('UZ');
    setError(null);
    setForm(row
      ? {
          slug: '',
          photoMediaId: row.photoMediaId ?? null,
          coverMediaId: row.coverMediaId ?? null,
          birthDate: row.birthDate || '',
          active: row.active !== false,
          featured: Boolean(row.featured),
          sortOrder: row.sortOrder ?? 0,
          translations: { UZ: {}, RU: {}, EN: {}, ...(row.translations || {}) },
        }
      : empty());
  }, [open, row]);

  const setTr = (field, value) =>
    setForm((prev) => ({
      ...prev,
      translations: {
        ...prev.translations,
        [locale]: { ...(prev.translations[locale] || {}), [field]: value },
      },
    }));

  /** displayName bo'sh bo'lsa ism+familiya hisobga olinadi - backend ham shunday qiladi. */
  const filled = (code) => {
    const tr = form.translations?.[code] || {};
    return Boolean(
      (tr.displayName || '').trim() ||
      ((tr.firstName || '').trim() + (tr.lastName || '').trim())
    );
  };

  async function save() {
    if (!filled('UZ')) {
      setLocale('UZ');
      setError({ message: t('editor.uzRequired') });
      return;
    }
    setSaving(true);
    setError(null);
    const payload = {
      slug: form.slug || undefined,
      photoMediaId: form.photoMediaId,
      coverMediaId: form.coverMediaId,
      birthDate: form.birthDate || null,
      active: form.active,
      featured: form.featured,
      sortOrder: Number(form.sortOrder) || 0,
      translations: form.translations,
    };
    try {
      if (isEdit) await adminApi.updateCreator(row.id, payload);
      else await adminApi.createCreator(payload);
      onSaved();
      onClose();
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  }

  const tr = form.translations?.[locale] || {};

  return (
    <Modal
      open={open}
      title={isEdit ? t('cr.edit') : t('cr.new')}
      onClose={onClose}
      width={720}
      footer={
        <>
          {error && (
            <span style={{ color: 'var(--p-danger)', fontSize: 13, marginRight: 'auto' }} role="alert">
              {error.message}
            </span>
          )}
          <button type="button" className="uz-btn uz-btn-ghost" onClick={onClose}>
            {t('common.cancel')}
          </button>
          <button type="button" className="uz-btn uz-btn-primary" onClick={save} disabled={saving}>
            {saving ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <LocaleTabs active={locale} onChange={setLocale} isFilled={filled} />

      <div className="uz-row mb-4">
        <div className="uz-col">
          <label className="uz-label" htmlFor="cr-fn">{t('cr.firstName')}</label>
          <input id="cr-fn" className="uz-input" value={tr.firstName || ''}
                 onChange={(e) => setTr('firstName', e.target.value)} />
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="cr-ln">{t('cr.lastName')}</label>
          <input id="cr-ln" className="uz-input" value={tr.lastName || ''}
                 onChange={(e) => setTr('lastName', e.target.value)} />
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="cr-mn">{t('cr.middleName')}</label>
          <input id="cr-mn" className="uz-input" value={tr.middleName || ''}
                 onChange={(e) => setTr('middleName', e.target.value)} />
        </div>
      </div>

      <div className="mb-4">
        <label className="uz-label" htmlFor="cr-dn">{t('cr.displayName')}</label>
        <input id="cr-dn" className="uz-input" value={tr.displayName || ''}
               onChange={(e) => setTr('displayName', e.target.value)} />
        <p className="uz-muted mt-1" style={{ fontSize: 11 }}>{t('cr.displayHint')}</p>
      </div>

      <div className="mb-5">
        <label className="uz-label" htmlFor="cr-bio">{t('cr.bio')}</label>
        <textarea id="cr-bio" className="uz-input" rows={3} style={{ resize: 'vertical' }}
                  value={tr.bio || ''} onChange={(e) => setTr('bio', e.target.value)} />
      </div>

      <div className="uz-row mb-4">
        <div className="uz-col"><MediaField label={t('cr.photo')} value={form.photoMediaId}
              onChange={(id) => setForm({ ...form, photoMediaId: id })} /></div>
        <div className="uz-col"><MediaField label={t('cr.cover')} value={form.coverMediaId}
              onChange={(id) => setForm({ ...form, coverMediaId: id })} /></div>
      </div>

      <div className="uz-row">
        <div className="uz-col">
          <label className="uz-label" htmlFor="cr-bd">{t('cr.birthDate')}</label>
          <input id="cr-bd" className="uz-input" type="date" value={form.birthDate}
                 onChange={(e) => setForm({ ...form, birthDate: e.target.value })} />
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="cr-so">{t('common.sortOrder')}</label>
          <input id="cr-so" className="uz-input" type="number" value={form.sortOrder}
                 onChange={(e) => setForm({ ...form, sortOrder: e.target.value })} />
        </div>
        <div className="uz-col flex gap-4 flex-wrap">
          <label className="uz-check">
            <input type="checkbox" checked={form.featured}
                   onChange={(e) => setForm({ ...form, featured: e.target.checked })} />
            {t('creators.featured')}
          </label>
          <label className="uz-check">
            <input type="checkbox" checked={form.active}
                   onChange={(e) => setForm({ ...form, active: e.target.checked })} />
            {t('common.active')}
          </label>
        </div>
      </div>
    </Modal>
  );
}
