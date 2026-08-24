import { useState } from 'react';
import { adminApi, mediaUrl } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import ConfirmDialog, { useConfirm } from '../components/ConfirmDialog';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, Pagination, SearchInput, TableWrap } from '../components/Ui';
import { LOCALES, toBackendLocale, usePanelI18n } from '../i18n';
import TaxonomyForm from './TaxonomyForm';

/**
 * Kategoriya va janrlar bitta sahifa komponentida - tuzilishi bir xil.
 * Uchala tarjima ham ustunlarda ko'rinadi: tarjima yetishmasa darhol sezilsin.
 */
export default function TaxonomyPage({ kind }) {
  const { t } = usePanelI18n();
  const { can } = useAuth();
  const isCategory = kind === 'category';
  const [formOpen, setFormOpen] = useState(false);
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState(null);
  const [deleteError, setDeleteError] = useState(null);
  const canCreate = can(isCategory ? 'CATEGORY_CREATE' : 'GENRE_CREATE');
  const canEdit = can(isCategory ? 'CATEGORY_EDIT' : 'GENRE_EDIT');
  const canDelete = can(isCategory ? 'CATEGORY_DELETE' : 'GENRE_DELETE');

  // ⚠️ Bu HAQIQIY o'chirish, arxivlash emas (§16, §17) — kategoriya va
  // janrda `deleted_at` yo'q. Shuning uchun oddiy amaldan qattiqroq
  // tasdiq va o'chirilmagan holatga qaytarib bo'lmasligi haqidagi izoh.
  const confirmer = useConfirm(() => reload());

  // Qidiruv va sahifalash BACKENDDA: ro'yxat cheklanmagan va butun
  // jadvalni tortib olib xotirada filtrlash platforma o'sgani sari
  // sekinlashardi (ТЗ §51).
  const params = { q: q || undefined, page, size: 20 };
  const { data, error, loading, reload } = useApi(
    () => (isCategory ? adminApi.categories(params) : adminApi.genres(params)),
    [kind, q, page]
  );

  // Qidiruv o'zgarganda birinchi sahifaga qaytamiz: aks holda
  // «3-sahifa» da turib qidirsa, natija bo'sh ko'rinardi.
  const onSearch = (value) => { setQ(value); setPage(0); };

  return (
    <>
      <PageHeader
        title={isCategory ? t('categories.title') : t('genres.title')}
        subtitle={isCategory ? t('categories.subtitle') : t('genres.subtitle')}
        right={(
          <div className="flex items-center gap-3 flex-wrap">
            <SearchInput value={q} onChange={onSearch}
                         placeholder={t('common.search')} />
            {canCreate && (
            <button type="button" className="uz-btn uz-btn-primary"
                    onClick={() => { setEditing(null); setFormOpen(true); }}>
              + {t('common.create')}
            </button>
            )}
          </div>
        )}
      />

      {deleteError && (
        <div role="alert" className="mb-4 px-4 py-3"
             style={{ borderRadius: 'var(--p-radius)', background: 'var(--danger-soft)',
                      border: '1px solid var(--danger-border)', color: 'var(--p-danger)', fontSize: 13 }}>
          {deleteError.message}
        </div>
      )}

      <div className="uz-card overflow-hidden">
        {loading ? (
          <LoadingState />
        ) : error ? (
          <ErrorState error={error} onRetry={reload} />
        ) : !data?.items?.length ? (
          <EmptyState icon="▤" />
        ) : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  {isCategory && <th style={{ width: 60 }} />}
                  <th>{t('common.slug')}</th>
                  {LOCALES.map((l) => <th key={l}>{l.toUpperCase()}</th>)}
                  <th>{t('common.sortOrder')}</th>
                  <th>{t('common.active')}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.items.map((row) => (
                  <tr key={row.id}>
                    {isCategory && (
                      <td>
                        {row.iconMediaId ? (
                          <img
                            src={mediaUrl(row.iconMediaId)} alt="" loading="lazy"
                            style={{ width: 36, height: 36, borderRadius: 8, objectFit: 'cover' }}
                          />
                        ) : (
                          <div className="uz-skeleton" style={{ width: 36, height: 36 }} />
                        )}
                      </td>
                    )}
                    <td className="uz-muted" style={{ fontSize: 13 }}>{row.slug}</td>
                    {LOCALES.map((l) => {
                      const value = row.translations?.[toBackendLocale(l)]?.title;
                      return (
                        <td key={l} style={{ fontWeight: value ? 550 : 400 }}>
                          {value || <span style={{ color: 'var(--p-danger)' }}>—</span>}
                        </td>
                      );
                    })}
                    <td className="uz-mono uz-muted">{row.sortOrder}</td>
                    <td>
                      <Badge tone={row.active ? 'published' : 'draft'}>
                        {row.active ? t('common.active') : t('common.inactive')}
                      </Badge>
                    </td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                      {canEdit && (
                        <button type="button" className="uz-btn uz-btn-ghost"
                                style={{ minHeight: 34, padding: '0 12px', fontSize: 13 }}
                                onClick={() => { setEditing(row); setFormOpen(true); }}>
                          {t('common.edit')}
                        </button>
                      )}
                      {canDelete && (
                        <button type="button" className="uz-btn uz-btn-danger"
                                style={{ minHeight: 34, padding: '0 12px', fontSize: 13, marginLeft: 8 }}
                                onClick={() => {
                                  setDeleteError(null);
                                  const name = row.translations?.UZ?.title || row.slug;
                                  confirmer.ask({
                                    title: isCategory ? t('tax.deleteCategory') : t('tax.deleteGenre'),
                                    message: isCategory
                                      ? t('tax.confirmDeleteCategory', { name })
                                      : t('tax.confirmDeleteGenre', { name }),
                                    note: t('tax.deleteNote'),
                                    confirmLabel: t('common.remove'),
                                    // ⚠️ Xato ushlanadi va sahifada ko'rsatiladi, SO'NG
                                    // qayta tashlanadi: `useConfirm` xatoda oynani ochiq
                                    // qoldiradi (`setState(null)` faqat muvaffaqiyatda
                                    // chaqiriladi), ya'ni admin sababni ko'rib, keyin
                                    // «Bekor qilish» bosadi. Backend 409 xabarida
                                    // ANIQ nechta kontentda ishlatilgani yozilgan.
                                    run: async () => {
                                      try {
                                        await (isCategory
                                          ? adminApi.deleteCategory(row.id)
                                          : adminApi.deleteGenre(row.id));
                                      } catch (err) {
                                        setDeleteError(err);
                                        throw err;
                                      }
                                    },
                                  });
                                }}>
                          {t('common.remove')}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableWrap>
        )}

        {data?.items?.length > 0 && (
          <Pagination page={page} totalPages={data.totalPages} onPage={setPage} />
        )}
      </div>

      <TaxonomyForm
        open={formOpen}
        kind={kind}
        row={editing}
        onClose={() => setFormOpen(false)}
        onSaved={reload}
      />

      <ConfirmDialog {...confirmer.props} />
    </>
  );
}
