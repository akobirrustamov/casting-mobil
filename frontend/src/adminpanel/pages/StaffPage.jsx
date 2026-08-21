import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';

const ROLE_TONE = {
  HYPER_ADMIN: 'gold',
  SUPER_ADMIN: 'info',
  ADMIN: 'published',
  WORKER: 'draft',
};

export default function StaffPage() {
  const { t } = usePanelI18n();
  const { data, error, loading, reload } = useApi(() => adminApi.staff(), []);

  return (
    <>
      <PageHeader title={t('staff.title')} subtitle={t('staff.subtitle')} />

      <div className="uz-card overflow-hidden">
        {loading ? (
          <LoadingState />
        ) : error ? (
          <ErrorState error={error} onRetry={reload} />
        ) : !data?.length ? (
          <EmptyState icon="👥" />
        ) : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  <th>{t('staff.col.name')}</th>
                  <th>{t('staff.col.phone')}</th>
                  <th>{t('staff.col.role')}</th>
                  <th>{t('staff.col.permissions')}</th>
                </tr>
              </thead>
              <tbody>
                {data.map((u) => (
                  <tr key={u.id}>
                    <td style={{ fontWeight: 600 }}>{u.name || '—'}</td>
                    <td className="uz-muted uz-mono" style={{ fontSize: 13 }}>{u.phone}</td>
                    <td><Badge tone={ROLE_TONE[u.role] || 'draft'}>{u.role}</Badge></td>
                    <td className="uz-muted" style={{ fontSize: 12 }}>
                      {u.role === 'WORKER'
                        ? t('staff.permissionCount', { n: (u.permissions || []).length })
                        : t('staff.permissionsAll')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableWrap>
        )}
      </div>
    </>
  );
}
