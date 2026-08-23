import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, SearchInput, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';
import { money } from '../utils/format';

/**
 * Obunalar ro'yxati (ТЗ §71, §107).
 *
 * <h2>Nega kerak bo'ldi</h2>
 * Dashboard obuna daromadini ko'rsatardi, lekin admin QAYSI obunalar bu
 * raqamni bergani ko'ra olmasdi — na endpoint, na sahifa bor edi. Ya'ni
 * moliyaviy ko'rsatkichni tekshirib bo'lmasdi.
 *
 * ⚠️ Tahrirlash va o'chirish tugmasi ATAYLAB yo'q: obuna moliyaviy
 * yozuv, uni qo'lda o'zgartirish tarixni buzadi (§42, §58). Premiumni
 * berish yoki bekor qilish «Foydalanuvchilar» bo'limida, auditga
 * tushadigan amal sifatida bajariladi (§38).
 */
export default function SubscriptionsPage() {
  const { t } = usePanelI18n();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [active, setActive] = useState('');
  const [source, setSource] = useState('');

  const reset = (setter) => (value) => { setter(value); setPage(0); };

  const { data, error, loading, reload } = useApi(
    () => adminApi.subscriptions({
      page,
      size: 30,
      q: q || undefined,
      active: active === '' ? undefined : active === 'true',
      source: source || undefined,
    }),
    [page, q, active, source]
  );

  return (
    <>
      <PageHeader
        title={t('sub.title')}
        subtitle={t('sub.subtitle')}
        right={<SearchInput value={q} onChange={reset(setQ)}
                            placeholder={t('sub.searchHint')} />}
      />

      <div className="uz-card mb-4" style={{ padding: 14, display: 'flex',
           gap: 12, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <label>
          <span className="uz-label">{t('sub.status')}</span>
          <select className="uz-select" value={active}
                  onChange={(e) => reset(setActive)(e.target.value)}>
            <option value="">{t('common.all')}</option>
            <option value="true">{t('sub.active')}</option>
            <option value="false">{t('sub.ended')}</option>
          </select>
        </label>
        <label>
          <span className="uz-label">{t('sub.source')}</span>
          <select className="uz-select" value={source}
                  onChange={(e) => reset(setSource)(e.target.value)}>
            <option value="">{t('common.all')}</option>
            <option value="PURCHASE">{t('sub.purchase')}</option>
            <option value="ADMIN_GIFT">{t('sub.gift')}</option>
          </select>
        </label>
        {(q || active || source) && (
          <button type="button" className="uz-btn uz-btn-ghost"
                  onClick={() => { setQ(''); setActive(''); setSource(''); setPage(0); }}>
            {t('common.reset')}
          </button>
        )}
      </div>

      <div className="uz-card overflow-hidden">
        {loading ? <LoadingState /> :
         error ? <ErrorState error={error} onRetry={reload} /> :
         !data?.items?.length ? <EmptyState icon="👑" title={t('sub.empty')} /> : (
          <>
            <TableWrap>
              <table className="uz-table">
                <thead>
                  <tr>
                    <th>{t('sub.user')}</th>
                    <th>{t('sub.tariff')}</th>
                    <th>{t('sub.period')}</th>
                    <th>{t('sub.source')}</th>
                    <th>{t('sub.paid')}</th>
                    <th>{t('sub.status')}</th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((s) => (
                    <tr key={s.id}>
                      <td>
                        <div style={{ fontWeight: 600, fontSize: 14 }}>{s.userName || '—'}</div>
                        <div className="uz-mono uz-muted" style={{ fontSize: 12 }}>
                          {s.userPhone || '—'}
                        </div>
                      </td>
                      <td>{s.tariffName || '—'}</td>
                      <td className="uz-mono uz-muted" style={{ fontSize: 12, whiteSpace: 'nowrap' }}>
                        {s.startAt?.slice(0, 10)} → {s.endAt?.slice(0, 10)}
                      </td>
                      <td>
                        <Badge tone={s.source === 'ADMIN_GIFT' ? 'gold' : 'info'}>
                          {s.source === 'ADMIN_GIFT' ? t('sub.gift') : t('sub.purchase')}
                        </Badge>
                      </td>
                      <td className="uz-mono">{money(s.paidAmount)}</td>
                      <td>
                        <Badge tone={s.active ? 'published' : 'draft'}>
                          {s.revokedAt ? t('sub.revoked')
                            : s.active ? t('sub.active') : t('sub.ended')}
                        </Badge>
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
