import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import TrendChart from '../components/TrendChart';
import BarChart from '../components/charts/BarChart';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { PageHeader, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';
import { count, money } from '../utils/format';
import ReportFilters, { EMPTY_REPORT_FILTER, toReportParams } from './reports/ReportFilters';

function Stat({ label, value, suffix, accent }) {
  return (
    <div className="uz-card p-4">
      <div className="uz-muted" style={{ fontSize: 12, fontWeight: 600 }}>{label}</div>
      <div className="uz-mono" style={{
        fontSize: 26, fontWeight: 700, marginTop: 6,
        color: accent || 'var(--p-text)',
      }}>
        {value}
        {suffix && <span style={{ fontSize: 14, marginLeft: 4 }}>{suffix}</span>}
      </div>
    </div>
  );
}

/**
 * Hisobotlar (ТЗ §45, §47).
 *
 * Barcha raqamlar kunlik jamlanmadan keladi — xom hodisalar ustida
 * hech qanday hisob-kitob yo'q.
 *
 * <h2>Filtr qo'llanganda buni SAHIFA aytadi</h2>
 * Backend javobda `appliedFilters` ni qaytaradi va panel uni ochiq
 * ko'rsatadi. Usiz admin «bu son butun platformanikimi yoki
 * filtrlanganmi?» degan savolga javob topa olmasdi — ayniqsa saqlangan
 * yoki hamkasbga yuborilgan skrinshotda.
 */
export default function ReportsPage() {
  const { t } = usePanelI18n();
  const [filter, setFilter] = useState(EMPTY_REPORT_FILTER);
  const params = toReportParams(filter);

  const { data, error, loading, reload } = useApi(
    () => adminApi.reportOverview(params),
    [
      params.period, params.from, params.to,
      params.contentId, params.categoryId, params.creatorId,
      params.tariffId, params.advertisementId,
    ]
  );

  const applied = data?.appliedFilters || {};
  const isFiltered = Object.values(applied).some((v) => v !== null && v !== undefined);

  // Filtr qo'llangan, lekin mos ma'lumot yo'q — bu BO'SH NATIJA, xato emas.
  // Ikkalasini bir xil ko'rsatish admin uchun butunlay boshqa xulosa bo'lardi.
  const noMatch = isFiltered && data && !data.series?.length
    && !data.topContent?.length && !data.topAds?.length;

  return (
    <>
      <PageHeader title={t('rp.title')} subtitle={t('rp.subtitle')} />

      <ReportFilters value={filter} onChange={setFilter} />

      {loading ? <LoadingState rows={4} /> :
       error ? <ErrorState error={error} onRetry={reload} /> :
       !data ? <EmptyState icon="📊" /> : (
        <>
          {isFiltered && (
            <div className="mb-4 px-4 py-3"
                 style={{ borderRadius: 'var(--p-radius)', background: 'var(--brand-primary-soft, var(--p-surface-2))',
                          border: '1px solid var(--p-primary)', color: 'var(--p-text)',
                          fontSize: 13 }}>
              <strong>{t('rp.filtered')}.</strong> {t('rp.filteredHint')}
            </div>
          )}

          {/* Kechikish ochiq ko'rsatiladi — raqamlar «jonli» emasligini admin bilsin */}
          {data.pendingEvents > 0 && (
            <div className="mb-4 px-4 py-3"
                 style={{ borderRadius: 'var(--p-radius)', background: 'var(--warning-soft)',
                          border: '1px solid var(--warning-border)', color: 'var(--p-warning)',
                          fontSize: 13 }}>
              {t('rp.pending')}: <strong>{count(data.pendingEvents)}</strong> — {t('rp.pendingHint')}
            </div>
          )}

          <p className="uz-muted mb-4 uz-mono" style={{ fontSize: 12 }}>
            {data.from} — {data.to}
          </p>

          <div className="grid gap-4 mb-6"
               style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))' }}>
            <Stat label={t('rp.views')} value={count(data.totalViews)} accent="var(--p-accent)" />
            <Stat label={t('rp.plays')} value={count(data.totalPlays)} />
            <Stat label={t('rp.completes')} value={count(data.totalCompletes)} accent="var(--p-success)" />
            <Stat label={t('rp.completionRate')}
                  value={(data.completionRate || 0).toFixed(1)} suffix="%" />
            <Stat label={t('rp.impressions')} value={count(data.adImpressions)} />
            <Stat label={t('rp.clicks')} value={count(data.adClicks)} />
            <Stat label={t('rp.ctr')} value={(data.adCtr || 0).toFixed(2)} suffix="%"
                  accent="var(--p-gold)" />
            {/* ⚠️ Backend bu ko'rsatkichni allaqachon qaytarardi
                (`subscriptionRevenue`), lekin sahifa uni ko'rsatmasdi.
                `money()` `null` ni «—» qiladi: obuna daromadi
                hisoblanmagan holat nol bilan aralashib ketmasin (§103). */}
            <Stat label={t('rp.subRevenue')} value={money(data.subscriptionRevenue)}
                  suffix={t('common.currency')} accent="var(--p-success)" />
          </div>

          {noMatch && (
            <p className="uz-muted mb-4" style={{ fontSize: 13 }}>{t('rp.noMatch')}</p>
          )}

          <div className="uz-card p-5 mb-6">
            <div className="uz-h2 mb-3" style={{ fontSize: 15 }}>{t('rp.chart')}</div>
            <TrendChart points={data.series} />
          </div>

          <p className="uz-muted mb-3" style={{ fontSize: 12 }}>{t('rp.uniqueHint')}</p>

          <div className="grid gap-6" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))' }}>
            <div>
              <div className="uz-h2 mb-3" style={{ fontSize: 15 }}>{t('rp.topContent')}</div>
              <div className="uz-card overflow-hidden">
                {!data.topContent?.length ? <EmptyState icon="🎬" body={t('rp.noData')} /> : (
                  <TableWrap>
                    <table className="uz-table">
                      <thead>
                        <tr>
                          <th>{t('common.slug')}</th>
                          <th style={{ textAlign: 'right' }}>{t('rp.views')}</th>
                          <th style={{ textAlign: 'right' }}>{t('rp.plays')}</th>
                          <th style={{ textAlign: 'right' }}>{t('rp.unique')}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.topContent.map((c) => (
                          <tr key={c.contentId}>
                            <td style={{ fontWeight: 600, fontSize: 13 }}>{c.slug}</td>
                            <td className="uz-mono" style={{ textAlign: 'right' }}>{count(c.views)}</td>
                            <td className="uz-mono uz-muted" style={{ textAlign: 'right' }}>{count(c.plays)}</td>
                            <td className="uz-mono uz-muted" style={{ textAlign: 'right' }}>{count(c.uniqueViewers)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </TableWrap>
                )}
              </div>
            </div>

            <div>
              <div className="uz-h2 mb-3" style={{ fontSize: 15 }}>{t('rp.topAds')}</div>

              {/*
                ⚠️ Grafik va jadval BIR-BIRINI TAKRORLAMAYDI.

                Grafik bitta savolga javob beradi: qaysi reklama ko'proq
                ko'rsatilgan va bosilgan. Jadval esa aniq sonlarni va
                CTR ni beradi.

                CTR grafikka QO'SHILMAYDI: u foiz, ustunlar esa son.
                Bitta o'qqa qo'yilsa 7.57 ustuni 4358 yonida ko'rinmas
                chiziqqa aylanardi; ikkinchi o'q qo'shilsa esa ikkita
                shkala bir-birini yolg'on taqqoslardi.
              */}
              {data.topAds?.length > 0 && (
                <div className="uz-card p-4 mb-3">
                  <BarChart
                    height={Math.max(160, data.topAds.length * 42)}
                    formatValue={count}
                    data={data.topAds.map((a) => ({
                      label: a.name,
                      impressions: a.impressions,
                      clicks: a.clicks,
                    }))}
                    bars={[
                      { key: 'impressions', label: t('rp.impressions') },
                      { key: 'clicks', label: t('rp.clicks') },
                    ]}
                  />
                </div>
              )}

              <div className="uz-card overflow-hidden">
                {!data.topAds?.length ? <EmptyState icon="📢" body={t('rp.noData')} /> : (
                  <TableWrap>
                    <table className="uz-table">
                      <thead>
                        <tr>
                          <th>{t('ads.name')}</th>
                          <th style={{ textAlign: 'right' }}>{t('rp.impressions')}</th>
                          <th style={{ textAlign: 'right' }}>{t('rp.clicks')}</th>
                          <th style={{ textAlign: 'right' }}>{t('rp.ctr')}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.topAds.map((a) => (
                          <tr key={a.advertisementId}>
                            <td style={{ fontWeight: 600, fontSize: 13 }}>{a.name}</td>
                            <td className="uz-mono" style={{ textAlign: 'right' }}>{count(a.impressions)}</td>
                            <td className="uz-mono uz-muted" style={{ textAlign: 'right' }}>{count(a.clicks)}</td>
                            <td className="uz-mono" style={{ textAlign: 'right', color: 'var(--p-gold)' }}>
                              {(a.ctr || 0).toFixed(2)}%
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </TableWrap>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}
