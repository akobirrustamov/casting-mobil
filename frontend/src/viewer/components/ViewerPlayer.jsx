import Hls from 'hls.js';
import { useCallback, useEffect, useRef, useState } from 'react';

import { BASE_URL, fetchProgress, saveProgress } from '../api/client';
import { useViewerI18n } from '../i18n';

/**
 * Tomoshabin pleyeri: HLS, sifat tanlash, davom ettirish.
 *
 * <h2>Sifat almashtirish — `nextLevel`</h2>
 * `hls.js` da ikki yo'l bor va ikkalasi ham `currentTime` ni
 * saqlaydi. Farq buferda:
 *
 * <pre>
 *   currentLevel → buferni TOZALAYDI → 1-3 soniya to'xtash
 *   nextLevel    → keyingi segment chegarasida → uzilishsiz
 * </pre>
 *
 * Panelda aynan `currentLevel` «video qaytadan yuklanyapti» degan
 * shikoyatni keltirib chiqargan edi.
 *
 * <h2>⚠️ Serverga pozitsiya — kamdan-kam</h2>
 * `timeupdate` sekundiga bir necha marta keladi. Har biri so'rov
 * bo'lsa, bitta tomoshabin daqiqasiga yuzlab so'rov yuborardi.
 */

/** Serverga pozitsiya shu qadar soniyada bir marta yuboriladi. */
const SAVE_EVERY_SECONDS = 15;

/** Shundan oldin to'xtagan bo'lsa davom ettirilmaydi. */
const MIN_RESUME_SECONDS = 15;

/** Oxiriga shu ulushdan yaqin bo'lsa boshidan boshlanadi. */
const RESUME_MAX_RATIO = 0.95;

/** Nisbiy manzilga backend manzilini qo'shadi; mutlaqni tegmay qo'yadi. */
function absolute(url) {
  if (!url) return null;
  return url.startsWith('/') ? `${BASE_URL}${url}` : url;
}

