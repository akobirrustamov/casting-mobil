import { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import Modal from './Modal';
import { adminApi, BASE_URL } from '../api/client';

/**
 * Manzilni to'liq holga keltiradi.
 *
 * ⚠️ MUTLAQ manzilga tegilmaydi. S3 imzolangan havolasi
 * `https://...` bilan keladi va oldiga `BASE_URL` qo'shilsa
 * `http://localhost:8080https://...` chiqardi — video umuman
 * ochilmasdi.
 *
 * Nisbiy manzil esa to'ldiriladi: server o'z tashqi domenini
 * ishonchli bilmaydi (proksi ortida `Host` almashishi mumkin),
 * klient esa biladi.
 */
const absolute = (url) =>
  (!url || url.startsWith('http') ? url : `${BASE_URL}${url}`);

/**
 * Videoni panelda ko'rish — sifat internetga qarab tanlanadi.
 *
 * <h2>⚠️ Nima uchun kerak bo'ldi</h2>
 * Xodim video yuklardi, lekin uni HECH QACHON ko'ra olmasdi — panelda
 * pleyer umuman yo'q edi. Videoning buzuq emasligini tekshirishning
 * yagona yo'li kontentni NASHR QILIB, ilovadan ochish edi.
 *
 * <h2>⚠️ Nega HLS, asl fayl EMAS</h2>
 * Asl fayl — BITTA sifat. 4K manbada u 600 MB va sekin internetda
 * umuman ochilmasdi.
 *
 * HLS esa uch variantni e'lon qiladi (1080 / 720 / 480) va pleyer
 * O'LCHANGAN tezlikka qarab tanlaydi. Tezlik o'zgarsa — o'ynash
 * paytida ham almashadi.
 *
 * <h2>⚠️ `hls.js` NEGA KERAK</h2>
 * HLS'ni Safari o'zi o'ynatadi, Chrome va Firefox esa YO'Q.
 * Kutubxonasiz bu xususiyat brauzerlarning yarmida ishlamasdi.
 *
 * Safari'da kutubxona ISHLATILMAYDI: o'rnatilgan qo'llab-quvvatlash
 * tezroq va batareyani kamroq yeydi.
 *
 * <h2>⚠️ HLS bo'lmasa — asl faylga qaytadi</h2>
 * Transcoding tugamagan yoki yiqilgan bo'lishi mumkin. O'shanda
 * pleyer umuman ochilmasligi noto'g'ri bo'lardi: admin aynan «nima
 * yuklandi» degan savolga javob izlayotgan bo'ladi.
 *
 * <h2>⚠️ Manzil har safar QAYTA so'raladi</h2>
 * U imzolangan va muddati cheklangan. Bir marta olib saqlab qo'yilsa
 * bir necha soatdan keyin «video ochilmadi» bo'lardi va sababi
 * ko'rinmasdi.
 *
 * ⚠️ Havola faqat modal OCHILGANDA so'raladi — aks holda qism
 * muharriridagi har bir video maydoni sahifa ochilishida ortiqcha
 * so'rov yuborardi.
 */
export default function VideoPreview({ open, mediaId, title, onClose, t }) {
  const videoRef = useRef(null);

  /**
   * Ishlab turgan `hls.js` — qo'lda sifat tanlash uchun.
   *
   * ⚠️ `state` emas, `ref`: uni o'zgartirish qayta chizishga sabab
   * bo'lmasligi kerak, aks holda pleyer har tanlovda qaytadan
   * ulanardi va video boshidan boshlanardi.
   */
  const hlsRef = useRef(null);
  const [source, setSource] = useState(null);
  const [error, setError] = useState(null);

  /** Pleyer hozir qaysi sifatni ko'rsatyapti — adminga ko'rinadi. */
  const [quality, setQuality] = useState(null);

  /** Mavjud variantlar — `[{ index, label }]`. */
  const [levels, setLevels] = useState([]);

  /**
   * Tanlangan variant. `-1` — avtomatik.
   *
   * ⚠️ Avtomatik SUKUT bo'lib qoladi: admin aynan shuni so'radi.
   * Qo'lda tanlash esa kerak bo'lganda — masalan «sekin internetda
   * qanday ko'rinadi» degan savolga javob izlaganda.
   */
  const [selected, setSelected] = useState(-1);

  useEffect(() => {
    if (!open || !mediaId) {
      setSource(null);
      setError(null);
      setQuality(null);
      setLevels([]);
      setSelected(-1);
      return undefined;
    }

    let alive = true;
    setSource(null);
    setError(null);
    setQuality(null);

    adminApi.mediaPreview(mediaId)
      .then((r) => { if (alive) setSource(r); })
      .catch((e) => { if (alive) setError(e?.message || t('error.title')); });

    return () => { alive = false; };
  }, [open, mediaId, t]);

  /** HLS ni ulash — manba kelgandan keyin. */
  useEffect(() => {
    const video = videoRef.current;
    if (!video || !source?.hlsUrl) {
      return undefined;
    }

    // ⚠️ Nisbiy manzil to'ldiriladi: server o'z tashqi domenini
    // ishonchli bilmaydi (proksi ortida `Host` almashishi mumkin),
    // klient esa o'z `BASE_URL` ini biladi.
    const url = absolute(source.hlsUrl);

    // ⚠️ TARTIB MUHIM: avval `hls.js`, keyin o'rnatilgan pleyer.
    //
    // Chrome `canPlayType('application/vnd.apple.mpegurl')` uchun
    // «maybe» qaytaradi — bu ROSTGA teng, lekin Chrome HLS'ni
    // aslida O'YNATA OLMAYDI.
    //
    // Ilgari shu tekshiruv birinchi turardi va Chrome o'sha yo'lga
    // kirardi: `src` m3u8 ga qo'yilardi, pleyer esa abadiy qotardi —
    // `readyState: 0`, spinner aylanadi, XATO YO'Q. Sababini
    // brauzerdan tashqarida ko'rib bo'lmasdi.
    if (Hls.isSupported()) {
      // pastda ulanadi
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      // Safari — o'rnatilgan qo'llab-quvvatlash, kutubxonasiz.
      video.src = url;
      return undefined;
    } else {
      // ⚠️ Juda eski brauzer. Asl faylga qaytamiz — sifat bitta,
      // lekin video baribir ochiladi.
      video.src = absolute(source.url);
      return undefined;
    }

    const hls = new Hls({
      // ⚠️ Boshlang'ich sifat AVTOMATIK: `-1` = «o'zing hisobla».
      // Qat'iy 1080p qo'yilsa sekin internetda birinchi soniyalar
      // uzilib-uzilib ketardi.
      startLevel: -1,
    });

    hls.on(Hls.Events.LEVEL_SWITCHED, (_, data) => {
      const level = hls.levels[data.level];
      if (!level) return;

      // ⚠️ Sifat QISQA TOMON bo'yicha nomlanadi.
      //
      // `height` ni olish yotma videoda to'g'ri ishlaydi, TIK videoda
      // esa yolg'on chiqadi: 1080x1920 uchun «1920p» deb ko'rsatardi
      // va bunday sifat umuman yo'q.
      //
      // Transcoding profillari ham aynan qisqa tomon bilan nomlangan
      // (1080p, 720p, 480p) — ya'ni bu shunchaki ko'rinish emas,
      // moslik masalasi.
      const short = Math.min(level.width || 0, level.height || 0);
      setQuality(`${short || level.height}p`);
    });

    hls.on(Hls.Events.ERROR, (_, data) => {
      // ⚠️ Faqat TUZATIB BO'LMAYDIGAN xato ko'rsatiladi. Tarmoqdagi
      // qisqa uzilishlarni `hls.js` o'zi qayta uradi va ularni
      // ekranga chiqarish admin uchun shovqin bo'lardi.
      if (data.fatal) {
        setError(`${t('media.previewUnsupported')} (${data.type})`);
      }
    });

    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      // ⚠️ Faqat HAQIQATAN mavjud variantlar ko'rsatiladi.
      //
      // Manba 720p bo'lsa 1080p umuman yasalmaydi va uni ro'yxatda
      // ko'rsatish adminni aldardi: bosardi, hech narsa o'zgarmasdi.
      setLevels(hls.levels.map((level, index) => ({
        index,
        label: `${Math.min(level.width || 0, level.height || 0) || level.height}p`,
      })));
    });

    hls.loadSource(url);
    hls.attachMedia(video);
    hlsRef.current = hls;

    return () => {
      hlsRef.current = null;
      hls.destroy();
    };
  }, [source, t]);

  return (
    <Modal open={open} title={title || t('media.preview')} onClose={onClose}>
      {error && <div className="uz-alert uz-alert-danger">{error}</div>}

      {!source && !error && (
        <div className="uz-muted" style={{ padding: 24, textAlign: 'center' }}>
          {t('common.loading')}
        </div>
      )}

      {source && (
        <>
          <video
            ref={videoRef}
            controls
            // ⚠️ Butun fayl oldindan tortilmaydi. HLS'da bu baribir
            // shunday, lekin asl faylga qaytilganda muhim: 600 MB
            // modal ochilishi bilan yuklanardi.
            preload="metadata"
            // HLS bo'lmasa manba shu yerdan beriladi.
            src={source.hlsUrl ? undefined : absolute(source.url)}
            style={{
              width: '100%',
              maxHeight: '70vh',
              background: 'var(--p-video-backdrop)',
            }}
          >
            {t('media.previewUnsupported')}
          </video>

          {/* ⚠️ Tanlov FAQAT HLS bo'lganda. Asl faylda bitta sifat
              bor va tugmalarni ko'rsatish yolg'on tanlov berardi. */}
          {source.hlsUrl && levels.length > 0 && (
            <div className="flex gap-2 mt-3 flex-wrap items-center">
              <span className="uz-muted" style={{ fontSize: 12 }}>
                {t('media.quality')}:
              </span>

              <QualityButton
                active={selected === -1}
                onClick={() => {
                  setSelected(-1);
                  // ⚠️ `-1` — `hls.js` da «o'zing tanla» degani.
                  if (hlsRef.current) hlsRef.current.currentLevel = -1;
                }}
                label={t('media.qualityAuto')}
              />

              {/* Yuqori sifat birinchi — odam odatda shuni qidiradi. */}
              {[...levels].reverse().map((level) => (
                <QualityButton
                  key={level.index}
                  active={selected === level.index}
                  onClick={() => {
                    setSelected(level.index);
                    if (hlsRef.current) hlsRef.current.currentLevel = level.index;
                  }}
                  label={level.label}
                />
              ))}
            </div>
          )}

          <p className="uz-muted mt-2" style={{ fontSize: 12 }}>
            {source.hlsUrl
              ? (selected === -1
                // Avtomatikda HOZIR qaysi sifat ketayotgani aytiladi.
                ? <>{t('media.adaptive')}{quality ? ` — ${quality}` : ''}</>
                : t('media.qualityFixed'))
              // ⚠️ Sabab AYTILADI: admin nega sifat tanlanmayotganini
              // bilsin va transcoding tugashini kutsin.
              : t('media.originalOnly')}
          </p>
        </>
      )}
    </Modal>
  );
}

/**
 * Sifat tugmasi.
 *
 * ⚠️ Tanlangani KO'RINIB turishi kerak: admin qaysi sifatni
 * majburlaganini eslab qolmasligi mumkin va «nega buzuq» degan
 * savol o'sha yerdan tug'ilardi.
 */
function QualityButton({ active, onClick, label }) {
  return (
    <button
      type="button"
      className={active ? 'uz-btn uz-btn-primary' : 'uz-btn uz-btn-ghost'}
      style={{ minHeight: 28, fontSize: 12, padding: '0 10px' }}
      onClick={onClick}
      aria-pressed={active}
    >
      {label}
    </button>
  );
}
