import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '../api/client';
import ConfirmDialog, { useConfirm } from '../components/ConfirmDialog';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { PageHeader } from '../components/Ui';
import { usePanelI18n } from '../i18n';

/** Baytlarni o'qiladigan ko'rinishga keltiradi. */
const humanSize = (bytes) => {
  if (!bytes) return '0';
  const kb = bytes / 1024;
  if (kb < 1024) return `${Math.round(kb)} KB`;
  const mb = kb / 1024;
  return mb < 1024 ? `${mb.toFixed(1)} MB` : `${(mb / 1024).toFixed(2)} GB`;
};

/**
 * Ombor holati.
 *
 * <h2>⚠️ IKKI XIL «ISHLATILMAGAN» ALOHIDA KO'RSATILADI</h2>
 *
 * <b>Yetim fayl</b> — omborda bor, bazada yozuvi yo'q. Uni hech narsa
 * ko'rsatmaydi va u hech qachon ochilmaydi. O'chirish xavfsiz.
 *
 * <b>Biriktirilmagan media</b> — bazada bor, lekin hech qaysi
 * kontentga ulanmagan. Kutubxonada ko'rinadi va admin uni ataylab
 * saqlab turgan bo'lishi mumkin.
 *
 * Ularni bitta ro'yxatga qo'shish xavfli bo'lardi: birinchisini
 * o'chirish xavfsiz, ikkinchisi esa adminning ishi bo'lishi mumkin.
 *
 * <h2>⚠️ Sahifa O'ZI skanerlamaydi</h2>
 * Skanerlash S3 ga o'nlab so'rov yuboradi va pul turadi. Sahifa
 * ochilganda faqat OXIRGI natija ko'rsatiladi — yonida uning vaqti
 * bilan. Yangilash adminning ongli qarori.
 */