/** 5565 → «1:32:45». */
export function formatTime(seconds) {
  const s = Math.max(0, Math.floor(seconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const mm = h > 0 ? String(m).padStart(2, '0') : String(m);
  return `${h > 0 ? `${h}:` : ''}${mm}:${String(sec).padStart(2, '0')}`;
}

/**
 * Saqlangan pozitsiyadan davom etish kerakmi.
 *
 * ⚠️ `null` qaytaradi, nol emas: nol ham pozitsiya, va chaqiruvchi
 * «boshidan boshlash» ni «tegmaslik» dan ajrata olmasdi.
 */
export function resumePosition(progress) {
  if (!progress || typeof progress.position !== 'number') return null;
  if (progress.completed === true) return null;
  if (progress.position < MIN_RESUME_SECONDS) return null;

  const duration = progress.duration;
  if (duration && progress.position >= duration * RESUME_MAX_RATIO) return null;

  return progress.position;
}

/** Sifat yorlig'i — QISQA tomon bo'yicha (tik videoda «1920p» yolg'on bo'lardi). */
export function qualityLabel(level) {
  const short = Math.min(level.width || 0, level.height || 0);
  return `${short || level.height}p`;
}

export default function ViewerPlayer({ type, targetId, source, title, orientation }) {
  const { t } = useViewerI18n();
  const videoRef = useRef(null);
  const hlsRef = useRef(null);

  const [levels, setLevels] = useState([]);
  const [selected, setSelected] = useState(-1);
  const [error, setError] = useState(null);
  const [resumedAt, setResumedAt] = useState(null);

  /**
   * ⚠️ Ref, holat emas: qiymat sahifa yopilganda o'qiladi, oddiy
   * o'zgaruvchi esa o'sha paytda birinchi renderdagi nolni berardi —
   * odam yarmini ko'rib, boshiga qaytarilardi.
   */
  const latest = useRef({ position: 0, duration: null });
  const lastSaved = useRef(0);

  const quality = selected === -1 ? 'auto' : (levels[selected]?.label ?? 'auto');
  const qualityRef = useRef(quality);
  qualityRef.current = quality;

  /** Pozitsiyani yuboradi. Xato YUTILADI — tomosha undan muhimroq. */
  const persist = useCallback(
    (position, duration) => {
      if (!targetId || position <= 0) return;
      saveProgress(type, targetId, position, duration, qualityRef.current).catch(() => {
        // Tarmoq yo'q yoki mehmon — video davom etaveradi.
      });
    },
    [type, targetId]
  );

  // ------------------------------------------------------------ HLS ulash
  useEffect(() => {
    const video = videoRef.current;
    if (!video || !source) return undefined;

    setError(null);
    const url = absolute(source.hlsUrl);

    // ⚠️ Tartib MUHIM: `Hls.isSupported()` birinchi.
    //
    // Chrome `canPlayType('application/vnd.apple.mpegurl')` uchun
    // «maybe» qaytaradi, lekin HLS ni o'ynata olmaydi — pleyer
    // abadiy osilib qolardi, xatosiz.
    if (!url) {
      // Transkodlash tugamagan — asl fayl. Sifat bitta.
      video.src = absolute(source.url);
      return undefined;
    }
    if (!Hls.isSupported()) {
      if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = url; // Safari — o'rnatilgan qo'llab-quvvatlash
      } else {
        video.src = absolute(source.url);
      }
      return undefined;
    }

    const hls = new Hls({ startLevel: -1 });

    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      // ⚠️ Faqat HAQIQATAN mavjud variantlar. Manba 720p bo'lsa
      // 1080p yasalmaydi va uni ro'yxatda ko'rsatish odamni aldardi.
      setLevels(hls.levels.map((level, index) => ({ index, label: qualityLabel(level) })));
    });

    hls.on(Hls.Events.ERROR, (_, data) => {
      // Qisqa uzilishlarni `hls.js` o'zi qayta uradi.
      if (data.fatal) setError(t('error.playback'));
    });

    hls.loadSource(url);
    hls.attachMedia(video);
    hlsRef.current = hls;

    return () => {
      hlsRef.current = null;
      hls.destroy();
    };
  }, [source, t]);

  // -------------------------------------------------------- davom ettirish
  useEffect(() => {
    const video = videoRef.current;
    if (!video || !targetId) return undefined;

    let cancelled = false;
    let onReady = null;
    lastSaved.current = 0;

    /**
     * ⚠️ Metama'lumot yuklanmaguncha SURIB BO'LMAYDI.
     *
     * Bu poyga: pozitsiya so'rovi ham, HLS manifesti ham bir vaqtda
     * ketadi. So'rov birinchi qaytsa, `readyState` hali 0 bo'ladi va
     * o'sha lahzada qo'yilgan `currentTime` JIMGINA yo'qoladi — video
     * boshidan boshlanadi.
     *
     * Xatolik ORALIQ: sekin tarmoqda manifest oldin keladi va
     * davom ettirish ishlaydi, tez tarmoqda esa yo'q. Aynan shuning
     * uchun uni qo'lda sinab topish qiyin — bir marta ishlaydi, bir
     * marta yo'q.
     *
     * `readyState >= HAVE_METADATA` bo'lsa darhol suramiz, aks holda
     * `loadedmetadata` ni kutamiz.
     */
    const seekTo = (position) => {
      if (video.readyState >= 1) {
        video.currentTime = position;
        return;
      }
      onReady = () => {
        if (!cancelled) video.currentTime = position;
      };
      video.addEventListener('loadedmetadata', onReady, { once: true });
    };

    fetchProgress(type, targetId)
      .then((progress) => {
        // Sahifa yopilgan bo'lishi mumkin — o'lik elementga tegmaymiz.
        if (cancelled) return;
        const position = resumePosition(progress);
        if (position === null) return;

        seekTo(position);
        latest.current = { ...latest.current, position };
        setResumedAt(position);
      })
      .catch(() => {
        // Mehmon, tarmoq yo'q yoki eski backend — boshidan ko'radi.
      });

    return () => {
      cancelled = true;
      if (onReady) video.removeEventListener('loadedmetadata', onReady);
    };
  }, [type, targetId]);

  // -------------------------------------------------------------- saqlash
  useEffect(() => {
    const video = videoRef.current;
    if (!video || !targetId) return undefined;

    const onTime = () => {
      const duration = Number.isFinite(video.duration) && video.duration > 0
        ? video.duration
        : null;
      latest.current = { position: video.currentTime, duration };

      // ⚠️ MODUL bo'yicha: odam orqaga surgan bo'lishi mumkin va
      // farq manfiy chiqadi. Modulsiz saqlash u eski joyga
      // yetguncha to'xtab qolardi.
      if (Math.abs(video.currentTime - lastSaved.current) >= SAVE_EVERY_SECONDS) {
        lastSaved.current = video.currentTime;
        persist(video.currentTime, duration);
      }
    };

    // ⚠️ Pauza — sahifadan ketishning eng ehtimolli lahzasi.
    // Keyingi tikni kutib bo'lmaydi: odam bir soniyada yopishi mumkin.
    const onPause = () => {
      const { position, duration } = latest.current;
      lastSaved.current = position;
      persist(position, duration);
    };

    video.addEventListener('timeupdate', onTime);
    video.addEventListener('pause', onPause);

    // Yopish yoki boshqa sahifaga o'tish — oxirgi imkoniyat.
    window.addEventListener('beforeunload', onPause);

    return () => {
      video.removeEventListener('timeupdate', onTime);
      video.removeEventListener('pause', onPause);
      window.removeEventListener('beforeunload', onPause);

      const { position, duration } = latest.current;
      persist(position, duration);
    };
  }, [targetId, persist]);

  /**
   * Sifatni almashtiradi — videoni to'xtatmasdan.
   *
   * `nextLevel`, `currentLevel` emas: ikkalasi ham pozitsiyani
   * saqlaydi, lekin `currentLevel` buferni tozalab, ko'zga
   * tashlanadigan to'xtash beradi.
   */
  const switchQuality = (index) => {
    setSelected(index);
    if (hlsRef.current) hlsRef.current.nextLevel = index;
  };

  return (
    <div className="uz-viewer-player">
      {error && <div className="uz-alert uz-alert-danger">{error}</div>}

      {title && <h1 className="uz-viewer-title">{title}</h1>}

      {/* ⚠️ Kadr shakli SERVERDAN keladi, videodan emas.
          `videoWidth` faqat metama'lumot yuklangach ma'lum bo'ladi —
          ya'ni ramka avval keng chiziladi, keyin sakrab o'zgarardi.
          `orientation` esa birinchi renderda ma'lum.

          Tik videoni keng ramkaga solish uni yon tomonlarda katta
          qora maydonli tasmaga aylantiradi: bu platformada Reels
          formatidagi kontent bor va u alohida shaklga muhtoj. */}
      <video
        ref={videoRef}
        controls
        playsInline
        className={
          orientation === 'VERTICAL'
            ? 'uz-viewer-video uz-viewer-video-vertical'
            : 'uz-viewer-video'
        }
        // ⚠️ `crossOrigin` YO'Q va bu ataylab: segmentlar imzolangan
        // havola bilan to'g'ridan-to'g'ri omborga ketadi, va S3 bir
        // vaqtda ikki xil avtorizatsiyani qabul qilmaydi.
      >
        <track kind="captions" />
      </video>

      {resumedAt !== null && (
        <p className="uz-viewer-resumed">
          {t('watch.resumed', { time: formatTime(resumedAt) })}
        </p>
      )}

      {/* ⚠️ Tanlov faqat HLS bo'lganda: asl faylda sifat bitta va
          tugmalarni ko'rsatish yolg'on tanlov berardi. */}
      {source?.hlsUrl && levels.length > 0 && (
        <div className="uz-viewer-quality">
          <span className="uz-muted">{t('watch.quality')}:</span>

          <button
            type="button"
            className={selected === -1 ? 'uz-chip uz-chip-active' : 'uz-chip'}
            onClick={() => switchQuality(-1)}
          >
            {t('watch.qualityAuto')}
          </button>

          {/* Yuqori sifat birinchi — odam odatda shuni qidiradi. */}
          {[...levels].reverse().map((level) => (
            <button
              key={level.index}
              type="button"
              className={selected === level.index ? 'uz-chip uz-chip-active' : 'uz-chip'}
              onClick={() => switchQuality(level.index)}
            >
              {level.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
