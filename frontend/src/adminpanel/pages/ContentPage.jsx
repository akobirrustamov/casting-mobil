import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { adminApi, mediaUrl } from '../api/client';
import ConfirmDialog, { useConfirm } from '../components/ConfirmDialog';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, SearchInput, StatusBadge, TableWrap } from '../components/Ui';
import { toBackendLocale, usePanelI18n } from '../i18n';
import { count, money } from '../utils/format';
import ContentStatsModal from './reports/ContentStatsModal';
import Select from '../components/Select';

const STATUSES = ['PUBLISHED', 'DRAFT', 'SCHEDULED', 'IN_REVIEW', 'ARCHIVED', 'BLOCKED'];
const TYPES = ['MOVIE', 'SERIES', 'MINI_SERIES', 'SHORT_FILM', 'PODCAST', 'SHOW', 'INTERVIEW', 'STREAM', 'CLIP'];

export default function ContentPage() {
  const { t, locale } = usePanelI18n();
  const { can } = useAuth();
  const navigate = useNavigate();

  // ⚠️ Filtr va sahifa raqami MANZILDA turadi — komponent holatida emas.
  // Muharrir endi alohida sahifa (modal emas), ya'ni ro'yxat undan
  // qaytganda qaytadan chiziladi. Holatda tursa, qidiruv, filtr va
  // sahifa raqami har saqlashdan keyin nolga qaytardi va admin
  // 7-sahifadagi kontentni tuzatgach yana boshidan izlardi.
  const [params, setParams] = useSearchParams();
  const page = Number(params.get('page') || 0);
  const status = params.get('status') || '';
  const type = params.get('type') || '';
  const q = params.get('q') || '';

  /** Bitta filtrni almashtiradi; bo'sh qiymat manzildan olib tashlanadi. */
  const setParam = (patch) => {
    const next = new URLSearchParams(params);
    Object.entries(patch).forEach(([key, value]) => {
      if (value === '' || value === null || value === undefined || value === 0) next.delete(key);
      else next.set(key, String(value));
    });
    // Tarixni to'ldirmaymiz: «orqaga» tugmasi har bir harfni emas,
    // oldingi SAHIFANI qaytarishi kerak.
    setParams(next, { replace: true });
  };

  // Bitta kontent bo'yicha tomosha voronkasi (ТЗ §46).
  const [statsFor, setStatsFor] = useState(null);

  const confirmer = useConfirm(() => reload());

  const { data, error, loading, reload } = useApi(
    () => adminApi.content({ page, size: 20, status: status || undefined, type: type || undefined, q: q || undefined }),
    [page, status, type, q]
  );

  /** Muharrirga o'tamiz va joriy filtrlarni qaytish uchun beramiz. */
  const openEditor = (id) => {
    navigate(`/app/panel/content/${id ?? 'new'}`, {
      state: { from: params.toString() ? `?${params.toString()}` : '' },
    });
  };

  const backendLocale = toBackendLocale(locale);

  /** Tanlangan tildagi sarlavha; tarjima yo'q bo'lsa boshqa tildan olinadi. */
  const titleOf = (item) => {
    const tr = item.translations || {};
    return tr[backendLocale]?.title || tr.UZ?.title || Object.values(tr)[0]?.title || item.slug;
  };

  /** Til uchun alohida afisha bormi - bo'lsa o'sha, bo'lmasa umumiysi. */
  const posterOf = (item) => {
    const localeSpecific = item.localePosters?.[backendLocale];
    return mediaUrl(localeSpecific || item.posterMediaId);
  };

  const hasLocalePoster = (item) => Boolean(item.localePosters?.[backendLocale]);

  return (
    <>
      <PageHeader
        title={t('content.title')}
        subtitle={t('content.subtitle')}
        right={
          <>
            <SearchInput value={q} onChange={(v) => setParam({ q: v, page: 0 })} placeholder={t('content.search')} />
            <Select
              className="uz-select" style={{ width: 'auto' }} value={status}
              aria-label={t('content.col.status')}
              onChange={(e) => setParam({ status: e.target.value, page: 0 })}
            >
              <option value="">{t('content.allStatuses')}</option>
              {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
            </Select>
            <Select
              className="uz-select" style={{ width: 'auto' }} value={type}
              aria-label={t('content.col.type')}
              onChange={(e) => setParam({ type: e.target.value, page: 0 })}
            >
              <option value="">{t('content.allTypes')}</option>
              {TYPES.map((s) => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
            </Select>
            {can('CONTENT_CREATE') && (
              <button
                type="button"
                className="uz-btn uz-btn-primary"
                onClick={() => openEditor(null)}
              >
                + {t('editor.new')}
              </button>
            )}
          </>
        }
      />

      <div className="uz-card overflow-hidden">
        {loading ? (
          <LoadingState />
        ) : error ? (
          <ErrorState error={error} onRetry={reload} />
        ) : !data?.items?.length ? (
          <EmptyState icon="🎬" />
        ) : (
          <>
            <TableWrap>
              <table className="uz-table">
                <thead>
                  <tr>
                    <th style={{ width: 64 }} />
                    <th>{t('content.col.title')}</th>
                    <th>{t('content.col.type')}</th>
                    <th>{t('content.col.orientation')}</th>
                    <th>{t('content.col.status')}</th>
                    <th>{t('content.col.access')}</th>
                    <th style={{ textAlign: 'right' }}>{t('content.col.views')}</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((item) => (
                    <tr key={item.id}>
                      <td>
                        {posterOf(item) ? (
                          <img
                            src={posterOf(item)}
                            alt=""
                            loading="lazy"
                            style={{
                              width: 48, height: 48, objectFit: 'cover',
                              borderRadius: 8, border: '1px solid var(--p-border)',
                            }}
                          />
                        ) : (
                          <div className="uz-skeleton" style={{ width: 48, height: 48 }} />
                        )}
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{titleOf(item)}</div>
                        <div className="uz-muted" style={{ fontSize: 12 }}>
                          {item.slug}
                          {hasLocalePoster(item) && (
                            <span style={{ marginLeft: 8, color: 'var(--p-accent)' }}>
                              • {t('content.localePoster')}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="uz-muted" style={{ fontSize: 13 }}>
                        {String(item.contentType).replace(/_/g, ' ')}
                      </td>
                      <td>
                        <Badge tone={item.orientation === 'VERTICAL' ? 'gold' : 'info'}>
                          {item.orientation === 'VERTICAL' ? t('content.vertical') : t('content.landscape')}
                        </Badge>
                      </td>
                      <td><StatusBadge status={item.status} /></td>
                      <td className="uz-muted" style={{ fontSize: 12 }}>
                        {item.accessPolicy === 'FREE'
                          ? t('common.free')
                          : String(item.accessPolicy).replace(/_/g, ' ')}
                        {item.premierePrice && (
                          <div style={{ color: 'var(--p-gold)', fontWeight: 600 }}>
                            {money(item.premierePrice)} {t('common.currency')}
                          </div>
                        )}
                      </td>
                      <td className="uz-mono" style={{ textAlign: 'right' }}>
                        {count(item.viewCount)}
                      </td>
                      <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                        {/* ⚠️ Statistika `REPORT_VIEW` ruxsatini talab
                            qiladi — kontentni ko'rish huquqi analitikani
                            ko'rish huquqini bermaydi. Ruxsat yo'q
                            xodimga tugmani ko'rsatish uni 403 ga olib
                            borardi. */}
                        {can('REPORT_VIEW') && (
                          <button
                            type="button"
                            className="uz-btn uz-btn-ghost"
                            style={{ minHeight: 34, padding: '0 12px', fontSize: 13, marginRight: 8 }}
                            onClick={() => setStatsFor(item)}
                            title={t('stat.title')} aria-label={t('stat.title')}
                          >
                            📊
                          </button>
                        )}
                        {can('CONTENT_EDIT') && (
                          <button
                            type="button"
                            className="uz-btn uz-btn-ghost"
                            style={{ minHeight: 34, padding: '0 12px', fontSize: 13 }}
                            onClick={() => openEditor(item.id)}
                          >
                            {t('common.edit')}
                          </button>
                        )}
                        {can('CONTENT_DELETE') && (
                          <button
                            type="button"
                            className="uz-btn uz-btn-danger"
                            style={{ minHeight: 34, padding: '0 12px', fontSize: 13, marginLeft: 8 }}
                            onClick={() => confirmer.ask({
                                message: `${item.slug} — ${t('common.remove')}?`,
                                confirmLabel: t('common.remove'),
                                run: () => adminApi.archiveContent(item.id),
                              })}
                          >
                            {t('common.remove')}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableWrap>
            <Pagination page={data.page} totalPages={data.totalPages} onPage={(p) => setParam({ page: p })} />
          </>
        )}
      </div>

      {data && (
        <p className="uz-muted mt-3 text-sm">{t('common.total', { n: data.totalItems })}</p>
      )}

      {statsFor && (
        <ContentStatsModal
          content={statsFor}
          name={titleOf(statsFor)}
          onClose={() => setStatsFor(null)}
        />
      )}

      <ConfirmDialog {...confirmer.props} />
    </>
  );
}
