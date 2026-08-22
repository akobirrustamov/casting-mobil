import { useState } from 'react';
import { adminApi, mediaUrl } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, SearchInput } from '../components/Ui';
import { toBackendLocale, usePanelI18n } from '../i18n';
import CreatorForm from './CreatorForm';

export default function CreatorsPage() {
  const { t, locale } = usePanelI18n();
  const { can } = useAuth();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);

  // ⚠️ Qidiruvsiz ilgari BUTUN jadval kelardi. Ijodkorlar soni
  // cheklanmagan (ТЗ §24: har bir kino uchun aktyorlar, rejissyor,
  // operator...) — ro'yxat tez o'sadi.
  const { data, error, loading, reload } = useApi(
    () => adminApi.creators({ q: q || undefined, page, size: 24 }),
    [q, page]
  );

  // Qidiruv o'zgarganda birinchi sahifaga qaytamiz.
  const onSearch = (value) => { setQ(value); setPage(0); };

  const bl = toBackendLocale(locale);
  const nameOf = (c) => {
    const tr = c.translations || {};
    return tr[bl]?.displayName || tr.UZ?.displayName || Object.values(tr)[0]?.displayName || c.slug;
  };
  const bioOf = (c) => (c.translations || {})[bl]?.bio || '';

  return (
    <>
      <PageHeader
        title={t('creators.title')}
        subtitle={t('creators.subtitle')}
        right={
          <>
            <SearchInput value={q} onChange={onSearch} placeholder={t('creators.search')} />
            {can('CREATOR_CREATE') && (
              <button type="button" className="uz-btn uz-btn-primary"
                      onClick={() => { setEditing(null); setFormOpen(true); }}>
                + {t('cr.new')}
              </button>
            )}
          </>
        }
      />

      {loading ? (
        <div className="uz-card"><LoadingState /></div>
      ) : error ? (
        <div className="uz-card"><ErrorState error={error} onRetry={reload} /></div>
      ) : !data?.items?.length ? (
        <div className="uz-card"><EmptyState icon="★" /></div>
      ) : (
        <div className="grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))' }}>
          {data.items.map((c) => (
            <div key={c.id} className="uz-card uz-card-hover overflow-hidden">
              <div style={{ position: 'relative', aspectRatio: '16 / 9', background: 'var(--p-surface-2)' }}>
                {c.coverMediaId && (
                  <img
                    src={mediaUrl(c.coverMediaId)} alt="" loading="lazy"
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                )}
                {c.featured && (
                  <div style={{ position: 'absolute', top: 10, right: 10 }}>
                    <Badge tone="gold">{t('creators.featured')}</Badge>
                  </div>
                )}
              </div>
              <div className="p-4 flex gap-3">
                {c.photoMediaId && (
                  <img
                    src={mediaUrl(c.photoMediaId)} alt="" loading="lazy"
                    style={{
                      width: 46, height: 46, borderRadius: '50%', objectFit: 'cover',
                      border: '2px solid var(--p-border)', marginTop: -30, background: 'var(--p-surface)',
                    }}
                  />
                )}
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontWeight: 650, fontSize: 14 }}>{nameOf(c)}</div>
                  <div className="uz-muted" style={{ fontSize: 12, marginTop: 2 }}>
                    ⭐ {Number(c.starsReceived || 0).toLocaleString()} {t('creators.stars')}
                  </div>
                  {bioOf(c) && (
                    <p className="uz-muted" style={{
                      fontSize: 12, marginTop: 6, display: '-webkit-box',
                      WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden',
                    }}>
                      {bioOf(c)}
                    </p>
                  )}
                </div>
              </div>
              {can('CREATOR_EDIT') && (
                <div className="px-4 pb-4">
                  <button type="button" className="uz-btn uz-btn-ghost w-full"
                          style={{ minHeight: 36, fontSize: 13 }}
                          onClick={() => { setEditing(c); setFormOpen(true); }}>
                    {t('common.edit')}
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {data?.items?.length > 0 && (
        <Pagination page={page} totalPages={data.totalPages} onPage={setPage} />
      )}

      <CreatorForm
        open={formOpen}
        row={editing}
        onClose={() => setFormOpen(false)}
        onSaved={reload}
      />
    </>
  );
}
