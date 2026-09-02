import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';

import { fetchWatch, getViewer, signOut } from '../api/client';
import ViewerPlayer from '../components/ViewerPlayer';
import { toBackendLocale, useViewerI18n } from '../i18n';

/**
 * Tomosha sahifasi — `/tomosha/:type/:id`.
 *
 * <h2>⚠️ Huquqni FAQAT server hal qiladi</h2>
 * Sahifa obuna, xarid va bepullikni o'zi hisoblamaydi. U
 * {@code /api/v1/app/watch/**} javobidagi `allowed` va
 * `requiredAction` ni ko'rsatadi, xolos.
 *
 * Aks holda kirish qoidasi ikki joyda yashardi — serverda va shu
 * yerda — va tarif o'zgargan birinchi kuni ular ajralib ketardi.
 * Bundan tashqari klientdagi tekshiruv chetlab o'tiladi; fayl
 * endpointi ham alohida tekshiradi, ya'ni himoya ikki qavatli.
 *
 * <h2>⚠️ Rad javobida manba BERILMAYDI</h2>
 * Server `sources` ni bo'sh qaytaradi. Shuning uchun bu yerda
 * «ko'rsatmaslik» degan shart yozish shart emas: ko'rsatadigan narsa
 * yo'q.
 *
 * <h2>To'lov bu yerda yo'q</h2>
 * Pullik kontentda sabab tushuntiriladi, lekin sotib olish tugmasi
 * yo'q: to'lov oqimi mobil ilovada. Yarim ishlaydigan tugma
 * qo'yishdan ko'ra, qayerda to'lash mumkinligini aytish to'g'riroq.
 */
export default function WatchPage() {
  const { type, id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  /**
   * ⚠️ Kirishdan keyin odam SHU videoga qaytadi.
   *
   * Usiz u ildizga (`/`) tushardi — u yerda esa butunlay boshqa
   * mahsulot, eski casting sayti. Odam kirish tugmasini video uchun
   * bosgan, natijada esa anketalar ro'yxatini ko'rardi va videoni
   * qaytadan qidirishga majbur bo'lardi.
   */
  const signInPath = `/kirish?next=${encodeURIComponent(location.pathname)}`;
  const { t, locale, setLocale } = useViewerI18n();

  const [state, setState] = useState({ status: 'loading', data: null });
  const viewer = getViewer();

  useEffect(() => {
    let cancelled = false;
    setState({ status: 'loading', data: null });

    fetchWatch(type, id, toBackendLocale(locale))
      .then((data) => {
        if (!cancelled) setState({ status: 'ready', data });
      })
      .catch((error) => {
        if (cancelled) return;
        // 404 — bunday kontent yo'q. Qolgani tarmoq yoki server.
        const status = error?.response?.status;
        setState({ status: status === 404 ? 'notFound' : 'error', data: null });
      });

    return () => {
      cancelled = true;
    };
  }, [type, id, locale]);

  if (state.status === 'loading') {
    return <Shell>{t('watch.loading')}</Shell>;
  }
  if (state.status === 'notFound') {
    return <Shell>{t('watch.notFound')}</Shell>;
  }
  if (state.status === 'error') {
    return <Shell>{t('error.network')}</Shell>;
  }

  const watch = state.data;

  return (
    <div className="uz-viewer">
      <header className="uz-viewer-header">
        <select
          value={locale}
          onChange={(e) => setLocale(e.target.value)}
          aria-label="til"
          className="uz-select"
        >
          <option value="uz">O&apos;zbekcha</option>
          <option value="ru">Русский</option>
          <option value="en">English</option>
        </select>

        {viewer && (
          <button
            type="button"
            className="uz-btn uz-btn-ghost"
            onClick={() => {
              signOut();
              navigate(signInPath);
            }}
          >
            {t('watch.signOut')}
          </button>
        )}
      </header>

      {watch.allowed ? (
        <Allowed watch={watch} type={type} id={id} t={t} />
      ) : (
        <Denied watch={watch} t={t} onSignIn={() => navigate(signInPath)} />
      )}
    </div>
  );
}

/**
 * Ruxsat bor — pleyer.
 *
 * ⚠️ Manbalar bir nechta bo'lishi mumkin: Reels formatida qism
 * bo'laklarga bo'linadi (`partNumber`). Har bo'lak — alohida pleyer
 * va alohida pozitsiya emas: davom ettirish qism darajasida,
 * shuning uchun progress faqat BIRINCHI bo'lakka ulanadi.
 */
function Allowed({ watch, type, id, t }) {
  const sources = watch.sources ?? [];

  if (sources.length === 0) {
    return <Shell>{t('watch.notFound')}</Shell>;
  }

  const heading = watch.episodeNumber
    ? `${watch.title ?? ''} — ${t('watch.episode', { number: watch.episodeNumber })}`
    : watch.title;

  return (
    <>
      {sources.map((source, index) => (
        <ViewerPlayer
          key={source.mediaId ?? index}
          type={type}
          // ⚠️ Pozitsiya faqat birinchi bo'lakda saqlanadi: qolganlari
          // ayni qismning davomi va ular uchun alohida yozuv
          // «qayerda to'xtadi» degan savolni ikkiga bo'lardi.
          targetId={index === 0 ? id : null}
          source={source}
          title={index === 0 ? heading : null}
          // Kadr shakli — tik yoki yotma. Videodan o'lchab bo'lmaydi:
          // `videoWidth` metama'lumot yuklangach paydo bo'ladi va
          // ramka avval keng chizilib, keyin sakrab o'zgarardi.
          orientation={watch.orientation}
        />
      ))}
    </>
  );
}

/** Ruxsat yo'q — sabab va nima qilish kerakligi. */
function Denied({ watch, t, onSignIn }) {
  const action = watch.requiredAction ?? 'NONE';

  return (
    <Shell>
      <p className="uz-viewer-denied">{t(`denied.${action}`)}</p>

      {action === 'SIGN_IN' ? (
        <button type="button" className="uz-btn uz-btn-primary" onClick={onSignIn}>
          {t('signIn.submit')}
        </button>
      ) : (
        action !== 'NONE' && <p className="uz-muted">{t('denied.buyInApp')}</p>
      )}
    </Shell>
  );
}

function Shell({ children }) {
  return (
    <div className="uz-viewer">
      <div className="uz-viewer-state">{children}</div>
    </div>
  );
}
