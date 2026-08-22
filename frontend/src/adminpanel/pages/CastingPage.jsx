import { useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { PageHeader, SearchInput, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';

/**
 * Eski casting moduli (ТЗ §49 — «Existing casting modules»).
 *
 * <h2>⚠️ Eski tizim O'ZGARTIRILMAYDI</h2>
 * Bu sahifa mavjud `/api/v1/casting-user/web` endpointiga murojaat
 * qiladi. Yo'l ham, javob shakli ham ataylab eski: buyurtmachi talabi —
 * casting moduli regressiyaga uchramasin.
 *
 * Shu sababli bu yerda tahrirlash yo'q — faqat ko'rish. Anketalar
 * Telegram bot orqali keladi va ular bilan ishlash eski oqimda qoladi.
 */
export default function CastingPage() {
  const { t } = usePanelI18n();
  const [query, setQuery] = useState('');

  const { data, error, loading, reload } = useApi(
    () => adminApi.castingApplications({ search: query || undefined }),
    [query]
  );

  // Eski endpoint sahifalanmagan ro'yxat ham qaytarishi mumkin.
  const rows = Array.isArray(data) ? data : (data?.items || []);

  return (
    <>
      <PageHeader
        title={t('cs.title')}
        subtitle={t('cs.subtitle')}
        right={<SearchInput value={query} onChange={setQuery} placeholder={t('cs.search')} />}
      />
      <p className="uz-muted mb-4 text-sm">{t('cs.hint')}</p>

      <div className="uz-card overflow-hidden">
        {loading ? <LoadingState /> :
         error ? <ErrorState error={error} onRetry={reload} /> :
         !rows.length ? <EmptyState icon="🎭" /> : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  <th>{t('cs.name')}</th>
                  <th>{t('cs.age')}</th>
                  <th>{t('cs.region')}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((c, i) => (
                  <tr key={c.id ?? i}>
                    <td>{c.fullName || c.name || '—'}</td>
                    <td className="uz-muted">{c.age ?? '—'}</td>
                    <td className="uz-muted">{c.region || c.address || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableWrap>
        )}
      </div>
    </>
  );
}
