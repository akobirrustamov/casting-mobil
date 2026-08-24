import { useState } from 'react';
import { adminApi } from '../../api/client';
import { useApi } from '../../api/useApi';
import Modal from '../../components/Modal';
import TrendChart from '../../components/TrendChart';
import { ErrorState, LoadingState } from '../../components/States';
import { TableWrap } from '../../components/Ui';
import { usePanelI18n } from '../../i18n';
import { count } from '../../utils/format';
import { PeriodPicker, RangeLine, StatTile } from './StatBits';

/**
 * Bitta reklamaning statistikasi (ТЗ §29, §81 — BOSQICH F5).
 *
 * <h2>Nega har bir reklama uchun alohida</h2>
 * Umumiy hisobotda faqat TOP-10 banner chiqadi. 30 ta banneri bor
 * admin 25-chisining natijasini umuman ko'ra olmasdi — ТЗ esa «har bir
 * reklama uchun» deydi.
 */
export default function AdStatsModal({ ad, name, onClose }) {
  const { t } = usePanelI18n();
  const [days, setDays] = useState(30);

  const { data, error, loading, reload } = useApi(
    () => adminApi.adStatistics(ad.id, days),
    [ad.id, days]
  );

  return (
    <Modal
      open
      title={`${t('stat.title')} — ${name}`}
      onClose={onClose}
      width={860}
      footer={
        <button type="button" className="uz-btn uz-btn-ghost" onClick={onClose}>
          {t('common.close')}
        </button>
      }
    >
      <div className="mb-4">
        <PeriodPicker days={days} onChange={setDays} disabled={loading} />
      </div>

      {loading ? <LoadingState rows={3} /> :
       error ? <ErrorState error={error} onRetry={reload} /> : (
        <>
          <RangeLine from={data.from} to={data.to} />

          <div className="grid gap-3 mb-4"
               style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))' }}>
            <StatTile label={t('stat.impressions')} value={data.impressions}
                      unique={data.uniqueImpressions} accent="var(--p-accent)" />
            <StatTile label={t('stat.clicks')} value={data.clicks}
                      unique={data.uniqueClicks} />
            {/* ⚠️ CTR nol bo'lishi mumkin va bu HAQIQIY qiymat:
                ko'rsatish bo'lmasa nolga bo'linish o'rniga nol beriladi. */}
            <StatTile label={t('stat.ctr')} value={(data.ctr || 0).toFixed(2)} suffix="%"
                      accent="var(--p-gold)" note={t('stat.ctrZeroHint')} />
          </div>

          <p className="uz-muted mb-4" style={{ fontSize: 12, lineHeight: 1.6 }}>
            {t('stat.uniqueHint')}
          </p>

          {!data.daily?.length ? (
            <p className="uz-muted" style={{ fontSize: 13 }}>{t('stat.noData')}</p>
          ) : (
            <>
              <div className="uz-card p-4 mb-4">
                <TrendChart
                  height={150}
                  points={data.daily.map((d) => ({
                    day: d.date,
                    impressions: d.impressions,
                    clicks: d.clicks,
                  }))}
                  series={[
                    { key: 'impressions', color: 'var(--p-accent)', label: t('stat.impressions') },
                    { key: 'clicks', color: 'var(--p-primary)', label: t('stat.clicks') },
                  ]}
                />
              </div>

              <div className="uz-h2 mb-2" style={{ fontSize: 14 }}>{t('stat.daily')}</div>
              <div className="uz-card overflow-hidden">
                <TableWrap>
                  <table className="uz-table">
                    <thead>
                      <tr>
                        <th>{t('stat.date')}</th>
                        <th style={{ textAlign: 'right' }}>{t('stat.impressions')}</th>
                        <th style={{ textAlign: 'right' }}>{t('stat.clicks')}</th>
                        <th style={{ textAlign: 'right' }}>{t('stat.uniqueImpressions')}</th>
                        <th style={{ textAlign: 'right' }}>{t('stat.uniqueClicks')}</th>
                        <th style={{ textAlign: 'right' }}>{t('stat.ctr')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.daily.map((d) => (
                        <tr key={d.date}>
                          <td className="uz-mono">{d.date}</td>
                          <td className="uz-mono" style={{ textAlign: 'right' }}>{count(d.impressions)}</td>
                          <td className="uz-mono" style={{ textAlign: 'right' }}>{count(d.clicks)}</td>
                          <td className="uz-mono uz-muted" style={{ textAlign: 'right' }}>
                            {count(d.uniqueImpressions)}
                          </td>
                          <td className="uz-mono uz-muted" style={{ textAlign: 'right' }}>
                            {count(d.uniqueClicks)}
                          </td>
                          <td className="uz-mono" style={{ textAlign: 'right', color: 'var(--p-gold)' }}>
                            {(d.ctr || 0).toFixed(2)}%
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </TableWrap>
              </div>
            </>
          )}
        </>
      )}
    </Modal>
  );
}
