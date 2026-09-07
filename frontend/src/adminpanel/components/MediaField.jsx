import { useEffect, useState } from 'react';
import { adminApi, mediaUrl, BASE_URL } from '../api/client';
import { usePanelI18n } from '../i18n';
import MediaPicker from './MediaPicker';
import MediaSpec from './MediaSpec';
import VideoPreview from './VideoPreview';

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
/**
 * Video eskizining manbasi.
 *
 * ⚠️ MUTLAQ manzilga tegilmaydi: S3 imzolangan havolasi `https://`
 * bilan keladi va oldiga `BASE_URL` qo'shilsa
 * `http://localhost:8080https://...` chiqardi.
 *
 * ⚠️ `#t=0.1` — SHART. Usiz Chrome faqat metama'lumotni oladi va
 * KADRNI CHIZMAYDI: quti qop-qora bo'lib turadi, xato esa yo'q.
 * Vaqt belgisi berilsa pleyer o'sha soniyaga o'tadi va kadr
 * chiziladi. `0` emas, `0.1`: ba'zi kodeklarda birinchi kadr
 * qora bo'ladi.
 *
 * Fragment (`#`) serverga UMUMAN yuborilmaydi — imzo ham, chipta ham
 * buzilmaydi.
 */
const posterSrc = (url) => {
  if (!url) return null;
  return `${url.startsWith('http') ? url : `${BASE_URL}${url}`}#t=0.1`;
};

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

  /**
   * Eskiz manzili — videoning birinchi kadri shundan chiziladi.
   *
   * ⚠️ ALOHIDA so'rov QILINMAYDI: manzil `mediaAsset` javobining
   * o'zida keladi (`MediaDto.previewUrl`). Alohida so'rov bo'lsa
   * qism muharriridagi har bir video maydoni sahifa ochilishida
   * ikkinchi murojaat yuborardi.
   */
  const [poster, setPoster] = useState(null);

  /**
   * Kadrni chizib bo'lmadi — brauzer faylni ochmadi.
   *
   * ⚠️ `notPlayable` dan boshqa narsa: u fayl NOMIGA qarab
   * oldindan aytadi, bu esa brauzer HAQIQATAN yiqilganda yonadi
   * (masalan chipta muddati o'tgan). Belgiga qaytamiz — bo'sh
   * qora quti «video yo'q» degan yolg'on taassurot berardi.
   */
  const [posterFailed, setPosterFailed] = useState(false);

  useEffect(() => {
    if (!value || type !== 'VIDEO') {
      setNotPlayable(false);
      setTranscoding(null);
      setPoster(null);
      setPosterFailed(false);
      return undefined;
    }
    let alive = true;
    setPosterFailed(false);
    adminApi.mediaAsset(value)
      .then((m) => {
        if (!alive) return;
        setNotPlayable(m.playable === false);
        setTranscoding(m.transcoding ?? null);
        setPoster(m.previewUrl ?? null);
      })
      // Ogohlantirishni chizolmaslik maydonni ishdan chiqarmasin.
      .catch(() => {
        if (!alive) return;
        setNotPlayable(false);
        setTranscoding(null);
        setPoster(null);
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
          taassurot berardi, aslida fayl joyida edi.

          ⚠️ Endi o'rniga VIDEONING O'ZI qo'yiladi — `<video>` birinchi
          kadrni chizadi va maydon nima biriktirilganini KO'RSATADI.
          Ilgari bu yerda faqat 🎞 belgisi turardi: video yuklangani
          bilan quti bo'm-bo'sh qolardi va admin «yuklanmadi» deb
          o'ylardi. */}
      {value && type !== 'VIDEO' ? (
        <img className="uz-thumb" src={mediaUrl(value)} alt="" loading="lazy" />
      ) : value && type === 'VIDEO' && poster && !posterFailed ? (
        <video
          className="uz-thumb"
          src={posterSrc(poster)}
          // ⚠️ Butun fayl tortilmaydi — faqat kadr uchun kerakli bo'lak.
          preload="metadata"
          // Eskiz — bosilganda «Ko'rish» oynasi ochiladi, shu yerda
          // o'ynatilmaydi: boshqaruv tugmalari kichkina qutida
          // eskizni butunlay yopib qo'yardi.
          muted
          playsInline
          // Kadrni chizib bo'lmasa — belgiga qaytamiz.
          onError={() => setPosterFailed(true)}
          // `uz-thumb` o'lcham va `object-fit` ni allaqachon beradi.
          style={{ cursor: 'pointer' }}
          onClick={() => setPreviewOpen(true)}
        />
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

      {/* ⚠️ BU BLOK IKKI MARTA YO'QOLGAN — commitlar ustidan yozgan.
          «Ko'rish» tugmasi va `previewOpen` holati qolib, pleyerning
          o'zi yo'qolardi: tugma bosiladi, hech narsa ochilmaydi va
          xato ham chiqmaydi.

          `mediaFieldPreview.test.jsx` shuni qo'riqlaydi — ikkinchi
          yo'qolishni ham o'sha ushladi. */}
      <VideoPreview
        open={previewOpen}
        mediaId={value}
        title={label}
        onClose={() => setPreviewOpen(false)}
        t={t}
      />
    </div>
  );
}
