import { useEffect, useState } from 'react';
import { adminApi, mediaUrl } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import ConfirmDialog, { useConfirm } from '../components/ConfirmDialog';
import Modal from '../components/Modal';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, SearchInput } from '../components/Ui';
import { usePanelI18n } from '../i18n';

/** Baytlarni o'qiladigan ko'rinishga keltiradi. */
const humanSize = (bytes) => {
  if (!bytes) return '—';
  const kb = bytes / 1024;
  return kb > 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${Math.round(kb)} KB`;
};

const humanDuration = (seconds) => {
  if (!seconds) return null;
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
};

/**
 * Media kutubxonasi (ТЗ §26 — BOSQICH F2).
 *
 * Backend to'rtta amalni taklif qilardi (qayerda ishlatilgani,
 * arxivlash, tiklash, o'chirish), panel esa faqat ro'yxatni
 * ko'rsatardi. Ya'ni `MEDIA_DELETE` ruxsati mavjud, lekin uni amalga
 * oshiradigan tugma yo'q edi — bu xodimga noto'g'ri tasavvur berardi.
 */
export default function MediaPage() {
  const { t } = usePanelI18n();
  const { can } = useAuth();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [type, setType] = useState('');
  const [status, setStatus] = useState('');
  const [details, setDetails] = useState(null);

  // Backend qidiruv va filtrni qo'llab-quvvatlaydi (ТЗ §26) — panel
  // ulardan foydalanmasdi va admin faylni sahifalab qidirishga majbur
  // edi.
  const { data, error, loading, reload } = useApi(
    () => adminApi.media({
      q: q || undefined,
      type: type || undefined,
      // ⚠️ Bo'sh qiymat «hammasi» EMAS: backend holat berilmasa
      // faqat READY qaytaradi (§26). Arxivlangan fayllarni ko'rish
      // uchun ular ataylab so'ralishi kerak.
      status: status || undefined,
      page,
      size: 40,
    }),
    [q, type, status, page]
  );

  const onFilter = (setter) => (value) => { setter(value); setPage(0); };

  return (
    <>
      <PageHeader
        title={t('media.title')}
        subtitle={t('media.subtitle')}
        right={(
          <div className="flex items-center gap-3 flex-wrap">
            <SearchInput value={q} onChange={onFilter(setQ)}
                         placeholder={t('media.search')} />
            <select className="uz-select" value={type} aria-label={t('media.allTypes')}
                    onChange={(e) => onFilter(setType)(e.target.value)}>
              <option value="">{t('media.allTypes')}</option>
              <option value="IMAGE">IMAGE</option>
              <option value="VIDEO">VIDEO</option>
              <option value="AUDIO">AUDIO</option>
              <option value="DOCUMENT">DOCUMENT</option>
            </select>
            <select className="uz-select" value={status} aria-label={t('media.allStatuses')}
                    onChange={(e) => onFilter(setStatus)(e.target.value)}>
              <option value="">{t('media.status.READY')}</option>
              <option value="ARCHIVED">{t('media.status.ARCHIVED')}</option>
            </select>
          </div>
        )}
      />

      <p className="uz-muted mb-4 text-sm">{t('media.archivedHint')}</p>

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
              <button
                key={m.id}
                type="button"
                className="uz-card uz-card-hover overflow-hidden"
                style={{ padding: 0, textAlign: 'left', cursor: 'pointer', border: 0 }}
                onClick={() => setDetails(m)}
                aria-label={`${t('media.details')}: ${m.originalFilename || m.id}`}
              >
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
                  {m.status === 'ARCHIVED' && (
                    <div style={{ position: 'absolute', top: 6, right: 6 }}>
                      <Badge tone="archived">{t('media.status.ARCHIVED')}</Badge>
                    </div>
                  )}
                </div>
                <div className="p-2">
                  <div className="uz-muted" style={{ fontSize: 11, wordBreak: 'break-all' }}>
                    {m.width && m.height ? `${m.width}×${m.height}` : ''} · {humanSize(m.sizeBytes)}
                  </div>
                </div>
              </button>
            ))}
          </div>
          <div className="uz-card mt-4">
            <Pagination page={data.page} totalPages={data.totalPages} onPage={setPage} />
          </div>
          <p className="uz-muted mt-3 text-sm">{t('common.total', { n: data.totalItems })}</p>
        </>
      )}

      {/* ⚠️ Shartli chizish: oyna joriy ishlatilish ro'yxatini o'zi
          yuklaydi va uni har ochilishda YANGIDAN so'rash kerak — fayl
          shu orada bo'shatilgan yoki band qilingan bo'lishi mumkin. */}
      {details && (
        <MediaDetails
          media={details}
          canManage={can('MEDIA_DELETE')}
          onClose={() => setDetails(null)}
          onChanged={() => { setDetails(null); reload(); }}
        />
      )}
    </>
  );
}

/**
 * Bitta fayl haqidagi oyna: ma'lumot, qayerda ishlatilgani va amallar.
 *
 * <h2>Nega o'chirish tugmasi ishlatilish ro'yxatiga bog'langan</h2>
 * Backend ishlatilayotgan faylni o'chirishga 409 qaytaradi. Panel buni
 * kutib turib, keyin xato ko'rsatishi mumkin edi — lekin unda admin
 * «o'chirdim» deb o'ylab, keyin tushunarsiz xabar olardi. Ro'yxat
 * OLDINDAN ko'rsatilsa, qaror boshidanoq to'g'ri bo'ladi.
 *
 * ⚠️ Bu tekshiruv qulaylik uchun: ikki admin bir vaqtda ishlayotgan
 * bo'lsa fayl oyna ochiq turganda band qilinishi mumkin. Shuning uchun
 * 409 javobi ham baribir ushlanadi va sabab ro'yxati bilan ko'rsatiladi.
 */
function MediaDetails({ media, canManage, onClose, onChanged }) {
  const { t } = usePanelI18n();
  const confirmer = useConfirm(onChanged);

  const [usage, setUsage] = useState(null);
  const [usageError, setUsageError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    adminApi.mediaUsage(media.id)
      .then((rows) => { if (!cancelled) setUsage(rows); })
      .catch((err) => { if (!cancelled) setUsageError(err); });
    return () => { cancelled = true; };
  }, [media.id]);

  const name = media.originalFilename || `#${media.id}`;
  const inUse = Array.isArray(usage) && usage.length > 0;
  const isArchived = media.status === 'ARCHIVED';
  const duration = humanDuration(media.durationSeconds);

  return (
    <>
      <Modal
        open
        title={t('media.details')}
        onClose={onClose}
        width={720}
        footer={
          <>
            {canManage ? (
              <>
                {isArchived ? (
                  <button type="button" className="uz-btn uz-btn-ghost"
                          style={{ marginRight: 'auto' }}
                          onClick={() => confirmer.ask({
                            title: t('media.restore'),
                            message: t('media.confirmRestore', { name }),
                            confirmLabel: t('media.restore'),
                            danger: false,
                            run: () => adminApi.restoreMedia(media.id),
                          })}>
                    {t('media.restore')}
                  </button>
                ) : (
                  <button type="button" className="uz-btn uz-btn-ghost"
                          style={{ marginRight: 'auto' }}
                          onClick={() => confirmer.ask({
                            title: t('media.archive'),
                            message: t('media.confirmArchive', { name }),
                            note: t('media.archiveNote'),
                            confirmLabel: t('media.archive'),
                            danger: false,
                            run: () => adminApi.archiveMedia(media.id),
                          })}>
                    {t('media.archive')}
                  </button>
                )}

                <button
                  type="button"
                  className="uz-btn uz-btn-danger"
                  /* Ishlatilish hali yuklanmagan bo'lsa ham bloklaymiz:
                     «bilmayman» holatida o'chirishga ruxsat berish
                     tekshiruvni butunlay ma'nosiz qilardi. */
                  disabled={usage === null || inUse}
                  title={inUse ? t('media.deleteBlocked') : undefined}
                  onClick={() => confirmer.ask({
                    title: t('media.delete'),
                    message: t('media.confirmDelete', { name }),
                    note: t('media.deleteNote'),
                    confirmLabel: t('media.delete'),
                    run: () => adminApi.deleteMedia(media.id),
                  })}
                >
                  {t('media.delete')}
                </button>
              </>
            ) : (
              <span className="uz-muted" style={{ fontSize: 12, marginRight: 'auto' }}>
                {t('media.noDeletePermission')}
              </span>
            )}
            <button type="button" className="uz-btn uz-btn-ghost" onClick={onClose}>
              {t('common.close')}
            </button>
          </>
        }
      >
        <div className="uz-row" style={{ alignItems: 'flex-start' }}>
          <div style={{ flex: '0 0 200px' }}>
            <div style={{ aspectRatio: '1 / 1', background: 'var(--p-surface-2)',
                          borderRadius: 'var(--p-radius)', overflow: 'hidden' }}>
              {media.type === 'IMAGE' ? (
                <img src={mediaUrl(media.id)} alt={name}
                     style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              ) : (
                <div className="flex items-center justify-center h-full"
                     style={{ fontSize: 44 }} aria-hidden="true">🎞</div>
              )}
            </div>
            <a className="uz-btn uz-btn-ghost mt-3"
               style={{ width: '100%', textDecoration: 'none' }}
               href={mediaUrl(media.id)} target="_blank" rel="noreferrer">
              {t('media.openRaw')}
            </a>
          </div>

          <div className="uz-col" style={{ minWidth: 240 }}>
            <Field label={t('media.filename')} value={name} mono />
            <Field label={t('media.size')} value={humanSize(media.sizeBytes)} mono />
            <Field
              label={t('media.dimensions')}
              value={media.width && media.height ? `${media.width}×${media.height}` : '—'}
              mono
            />
            {duration && <Field label={t('media.duration')} value={duration} mono />}
            <Field label={t('media.uploadedAt')}
                   value={media.createdAt ? media.createdAt.replace('T', ' ').slice(0, 16) : '—'} mono />
            <div className="mt-3 flex gap-2">
              <Badge tone={media.type === 'VIDEO' ? 'gold' : 'info'}>{media.type}</Badge>
              <Badge tone={isArchived ? 'archived' : 'published'}>
                {t(`media.status.${media.status || 'READY'}`)}
              </Badge>
            </div>
          </div>
        </div>

        <div className="mt-5">
          <div className="uz-label">{t('media.usage')}</div>

          {usageError ? (
            <p style={{ color: 'var(--p-danger)', fontSize: 13 }} role="alert">
              {usageError.message}
            </p>
          ) : usage === null ? (
            <p className="uz-muted" style={{ fontSize: 13 }}>{t('media.usageLoading')}</p>
          ) : usage.length === 0 ? (
            <p className="uz-muted" style={{ fontSize: 13 }}>{t('media.usageEmpty')}</p>
          ) : (
            <>
              <ul style={{ margin: '4px 0 0', paddingLeft: 18, fontSize: 13, lineHeight: 1.8 }}>
                {usage.map((u) => (
                  <li key={u.where}>
                    {u.where} — <span className="uz-mono">{u.count}</span>
                  </li>
                ))}
              </ul>
              <p className="mt-2" style={{ fontSize: 12, color: 'var(--p-warning)' }}>
                {t('media.deleteBlocked')}
              </p>
            </>
          )}
        </div>
      </Modal>

      <ConfirmDialog {...confirmer.props} />
    </>
  );
}

function Field({ label, value, mono = false }) {
  return (
    <div className="mb-2">
      <div className="uz-muted" style={{ fontSize: 11 }}>{label}</div>
      <div className={mono ? 'uz-mono' : ''} style={{ fontSize: 13, wordBreak: 'break-all' }}>
        {value}
      </div>
    </div>
  );
}
