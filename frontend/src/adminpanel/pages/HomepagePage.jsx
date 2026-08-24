import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import LocaleTabs from '../components/LocaleTabs';
import Modal from '../components/Modal';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, TableWrap } from '../components/Ui';
import { LOCALES, toBackendLocale, usePanelI18n } from '../i18n';
import CreatorsPreviewModal from './homepage/CreatorsPreviewModal';
import SectionItemsModal from './homepage/SectionItemsModal';

/**
 * Bosh sahifa bo'limlari (ТЗ §31 — BOSQICH F4).
 *
 * Mobil ilova bosh sahifasi klientda qotirilmaydi — u shu ro'yxatdan quriladi.
 * Shu sababli bo'limni yoqish/o'chirish oddiy toggle bilan bo'ladi.
 *
 * <h2>Tartib alohida saqlanadi</h2>
 * «Yuqoriga / pastga» tugmalari faqat MAHALLIY ro'yxatni o'zgartiradi;
 * bazaga esa «Tartibni saqlash» bosilganda BITTA so'rov ketadi. Har
 * bosishda alohida `PUT` yuborilsa, oradagi lahzada ikkita bo'lim bir
 * xil raqamda turardi va o'sha paytda `/app/home` ni so'ragan
 * foydalanuvchi aralashib ketgan bosh sahifani ko'rardi.
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

  const [itemsFor, setItemsFor] = useState(null);
  const [creatorsFor, setCreatorsFor] = useState(null);

  /**
   * Mahalliy tartib — ID lar ro'yxati sifatida.
   *
   * ⚠️ Bo'lim OBYEKTLARI emas, aynan ID lar. Toggle bosilganda ro'yxat
   * serverdan qayta yuklanadi va obyektlar almashadi; ID lar esa
   * o'zgarmaydi, ya'ni saqlanmagan tartib yo'qolmaydi.
   */
  const [orderIds, setOrderIds] = useState(null);

  const canEdit = can('HOMEPAGE_EDIT');
  const sections = data || [];

  const ordered = orderIds
    ? orderIds
      .map((id) => sections.find((s) => s.id === id))
      .filter(Boolean)
      // Ro'yxat tuzilgandan keyin qo'shilgan bo'lim yo'qolib qolmasin.
      .concat(sections.filter((s) => !orderIds.includes(s.id)))
    : sections;

  const orderDirty = Boolean(orderIds)
    && sections.map((s) => s.id).join(',') !== ordered.map((s) => s.id).join(',');

  const move = (index, delta) => {
    const ids = ordered.map((s) => s.id);
    const target = index + delta;
    if (target < 0 || target >= ids.length) return;
    [ids[index], ids[target]] = [ids[target], ids[index]];
    setOrderIds(ids);
  };

  const saveOrder = async () => {
    setSaving(true);
    setSaveError(null);
    try {
      await adminApi.reorderHomepageSections(ordered.map((s) => s.id));
      setOrderIds(null);
      reload();
    } catch (err) {
      setSaveError(err);
    } finally {
      setSaving(false);
    }
  };

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
      <PageHeader
        title={t('hp.title')}
        subtitle={t('hp.subtitle')}
        right={canEdit && orderDirty && (
          <div className="flex items-center gap-3 flex-wrap">
            <span style={{ fontSize: 13, color: 'var(--p-warning)' }}>{t('hp.orderDirty')}</span>
            <button type="button" className="uz-btn uz-btn-ghost" disabled={saving}
                    onClick={() => setOrderIds(null)}>
              {t('hp.resetOrder')}
            </button>
            <button type="button" className="uz-btn uz-btn-primary" disabled={saving}
                    onClick={saveOrder}>
              {saving ? t('common.saving') : t('hp.saveOrder')}
            </button>
          </div>
        )}
      />

      <p className="uz-muted mb-2 text-sm">{t('hp.hint')}</p>
      <p className="uz-muted mb-4" style={{ fontSize: 12 }}>{t('hp.orderNote')}</p>

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
         !ordered.length ? <EmptyState icon="▦" /> : (
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
                {ordered.map((s, i) => (
                  <tr key={s.id} style={{ opacity: s.enabled ? 1 : 0.5 }}>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <span className="uz-mono uz-muted" style={{ marginRight: 8 }}>{i + 1}</span>
                      {canEdit && (
                        <>
                          <button type="button" className="uz-icon-btn" onClick={() => move(i, -1)}
                                  disabled={i === 0 || saving}
                                  title={t('hp.moveUp')} aria-label={t('hp.moveUp')}>
                            ↑
                          </button>
                          <button type="button" className="uz-icon-btn" onClick={() => move(i, 1)}
                                  disabled={i === ordered.length - 1 || saving}
                                  title={t('hp.moveDown')} aria-label={t('hp.moveDown')}>
                            ↓
                          </button>
                        </>
                      )}
                    </td>
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
                      {/* «Mashhur ijodkorlar» qatori kontent bilan
                          to'ldirilmaydi — u `featured` bayrog'i va
                          reyting sozlamasidan quriladi. Shuning uchun
                          u yerda «Kontent» emas, «Ijodkorlar» ko'rinadi. */}
                      {s.type === 'POPULAR_CREATORS' ? (
                        <button type="button" className="uz-btn uz-btn-ghost"
                                style={{ minHeight: 32, padding: '0 12px', fontSize: 12 }}
                                onClick={() => setCreatorsFor(s)}>
                          {t('hp.creators')}
                        </button>
                      ) : (
                        <button type="button" className="uz-btn uz-btn-ghost"
                                style={{ minHeight: 32, padding: '0 12px', fontSize: 12 }}
                                onClick={() => setItemsFor(s)}>
                          {t('hp.items')}
                        </button>
                      )}
                      {canEdit && (
                        <>
                          <button type="button" className="uz-btn uz-btn-ghost"
                                  style={{ minHeight: 32, padding: '0 12px', fontSize: 12, marginLeft: 8 }}
                                  onClick={() => toggle(s)}
                                  title={s.enabled ? t('common.inactive') : t('common.active')}
                                  aria-label={s.enabled ? t('common.inactive') : t('common.active')}>
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

      {/* Shartli chizish: oyna o'z ro'yxatini har ochilishda yangidan
          yuklaydi — boshqa admin shu orada qatorni o'zgartirgan bo'lishi
          mumkin. */}
      {itemsFor && (
        <SectionItemsModal
          section={itemsFor}
          onClose={() => setItemsFor(null)}
          onSaved={reload}
        />
      )}

      {creatorsFor && (
        <CreatorsPreviewModal
          limit={creatorsFor.itemLimit}
          onClose={() => setCreatorsFor(null)}
        />
      )}
    </>
  );
}
