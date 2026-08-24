import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';

/**
 * Donatlar hisoboti (ТЗ §42).
 *
 * ⚠️ STARS va COIN QO'SHILMAYDI. Ularning kursi admin panelida alohida
 * belgilanadi (§40, §41) va hozircha 0. Ikkalasini bitta «jami» ga
 * qo'shish 10 so'm va 10 dollarni qo'shishday bo'lardi.
 *
 * ⚠️ Tahrirlash va o'chirish tugmasi ATAYLAB yo'q: moliyaviy tarix
 * o'zgarmas (§42).
 */
export default function DonationsPage() {
  const { t } = usePanelI18n();
  const [page, setPage] = useState(0);

  const report = useApi(() => adminApi.donationReport({ limit: 10, days: 30 }), []);
  const list = useApi(
    () => adminApi.donationTransactions({ page, size: 20 }),
    [page]
  );

  const byKind = report.data?.byKind || [];

  return (
    <>
      <PageHeader title={t('dn.title')} subtitle={t('dn.subtitle')} />

      {/* Valyuta bo'yicha jamlanma */}
      {report.loading ? <LoadingState rows={2} /> :
       report.error ? <ErrorState error={report.error} onRetry={report.reload} /> : (
        <div className="uz-grid-cards mb-6">
          {byKind.length === 0 ? (
            <div className="uz-card p-5">
              <EmptyState icon="✨" title={t('dn.noDonations')} />
            </div>
          ) : byKind.map((k) => (
            <div className="uz-card p-5" key={k.kind}>
              <div className="uz-muted text-sm">{k.kind}</div>
              <div className="text-2xl font-bold">{k.total}</div>
              <div className="uz-muted text-xs">
                {k.transactions} {t('dn.transactions')}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Top ijodkorlar va kontent — ALOHIDA ro'yxatlar (ТЗ §42) */}
      <div className="uz-two-col mb-6">
        <TopList
          title={t('dn.topCreators')}
          rows={report.data?.topCreators}
          emptyIcon="★"
        />
        <TopList
          title={t('dn.topContent')}
          rows={report.data?.topContent}
          emptyIcon="🎬"
        />
      </div>

      {/* Tranzaksiyalar */}
      <div className="uz-card overflow-hidden">
        {list.loading ? <LoadingState /> :
         list.error ? <ErrorState error={list.error} onRetry={list.reload} /> :
         !list.data?.items?.length ? <EmptyState icon="📭" /> : (
          <>
            <TableWrap>
              <table className="uz-table">
                <thead>
                  <tr>
                    <th>{t('dn.when')}</th>
                    <th>{t('dn.sender')}</th>
                    <th>{t('dn.target')}</th>
                    <th>{t('dn.kind')}</th>
                    <th className="text-right">{t('dn.amount')}</th>
                  </tr>
                </thead>
                <tbody>
                  {list.data.items.map((d) => (
                    <tr key={d.id}>
                      <td className="uz-muted text-sm">
                        {d.createdAt?.replace('T', ' ').slice(0, 16)}
                      </td>
                      <td>{d.senderName || '—'}</td>
                      <td>
                        {d.targetName
                          ? <div style={{ fontWeight: 600 }}>{d.targetName}</div>
                          : <div className="uz-muted text-sm">#{d.targetId}</div>}
                        <div className="uz-muted" style={{ fontSize: 11 }}>
                          {d.targetType}
                        </div>
                      </td>
                      <td><Badge tone="info">{d.kind}</Badge></td>
                      <td className="text-right font-semibold">{d.amount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableWrap>
            <Pagination
              page={page}
              totalPages={list.data.totalPages}
              onPage={setPage}
            />
          </>
        )}
      </div>
    </>
  );
}

/** Reyting ro'yxati. Bo'sh bo'lsa — bo'sh holat, soxta qator emas. */
function TopList({ title, rows, emptyIcon }) {
  return (
    <div className="uz-card overflow-hidden">
      <div className="px-5 py-4 font-semibold">{title}</div>
      {!rows?.length ? <EmptyState icon={emptyIcon} /> : (
        <TableWrap>
          <table className="uz-table">
            <tbody>
              {rows.map((r) => (
                <tr key={`${r.targetType}-${r.targetId}-${r.kind}`}>
                  <td>
                    {/* ⚠️ Nom topilmasa `#5` qoladi - bu halol.
                        O'chirilgan ijodkorga berilgan eski donat shu
                        holatda bo'ladi va uni to'qib chiqarmaymiz. */}
                    {r.targetName
                      ? <span style={{ fontWeight: 600 }}>{r.targetName}</span>
                      : <span className="uz-muted text-sm">#{r.targetId}</span>}
                  </td>
                  <td><Badge tone="info">{r.kind}</Badge></td>
                  <td className="text-right font-semibold">{r.total}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </TableWrap>
      )}
    </div>
  );
}
