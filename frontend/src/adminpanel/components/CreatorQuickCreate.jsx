import { useEffect, useState } from 'react';
import { adminApi } from '../api/client';
import { useFieldErrors } from '../api/useFieldErrors';
import { LOCALES, toBackendLocale, usePanelI18n } from '../i18n';
import LocaleTabs from './LocaleTabs';
import MediaField from './MediaField';
import Modal from './Modal';

/**
 * Ijodkorni JOYIDA yaratish (ТЗ §54).
 *
 * <h2>Nima uchun kerak</h2>
 * Kontent qo'shayotgan admin kerakli ijodkorni topolmasa, ilgari
 * muharrirni tark etib, «Ijodkorlar» bo'limiga o'tib, u yerda yaratib,
 * keyin qaytishi kerak edi — va saqlanmagan o'zgarishlarini yo'qotardi.
 *
 * <h2>⚠️ Nima uchun uchala til so'raladi</h2>
 * Backend FAOL ijodkor uchun ismni uchala tilda talab qiladi: u kontent
 * sahifasida va «Mashhur ijodkorlar» bo'limida chiqadi.
 *
 * Faqat o'zbekchasini so'rab, ijodkorni NOFAOL yaratish ham mumkin edi —
 * lekin u holda kontentga biriktirilgan ijodkor hech qayerda ko'rinmasdi
 * va admin buning sababini tushunmasdi. Uch qisqa maydon so'rash shundan
 * yaxshiroq.
 */
export default function CreatorQuickCreate({ open, initialName, onClose, onCreated }) {
  const { t, locale } = usePanelI18n();
  const [form, setForm] = useState({ photoMediaId: null, translations: {} });
  const [tab, setTab] = useState(locale);
  const [saving, setSaving] = useState(false);
  const fields = useFieldErrors();

  // Oyna ochilganda qidiruv matni o'zbekcha ismga oldindan qo'yiladi:
  // admin uni ikkinchi marta yozmasin.
  useEffect(() => {
    if (!open) return;
    setForm({
      photoMediaId: null,
      translations: initialName ? { UZ: { displayName: initialName } } : {},
    });
    setTab(locale);
    fields.clear();
    // `fields` har renderda yangilanadi — bog'liqlikka qo'shilsa halqa
    // hosil bo'lardi.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, initialName, locale]);

  const bl = toBackendLocale(tab);
  const tr = form.translations?.[bl] || {};

  const setName = (key, value) => setForm((prev) => ({
    ...prev,
    translations: {
      ...prev.translations,
      [bl]: { ...(prev.translations?.[bl] || {}), [key]: value },
    },
  }));

  /** Shu tilda ism bormi — ism yoki familiya ham yetarli. */
  const filled = (code) => {
    const n = form.translations?.[code] || {};
    return Boolean((n.displayName || '').trim()
      || ((n.firstName || '').trim() + (n.lastName || '').trim()));
  };

  const missing = LOCALES.map(toBackendLocale).filter((code) => !filled(code));

  async function save() {
    setSaving(true);
    fields.clear();
    try {
      const created = await adminApi.createCreator({
        photoMediaId: form.photoMediaId,
        active: true,
        featured: false,
        sortOrder: 0,
        translations: form.translations,
      });
      // ⚠️ Yaratilgach DARHOL kontentga biriktiriladi (ТЗ §54) — admin
      // uni ro'yxatdan qayta qidirishi shart emas.
      onCreated(created);
      onClose();
    } catch (err) {
      fields.apply(err);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open}
      title={t('cr.quickCreate')}
      onClose={saving ? () => {} : onClose}
      width={560}
      footer={
        <>
          <button type="button" className="uz-btn uz-btn-ghost"
                  onClick={onClose} disabled={saving}>
            {t('common.cancel')}
          </button>
          <button type="button" className="uz-btn uz-btn-primary"
                  onClick={save} disabled={saving || missing.length > 0}>
            {saving ? t('common.saving') : t('cr.createAndAttach')}
          </button>
        </>
      }
    >
      <MediaField
        label={t('cr.photo')}
        value={form.photoMediaId}
        spec="creatorPhoto"
        type="IMAGE"
        onChange={(id) => setForm((prev) => ({ ...prev, photoMediaId: id }))}
      />

      <div className="mt-4">
        <LocaleTabs active={tab} onChange={setTab} isFilled={filled} />
      </div>

      <div className="uz-row mt-4">
        <div className="uz-col">
          <label className="uz-label" htmlFor="qc-fn">{t('cr.firstName')}</label>
          <input id="qc-fn" className="uz-input" value={tr.firstName || ''}
                 onChange={(e) => setName('firstName', e.target.value)} />
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="qc-ln">{t('cr.lastName')}</label>
          <input id="qc-ln" className="uz-input" value={tr.lastName || ''}
                 onChange={(e) => setName('lastName', e.target.value)} />
        </div>
      </div>

      <div className="uz-row">
        <div className="uz-col">
          <label className="uz-label" htmlFor="qc-mn">{t('cr.middleName')}</label>
          <input id="qc-mn" className="uz-input" value={tr.middleName || ''}
                 onChange={(e) => setName('middleName', e.target.value)} />
        </div>
        <div className="uz-col">
          <label className="uz-label" htmlFor="qc-dn">{t('cr.displayName')}</label>
          <input id="qc-dn" className="uz-input" value={tr.displayName || ''}
                 onChange={(e) => setName('displayName', e.target.value)} />
        </div>
      </div>

      {/* ⚠️ Uchala til kerakligi TUGMANI BOSISHDAN OLDIN aytiladi.
          Aks holda admin formani to'ldirib, saqlashga bosgandan keyin
          xato ko'rardi va qaysi tabga qaytishni o'zi topishi kerak
          bo'lardi. */}
      {missing.length > 0 && (
        <div className="uz-field-error mt-3">
          {t('cr.missingLocales')}: {missing.join(', ')}
        </div>
      )}

      {fields.formError && (
        <div className="uz-field-error mt-3">{fields.formError.message}</div>
      )}
    </Modal>
  );
}
