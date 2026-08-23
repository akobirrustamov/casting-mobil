import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, SearchInput, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';

const ROLE_TONE = {
  HYPER_ADMIN: 'gold',
  SUPER_ADMIN: 'info',
  ADMIN: 'published',
  WORKER: 'draft',
};

const ROLES = ['HYPER_ADMIN', 'SUPER_ADMIN', 'ADMIN', 'WORKER'];
const STATUSES = ['ACTIVE', 'INACTIVE', 'BLOCKED'];

/**
 * Xodimlar ro'yxati (ТЗ §12, §51).
 *
 * Qidiruv, filtrlar va sahifalash BACKENDDA bajariladi — panel butun
 * jadvalni tortib olib xotirada filtrlamaydi.
 */
export default function StaffPage() {
  const { t } = usePanelI18n();
  const [q, setQ] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);

  const { data, error, loading, reload } = useApi(
    () => adminApi.staff({
      q: q || undefined,
      role: role || undefined,
      status: status || undefined,
      page,
      size: 20,
    }),
    [q, role, status, page]
  );

  // Har qanday filtr o'zgarganda birinchi sahifaga qaytamiz: aks holda
  // «3-sahifa» da turib filtrlasa, natija bo'sh ko'rinardi va buni
  // foydalanuvchi «hech narsa topilmadi» deb tushunardi.
  const onFilter = (setter) => (value) => { setter(value); setPage(0); };

  return (
    <>
      <PageHeader
        title={t('staff.title')}
        subtitle={t('staff.subtitle')}
        right={(
          <div className="flex items-center gap-3 flex-wrap">
            <SearchInput value={q} onChange={onFilter(setQ)}
                         placeholder={t('staff.search')} />
            <select className="uz-select" value={role}
                    onChange={(e) => onFilter(setRole)(e.target.value)}>
              <option value="">{t('staff.allRoles')}</option>
              {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
            <select className="uz-select" value={status}
                    onChange={(e) => onFilter(setStatus)(e.target.value)}>
              <option value="">{t('staff.allStatuses')}</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>{t(`staff.status.${s}`)}</option>
              ))}
            </select>
          </div>
        )}
      />

      <div className="uz-card overflow-hidden">
        {loading ? (
          <LoadingState />
        ) : error ? (
          <ErrorState error={error} onRetry={reload} />
        ) : !data?.items?.length ? (
          <EmptyState icon="👥" />
        ) : (
          <>
            <TableWrap>
              <table className="uz-table">
                <thead>
                  <tr>
                    <th>{t('staff.col.name')}</th>
                    <th>{t('staff.col.phone')}</th>
                    <th>{t('staff.col.role')}</th>
                    <th>{t('staff.col.status')}</th>
                    <th>{t('staff.col.permissions')}</th>
                    <th>{t('staff.col.lastLogin')}</th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((u) => (
                    <tr key={u.id}>
                      <td style={{ fontWeight: 600 }}>{u.name || '—'}</td>
                      <td className="uz-muted uz-mono" style={{ fontSize: 13 }}>{u.phone}</td>
                      <td><Badge tone={ROLE_TONE[u.role] || 'draft'}>{u.role}</Badge></td>
                      <td>
                        <Badge tone={u.status === 'ACTIVE' ? 'published' : 'draft'}>
                          {t(`staff.status.${u.status || 'ACTIVE'}`)}
                        </Badge>
                      </td>
                      <td className="uz-muted" style={{ fontSize: 12 }}>
                        {u.role === 'WORKER'
                          ? t('staff.permissionCount', { n: (u.permissions || []).length })
                          : t('staff.permissionsAll')}
                      </td>
                      <td className="uz-muted" style={{ fontSize: 12 }}>
                        {/* Hech qachon kirmagan xodim — bo'sh katak emas,
                            aniq belgi: «—» ma'lumot yo'qligini bildiradi. */}
                        {u.lastLoginAt ? u.lastLoginAt.replace('T', ' ').slice(0, 16) : '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableWrap>

            <Pagination page={page} totalPages={data.totalPages} onPage={setPage} />
          </>
        )}
      </div>
    </>
  );
}
