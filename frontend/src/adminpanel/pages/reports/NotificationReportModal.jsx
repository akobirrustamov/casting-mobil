import { adminApi } from '../../api/client';
import { useApi } from '../../api/useApi';
import Modal from '../../components/Modal';
import { ErrorState, LoadingState } from '../../components/States';
import { Badge } from '../../components/Ui';
import { usePanelI18n } from '../../i18n';
import { StatTile } from './StatBits';

const at = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : '—');

/**
 * Bildirishnoma hisoboti (ТЗ §33 — BOSQICH F5).
 *
 * <h2>Nol EMAS, bo'sh</h2>
 * Backend har bir ko'rsatkichni `{available, value, unique,
 * unavailableReason}` ko'rinishida beradi. `available: false` —
 * «o'lchanmaydi», va panel uni bo'sh katak sifatida ko'rsatadi.
 *
 * Nol ko'rsatish yolg'on bo'lardi: `delivered = 0` admin uchun «hech
 * kimga yetib bormadi» degani va u butunlay boshqa muammoni qidirib
 * ketardi. Aslida esa push provayderi hali ulanmagan va kvitansiya
 * umuman kelmaydi.
 *
 * <h2>Voronka QABUL QILUVCHILAR bo'yicha</h2>
 * Har bir raqam — odamlar soni, xabarning o'z holati emas. Xabar
 * holati alohida qatorda ({@code status}), aks holda «1 kishiga
 * yuborilgan xabarni 250 kishi ochgan» degan ma'nosiz voronka
 * chiqardi.
 */
export default function NotificationReportModal({ notification, name, onClose }) {
  const { t } = usePanelI18n();

  const { data, error, loading, reload } = useApi(
    () => adminApi.notificationReport(notification.id),
    [notification.id]
  );

  /** `Metric` ni kartochkaga o'giradi: o'lchanmasa — `null`. */
  const tile = (label, metric, accent) => (
    <StatTile
      label={label}
      value={metric?.available ? (metric.value ?? 0) : null}
      unique={metric?.available ? metric.unique : null}
      accent={accent}
      note={metric?.available ? null : metric?.unavailableReason}
    />
  );

  return (
    <Modal
      open
      title={`${t('stat.report')} — ${name}`}
      onClose={onClose}
      width={760}
      footer={
        <button type="button" className="uz-btn uz-btn-ghost" onClick={onClose}>
          {t('common.close')}
        </button>
      }
    >
      {loading ? <LoadingState rows={3} /> :
       error ? <ErrorState error={error} onRetry={reload} /> : (
        <>
          <div className="flex items-center gap-4 flex-wrap mb-4">
            <span className="flex items-center gap-2">
              <span className="uz-muted" style={{ fontSize: 12 }}>{t('stat.status')}</span>
              <Badge tone={data.status === 'SENT' ? 'published'
                : data.status === 'FAILED' ? 'blocked' : 'draft'}>
                {data.status}
              </Badge>
            </span>
            <span className="uz-muted uz-mono" style={{ fontSize: 12 }}>
              {t('stat.scheduledAt')}: {at(data.scheduledAt)}
            </span>
            <span className="uz-muted uz-mono" style={{ fontSize: 12 }}>
              {t('stat.sentAt')}: {at(data.sentAt)}
            </span>
          </div>

          {data.failureReason && (
            <div role="alert" className="mb-4 px-4 py-3"
                 style={{ borderRadius: 'var(--p-radius)', background: 'var(--danger-soft)',
                          border: '1px solid var(--danger-border)',
                          color: 'var(--p-danger)', fontSize: 13 }}>
              {t('stat.failureReason')}: {data.failureReason}
            </div>
          )}

          <div className="grid gap-3 mb-4"
               style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(170px, 1fr))' }}>
            {/* ⚠️ Auditoriya hajmi «yuborildi» EMAS — bu nishon
                auditoriyasining HOZIRGI hajmi. Usiz `opened` ni umuman
                talqin qilib bo'lmaydi: 250 ta ochilish auditoriya 300
                kishimi yoki 300 000 kishimi — butunlay boshqa xulosa. */}
            {tile(t('stat.audienceSize'), data.audienceSize, 'var(--p-accent)')}
            {tile(t('stat.sent'), data.sent)}
            {tile(t('stat.delivered'), data.delivered)}
            {tile(t('stat.opened'), data.opened, 'var(--p-success)')}
            {tile(t('stat.clicked'), data.clicked, 'var(--p-gold)')}
            {tile(t('stat.failed'), data.failed, 'var(--p-danger)')}
          </div>

          <p className="uz-muted" style={{ fontSize: 12, lineHeight: 1.6 }}>
            {t('stat.nullHint')}
          </p>
        </>
      )}
    </Modal>
  );
}
