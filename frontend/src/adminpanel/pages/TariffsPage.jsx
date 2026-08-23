import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import LocaleTabs from '../components/LocaleTabs';
import Modal from '../components/Modal';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, TableWrap } from '../components/Ui';
import { toBackendLocale, usePanelI18n } from '../i18n';

const emptyTariff = () => ({
  code: '', durationMonths: 1, price: '', currency: 'UZS',
  active: true, highlighted: false, sortOrder: 0,
  translations: { UZ: {}, RU: {}, EN: {} },
});

/**
 * Premium tariflari va valyuta paketlari.
 *
 * Narxlar kodda qotirilmagan — hammasi shu yerdan o'zgaradi (§36).
 */
export default function TariffsPage() {
  const { t, locale } = usePanelI18n();
  const { can } = useAuth();
  const bl = toBackendLocale(locale);

  const tariffs = useApi(() => adminApi.tariffs(), []);
  const packages = useApi(() => adminApi.currencyPackages(), []);

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyTariff);
  const [formLocale, setFormLocale] = useState('UZ');
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [pkgDrafts, setPkgDrafts] = useState({});

  const canEdit = can('TARIFF_EDIT');
  const canPkg = can('DONATION_PACKAGE_EDIT');

  const nameOf = (x) =>
    x.translations?.[bl]?.title || x.translations?.UZ?.title || x.code;

  const openForm = (row) => {
    setEditing(row);
    setFormLocale('UZ');
    setActionError(null);
    setForm(row ? {
      code: row.code, durationMonths: row.durationMonths, price: row.price,
      currency: row.currency, active: row.active, highlighted: row.highlighted,
      sortOrder: row.sortOrder,
      translations: { UZ: {}, RU: {}, EN: {}, ...(row.translations || {}) },
    } : emptyTariff());
    setOpen(true);
  };

  const setTr = (field, value) =>
    setForm((p) => ({
      ...p,
      translations: {
        ...p.translations,
        [formLocale]: { ...(p.translations[formLocale] || {}), [field]: value },
      },
    }));

  const save = async () => {
    if (!form.translations?.UZ?.title?.trim()) {
      setFormLocale('UZ');
      setActionError({ message: t('editor.uzRequired') });
      return;
    }
    setSaving(true);
    setActionError(null);
    const payload = {
      code: form.code || undefined,
      durationMonths: Number(form.durationMonths),
      price: form.price === '' ? null : Number(form.price),
      currency: form.currency,
      active: form.active,
      highlighted: form.highlighted,
      sortOrder: Number(form.sortOrder) || 0,
      translations: form.translations,
    };
    try {
      if (editing) await adminApi.updateTariff(editing.id, payload);
      else await adminApi.createTariff(payload);
      setOpen(false);
      tariffs.reload();
    } catch (err) {
      setActionError(err);
    } finally {
      setSaving(false);
    }
  };

  const savePkg = async (p) => {
    const draft = pkgDrafts[p.id];
    if (draft === undefined) return;
    try {
      await adminApi.savePackage(p.id, { ...p, price: Number(draft) });
      setPkgDrafts((d) => {
        const n = { ...d };
        delete n[p.id];
        return n;
      });
      packages.reload();
    } catch (err) {
      setActionError(err);
    }
  };


  return (
    <>
      <PageHeader
        title={t('tr.title')}
        subtitle={t('tr.subtitle')}
        right={canEdit && (
          <button type="button" className="uz-btn uz-btn-primary" onClick={() => openForm(null)}>
            + {t('tr.newTariff')}
          </button>
        )}
      />

      {actionError && !open && (
        <div role="alert" className="mb-4 px-4 py-3"
             style={{ borderRadius: 'var(--p-radius)', background: 'var(--danger-soft)',
                      border: '1px solid var(--danger-border)', color: 'var(--p-danger)', fontSize: 13 }}>
          {actionError.message}
        </div>
      )}

      {/* ------------------------------------------------------------ tariflar */}
      <div className="uz-h2 mb-3" style={{ fontSize: 16 }}>{t('tr.tariffs')}</div>
      <div className="uz-card overflow-hidden mb-8">
        {tariffs.loading ? <LoadingState /> :
         tariffs.error ? <ErrorState error={tariffs.error} onRetry={tariffs.reload} /> :
         !tariffs.data?.length ? <EmptyState icon="👑" /> : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  <th>{t('editor.title')}</th>
                  <th>{t('tr.months')}</th>
                  <th>{t('tr.price')}</th>
                  <th>{t('tr.monthly')}</th>
                  <th>{t('common.active')}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {tariffs.data.map((x) => (
                  <tr key={x.id}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{nameOf(x)}</div>
                      <div className="uz-muted uz-mono" style={{ fontSize: 11 }}>{x.code}</div>
                      {x.highlighted && x.translations?.[bl]?.shortDescription && (
                        <Badge tone="gold">{x.translations[bl].shortDescription}</Badge>
                      )}
                    </td>
                    <td className="uz-mono">{x.durationMonths}</td>
                    <td className="uz-mono" style={{ fontWeight: 600 }}>
                      {money(x.price)} {t('common.currency')}
                    </td>
                    <td className="uz-mono uz-muted">{money(x.monthlyPrice)}</td>
                    <td>
                      <Badge tone={x.active ? 'published' : 'draft'}>
                        {x.active ? t('common.active') : t('common.inactive')}
                      </Badge>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      {canEdit && (
                        <button type="button" className="uz-btn uz-btn-ghost"
                                style={{ minHeight: 30, padding: '0 12px', fontSize: 12 }}
                                onClick={() => openForm(x)}>
                          {t('common.edit')}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableWrap>
        )}
      </div>

      {/* ------------------------------------------------------------- paketlar */}
      <div className="uz-h2 mb-1" style={{ fontSize: 16 }}>{t('tr.packages')}</div>
      <p className="uz-muted mb-3 text-sm">{t('tr.rateWarning')}</p>
      <div className="uz-card overflow-hidden">
        {packages.loading ? <LoadingState /> :
         packages.error ? <ErrorState error={packages.error} onRetry={packages.reload} /> :
         !packages.data?.length ? <EmptyState icon="⭐" /> : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  <th>{t('tr.kind')}</th>
                  <th>{t('tr.amount')}</th>
                  <th style={{ width: 200 }}>{t('tr.price')}</th>
                  <th>{t('common.active')}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {packages.data.map((p) => {
                  const draft = pkgDrafts[p.id];
                  const dirty = draft !== undefined && Number(draft) !== Number(p.price);
                  return (
                    <tr key={p.id}>
                      <td>
                        <Badge tone={p.kind === 'STARS' ? 'gold' : 'info'}>
                          {p.kind === 'STARS' ? '⭐ STARS' : '◎ COIN'}
                        </Badge>
                      </td>
                      <td className="uz-mono" style={{ fontWeight: 600 }}>{p.amount}</td>
                      <td>
                        <input className="uz-input uz-mono" style={{ minHeight: 34 }}
                               type="number" min="0" disabled={!canPkg}
                               value={draft ?? p.price}
                               onChange={(e) => setPkgDrafts({ ...pkgDrafts, [p.id]: e.target.value })} />
                      </td>
                      <td>
                        <Badge tone={p.active ? 'published' : 'draft'}>
                          {p.active ? t('common.active') : t('common.inactive')}
                        </Badge>
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        {canPkg && dirty && (
                          <button type="button" className="uz-btn uz-btn-primary"
                                  style={{ minHeight: 30, padding: '0 12px', fontSize: 12 }}
                                  onClick={() => savePkg(p)}>
                            {t('common.save')}
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

      <Modal
        open={open}
        title={editing ? t('tr.editTariff') : t('tr.newTariff')}
        onClose={() => setOpen(false)}
        width={640}
        footer={
          <>
            {actionError && (
              <span style={{ color: 'var(--p-danger)', fontSize: 13, marginRight: 'auto' }} role="alert">
                {actionError.message}
              </span>
            )}
            <button type="button" className="uz-btn uz-btn-ghost" onClick={() => setOpen(false)}>
              {t('common.cancel')}
            </button>
            <button type="button" className="uz-btn uz-btn-primary" onClick={save} disabled={saving}>
              {saving ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        <div className="uz-row mb-4">
          <div className="uz-col">
            <label className="uz-label" htmlFor="tf-m">{t('tr.months')}</label>
            <input id="tf-m" className="uz-input" type="number" min="1" value={form.durationMonths}
                   onChange={(e) => setForm({ ...form, durationMonths: e.target.value })} />
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="tf-p">{t('tr.price')}</label>
            <input id="tf-p" className="uz-input" type="number" min="0" step="1000" value={form.price}
                   onChange={(e) => setForm({ ...form, price: e.target.value })} />
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="tf-o">{t('common.sortOrder')}</label>
            <input id="tf-o" className="uz-input" type="number" value={form.sortOrder}
                   onChange={(e) => setForm({ ...form, sortOrder: e.target.value })} />
          </div>
        </div>

        <LocaleTabs active={formLocale} onChange={setFormLocale}
                    isFilled={(c) => Boolean(form.translations?.[c]?.title?.trim())} />

        <div className="mb-4">
          <label className="uz-label" htmlFor="tf-n">
            {t('editor.title')}{formLocale === 'UZ' && <span style={{ color: 'var(--p-danger)' }}> *</span>}
          </label>
          <input id="tf-n" className="uz-input" value={form.translations?.[formLocale]?.title || ''}
                 onChange={(e) => setTr('title', e.target.value)} />
        </div>
        <div className="mb-4">
          <label className="uz-label" htmlFor="tf-b">{t('tr.badge')}</label>
          <input id="tf-b" className="uz-input" value={form.translations?.[formLocale]?.shortDescription || ''}
                 onChange={(e) => setTr('shortDescription', e.target.value)} />
        </div>
        <div className="mb-4">
          <label className="uz-label" htmlFor="tf-f">{t('tr.features')}</label>
          <textarea id="tf-f" className="uz-input" rows={5} style={{ resize: 'vertical' }}
                    value={form.translations?.[formLocale]?.description || ''}
                    onChange={(e) => setTr('description', e.target.value)} />
          <p className="uz-muted mt-1" style={{ fontSize: 11 }}>{t('tr.featuresHint')}</p>
        </div>

        <div className="flex gap-5 flex-wrap">
          <label className="uz-check">
            <input type="checkbox" checked={form.highlighted}
                   onChange={(e) => setForm({ ...form, highlighted: e.target.checked })} />
            {t('tr.highlighted')}
          </label>
          <label className="uz-check">
            <input type="checkbox" checked={form.active}
                   onChange={(e) => setForm({ ...form, active: e.target.checked })} />
            {t('common.active')}
          </label>
        </div>
      </Modal>
    </>
  );
}
