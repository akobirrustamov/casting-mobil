import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '../api/client';
import { EmptyState, ErrorState, LoadingState } from '../components/States';

/** Baytlarni o'qiladigan ko'rinishga keltiradi. */
const humanSize = (bytes) => {
  if (!bytes) return '—';
  const kb = bytes / 1024;
  if (kb < 1024) return `${Math.round(kb)} KB`;
  const mb = kb / 1024;
  return mb < 1024 ? `${mb.toFixed(1)} MB` : `${(mb / 1024).toFixed(2)} GB`;
};

/**
 * Ombor bo'ylab yurish — fayl menejeridagi kabi.
 *
 * <h2>⚠️ Nima uchun kerak</h2>
 * Umumiy raqamlar («192 obyekt, 352 MB») nima borligini aytmaydi.
 * Admin qaysi video qancha joy egallaganini, transkodlash natijasi
 * qayerda turganini ko'ra olmasdi.
 *
 * <h2>⚠️ Kalit nomlari UUID — ular hech narsa aytmaydi</h2>
 * Fayl nomlari server tomonida yasaladi
 * ({@code content/2ac6ed2b-....png}), ya'ni ro'yxatda ular
 * ma'nosiz ko'rinadi.
 *
 * Shuning uchun har qator bazadagi media bilan bog'lanadi va ASL
 * fayl nomi ko'rsatiladi. Bog'lanmagani «yetim» deb belgilanadi.
 *
 * <h2>⚠️ Har daraja ALOHIDA so'raladi</h2>
 * S3 bitta darajani qaytaradi, ichkariga kirmaydi. Butun omborni
 * yuklab, brauzerda daraxt qurish 200 000 obyektda ishlamasdi.
 */
export default function StorageBrowser({ t, onDeleteOrphan }) {
  const [prefix, setPrefix] = useState('');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback((next) => {
    setLoading(true);
    setError(null);
    adminApi.storageBrowse(next)
      .then(setData)
      .catch((e) => setError(e?.message || t('error.title')))
      .finally(() => setLoading(false));
  }, [t]);

  useEffect(() => { load(prefix); }, [prefix, load]);

  /**
   * Yo'l bo'laklari — «ortga» tugmasi o'rniga.
   *
   * ⚠️ Chuqur papkada (`videos/146/hls/480p/`) bitta «ortga» tugmasi
   * to'rt marta bosishni talab qilardi. Yo'l bo'laklari esa istalgan
   * darajaga bir bosishda qaytaradi.
   */
  const parts = prefix ? prefix.replace(/\/$/, '').split('/') : [];

  return (
    <div>
      <div className="flex gap-2 items-center mb-4 flex-wrap" style={{ fontSize: 13 }}>
        <button
          type="button"
          className="uz-btn uz-btn-ghost"
          style={{ minHeight: 30, fontSize: 13 }}
          onClick={() => setPrefix('')}
        >
          {t('storage.root')}
        </button>
        {parts.map((part, i) => (
          <span key={part + i} className="flex items-center gap-2">
            <span className="uz-muted">/</span>
            <button
              type="button"
              className="uz-btn uz-btn-ghost"
              style={{ minHeight: 30, fontSize: 13 }}
              onClick={() => setPrefix(`${parts.slice(0, i + 1).join('/')}/`)}
            >
              {part}
            </button>
          </span>
        ))}
      </div>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} onRetry={() => load(prefix)} />}

      {!loading && !error && data && (
        data.entries.length === 0
          ? <EmptyState text={t('storage.emptyFolder')} />
          : (
            <table className="uz-table">
              <thead>
                <tr>
                  <th>{t('storage.name')}</th>
                  <th>{t('storage.size')}</th>
                  <th>{t('storage.belongsTo')}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.entries.map((e) => (
                  <tr key={e.key}>
                    <td>
                      {e.folder ? (
                        <button
                          type="button"
                          className="uz-link"
                          onClick={() => setPrefix(e.key)}
                        >
                          📁 {e.name}
                        </button>
                      ) : (
                        <span style={{ fontSize: 13 }}>📄 {e.name}</span>
                      )}
                    </td>
                    <td style={{ fontVariantNumeric: 'tabular-nums' }}>
                      {e.folder ? '—' : humanSize(e.sizeBytes)}
                    </td>
                    <td style={{ fontSize: 13 }}>
                      {e.mediaFilename
                        ? <span>{e.mediaFilename}{e.mediaId ? ` (#${e.mediaId})` : ''}</span>
                        : e.orphan
                          ? <span style={{ color: 'var(--p-warning)' }}>{t('storage.orphanTag')}</span>
                          : <span className="uz-muted">—</span>}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      {/* ⚠️ O'chirish FAQAT yetim faylda. Bog'langan
                          faylni bu yerdan o'chirish media yozuvini
                          ochilmaydigan holga keltirardi — buning
                          uchun kutubxona bor. */}
                      {!e.folder && e.orphan && (
                        <button
                          type="button"
                          className="uz-btn uz-btn-danger"
                          style={{ minHeight: 30, fontSize: 12 }}
                          onClick={() => onDeleteOrphan(e.key, e.sizeBytes,
                            () => load(prefix))}
                        >
                          {t('media.delete')}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
      )}
    </div>
  );
}
