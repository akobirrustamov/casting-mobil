import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, SearchInput, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';

/**
 * before/after qiymati — JSON matni. Chiroyli ko'rinishga keltiramiz,
 * lekin buzilgan JSON bo'lsa xom matnni ko'rsatamiz: audit yozuvini
 * o'qib bo'lmay qolgandan ko'ra, qanday bo'lsa shundayligicha ko'rsatish
 * afzal.
 */
function StateBlock({ label, json }) {
  let text = json;
  try {
    text = JSON.stringify(JSON.parse(json), null, 2);
  } catch {
    /* xom matn qoladi */
  }
  return (
    <div>
      <span className="uz-label">{label}</span>
      <pre className="uz-mono" style={{ fontSize: 11, margin: 0, whiteSpace: 'pre-wrap',
           wordBreak: 'break-all' }}>{text}</pre>
    </div>
  );
}

const ROLE_TONE = {
  HYPER_ADMIN: 'gold', SUPER_ADMIN: 'info', ADMIN: 'published', WORKER: 'draft',
};

/**
 * Audit jurnali — faqat o'qish.
 *
 * O'chirish yoki tahrirlash tugmasi ATAYLAB yo'q: jurnal o'zgarmas bo'lishi
 * kerak, aks holda uning ma'nosi qolmaydi (§59).
 */
export default function AuditPage() {
  const { t } = usePanelI18n();
  const [page, setPage] = useState(0);
  const [action, setAction] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  // Qaysi qatorning tafsiloti ochiq. Bir vaqtda bittasi — before/after
  // JSON'lari uzun bo'lishi mumkin, hammasi ochiq tursa jadval o'qilmasdi.
  const [openId, setOpenId] = useState(null);

  const { data, error, loading, reload } = useApi(
    () => adminApi.auditLogs({
      page, size: 50,
      action: action || undefined,
      from: from || undefined,
      to: to || undefined,
    }),
    [page, action, from, to]
  );

  const resetPage = (setter) => (value) => { setter(value); setPage(0); setOpenId(null); };

  return (
    <>
      <PageHeader
        title={t('au.title')}
        subtitle={t('au.subtitle')}
        right={<SearchInput value={action} onChange={resetPage(setAction)}
                            placeholder={t('au.searchHint')} />}
      />
      <p className="uz-muted mb-4 text-sm">{t('au.hint')}</p>

      <div className="uz-card mb-4" style={{ padding: 14, display: 'flex',
           gap: 12, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <label>
          <span className="uz-label">{t('au.from')}</span>
          <input className="uz-input" type="date" value={from}
                 onChange={(e) => resetPage(setFrom)(e.target.value)} />
        </label>
        <label>
          <span className="uz-label">{t('au.to')}</span>
          <input className="uz-input" type="date" value={to}
                 onChange={(e) => resetPage(setTo)(e.target.value)} />
        </label>
        {(from || to || action) && (
          <button type="button" className="uz-btn uz-btn-ghost"
                  onClick={() => { setFrom(''); setTo(''); setAction(''); setPage(0); }}>
            {t('common.reset')}
          </button>
        )}
      </div>

      <div className="uz-card overflow-hidden">
        {loading ? <LoadingState /> :
         error ? <ErrorState error={error} onRetry={reload} /> :
         !data?.items?.length ? <EmptyState icon="📜" /> : (
          <>
            <TableWrap>
              <table className="uz-table">
                <thead>
                  <tr>
                    <th>{t('au.when')}</th>
                    <th>{t('au.actor')}</th>
                    <th>{t('au.action')}</th>
                    <th>{t('au.entity')}</th>
                    <th>IP</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((a) => [
                    <tr key={a.id}>
                      <td className="uz-mono uz-muted" style={{ fontSize: 12, whiteSpace: 'nowrap' }}>
                        {a.createdAt?.slice(0, 19).replace('T', ' ')}
                      </td>
                      <td>
                        <Badge tone={ROLE_TONE[a.actorRole] || 'draft'}>{a.actorRole || '—'}</Badge>
                      </td>
                      <td className="uz-mono" style={{ fontSize: 12, fontWeight: 600 }}>{a.action}</td>
                      <td className="uz-muted" style={{ fontSize: 12 }}>
                        {a.entityType ? `${a.entityType} #${a.entityId}` : '—'}
                      </td>
                      <td className="uz-mono uz-muted" style={{ fontSize: 11 }}>{a.ip || '—'}</td>
                      <td style={{ textAlign: 'right' }}>
                        {(a.beforeState || a.afterState || a.userAgent) && (
                          <button type="button" className="uz-btn uz-btn-ghost"
                                  style={{ padding: '2px 10px', fontSize: 12 }}
                                  onClick={() => setOpenId(openId === a.id ? null : a.id)}>
                            {openId === a.id ? '▴' : '▾'}
                          </button>
                        )}
                      </td>
                    </tr>,
                    openId === a.id && (
                      <tr key={`${a.id}-detail`}>
                        <td colSpan={6} style={{ background: 'var(--p-surface-2)' }}>
                          <div style={{ display: 'grid', gap: 10, padding: '4px 2px' }}>
                            {a.beforeState && <StateBlock label={t('au.before')} json={a.beforeState} />}
                            {a.afterState && <StateBlock label={t('au.after')} json={a.afterState} />}
                            {a.userAgent && (
                              <div>
                                <span className="uz-label">{t('au.device')}</span>
                                <div className="uz-mono uz-muted" style={{ fontSize: 11,
                                     wordBreak: 'break-all' }}>{a.userAgent}</div>
                              </div>
                            )}
                          </div>
                        </td>
                      </tr>
                    ),
                  ])}
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
