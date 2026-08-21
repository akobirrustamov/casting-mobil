import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, SearchInput, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';

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

  const { data, error, loading, reload } = useApi(
    () => adminApi.auditLogs({ page, size: 50, action: action || undefined }),
    [page, action]
  );

  return (
    <>
      <PageHeader
        title={t('au.title')}
        subtitle={t('au.subtitle')}
        right={<SearchInput value={action} onChange={(v) => { setAction(v); setPage(0); }}
                            placeholder="CONTENT_PUBLISHED..." />}
      />
      <p className="uz-muted mb-4 text-sm">{t('au.hint')}</p>

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
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((a) => (
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
