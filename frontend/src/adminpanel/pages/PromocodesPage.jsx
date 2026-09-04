import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import Modal from '../components/Modal';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';

const emptyPromo = () => ({
  code: '', grantType: 'PREMIUM_DAYS', grantDays: 30, maxRedemptions: '',
  validFrom: '', validUntil: '', active: true, note: '',
});

/**
 * Kod nima beradi.
 *
 * ⚠️ Bu ro'yxat backenddagi `PromocodeGrantType` ning nusxasi. Yangi
 * qiymat qo'shilsa shu yerga ham qo'shilsin, aks holda u panelda
 * tanlanmaydigan bo'lib qoladi.
 */
const GRANT_TYPES = ['PREMIUM_DAYS', 'CASTING_DAYS'];

/** Holat → belgi rangi. Holatning o'zini backend hisoblaydi (`status`). */
const STATUS_TONE = {
  ACTIVE: 'published',
  SCHEDULED: 'info',
  EXPIRED: 'draft',
  EXHAUSTED: 'draft',
  DISABLED: 'draft',
};

/** `2026-09-04T10:00:00` → `04.09.2026`. Sahifada soat kerak emas. */
const fmtDate = (iso) => (iso ? iso.slice(0, 10).split('-').reverse().join('.') : '—');

/** Backend `LocalDateTime` → `<input type="datetime-local">` qiymati. */
const toInput = (iso) => (iso ? iso.slice(0, 16) : '');

/** Bo'sh input → `null`; aks holda backend `LocalDateTime` (soniyasiz ham o'qiydi). */
const fromInput = (v) => (v ? v : null);

/**
 * Promokodlar — bepul Premium kunlar (buyurtmachi, 04.09.2026).
 *
 * <h2>Kod o'chirilmaydi</h2>
 * Faqat to'xtatiladi. Ishlatilgan kodni o'chirish «bu odamga premium
 * qayerdan kelgan» degan savolni javobsiz qoldirardi.
 *
 * <h2>Kod tahrirlashda o'zgarmaydi</h2>
 * U tarqatilgan bo'lishi mumkin. Formada kod maydoni faqat yaratishda
 * ochiq — bo'sh qoldirilsa backend o'zi yaratadi.
 */
