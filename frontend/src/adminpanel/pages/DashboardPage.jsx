import { useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import TrendChart from '../components/TrendChart';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { PageHeader, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';
import { count, money } from '../utils/format';

const PERIODS = [7, 30, 90];

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
          {count(value)}
        </div>
      )}
    </div>
  );
}

/** Bitta grafik — sarlavha, izoh va bo'sh holat bilan. */
function ChartCard({ title, note, points, series, formatValue }) {
  return (
    <div className="uz-card p-5">
      <div className="uz-h2 mb-1" style={{ fontSize: 15 }}>{title}</div>
      {note && <p className="uz-muted mb-3" style={{ fontSize: 12 }}>{note}</p>}
      <TrendChart points={points} series={series} height={150} formatValue={formatValue} />
    </div>
  );
}

/** Dashboard jadvali — ustunlari sahifada aniqlanadi. */
function MiniTable({ title, rows, columns, note }) {
  const { t } = usePanelI18n();
  return (
    <div>
      <div className="uz-h2 mb-2" style={{ fontSize: 15 }}>{title}</div>
      {note && <p className="uz-muted mb-2" style={{ fontSize: 11 }}>{note}</p>}
      <div className="uz-card overflow-hidden">
        {!rows?.length ? (
          /* Ixcham bo'sh holat: dashboardda beshta jadval yonma-yon
             turadi va har birida to'liq ekranli blok sahifani cho'zib
             yuborardi. Ko'rinish esa boshqa sahifalar bilan bir xil. */
          <EmptyState compact icon="—" title={t('dash.tableEmpty')} />
        ) : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  {columns.map((c) => (
                    <th key={c.key} style={c.align ? { textAlign: c.align } : undefined}>
                      {c.label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((row, i) => (
                  // Backend ba'zi jadvallarda `id` bermaydi (masalan yangi
                  // foydalanuvchilar ro'yxatida) — o'sha yerda indeks
                  // yagona barqaror kalit bo'ladi.
                  <tr key={row.id != null ? `${row.id}-${i}` : i}>
                    {columns.map((c) => (
                      <td key={c.key}
                          className={c.mono ? 'uz-mono' : ''}
                          style={c.align ? { textAlign: c.align } : undefined}>
                        {c.render(row)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </TableWrap>
        )}
      </div>
    </div>
  );
}

const day = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : '—');

/**
 * Boshqaruv paneli (ТЗ §48 — BOSQICH F3).
 *
 * <h2>So'rovlar parallel ketadi (§73)</h2>
 * Uchta endpoint bor: `summary`, `tables` va `charts`. Birinchi ikkitasi
 * bitta `Promise.all` da, uchinchisi esa alohida hook'da — ikkala effekt
 * ham bir renderda ishga tushadi, ya'ni so'rovlar bir vaqtda ketadi.
 *
 * <h2>Nega grafiklar alohida hook'da</h2>
 * Davr almashtirilganda (7 / 30 / 90 kun) FAQAT grafiklar qayta
 * so'raladi. Uchalasi bitta hook'da bo'lsa, har bosishda kartochkalar va
 * jadvallar ham bekorga qayta yuklanardi — ular davrga bog'liq emas.
 *
 * <h2>Ma'lumot yo'q bo'lsa</h2>
 * Bo'sh holat ko'rsatiladi, soxta grafik emas (§45). Nol qiymatli
 * chiziq «hech kim ko'rmagan» degan ma'noni berardi — bilmaslik esa
 * boshqa narsa.
 */
export default function DashboardPage() {
  const { t } = usePanelI18n();
  const [days, setDays] = useState(30);

  const summary = useApi(
    () => Promise.all([adminApi.dashboard(), adminApi.dashboardTables(5)]),
    []
  );
  const charts = useApi(() => adminApi.dashboardCharts(days), [days]);

  if (summary.loading) return <LoadingState rows={4} />;
  if (summary.error) return <ErrorState error={summary.error} onRetry={summary.reload} />;

  const [data, tables] = summary.data;

  // Donatlar valyuta bo'yicha ajratilgan qatorlarda keladi
  // ({day, series: 'STARS', value}). Grafik uchun ularni kun bo'yicha
  // bitta qatorga yig'amiz — aks holda ikkala valyuta bitta chiziqqa
  // tushib, jami ma'nosiz raqam bo'lardi.
  const donationKinds = Array.from(
    new Set((charts.data?.donations || []).map((p) => p.series).filter(Boolean))
  );
  const donationsByDay = Object.values(
    (charts.data?.donations || []).reduce((acc, p) => {
      const row = acc[p.day] || (acc[p.day] = { day: p.day });
      row[p.series] = Number(p.value) || 0;
      return acc;
    }, {})
  ).sort((a, b) => (a.day < b.day ? -1 : 1));

  const DONATION_COLORS = ['var(--p-gold)', 'var(--p-accent)', 'var(--p-primary)'];

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
            {t('rp.pending')}: {count(data.pendingEvents)} — {t('rp.pendingHint')}
          </p>
        )}
      </div>

      {/* ─────────────────────────── Grafiklar (§48) */}
      <div className="flex items-center justify-between gap-3 mb-3 flex-wrap">
        <div className="uz-h2">{t('dash.charts')}</div>
        <div className="flex gap-2" role="group" aria-label={t('dash.period')}>
          {PERIODS.map((p) => (
            <button key={p} type="button"
                    className={`uz-chip ${days === p ? 'selected' : ''}`}
                    aria-pressed={days === p}
                    onClick={() => setDays(p)}>
              {t('dash.days', { n: p })}
            </button>
          ))}
        </div>
      </div>

      {charts.loading ? (
        <div className="uz-card mb-8"><LoadingState rows={2} /></div>
      ) : charts.error ? (
        // ⚠️ Grafik xatosi butun sahifani yiqitmaydi: kartochkalar va
        // jadvallar allaqachon kelgan va ular baribir foydali.
        <div className="uz-card mb-8">
          <ErrorState error={charts.error} onRetry={charts.reload} />
        </div>
      ) : (
        <div className="grid gap-4 mb-8"
             style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))' }}>
          <ChartCard
            title={t('dash.userGrowth')}
            note={t('dash.userGrowthNote')}
            points={charts.data?.userGrowth}
            series={[{ key: 'value', color: 'var(--p-primary)', label: t('dash.userGrowth') }]}
          />
          <ChartCard
            title={t('dash.viewsChart')}
            points={charts.data?.views}
            series={[{ key: 'value', color: 'var(--p-accent)', label: t('dash.viewsChart') }]}
          />
          <ChartCard
            title={t('dash.subRevenueChart')}
            points={charts.data?.subscriptionRevenue}
            series={[{ key: 'value', color: 'var(--p-success)', label: t('dash.subRevenueChart') }]}
            formatValue={money}
          />
          <ChartCard
            title={t('dash.donationsChart')}
            note={t('dash.donationsChartNote')}
            points={donationsByDay}
            series={donationKinds.map((kind, i) => ({
              key: kind,
              color: DONATION_COLORS[i % DONATION_COLORS.length],
              label: kind,
            }))}
          />
        </div>
      )}

      {/* ─────────────────────────── Jadvallar (§48) */}
      <div className="uz-h2 mb-3">{t('dash.tables')}</div>
      <div className="grid gap-6 mb-8"
           style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))' }}>
        <MiniTable
          title={t('dash.latestContent')}
          rows={tables?.latestContent}
          columns={[
            { key: 'name', label: t('common.slug'), render: (r) => r.name || `#${r.id}` },
            { key: 'at', label: t('dash.col.date'), align: 'right', mono: true,
              render: (r) => day(r.at) },
          ]}
        />
        <MiniTable
          title={t('dash.topContent')}
          note={t('dash.idOnlyNote')}
          rows={tables?.topContent}
          columns={[
            { key: 'id', label: t('dash.col.content'), mono: true,
              render: (r) => r.name || `#${r.id}` },
            { key: 'value', label: t('dash.col.views'), align: 'right', mono: true,
              render: (r) => count(r.value) },
          ]}
        />
        <MiniTable
          title={t('dash.latestUsers')}
          note={t('dash.noPhoneNote')}
          rows={tables?.latestUsers}
          columns={[
            { key: 'name', label: t('dash.col.user'), render: (r) => r.name || '—' },
            { key: 'at', label: t('dash.col.date'), align: 'right', mono: true,
              render: (r) => day(r.at) },
          ]}
        />
        <MiniTable
          title={t('dash.bestAds')}
          note={t('dash.idOnlyNote')}
          rows={tables?.bestAds}
          columns={[
            { key: 'id', label: t('dash.col.ad'), mono: true,
              render: (r) => r.name || `#${r.id}` },
            { key: 'value', label: t('dash.col.clicks'), align: 'right', mono: true,
              render: (r) => count(r.value) },
          ]}
        />
        <MiniTable
          title={t('dash.topCreators')}
          rows={tables?.topCreators}
          columns={[
            { key: 'id', label: t('dash.col.creator'), mono: true, render: (r) => `#${r.id}` },
            // `name` bu yerda ijodkor ismi EMAS — donat valyutasi
            // (STARS / COIN). Ustun sarlavhasi shuni ochiq aytadi.
            { key: 'name', label: t('dash.col.currency'), render: (r) => r.name || '—' },
            { key: 'value', label: t('dash.col.total'), align: 'right', mono: true,
              render: (r) => count(r.value) },
          ]}
        />
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
