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
 * Bitta kontentning tomosha statistikasi (ТЗ §46 — BOSQICH F5).
 *
 * <h2>Voronka uchta bosqichdan iborat</h2>
 * <pre>
 *   ochildi  →  o'ynatildi  →  oxirigacha ko'rildi
 * </pre>
 * Ularni bitta «ko'rishlar» soniga qo'shish farqni yo'q qilardi:
 * birinchi bosqichdagi tushish afisha yoki tavsif haqida, ikkinchisi
 * esa kontentning o'zi haqida gapiradi.
 */
export default function ContentStatsModal({ content, name, onClose }) {
  const { t } = usePanelI18n();
  const [days, setDays] = useState(30);

  const { data, error, loading, reload } = useApi(
    () => adminApi.contentStatistics(content.id, days),
    [content.id, days]
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
            <StatTile label={t('stat.views')} value={data.views} accent="var(--p-accent)" />
            <StatTile label={t('stat.plays')} value={data.plays} accent="var(--p-primary)" />
            <StatTile label={t('stat.completes')} value={data.completes} accent="var(--p-success)" />
            <StatTile label={t('stat.uniqueViewers')} value={data.uniqueViewers} />
            <StatTile label={t('stat.playRate')} value={(data.playRate || 0).toFixed(1)} suffix="%" />
            <StatTile label={t('stat.completionRate')}
                      value={(data.completionRate || 0).toFixed(1)} suffix="%" />
          </div>

          <p className="uz-muted mb-2" style={{ fontSize: 12, lineHeight: 1.6 }}>
            {t('stat.funnelHint')}
          </p>
          <p className="uz-muted mb-4" style={{ fontSize: 12, lineHeight: 1.6 }}>
            {t('stat.uniqueHint')}
          </p>

          {!data.daily?.length ? (
            <p className="uz-muted" style={{ fontSize: 13 }}>{t('stat.noData')}</p>
          ) : (
            <>
              <div className="uz-card p-4 mb-4">
                {/* Standart uchlik (views / plays / completes) — shuning
                    uchun `series` berilmaydi. */}
                <TrendChart height={150} points={data.daily.map((d) => ({
                  day: d.date, views: d.views, plays: d.plays, completes: d.completes,
                }))} />
              </div>

              <div className="uz-h2 mb-2" style={{ fontSize: 14 }}>{t('stat.daily')}</div>
              <div className="uz-card overflow-hidden">
                <TableWrap>
                  <table className="uz-table">
                    <thead>
                      <tr>
                        <th>{t('stat.date')}</th>
                        <th style={{ textAlign: 'right' }}>{t('stat.views')}</th>
                        <th style={{ textAlign: 'right' }}>{t('stat.plays')}</th>
                        <th style={{ textAlign: 'right' }}>{t('stat.completes')}</th>
                        <th style={{ textAlign: 'right' }}>{t('stat.uniqueViewers')}</th>
                        <th style={{ textAlign: 'right' }}>{t('stat.completionRate')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.daily.map((d) => (
                        <tr key={d.date}>
                          <td className="uz-mono">{d.date}</td>
                          <td className="uz-mono" style={{ textAlign: 'right' }}>{count(d.views)}</td>
                          <td className="uz-mono" style={{ textAlign: 'right' }}>{count(d.plays)}</td>
                          <td className="uz-mono" style={{ textAlign: 'right' }}>{count(d.completes)}</td>
                          <td className="uz-mono uz-muted" style={{ textAlign: 'right' }}>
                            {count(d.uniqueViewers)}
                          </td>
                          <td className="uz-mono" style={{ textAlign: 'right' }}>
                            {(d.completionRate || 0).toFixed(1)}%
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
