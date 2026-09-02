import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, TableWrap } from '../components/Ui';
import TrendChart from '../components/TrendChart';
import { usePanelI18n } from '../i18n';
import { count } from '../utils/format';

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

  /*
   * ⚠️ Kunlik qatorlar VALYUTA BO'YICHA ajratiladi.
   *
   * Backend `{date, kind, total}` qaytaradi. Ularni bitta chiziqqa
   * qo'shish 100 yulduz va 100 tangani qo'shishday bo'lardi — DTO
   * izohida bu qoida ochiq yozilgan.
   *
   * Shuning uchun har valyuta O'Z grafigida chiziladi: bitta o'q,
   * bitta o'lchov.
   */
  const dailyByKind = groupByKind(report.data?.daily, (r) => r.date);
  const monthlyByKind = groupByKind(
    report.data?.monthly,
    (r) => `${r.year}-${String(r.month).padStart(2, '0')}`,
  );

  return (
    <>
      <PageHeader title={t('dn.title')} subtitle={t('dn.subtitle')} />

      {/* Valyuta bo'yicha jamlanma */}
      {report.loading ? <LoadingState rows={2} /> :
       report.error ? <ErrorState error={report.error} onRetry={report.reload} /> : (
        /*
          ⚠️ `uz-grid-cards` klassi CSS da UMUMAN yo'q edi — u faqat
          shu bir joyda ishlatilardi va hech qachon yozilmagan. Natijada
          kartochkalar butun kenglikka cho'zilib, bittadan ustma-ust
          turardi.
        */
        <div className="grid gap-4 mb-6"
             style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>
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

      {/*
        ⚠️ Kunlik va oylik ma'lumot backendda ALLAQACHON bor edi
        (`DonationReportDto.daily` / `.monthly`, §42), lekin bu sahifa
        ikkalasini ham ishlatmasdi. Raqamlar hisoblanardi va hech kim
        ko'rmasdi.
      */}
      {!report.loading && !report.error && (
        <div className="grid gap-4 mb-6"
             style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))' }}>
          {Object.entries(dailyByKind).map(([kind, points]) => (
            <div className="uz-card p-5" key={`d-${kind}`}>
              <div className="uz-h2 mb-1" style={{ fontSize: 15 }}>
                {t('dn.dailyChart')} — {kind}
              </div>
              <p className="uz-muted mb-3" style={{ fontSize: 12 }}>{t('dn.perKindNote')}</p>
              <TrendChart
                height={150}
                points={points}
                formatValue={count}
                series={[{ key: 'total', label: kind }]}
              />
            </div>
          ))}

          {Object.entries(monthlyByKind).map(([kind, points]) => (
            <div className="uz-card p-5" key={`m-${kind}`}>
              <div className="uz-h2 mb-1" style={{ fontSize: 15 }}>
                {t('dn.monthlyChart')} — {kind}
              </div>
              <p className="uz-muted mb-3" style={{ fontSize: 12 }}>{t('dn.monthlyNote')}</p>
              <TrendChart
                height={150}
                points={points}
                formatValue={count}
                series={[{ key: 'total', label: kind }]}
              />
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

/**
 * Qatorlarni valyuta bo'yicha ajratadi.
 *
 * ⚠️ Bu shunchaki guruhlash emas, QOIDA: har valyuta o'z grafigida
 * chiziladi. Bitta grafikda ikkita valyuta bo'lsa, ularning shkalasi
 * ham bitta bo'lardi — va 100 yulduz 100 tanga bilan bir balandlikda
 * turib, ular tengdek ko'rinardi.
 *
 * @param toDay qatordan grafik uchun sana yorlig'ini oladi
 */
function groupByKind(rows, toDay) {
  const grouped = {};
  (rows || []).forEach((r) => {
    const kind = r.kind || '—';
    (grouped[kind] = grouped[kind] || []).push({
      day: toDay(r),
      total: Number(r.total) || 0,
    });
  });
  return grouped;
}
