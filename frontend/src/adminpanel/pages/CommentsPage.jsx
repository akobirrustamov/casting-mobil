import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, SearchInput, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';

const STATUSES = ['VISIBLE', 'HIDDEN', 'DELETED'];
const TONE = { VISIBLE: 'published', HIDDEN: 'scheduled', DELETED: 'draft' };

/**
 * Izohlar moderatsiyasi.
 *
 * Admin izoh YARATMAYDI — uni foydalanuvchi mobil ilovadan yozadi.
 * Bu yerda faqat yashirish, tiklash va o'chirilgan deb belgilash.
 */
export default function CommentsPage() {
  const { t } = usePanelI18n();
  const { can } = useAuth();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [q, setQ] = useState('');
  const [reportedOnly, setReportedOnly] = useState(false);
  const [busy, setBusy] = useState(null);

  const { data, error, loading, reload } = useApi(
    () => adminApi.comments({
      page, size: 20,
      status: status || undefined,
      q: q || undefined,
      reportedOnly: reportedOnly || undefined,
    }),
    [page, status, q, reportedOnly]
  );

  const change = async (id, next) => {
    setBusy(id);
    try {
      await adminApi.setCommentStatus(id, next);
      reload();
    } finally {
      setBusy(null);
    }
  };

  return (
    <>
      <PageHeader
        title={t('cm.title')}
        subtitle={t('cm.subtitle')}
        right={
          <>
            <SearchInput value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder={t('content.search')} />
            <select className="uz-select" style={{ width: 'auto' }} value={status}
                    aria-label={t('content.col.status')}
                    onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
              <option value="">{t('content.allStatuses')}</option>
              {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
            <label className="uz-check">
              <input type="checkbox" checked={reportedOnly}
                     onChange={(e) => { setReportedOnly(e.target.checked); setPage(0); }} />
              {t('cm.reportedOnly')}
            </label>
          </>
        }
      />

      <p className="uz-muted mb-4 text-sm">{t('cm.noHardDelete')}</p>

      <div className="uz-card overflow-hidden">
        {loading ? <LoadingState /> :
         error ? <ErrorState error={error} onRetry={reload} /> :
         !data?.items?.length ? <EmptyState icon="💬" /> : (
          <>
            <TableWrap>
              <table className="uz-table">
                <thead>
                  <tr>
                    <th>{t('cm.author')}</th>
                    <th>{t('cm.text')}</th>
                    <th>{t('cm.content')}</th>
                    <th>{t('cm.reports')}</th>
                    <th>{t('content.col.status')}</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((c) => (
                    <tr key={c.id}>
                      <td>
                        <div style={{ fontWeight: 600, fontSize: 13 }}>{c.authorName || '—'}</div>
                        <div className="uz-muted uz-mono" style={{ fontSize: 11 }}>{c.authorPhone}</div>
                      </td>
                      <td style={{ maxWidth: 380 }}>
                        <div style={{ fontSize: 13 }}>{c.text}</div>
                        <div className="uz-muted" style={{ fontSize: 11 }}>
                          {c.createdAt?.slice(0, 16).replace('T', ' ')}
                        </div>
                      </td>
                      <td className="uz-muted" style={{ fontSize: 12 }}>{c.contentSlug || '—'}</td>
                      <td>
                        {c.reportsCount > 0
                          ? <Badge tone="blocked">{c.reportsCount}</Badge>
                          : <span className="uz-muted">—</span>}
                      </td>
                      <td><Badge tone={TONE[c.status] || 'draft'}>{c.status}</Badge></td>
                      <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                        {can('COMMENT_MODERATE') && (
                          <>
                            {c.status !== 'HIDDEN' && (
                              <button type="button" className="uz-btn uz-btn-ghost"
                                      style={{ minHeight: 30, padding: '0 10px', fontSize: 12 }}
                                      disabled={busy === c.id}
                                      onClick={() => change(c.id, 'HIDDEN')}>
                                {t('cm.hide')}
                              </button>
                            )}
                            {c.status !== 'VISIBLE' && (
                              <button type="button" className="uz-btn uz-btn-ghost"
                                      style={{ minHeight: 30, padding: '0 10px', fontSize: 12, marginLeft: 6 }}
                                      disabled={busy === c.id}
                                      onClick={() => change(c.id, 'VISIBLE')}>
                                {t('cm.restore')}
                              </button>
                            )}
                            {c.status !== 'DELETED' && (
                              <button type="button" className="uz-btn uz-btn-danger"
                                      style={{ minHeight: 30, padding: '0 10px', fontSize: 12, marginLeft: 6 }}
                                      disabled={busy === c.id}
                                      onClick={() => change(c.id, 'DELETED')}>
                                ✕
                              </button>
                            )}
                          </>
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
    </>
  );
}
