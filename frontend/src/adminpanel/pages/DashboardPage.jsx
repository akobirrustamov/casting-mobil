import { Link } from 'react-router-dom';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { ErrorState, LoadingState } from '../components/States';
import { PageHeader } from '../components/Ui';
import { usePanelI18n } from '../i18n';

function StatCard({ label, value, accent }) {
  const { t } = usePanelI18n();
  const missing = value === null || value === undefined;
  return (
    <div className="uz-card uz-card-hover p-5" style={{ transition: 'background .15s, border-color .15s' }}>
      <div className="uz-muted" style={{ fontSize: 12, fontWeight: 600 }}>{label}</div>
      {missing ? (
        <>
          <div style={{ fontSize: 20, fontWeight: 700, marginTop: 8, color: 'var(--p-disabled)' }}>
            —
          </div>
          <div className="uz-muted" style={{ fontSize: 11, marginTop: 4 }}>{t('dash.noModule')}</div>
        </>
      ) : (
        <div
          className="uz-mono"
          style={{ fontSize: 30, fontWeight: 700, marginTop: 6, color: accent || 'var(--p-text)' }}
        >
          {Number(value).toLocaleString()}
        </div>
      )}
    </div>
  );
}

export default function DashboardPage() {
  const { t } = usePanelI18n();
  const { data, error, loading, reload } = useApi(() => adminApi.dashboard(), []);

  if (loading) return <LoadingState rows={4} />;
  if (error) return <ErrorState error={error} onRetry={reload} />;

  return (
    <>
      <PageHeader title={t('dash.title')} subtitle={t('dash.subtitle')} />

      <div className="grid gap-4 mb-8" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(190px, 1fr))' }}>
        <StatCard label={t('dash.totalContent')} value={data.totalContent} accent="var(--p-accent)" />
        <StatCard label={t('dash.published')} value={data.publishedContent} accent="var(--p-success)" />
        <StatCard label={t('dash.draft')} value={data.draftContent} />
        <StatCard label={t('dash.scheduled')} value={data.scheduledContent} accent="var(--p-warning)" />
        <StatCard label={t('dash.episodes')} value={data.totalEpisodes} />
        <StatCard label={t('dash.creators')} value={data.totalCreators} accent="var(--p-gold)" />
        <StatCard label={t('dash.categories')} value={data.totalCategories} />
        <StatCard label={t('dash.media')} value={data.totalMedia} />
        <StatCard label={t('dash.staff')} value={data.totalStaff} />
        <StatCard label={t('dash.applications')} value={data.totalCastingApplications} />
      </div>

      {/* Analitika - kunlik jamlanmadan, so'nggi 30 kun */}
      <div className="uz-card p-5 mb-6">
        <div className="flex items-center justify-between gap-3 mb-4 flex-wrap">
          <div className="uz-h2">{t('dash.analytics30d')}</div>
          <Link to="/app/panel/reports" className="uz-btn uz-btn-ghost"
                style={{ minHeight: 34, fontSize: 13, textDecoration: 'none' }}>
            {t('rp.title')} →
          </Link>
        </div>
        <div className="grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(190px, 1fr))' }}>
          <StatCard label={t('dash.users')} value={data.totalUsers} />
          <StatCard label={t('dash.premium')} value={data.premiumUsers} accent="var(--p-gold)" />
          <StatCard label={t('rp.views')} value={data.contentViews30d} accent="var(--p-accent)" />
          <StatCard label={t('dash.adImpressions')} value={data.adImpressions} />
          <StatCard label={t('dash.adClicks')} value={data.adClicks} />
          <StatCard label={t('rp.ctr')} value={data.adCtr == null ? null : Number(data.adCtr.toFixed(2))} />
          <StatCard label={t('dash.subRevenue')} value={data.subscriptionRevenue} accent="var(--p-success)" />
          <StatCard label={t('dash.comments')} value={data.totalComments} />
        </div>
        {data.pendingEvents > 0 && (
          <p className="uz-muted mt-4" style={{ fontSize: 12 }}>
            {t('rp.pending')}: {Number(data.pendingEvents).toLocaleString()} — {t('rp.pendingHint')}
          </p>
        )}
      </div>

      {/* Modul yo'q bo'lgan ko'rsatkichlar ochiq ko'rsatiladi - soxta raqam emas (§45) */}
      <div className="uz-card p-5">
        <div className="uz-h2 mb-1">{t('dash.pending')}</div>
        <p className="uz-muted text-sm mb-5">{t('dash.pendingNote')}</p>
        <div className="grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(190px, 1fr))' }}>
          <StatCard label={t('dash.donations')} value={data.donationRevenue} />
        </div>
        <p className="uz-muted mt-3" style={{ fontSize: 12 }}>{t('dash.donationNote')}</p>
      </div>
    </>
  );
}