export default function PromocodesPage() {
  const { t } = usePanelI18n();
  const { can } = useAuth();

  const promos = useApi(() => adminApi.promocodes(), []);

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyPromo);
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [copied, setCopied] = useState(null);
  const [viewing, setViewing] = useState(null);

  const canEdit = can('PROMOCODE_EDIT');

  const openForm = (row) => {
    setEditing(row);
    setActionError(null);
    setForm(row ? {
      code: row.code,
      grantType: row.grantType || 'PREMIUM_DAYS',
      grantDays: row.grantDays,
      maxRedemptions: row.maxRedemptions ?? '',
      validFrom: toInput(row.validFrom),
      validUntil: toInput(row.validUntil),
      active: row.active,
      note: row.note || '',
    } : emptyPromo());
    setOpen(true);
  };

  const save = async () => {
    setSaving(true);
    setActionError(null);
    const payload = {
      code: editing ? undefined : (form.code.trim() || undefined),
      // Tur ham kod kabi: faqat yaratishda. Tarqatilgan kod boshqa narsa
      // bera boshlashi odamlarni aldash bo'lardi.
      grantType: editing ? undefined : form.grantType,
      grantDays: Number(form.grantDays),
      maxRedemptions: form.maxRedemptions === '' ? null : Number(form.maxRedemptions),
      validFrom: fromInput(form.validFrom),
      validUntil: fromInput(form.validUntil),
      active: form.active,
      note: form.note.trim() || null,
    };
    try {
      if (editing) await adminApi.updatePromocode(editing.id, payload);
      else await adminApi.createPromocode(payload);
      setOpen(false);
      promos.reload();
    } catch (err) {
      setActionError(err);
    } finally {
      setSaving(false);
    }
  };

  /** Kodni nusxalash — admin uni Telegram/Instagram'ga qo'yadi. */
  const copy = async (code) => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(code);
      setTimeout(() => setCopied(null), 1500);
    } catch {
      // Brauzer ruxsat bermadi — kod baribir ekranda, qo'lda nusxalanadi.
    }
  };

  return (
    <>
      <PageHeader
        title={t('pc.title')}
        subtitle={t('pc.subtitle')}
        right={canEdit && (
          <button type="button" className="uz-btn uz-btn-primary" onClick={() => openForm(null)}>
            + {t('pc.newCode')}
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

      <div className="uz-card overflow-hidden">
        {promos.loading ? <LoadingState /> :
         promos.error ? <ErrorState error={promos.error} onRetry={promos.reload} /> :
         !promos.data?.length ? <EmptyState icon="🎟️" /> : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  <th>{t('pc.code')}</th>
                  <th>{t('pc.grantType')}</th>
                  <th>{t('pc.days')}</th>
                  <th>{t('pc.used')}</th>
                  <th>{t('pc.validity')}</th>
                  <th>{t('pc.statusCol')}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {promos.data.map((x) => (
                  <tr key={x.id}>
                    <td>
                      <div className="flex items-center gap-2">
                        <span className="uz-mono" style={{ fontWeight: 600, letterSpacing: '.04em' }}>
                          {x.code}
                        </span>
                        <button type="button" className="uz-btn uz-btn-ghost"
                                style={{ minHeight: 24, padding: '0 8px', fontSize: 11 }}
                                onClick={() => copy(x.code)}
                                title={t('pc.copy')}>
                          {copied === x.code ? t('pc.copied') : t('pc.copy')}
                        </button>
                      </div>
                      {x.note && <div className="uz-muted" style={{ fontSize: 11 }}>{x.note}</div>}
                    </td>
                    <td>
                      <Badge tone={x.grantType === 'CASTING_DAYS' ? 'info' : 'gold'}>
                        {t(`pc.type.${x.grantType || 'PREMIUM_DAYS'}`)}
                      </Badge>
                    </td>
                    <td className="uz-mono">{x.grantDays}</td>
                    <td className="uz-mono">
                      {x.redemptions} / {x.maxRedemptions ?? '∞'}
                    </td>
                    <td className="uz-muted" style={{ fontSize: 12 }}>
                      {x.validFrom || x.validUntil
                        ? `${fmtDate(x.validFrom)} – ${fmtDate(x.validUntil)}`
                        : t('pc.noLimit')}
                    </td>
                    <td>
                      <Badge tone={STATUS_TONE[x.status] || 'draft'}>
                        {t(`pc.status.${x.status}`)}
                      </Badge>
                    </td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <button type="button" className="uz-btn uz-btn-ghost"
                              style={{ minHeight: 30, padding: '0 12px', fontSize: 12 }}
                              onClick={() => setViewing(x)}>
                        {t('pc.redemptions')}
                      </button>
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

      {/* ------------------------------------------------------- yaratish/tahrir */}
      <Modal
        open={open}
        title={editing ? t('pc.editCode') : t('pc.newCode')}
        onClose={() => setOpen(false)}
        width={560}
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
        <div className="mb-4">
          <label className="uz-label" htmlFor="pc-code">{t('pc.code')}</label>
          <input id="pc-code" className="uz-input uz-mono" value={form.code}
                 disabled={Boolean(editing)}
                 placeholder={editing ? '' : t('pc.codePlaceholder')}
                 onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} />
          <p className="uz-muted mt-1" style={{ fontSize: 11 }}>
            {editing ? t('pc.codeImmutable') : t('pc.codeHint')}
          </p>
        </div>

        <div className="mb-4">
          <label className="uz-label" htmlFor="pc-t">{t('pc.grantType')}</label>
          <select id="pc-t" className="uz-input" value={form.grantType}
                  disabled={Boolean(editing)}
                  onChange={(e) => setForm({ ...form, grantType: e.target.value })}>
            {GRANT_TYPES.map((v) => (
              <option key={v} value={v}>{t(`pc.type.${v}`)}</option>
            ))}
          </select>
          <p className="uz-muted mt-1" style={{ fontSize: 11 }}>
            {editing ? t('pc.typeImmutable') : t(`pc.typeHint.${form.grantType}`)}
          </p>
        </div>

        <div className="uz-row mb-4">
          <div className="uz-col">
            <label className="uz-label" htmlFor="pc-d">{t('pc.days')}</label>
            <input id="pc-d" className="uz-input" type="number" min="1" max="3650" value={form.grantDays}
                   onChange={(e) => setForm({ ...form, grantDays: e.target.value })} />
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="pc-l">{t('pc.limit')}</label>
            <input id="pc-l" className="uz-input" type="number" min="1" value={form.maxRedemptions}
                   placeholder={t('pc.unlimited')}
                   onChange={(e) => setForm({ ...form, maxRedemptions: e.target.value })} />
            <p className="uz-muted mt-1" style={{ fontSize: 11 }}>{t('pc.limitHint')}</p>
          </div>
        </div>

        <div className="uz-row mb-4">
          <div className="uz-col">
            <label className="uz-label" htmlFor="pc-f">{t('pc.validFrom')}</label>
            <input id="pc-f" className="uz-input" type="datetime-local" value={form.validFrom}
                   onChange={(e) => setForm({ ...form, validFrom: e.target.value })} />
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="pc-u">{t('pc.validUntil')}</label>
            <input id="pc-u" className="uz-input" type="datetime-local" value={form.validUntil}
                   onChange={(e) => setForm({ ...form, validUntil: e.target.value })} />
          </div>
        </div>

        <div className="mb-4">
          <label className="uz-label" htmlFor="pc-n">{t('pc.note')}</label>
          <input id="pc-n" className="uz-input" value={form.note} maxLength={255}
                 placeholder={t('pc.noteHint')}
                 onChange={(e) => setForm({ ...form, note: e.target.value })} />
        </div>

        <label className="uz-check">
          <input type="checkbox" checked={form.active}
                 onChange={(e) => setForm({ ...form, active: e.target.checked })} />
          {t('common.active')}
        </label>
      </Modal>

      {/* ---------------------------------------------------------- kim ishlatgan */}
      {viewing && (
        <RedemptionsModal promo={viewing} onClose={() => setViewing(null)} />
      )}
    </>
  );
}

/** Bitta kodni kimlar ishlatgani — «bu kod qayerga ketdi» degan savolga. */
function RedemptionsModal({ promo, onClose }) {
  const { t } = usePanelI18n();
  const rows = useApi(() => adminApi.promocodeRedemptions(promo.id), [promo.id]);

  return (
    <Modal open title={`${t('pc.redemptionsOf')} ${promo.code}`} onClose={onClose} width={640}
           footer={
             <button type="button" className="uz-btn uz-btn-ghost" onClick={onClose}>
               {t('common.close')}
             </button>
           }>
      {rows.loading ? <LoadingState /> :
       rows.error ? <ErrorState error={rows.error} onRetry={rows.reload} /> :
       !rows.data?.length ? <EmptyState icon="🎟️" title={t('pc.noRedemptions')} /> : (
        <TableWrap>
          <table className="uz-table">
            <thead>
              <tr>
                <th>{t('pc.user')}</th>
                <th>{t('pc.redeemedAt')}</th>
                <th>{t('pc.grantedUntil')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.data.map((r) => (
                <tr key={r.id}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{r.userName || '—'}</div>
                    <div className="uz-muted uz-mono" style={{ fontSize: 11 }}>{r.userPhone || ''}</div>
                  </td>
                  <td className="uz-mono">{fmtDate(r.redeemedAt)}</td>
                  <td className="uz-mono">{fmtDate(r.grantedUntil)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </TableWrap>
      )}
    </Modal>
  );
}
