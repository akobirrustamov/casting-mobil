import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import LinkFields from '../components/LinkFields';
import LocaleTabs from '../components/LocaleTabs';
import MediaField from '../components/MediaField';
import Modal from '../components/Modal';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, SearchInput, TableWrap } from '../components/Ui';
import { count } from '../utils/format';
import { usePanelI18n } from '../i18n';
import NotificationReportModal from './reports/NotificationReportModal';

const TYPES = ['APP_NOTIFICATION', 'CASTING_NOTIFICATION'];
const AUDIENCES = ['ALL', 'PREMIUM_ONLY', 'NON_PREMIUM'];
const TONE = {
  DRAFT: 'draft', SCHEDULED: 'scheduled', SENDING: 'info',
  SENT: 'published', FAILED: 'blocked', CANCELLED: 'archived',
};

const empty = () => ({
  type: 'APP_NOTIFICATION', audience: 'ALL', imageMediaId: null,
  link: { linkType: 'NONE' }, scheduledAt: '',
  translations: { UZ: {}, RU: {}, EN: {} },
});

export default function NotificationsPage() {
  const { t } = usePanelI18n();
  const { can } = useAuth();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  // Til bo'yicha auditoriya: admin RU matnini kim o'qishini bilsin.
  const audience = useApi(() => adminApi.notificationAudience(), []);

  const { data, error, loading, reload } = useApi(
    () => adminApi.notifications({ q: q || undefined, page, size: 20 }),
    [q, page]
  );

  const onSearch = (value) => { setQ(value); setPage(0); };

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(empty);
  const [formLocale, setFormLocale] = useState('UZ');
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [reportFor, setReportFor] = useState(null);

  const audienceLabel = (a) => ({
    ALL: t('nt.audienceAll'),
    PREMIUM_ONLY: t('nt.audiencePremium'),
    NON_PREMIUM: t('nt.audienceNonPremium'),
  }[a] || a);

  const openForm = (row) => {
    setEditing(row);
    setFormLocale('UZ');
    setActionError(null);
    setForm(row ? {
      type: row.type, audience: row.audience, imageMediaId: row.imageMediaId,
      link: row.link || { linkType: 'NONE' },
      scheduledAt: row.scheduledAt ? row.scheduledAt.slice(0, 16) : '',
      translations: { UZ: {}, RU: {}, EN: {}, ...(row.translations || {}) },
    } : empty());
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
    const uz = form.translations?.UZ;
    if (!uz?.title?.trim() || !uz?.body?.trim()) {
      setFormLocale('UZ');
      setActionError({ message: t('editor.uzRequired') });
      return;
    }
    setSaving(true);
    setActionError(null);
    const payload = {
      type: form.type, audience: form.audience, imageMediaId: form.imageMediaId,
      link: form.link,
      scheduledAt: form.scheduledAt ? `${form.scheduledAt}:00` : null,
      translations: form.translations,
    };
    try {
      if (editing) await adminApi.updateNotification(editing.id, payload);
      else await adminApi.createNotification(payload);
      setOpen(false);
      reload();
    } catch (err) {
      setActionError(err);
    } finally {
      setSaving(false);
    }
  };

  const send = async (row) => {
    setActionError(null);
    try {
      await adminApi.sendNotification(row.id);
      reload();
    } catch (err) {
      // Provayder ulanmagan bo'lsa 503 keladi — bu kutilgan holat, xatoni ko'rsatamiz
      setActionError(err);
      reload();
    }
  };

  return (
    <>
      <PageHeader
        title={t('nt.title')}
        subtitle={t('nt.subtitle')}
        right={(
          <div className="flex items-center gap-3 flex-wrap">
            <SearchInput value={q} onChange={onSearch} placeholder={t('nt.search')} />
            {can('NOTIFICATION_CREATE') && (
              <button type="button" className="uz-btn uz-btn-primary" onClick={() => openForm(null)}>
                + {t('nt.new')}
              </button>
            )}
          </div>
        )}
      />

      {/* ⚠️ Uchala tarjima ham majburiy, lekin ularning OG'IRLIGI har xil.
          RU matnini shosha-pisha yozgan admin necha kishi o'sha matnni
          o'qishini ko'rsin. Hisobi yo'q foydalanuvchilar bu yerda YO'Q -
          ular hali ilovani ochmagan va til tanlamagan. */}
      {audience.data?.length > 0 && (
        <div className="uz-card mb-4" style={{ padding: '10px 14px', display: 'flex',
             gap: 16, flexWrap: 'wrap', alignItems: 'center' }}>
          <span className="uz-muted" style={{ fontSize: 13 }}>{t('nt.audienceByLang')}:</span>
          {audience.data.map((a) => (
            <span key={a.language} style={{ fontSize: 13 }}>
              <strong>{a.language}</strong> — {count(a.users)}
            </span>
          ))}
        </div>
      )}

      {/* Provayder ulanmagani ochiq aytiladi — admin nima bo'layotganini bilsin */}
      <div className="mb-4 px-4 py-3"
           style={{ borderRadius: 'var(--p-radius)', background: 'var(--warning-soft)',
                    border: '1px solid var(--warning-border)', color: 'var(--p-warning)', fontSize: 13 }}>
        {t('nt.providerWarning')}
      </div>

      {actionError && (
        <div role="alert" className="mb-4 px-4 py-3"
             style={{ borderRadius: 'var(--p-radius)', background: 'var(--danger-soft)',
                      border: '1px solid var(--danger-border)', color: 'var(--p-danger)', fontSize: 13 }}>
          {actionError.message}
        </div>
      )}

      <div className="uz-card overflow-hidden">
        {loading ? <LoadingState /> :
         error ? <ErrorState error={error} onRetry={reload} /> :
         !data?.items?.length ? <EmptyState icon="🔔" /> : (
          <>
            <TableWrap>
              <table className="uz-table">
                <thead>
                  <tr>
                    <th>{t('editor.title')}</th>
                    <th>{t('nt.type')}</th>
                    <th>{t('nt.audience')}</th>
                    <th>{t('content.col.status')}</th>
                    <th>{t('nt.scheduledAt')}</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((n) => (
                    <tr key={n.id}>
                      <td>
                        <div style={{ fontWeight: 600, fontSize: 13 }}>
                          {n.translations?.UZ?.title || '—'}
                        </div>
                        <div className="uz-muted" style={{ fontSize: 11 }}>
                          {n.translations?.UZ?.body}
                        </div>
                      </td>
                      <td className="uz-muted" style={{ fontSize: 12 }}>
                        {n.type.replace('_NOTIFICATION', '')}
                      </td>
                      <td className="uz-muted" style={{ fontSize: 12 }}>{audienceLabel(n.audience)}</td>
                      <td>
                        <Badge tone={TONE[n.status] || 'draft'}>{n.status}</Badge>
                        {n.failureReason && (
                          <div className="uz-muted" style={{ fontSize: 10, marginTop: 4, maxWidth: 220 }}>
                            {n.failureReason}
                          </div>
                        )}
                      </td>
                      <td className="uz-muted uz-mono" style={{ fontSize: 12 }}>
                        {n.scheduledAt ? n.scheduledAt.slice(0, 16).replace('T', ' ') : '—'}
                      </td>
                      <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                        {/* Hisobot HAR QANDAY holatda ochiladi — hatto
                            yuborilmagan xabarda ham auditoriya hajmi
                            foydali: «bu xabar necha kishiga tegadi?»
                            degan savolga javob yuborishdan OLDIN kerak. */}
                        <button type="button" className="uz-btn uz-btn-ghost"
                                style={{ minHeight: 30, padding: '0 10px', fontSize: 12,
                                         marginRight: 6 }}
                                onClick={() => setReportFor(n)}>
                          {t('stat.report')}
                        </button>
                        {n.status !== 'SENT' && can('NOTIFICATION_CREATE') && (
                          <button type="button" className="uz-btn uz-btn-ghost"
                                  style={{ minHeight: 30, padding: '0 10px', fontSize: 12 }}
                                  onClick={() => openForm(n)}>
                            {t('common.edit')}
                          </button>
                        )}
                        {n.status !== 'SENT' && can('NOTIFICATION_SEND') && (
                          <button type="button" className="uz-btn uz-btn-ghost"
                                  style={{ minHeight: 30, padding: '0 10px', fontSize: 12, marginLeft: 6 }}
                                  onClick={() => send(n)}>
                            {t('nt.send')}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableWrap>
            <Pagination page={data.page} totalPages={data.totalPages} onPage={setPage} />
          </>
        )}
      </div>

      <Modal
        open={open}
        title={editing ? t('nt.edit') : t('nt.new')}
        onClose={() => setOpen(false)}
        width={700}
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
            <label className="uz-label" htmlFor="n-type">{t('nt.type')}</label>
            <select id="n-type" className="uz-select" value={form.type}
                    onChange={(e) => setForm({ ...form, type: e.target.value })}>
              {TYPES.map((x) => <option key={x} value={x}>{x.replace(/_/g, ' ')}</option>)}
            </select>
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="n-aud">{t('nt.audience')}</label>
            <select id="n-aud" className="uz-select" value={form.audience}
                    onChange={(e) => setForm({ ...form, audience: e.target.value })}>
              {AUDIENCES.map((x) => <option key={x} value={x}>{audienceLabel(x)}</option>)}
            </select>
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="n-sch">{t('nt.scheduledAt')}</label>
            <input id="n-sch" className="uz-input" type="datetime-local" value={form.scheduledAt}
                   onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })} />
          </div>
        </div>

        <LocaleTabs active={formLocale} onChange={setFormLocale}
                    isFilled={(c) => Boolean(form.translations?.[c]?.title?.trim()
                                          && form.translations?.[c]?.body?.trim())} />

        <div className="mb-4">
          <label className="uz-label" htmlFor="n-ti">
            {t('editor.title')}{formLocale === 'UZ' && <span style={{ color: 'var(--p-danger)' }}> *</span>}
          </label>
          <input id="n-ti" className="uz-input" value={form.translations?.[formLocale]?.title || ''}
                 onChange={(e) => setTr('title', e.target.value)} />
        </div>
        <div className="mb-4">
          <label className="uz-label" htmlFor="n-body">
            {t('nt.body')}{formLocale === 'UZ' && <span style={{ color: 'var(--p-danger)' }}> *</span>}
          </label>
          <textarea id="n-body" className="uz-input" rows={3} style={{ resize: 'vertical' }}
                    value={form.translations?.[formLocale]?.body || ''}
                    onChange={(e) => setTr('body', e.target.value)} />
        </div>

        <div className="uz-row mb-4">
          <div className="uz-col" style={{ maxWidth: 260 }}>
            <MediaField label={t('ads.image')} value={form.imageMediaId}
                        onChange={(id) => setForm({ ...form, imageMediaId: id })} />
          </div>
        </div>

        <LinkFields value={form.link} onChange={(link) => setForm({ ...form, link })} />
      </Modal>

      {reportFor && (
        <NotificationReportModal
          notification={reportFor}
          name={reportFor.translations?.UZ?.title || `#${reportFor.id}`}
          onClose={() => setReportFor(null)}
        />
      )}
    </>
  );
}
