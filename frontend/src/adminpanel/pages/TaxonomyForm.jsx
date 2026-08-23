import { useEffect, useState } from 'react';
import { useFieldErrors } from '../api/useFieldErrors';
import { adminApi } from '../api/client';
import LocaleTabs from '../components/LocaleTabs';
import MediaField from '../components/MediaField';
import Modal from '../components/Modal';
import { usePanelI18n } from '../i18n';

const empty = () => ({
  slug: '',
  sortOrder: 0,
  active: true,
  iconMediaId: null,
  translations: { UZ: {}, RU: {}, EN: {} },
});

/** Kategoriya va janr uchun umumiy forma - maydonlari deyarli bir xil. */
export default function TaxonomyForm({ open, kind, row, onClose, onSaved }) {
  const { t } = usePanelI18n();
  const isCategory = kind === 'category';
  const isEdit = Boolean(row?.id);

  const [locale, setLocale] = useState('UZ');
  const [form, setForm] = useState(empty);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Backend maydon xatolari (ТЗ §52).
  const fields = useFieldErrors();

  useEffect(() => {
    if (!open) return;
    setLocale('UZ');
    setError(null);
    setForm(row
      ? {
          slug: '',
          sortOrder: row.sortOrder ?? 0,
          active: row.active !== false,
          iconMediaId: row.iconMediaId ?? null,
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

  const uzMissing = !form.translations?.UZ?.title?.trim();

  // Backend ichma-ich maydonni `translations[UZ].name` deb yuboradi.
  const nameError = fields.errorOf(`translations[${locale}].name`)
    || fields.errorOf('translations');

  async function save() {
    if (uzMissing) {
      setLocale('UZ');
      setError({ message: t('editor.uzRequired') });
      return;
    }
    setSaving(true);
    setError(null);
    const payload = {
      slug: form.slug || undefined,
      sortOrder: Number(form.sortOrder) || 0,
      active: form.active,
      iconMediaId: isCategory ? form.iconMediaId : undefined,
      translations: form.translations,
    };
    try {
      if (isCategory) {
        if (isEdit) await adminApi.updateCategory(row.id, payload);
        else await adminApi.createCategory(payload);
      } else if (isEdit) {
        await adminApi.updateGenre(row.id, payload);
      } else {
        await adminApi.createGenre(payload);
      }
      onSaved();
      onClose();
    } catch (err) {
      // Backend AYNAN qaysi maydon noto'g'ri ekanini aytadi (§52) —
      // umumiy xabar o'rniga uni maydon yoniga qo'yamiz.
      fields.apply(err);
      setError(err);
    } finally {
      setSaving(false);
    }
  }

  const title = isCategory
    ? (isEdit ? t('tax.editCategory') : t('tax.newCategory'))
    : (isEdit ? t('tax.editGenre') : t('tax.newGenre'));

  return (
    <Modal
      open={open}
      title={title}
      onClose={onClose}
      width={620}
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
      <LocaleTabs
        active={locale}
        onChange={setLocale}
        isFilled={(code) => Boolean(form.translations?.[code]?.title?.trim())}
      />

      <div className="mb-4">
        <label className="uz-label" htmlFor="tx-name">
          {t('tax.name')}
          {locale === 'UZ' && <span style={{ color: 'var(--p-danger)' }}> *</span>}
        </label>
        <input id="tx-name" className="uz-input"
               value={form.translations?.[locale]?.title || ''}
               aria-invalid={Boolean(nameError) || (locale === 'UZ' && uzMissing)}
               onChange={(e) => setTr('title', e.target.value)} />
        {/* Avval BACKEND xatosi: u aniqroq va u haqiqiy rad etish sababi.
            Klient tekshiruvi esa faqat yuborishdan oldingi ogohlantirish. */}
        {nameError ? (
          <div className="uz-field-error">{nameError}</div>
        ) : locale === 'UZ' && uzMissing ? (
          <div className="uz-field-error">{t('editor.uzRequired')}</div>
        ) : null}
      </div>

      {isCategory && (
        <div className="mb-4">
          <label className="uz-label" htmlFor="tx-desc">{t('tax.description')}</label>
          <textarea id="tx-desc" className="uz-input" rows={3} style={{ resize: 'vertical' }}
                    value={form.translations?.[locale]?.description || ''}
                    onChange={(e) => setTr('description', e.target.value)} />
        </div>
      )}

      <div className="uz-row">
        <div className="uz-col">
          <label className="uz-label" htmlFor="tx-order">{t('common.sortOrder')}</label>
          <input id="tx-order" className="uz-input" type="number" value={form.sortOrder}
                 onChange={(e) => setForm({ ...form, sortOrder: e.target.value })} />
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="tx-slug">{t('editor.slug')}</label>
          <input id="tx-slug" className="uz-input" value={form.slug} placeholder="auto"
                 onChange={(e) => setForm({ ...form, slug: e.target.value })} />
        </div>
        <div className="uz-col">
          <label className="uz-check">
            <input type="checkbox" checked={form.active}
                   onChange={(e) => setForm({ ...form, active: e.target.checked })} />
            {t('common.active')}
          </label>
        </div>
      </div>

      {isCategory && (
        <div className="mt-4" style={{ maxWidth: 260 }}>
          <MediaField label={t('tax.icon')} value={form.iconMediaId}
                      onChange={(id) => setForm({ ...form, iconMediaId: id })} />
        </div>
      )}
    </Modal>
  );
}
