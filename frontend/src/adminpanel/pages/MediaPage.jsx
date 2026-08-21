import { useState } from 'react';
import { adminApi, mediaUrl } from '../api/client';
import { useApi } from '../api/useApi';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination } from '../components/Ui';
import { usePanelI18n } from '../i18n';

export default function MediaPage() {
  const { t } = usePanelI18n();
  const [page, setPage] = useState(0);
  const { data, error, loading, reload } = useApi(
    () => adminApi.media({ page, size: 40 }),
    [page]
  );

  const size = (bytes) => {
    if (!bytes) return '—';
    const kb = bytes / 1024;
    return kb > 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${Math.round(kb)} KB`;
  };

  return (
    <>
      <PageHeader title={t('media.title')} subtitle={t('media.subtitle')} />

      {loading ? (
        <div className="uz-card"><LoadingState /></div>
      ) : error ? (
        <div className="uz-card"><ErrorState error={error} onRetry={reload} /></div>
      ) : !data?.items?.length ? (
        <div className="uz-card"><EmptyState icon="🖼" /></div>
      ) : (
        <>
          <div className="grid gap-3" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))' }}>
            {data.items.map((m) => (
              <div key={m.id} className="uz-card uz-card-hover overflow-hidden">
                <div style={{ aspectRatio: '1 / 1', background: 'var(--p-surface-2)', position: 'relative' }}>
                  {m.type === 'IMAGE' ? (
                    <img
                      src={mediaUrl(m.id)} alt={m.originalFilename || ''} loading="lazy"
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  ) : (
                    <div className="flex items-center justify-center h-full" style={{ fontSize: 34 }} aria-hidden="true">
                      🎞
                    </div>
                  )}
                  <div style={{ position: 'absolute', bottom: 6, left: 6 }}>
                    <Badge tone={m.type === 'VIDEO' ? 'gold' : 'info'}>{m.type}</Badge>
                  </div>
                </div>
                <div className="p-2">
                  <div className="uz-muted" style={{ fontSize: 11, wordBreak: 'break-all' }}>
                    {m.width && m.height ? `${m.width}×${m.height}` : ''} · {size(m.sizeBytes)}
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className="uz-card mt-4">
            <Pagination page={data.page} totalPages={data.totalPages} onPage={setPage} />
          </div>
          <p className="uz-muted mt-3 text-sm">{t('common.total', { n: data.totalItems })}</p>
        </>
      )}
    </>
  );
}