export default function StoragePage() {
  const { t } = usePanelI18n();

  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [scanning, setScanning] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    adminApi.storage()
      // ⚠️ 204 — hali skanerlanmagan. Bu XATO EMAS: bo'sh hisobot
      // ko'rsatish «ombor bo'sh» degan yolg'on taassurot berardi.
      .then((r) => setReport(r || null))
      .catch((e) => setError(e?.message || t('error.title')))
      .finally(() => setLoading(false));
  }, [t]);

  useEffect(load, [load]);

  // ⚠️ O'chirgandan keyin hisobot QAYTA olinadi: o'chirilgan qator
  // ro'yxatda qolsa admin uni yana o'chirishga urinardi va
  // «topilmadi» xatosini olardi.
  const confirmer = useConfirm(load);

  const scan = () => {
    setScanning(true);
    setError(null);
    adminApi.storageScan()
      .then(setReport)
      .catch((e) => setError(e?.message || t('error.title')))
      .finally(() => setScanning(false));
  };

  if (loading) return <LoadingState />;
  if (error) return <ErrorState message={error} onRetry={load} />;

  return (
    <>
      <PageHeader title={t('storage.title')} subtitle={t('storage.subtitle')} />

      <div className="flex gap-3 items-center mb-5 flex-wrap">
        <button
          type="button"
          className="uz-btn uz-btn-primary"
          onClick={scan}
          disabled={scanning}
        >
          {scanning ? t('storage.scanning') : t('storage.scan')}
        </button>

        {report && (
          <span className="uz-muted" style={{ fontSize: 12 }}>
            {t('storage.scannedAt')}: {new Date(report.scannedAt).toLocaleString()}
            {' · '}{report.scanMillis} ms
          </span>
        )}
      </div>

      <ConfirmDialog {...confirmer.props} />

      {!report && <EmptyState text={t('storage.neverScanned')} />}

      {report && (
        <>
          {/* ⚠️ Skanerlash chegaraga yetgan bo'lsa raqamlar TO'LIQ EMAS.
              Buni aytmaslik hisobotni yolg'onga aylantirardi. */}
          {!report.complete && (
            <div className="uz-alert uz-alert-warn mb-4">
              ⚠ {t('storage.incomplete')}
            </div>
          )}

          <div className="uz-row mb-5">
            <Stat label={t('storage.objects')} value={`${report.objectCount}`} />
            <Stat label={t('storage.total')} value={humanSize(report.totalBytes)} />
            <Stat
              label={t('storage.orphans')}
              value={`${report.orphanCount} · ${humanSize(report.orphanBytes)}`}
              tone={report.orphanCount > 0 ? 'warn' : null}
            />
            <Stat
              label={t('storage.unused')}
              value={`${report.unusedAssetCount} · ${humanSize(report.unusedAssetBytes)}`}
              tone={report.unusedAssetCount > 0 ? 'warn' : null}
            />
          </div>

          <Section title={t('storage.byFolder')}>
            <table className="uz-table">
              <thead>
                <tr>
                  <th>{t('storage.folder')}</th>
                  <th>{t('storage.objects')}</th>
                  <th>{t('storage.size')}</th>
                  <th>{t('storage.orphans')}</th>
                </tr>
              </thead>
              <tbody>
                {report.folders.map((f) => (
                  <tr key={f.name}>
                    <td><code>{f.name}/</code></td>
                    <td style={{ fontVariantNumeric: 'tabular-nums' }}>{f.count}</td>
                    <td style={{ fontVariantNumeric: 'tabular-nums' }}>{humanSize(f.sizeBytes)}</td>
                    <td style={{ fontVariantNumeric: 'tabular-nums' }}>
                      {f.orphanCount > 0
                        ? `${f.orphanCount} · ${humanSize(f.orphanBytes)}`
                        : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Section>

          <Section title={t('storage.orphansTitle')} hint={t('storage.orphansHint')}>
            {report.orphans.length === 0
              ? <EmptyState text={t('storage.noOrphans')} />
              : (
                <>
                  <table className="uz-table">
                    <thead>
                      <tr><th>{t('storage.key')}</th><th>{t('storage.size')}</th><th /></tr>
                    </thead>
                    <tbody>
                      {report.orphans.map((o) => (
                        <tr key={o.key}>
                          <td><code style={{ fontSize: 12 }}>{o.key}</code></td>
                          <td style={{ fontVariantNumeric: 'tabular-nums' }}>
                            {humanSize(o.sizeBytes)}
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <button
                              type="button"
                              className="uz-btn uz-btn-danger"
                              style={{ minHeight: 32, fontSize: 12 }}
                              onClick={() => confirmer.ask({
                                title: t('storage.deleteOrphan'),
                                message: `${o.key} — ${humanSize(o.sizeBytes)}`,
                                // ⚠️ Qaytarib bo'lmasligi ochiq aytiladi.
                                note: t('storage.deleteWarning'),
                                confirmLabel: t('media.delete'),
                                run: () => adminApi.storageDeleteOrphan(o.key),
                              })}
                            >
                              {t('media.delete')}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <Truncated shown={report.orphans.length} total={report.orphanCount}
                             limit={report.listLimit} t={t} />
                </>
              )}
          </Section>

          <Section title={t('storage.unusedTitle')} hint={t('storage.unusedHint')}>
            {report.unusedAssets.length === 0
              ? <EmptyState text={t('storage.noUnused')} />
              : (
                <table className="uz-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>{t('storage.file')}</th>
                      <th>{t('storage.type')}</th>
                      <th>{t('storage.size')}</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {report.unusedAssets.map((u) => (
                      <tr key={u.id}>
                        <td style={{ fontVariantNumeric: 'tabular-nums' }}>{u.id}</td>
                        <td>{u.filename}</td>
                        <td>{u.type}</td>
                        <td style={{ fontVariantNumeric: 'tabular-nums' }}>
                          {humanSize(u.sizeBytes)}
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <button
                            type="button"
                            className="uz-btn uz-btn-danger"
                            style={{ minHeight: 32, fontSize: 12 }}
                            onClick={() => confirmer.ask({
                              title: t('storage.deleteAsset'),
                              message: `${u.filename} — ${humanSize(u.sizeBytes)}`,
                              // ⚠️ Bu yerdagi ogohlantirish BOSHQACHA:
                              // fayl kutubxonada ko'rinadi va ataylab
                              // saqlanayotgan bo'lishi mumkin.
                              note: t('storage.deleteAssetWarning'),
                              confirmLabel: t('media.delete'),
                              run: () => adminApi.deleteMedia(u.id),
                            })}
                          >
                            {t('media.delete')}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
          </Section>
        </>
      )}
    </>
  );
}

function Stat({ label, value, tone }) {
  return (
    <div className="uz-col">
      <div className="uz-card p-4">
        <div className="uz-muted" style={{ fontSize: 12 }}>{label}</div>
        <div
          style={{
            fontSize: 20,
            fontWeight: 600,
            marginTop: 4,
            color: tone === 'warn' ? 'var(--p-warning)' : 'var(--p-text)',
          }}
        >
          {value}
        </div>
      </div>
    </div>
  );
}

function Section({ title, hint, children }) {
  return (
    <div className="uz-card p-4 mb-5">
      <div className="uz-h2 mb-1" style={{ fontSize: 15 }}>{title}</div>
      {hint && <p className="uz-muted mb-4" style={{ fontSize: 12 }}>{hint}</p>}
      {children}
    </div>
  );
}

/**
 * ⚠️ Ro'yxat kesilgan bo'lsa buni AYTISH shart.
 *
 * Aks holda admin 200 qatorni ko'rib «hammasi shu» deb o'ylardi va
 * qolgan minglab fayl e'tibordan chetda qolardi.
 */
function Truncated({ shown, total, limit, t }) {
  if (shown >= total) return null;
  return (
    <p className="uz-muted mt-2" style={{ fontSize: 12 }}>
      {t('storage.truncated')} — {shown} / {total} ({limit})
    </p>
  );
}
