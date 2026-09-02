import { createContext, useCallback, useContext, useMemo, useState } from 'react';

/**
 * Tomoshabin yuzasining tarjimalari — UZ / RU / EN.
 *
 * <h2>⚠️ Nega yana bitta lug'at</h2>
 * Loyihada allaqachon ikkitasi bor va ular ATAYLAB ajratilgan:
 * `src/i18next.js` — eski casting sayti, `adminpanel/i18n.js` —
 * boshqaruv paneli. Panel faylining o'zida shu qoida yozilgan:
 * «aralashtirish ikkalasini ham chalkashtiradi».
 *
 * Uchinchi yuza ham shu qoidaga bo'ysunadi. Panel lug'atiga
 * qo'shilsa, ikkalasi bitta `localStorage` kalitini bo'lishardi va
 * xodim panelda rus tilini tanlasa, tomoshabin sahifasi ham
 * ruschaga o'tardi — bu esa boshqa odam va boshqa sessiya.
 *
 * Matn hech qachon komponentga qotirilmaydi — faqat `t()` orqali.
 */

export const LOCALES = ['uz', 'ru', 'en'];

/** Kontent tarjimalari backendda katta harfda keladi (UZ/RU/EN). */
export const toBackendLocale = (l) => String(l || 'uz').toUpperCase();

const STORAGE_KEY = 'uzcasting.viewer.locale';

const dict = {
  uz: {
    'signIn.title': 'Kirish',
    'signIn.phone': 'Telefon raqami',
    'signIn.password': 'Parol',
    'signIn.submit': 'Kirish',
    'signIn.loading': 'Kirilmoqda...',
    'signIn.hint': 'Ro\'yxatdan o\'tish mobil ilovada',

    'watch.loading': 'Yuklanmoqda...',
    'watch.notFound': 'Video topilmadi',
    'watch.quality': 'Sifat',
    'watch.qualityAuto': 'Avto',
    'watch.resumed': '{time} dan davom etmoqda',
    'watch.signOut': 'Chiqish',
    'watch.episode': '{number}-qism',

    // Server bergan sabablar — har biri boshqa harakat talab qiladi.
    'denied.SIGN_IN': 'Ko\'rish uchun tizimga kiring',
    'denied.SUBSCRIBE': 'Bu kontent Premium obuna bilan ochiladi',
    'denied.BUY_EPISODE': 'Bu qismni sotib olish kerak',
    'denied.BUY_PREMIERE': 'Bu premyerani sotib olish kerak',
    'denied.BUY_OR_SUBSCRIBE': 'Obuna bo\'ling yoki alohida sotib oling',
    'denied.NONE': 'Bu kontent hozir mavjud emas',
    'denied.buyInApp': 'To\'lov mobil ilovada amalga oshiriladi',

    'error.network': 'Internetni tekshiring',
    'error.credentials': 'Telefon yoki parol noto\'g\'ri',
    'error.playback': 'Videoni ochib bo\'lmadi',
  },

  ru: {
    'signIn.title': 'Вход',
    'signIn.phone': 'Номер телефона',
    'signIn.password': 'Пароль',
    'signIn.submit': 'Войти',
    'signIn.loading': 'Вход...',
    'signIn.hint': 'Регистрация — в мобильном приложении',

    'watch.loading': 'Загрузка...',
    'watch.notFound': 'Видео не найдено',
    'watch.quality': 'Качество',
    'watch.qualityAuto': 'Авто',
    'watch.resumed': 'Продолжаем с {time}',
    'watch.signOut': 'Выйти',
    'watch.episode': 'Часть {number}',

    'denied.SIGN_IN': 'Войдите, чтобы смотреть',
    'denied.SUBSCRIBE': 'Контент доступен по подписке Premium',
    'denied.BUY_EPISODE': 'Эту серию нужно купить',
    'denied.BUY_PREMIERE': 'Эту премьеру нужно купить',
    'denied.BUY_OR_SUBSCRIBE': 'Оформите подписку или купите отдельно',
    'denied.NONE': 'Контент сейчас недоступен',
    'denied.buyInApp': 'Оплата — в мобильном приложении',

    'error.network': 'Проверьте интернет',
    'error.credentials': 'Неверный телефон или пароль',
    'error.playback': 'Не удалось открыть видео',
  },

  en: {
    'signIn.title': 'Sign in',
    'signIn.phone': 'Phone number',
    'signIn.password': 'Password',
    'signIn.submit': 'Sign in',
    'signIn.loading': 'Signing in...',
    'signIn.hint': 'Registration is in the mobile app',

    'watch.loading': 'Loading...',
    'watch.notFound': 'Video not found',
    'watch.quality': 'Quality',
    'watch.qualityAuto': 'Auto',
    'watch.resumed': 'Resuming from {time}',
    'watch.signOut': 'Sign out',
    'watch.episode': 'Part {number}',

    'denied.SIGN_IN': 'Sign in to watch',
    'denied.SUBSCRIBE': 'This content requires a Premium subscription',
    'denied.BUY_EPISODE': 'This episode must be purchased',
    'denied.BUY_PREMIERE': 'This premiere must be purchased',
    'denied.BUY_OR_SUBSCRIBE': 'Subscribe or buy separately',
    'denied.NONE': 'This content is not available right now',
    'denied.buyInApp': 'Payment is handled in the mobile app',

    'error.network': 'Check your connection',
    'error.credentials': 'Wrong phone or password',
    'error.playback': 'Could not open the video',
  },
};

const ViewerI18nContext = createContext(null);

export function ViewerI18nProvider({ children }) {
  const [locale, setLocaleState] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEY) || 'uz';
    } catch {
      // Brauzer saqlashni bloklagan — sukut til bilan ishlaymiz.
      return 'uz';
    }
  });

  const setLocale = useCallback((next) => {
    if (!LOCALES.includes(next)) return;
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // Saqlanmasa til sahifa yangilangunga qadar amal qiladi.
    }
    setLocaleState(next);
  }, []);

  /** t('key', {n: 5}) — kalit topilmasa kalitning o'zini qaytaradi. */
  const t = useCallback(
    (key, vars) => {
      const table = dict[locale] || dict.uz;
      let out = table[key] ?? dict.uz[key] ?? key;
      if (vars) {
        Object.entries(vars).forEach(([k, v]) => {
          out = out.replace(new RegExp(`\\{${k}\\}`, 'g'), String(v));
        });
      }
      return out;
    },
    [locale]
  );

  const value = useMemo(() => ({ locale, setLocale, t }), [locale, setLocale, t]);
  return <ViewerI18nContext.Provider value={value}>{children}</ViewerI18nContext.Provider>;
}

export function useViewerI18n() {
  const ctx = useContext(ViewerI18nContext);
  if (!ctx) throw new Error('useViewerI18n faqat ViewerI18nProvider ichida ishlaydi');
  return ctx;
}
