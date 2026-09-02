import { useEffect, useState } from 'react';
import { adminApi, mediaUrl } from '../api/client';
import { usePanelI18n } from '../i18n';
import MediaPicker from './MediaPicker';
import MediaSpec from './MediaSpec';

/**
 * Bitta rasm maydoni: oldindan ko'rish + tanlash/almashtirish/olib tashlash.
 *
 * `spec` - `mediaSpecs.js` dagi kalit. Berilsa, maydon tagida tavsiya
 * etilgan o'lcham chiqadi va u fayl tanlash oynasiga ham uzatiladi.
 *
 * ⚠️ Nega ikkala joyda. Maydon yonidagi yozuv adminni tayyorlaydi, lekin
 * u faylni brauzer oynasida tanlaydi - ya'ni panel ko'rinmay qoladi.
 * Oynadagi takror yozuv aynan tanlash ONIDA ko'z oldida turadi.
 */
export default function MediaField({ label, value, onChange, hint, spec, type = 'IMAGE' }) {
  const { t } = usePanelI18n();
  const [pickerOpen, setPickerOpen] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);

  /**
   * Tanlangan video pleyerda ochiladimi.
   *
   * ⚠️ Nega alohida so'rov. Maydonga faqat `mediaId` uziladi — fayl
   * nomi ham, formati ham bu yerda yo'q. Ogohlantirishsiz admin
   * `.mkv` ni epizodga biriktirib qo'yardi va nosozlik faqat
   * foydalanuvchi qora ekran ko'rganda, ancha keyin bilinardi.
   */
  const [notPlayable, setNotPlayable] = useState(false);

  /**
   * Video qayta ishlash holati.
   *
   * ⚠️ Bu `notPlayable` dan BOSHQA muammo:
   *
   *   notPlayable  → format noto'g'ri (.mkv), qayta ishlash YORDAM BERMAYDI
   *   transcoding  → format to'g'ri, lekin HLS hali TAYYOR EMAS
   *
   * Ikkalasini bitta ogohlantirishga qo'shish adminni chalkashtirardi:
   * birinchisida boshqa fayl kerak, ikkinchisida shunchaki kutish.
   */
  const [transcoding, setTranscoding] = useState(null);

  useEffect(() => {
    if (!value || type !== 'VIDEO') {
      setNotPlayable(false);
      setTranscoding(null);
      return undefined;
    }
    let alive = true;
    adminApi.mediaAsset(value)
      .then((m) => {
        if (!alive) return;
        setNotPlayable(m.playable === false);
        setTranscoding(m.transcoding ?? null);
      })
      // Ogohlantirishni chizolmaslik maydonni ishdan chiqarmasin.
      .catch(() => {
        if (!alive) return;
        setNotPlayable(false);
        setTranscoding(null);
      });
    return () => { alive = false; };
  }, [value, type]);

  // Qayta ishlash tugamagan — video hali ochilmaydi.
  const pending = transcoding
    && transcoding.status !== 'READY'
    && transcoding.status !== 'FAILED';

  const failed = transcoding && transcoding.status === 'FAILED';

  return (
    <div>
      <label className="uz-label">{label}</label>
      {/* ⚠️ VIDEO uchun `<img>` chizilmaydi. Ilgari chizilardi va qism
          muharriridagi har bir video qismi SINGAN rasm belgisini
          ko'rsatardi — admin uchun bu «video yuklanmadi» degan
          taassurot berardi, aslida fayl joyida edi. */}
      {value && type !== 'VIDEO' ? (
        <img className="uz-thumb" src={mediaUrl(value)} alt="" loading="lazy" />
      ) : (
        <div
          className="uz-thumb flex items-center justify-center"
          style={{ color: value ? 'var(--p-text)' : 'var(--p-disabled)', fontSize: 24 }}
          aria-hidden="true"
        >
          {type === 'VIDEO' ? '🎞' : '🖼'}
        </div>
      )}
      <div className="flex gap-2 mt-2 flex-wrap">
        <button
          type="button"
          className="uz-btn uz-btn-ghost"
          style={{ minHeight: 36, fontSize: 13 }}
          onClick={() => setPickerOpen(true)}
        >
          {value ? t('media.change') : t('media.upload')}
        </button>
        {/* ⚠️ Faqat VIDEO uchun. Rasm allaqachon eskizda ko'rinadi —
            unga tugma qo'yish ortiqcha bosqich bo'lardi. */}
        {value && type === 'VIDEO' && (
          <button
            type="button"
            className="uz-btn uz-btn-ghost"
            style={{ minHeight: 36, fontSize: 13 }}
            onClick={() => setPreviewOpen(true)}
          >
            {t('media.previewOpen')}
          </button>
        )}
        {value && (
          <button
            type="button"
            className="uz-btn uz-btn-ghost"
            style={{ minHeight: 36, fontSize: 13 }}
            onClick={() => onChange(null)}
          >
            {t('media.clear')}
          </button>
        )}
      </div>
      {notPlayable && (
        <p className="uz-field-warn mt-2" role="status">
          ⚠ {t('media.notPlayableHint')}
        </p>
      )}

      {/* ⚠️ Format to'g'ri, lekin HLS hali tayyor emas — shunchaki
          kutish kerak. `notPlayable` dan farqli: u yerda boshqa fayl
          kerak. */}
      {pending && !notPlayable && (
        <p className="uz-field-warn mt-2" role="status">
          ⏳ {t('tc.pendingHint')}
        </p>
      )}

      {/* Qayta ishlash yiqilgan — video HECH QACHON ochilmaydi.
          Admin buni bilishi va kutubxonada qayta urinishi kerak. */}
      {failed && (
        <p className="uz-field-warn mt-2" role="status">
          ⚠ {t('tc.FAILED')}
          {transcoding.error ? ` — ${transcoding.error}` : ''}
        </p>
      )}
      {/* Tavsiya etilgan o'lcham — MAYDONNING O'ZIDA.
          ⚠️ Ilgari u faqat fayl tanlash oynasida bor edi, ya'ni admin
          uni «Yuklash» tugmasini bosgandan KEYIN ko'rardi. Rasmni esa
          u bosishdan oldin, boshqa dasturda tayyorlaydi — o'lcham
          kechikkanda kerak bo'lgan fayl allaqachon noto'g'ri edi.
          `GalleryField` da bu yozuv bor edi, bitta rasm maydonida
          yo'q: aynan shuning uchun farq ko'zga tashlanmasdi. */}
      {spec && <MediaSpec name={spec} />}

      {hint && <p className="uz-muted mt-1" style={{ fontSize: 11 }}>{hint}</p>}

      <MediaPicker
        open={pickerOpen}
        type={type}
        spec={spec}
        onClose={() => setPickerOpen(false)}
        onSelect={onChange}
      />
    </div>
  );
}
