import { useEffect, useState } from 'react';
import Modal from './Modal';
import { adminApi } from '../api/client';
import { ErrorState } from './States';
import { usePanelI18n } from '../i18n';

/**
 * Ombordagi faylni ko'rish — rasm ko'rinadi, video o'ynaydi.
 *
 * <h2>⚠️ Nima uchun kerak bo'ldi</h2>
 * Yetim fayllar ro'yxatida faqat kalit turadi
 * (`content/2ac6ed2b-….mp4`) — nomlar UUID, chunki ular server
 * tomonida yasaladi. Admin «o'chirish xavfsiz» degan yozuvga
 * ishonishi uchun faylni KO'RISHI kerak. Usiz u yoki ko'r-ko'rona
 * o'chirardi, yoki qo'rqib hech narsa o'chirmasdi.
 *
 * <h2>⚠️ Havola HAR OCHILISHDA qaytadan so'raladi</h2>
 * Imzolangan havola muddati bor (sukut bo'yicha 10 daqiqa). Uni
 * komponent holatida keshlab qoldirsak, oyna ikkinchi marta
 * ochilganda muddati o'tgan havola ishlatilardi va brauzer
 * jimgina 403 olardi: rasm o'rnida siniq belgi, video o'rnida
 * bo'sh pleyer — sababini esa hech narsa aytmasdi.
 *
 * <h2>⚠️ Manzil MUTLAQ holda ishlatiladi</h2>
 * S3 imzolangan havolasi `https://` bilan keladi. Oldiga `BASE_URL`
 * qo'shilsa `http://localhost:8080https://...` chiqardi — shu xato
 * `VideoPreview` da bir marta bo'lgan.
 */
export default function StorageObjectPreview({ objectKey, kind, onClose }) {
  const { t } = usePanelI18n();
  const [state, setState] = useState({ loading: true });

  useEffect(() => {
    if (!objectKey) return undefined;

    let tashlandi = false;
    setState({ loading: true });

    adminApi.storagePreview(objectKey)
      .then((data) => {
        // ⚠️ Oyna yopilgandan keyin holatni o'zgartirmaymiz: React
        // ogohlantirish berardi va yopilgan oyna qayta chizilardi.
        if (!tashlandi) setState({ loading: false, data });
      })
      .catch((error) => {
        if (!tashlandi) {
          setState({
            loading: false,
            // ⚠️ `ErrorState` obyekt kutadi (`error.message`), satr emas.
            // Satr berilsa xabar o'rni BO'SH chiqardi — xato bor, lekin
            // ekranda faqat «⚠️» belgisi turardi.
            error: { message: error?.response?.data?.message || t('storage.previewFailed') },
          });
        }
      });

    return () => { tashlandi = true; };
  }, [objectKey, t]);

  const { loading, data, error } = state;
  const nomi = objectKey ? objectKey.split('/').pop() : '';

  return (
    <Modal open={!!objectKey} title={t('storage.previewTitle')} onClose={onClose} width={900}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <code style={{ fontSize: 12, wordBreak: 'break-all' }}>{objectKey}</code>

        {loading && (
          <div style={{ fontSize: 13, opacity: 0.7 }}>{t('storage.previewLoading')}</div>
        )}
        {error && <ErrorState error={error} />}

        {data && (
          <>
            {/* Tur SERVERDAN keladi — bu yerda kengaytma tahlil
                QILINMAYDI. Ikki joyda alohida ro'yxat bo'lsa, ular
                yangi format qo'shilganda jimgina uzilib qolardi. */}
            {(kind || data.kind) === 'IMAGE' && (
              <img
                src={data.url}
                alt={nomi}
                style={{
                  maxWidth: '100%', maxHeight: '70vh',
                  objectFit: 'contain', borderRadius: 8,
                }}
              />
            )}

            {(kind || data.kind) === 'VIDEO' && (
              // ⚠️ `controls` SHART: usiz pleyer ko'rinadi, lekin
              // uni to'xtatib yoki oldinga surib bo'lmasdi.
              // `preload="metadata"` — butun faylni tortmasin:
              // yetim video bir necha gigabayt bo'lishi mumkin.
              <video
                src={data.url}
                controls
                preload="metadata"
                // ⚠️ Rang tokendan. Komponentga hex yozilsa gammani
                // almashtirganda panel yarmi eski rangda qolardi
                // (ТЗ §50) — `DesignTokensTest` buni ushlaydi.
                style={{
                  width: '100%', maxHeight: '70vh', borderRadius: 8,
                  background: 'var(--p-video-backdrop)',
                }}
              />
            )}

            <div style={{ fontSize: 12, opacity: 0.7 }}>
              {data.contentType}
              {' · '}
              {t('storage.previewExpires')} {muddatDaqiqa(data.expiresAt)} {t('storage.previewMinutes')}
            </div>
          </>
        )}
      </div>
    </Modal>
  );
}

/**
 * Havola yana qancha yashaydi.
 *
 * ⚠️ Manfiy chiqmaydi: muddati o'tgan havola uchun `0` ko'rsatiladi.
 * «-3 daqiqa» yozuvi adminni chalkashtirardi.
 */
function muddatDaqiqa(expiresAt) {
  if (!expiresAt) return 0;
  const qoldi = new Date(expiresAt).getTime() - Date.now();
  return Math.max(0, Math.round(qoldi / 60000));
}
