import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import TrendChart from '../components/TrendChart';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { PageHeader, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';
import { count, money } from '../utils/format';

const PERIODS = ['today', 'yesterday', 'last7', 'last30'];

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
 * Hisobotlar.
 *
 * Barcha raqamlar kunlik jamlanmadan keladi — xom hodisalar ustida
 * hech qanday hisob-kitob yo'q.
 */
export default function ReportsPage() {
  const { t } = usePanelI18n();
  const [period, setPeriod] = useState('last30');
  const { data, error, loading, reload } = useApi(
    () => adminApi.reportOverview({ period }), [period]);


  return (
    <>
      <PageHeader
        title={t('rp.title')}
        subtitle={t('rp.subtitle')}
        right={
          <div className="flex gap-2 flex-wrap">
            {PERIODS.map((p) => (
              <button key={p} type="button"
                      className={`uz-chip ${period === p ? 'selected' : ''}`}
                      aria-pressed={period === p}
                      onClick={() => setPeriod(p)}>
                {t(`rp.${p}`)}
              </button>
            ))}
          </div>
        }
      />

      {loading ? <LoadingState rows={4} /> :
       error ? <ErrorState error={error} onRetry={reload} /> :
       !data ? <EmptyState icon="📊" /> : (
        <>
          {/* Kechikish ochiq ko'rsatiladi — raqamlar «jonli» emasligini admin bilsin */}
          {data.pendingEvents > 0 && (
            <div className="mb-4 px-4 py-3"
                 style={{ borderRadius: 'var(--p-radius)', background: 'var(--warning-soft)',
                          border: '1px solid var(--warning-border)', color: 'var(--p-warning)',
                          fontSize: 13 }}>
              {t('rp.pending')}: <strong>{count(data.pendingEvents)}</strong> — {t('rp.pendingHint')}
            </div>
          )}

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
          </div>

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
