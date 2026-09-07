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
    'signIn.submit': 'Kod olish',
    'signIn.loading': 'Yuborilmoqda...',
    'signIn.hint': 'Raqamingizga SMS orqali kod yuboramiz',

    'signIn.codeTitle': 'Kodni kiriting',
    'signIn.code': 'SMS kod',
    'signIn.codeSent': '{phone} raqamiga kod yuborildi',
    'signIn.codeSubmit': 'Tasdiqlash',
    'signIn.codeLoading': 'Tekshirilmoqda...',
    'signIn.resend': 'Kodni qayta yuborish',
    'signIn.resendIn': 'Qayta yuborish — {seconds} s',
    'signIn.back': 'Raqamni o\'zgartirish',

    'signIn.nameTitle': 'Ismingiz',
    'signIn.name': 'Ism',
    'signIn.nameSubmit': 'Davom etish',
    'signIn.nameHint': 'Birinchi marta kiryapsiz — ismingizni yozing',

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
    'error.credentials': 'Kod noto\'g\'ri yoki muddati o\'tgan',
    'error.phone': 'Telefon raqami noto\'g\'ri',
    'error.tooMany': 'Juda ko\'p urinish. Bir daqiqadan keyin urinib ko\'ring',
    'error.playback': 'Videoni ochib bo\'lmadi',
  },

  ru: {
    'signIn.title': 'Вход',
    'signIn.phone': 'Номер телефона',
    'signIn.submit': 'Получить код',
    'signIn.loading': 'Отправляем...',
    'signIn.hint': 'Отправим код по SMS на ваш номер',

    'signIn.codeTitle': 'Введите код',
    'signIn.code': 'Код из SMS',
    'signIn.codeSent': 'Код отправлен на {phone}',
    'signIn.codeSubmit': 'Подтвердить',
    'signIn.codeLoading': 'Проверяем...',
    'signIn.resend': 'Отправить код ещё раз',
    'signIn.resendIn': 'Отправить снова — {seconds} с',
    'signIn.back': 'Изменить номер',

    'signIn.nameTitle': 'Ваше имя',
    'signIn.name': 'Имя',
    'signIn.nameSubmit': 'Продолжить',
    'signIn.nameHint': 'Вы входите впервые — укажите имя',

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
    'error.credentials': 'Код неверный или истёк',
    'error.phone': 'Неверный номер телефона',
    'error.tooMany': 'Слишком много попыток. Попробуйте через минуту',
    'error.playback': 'Не удалось открыть видео',
  },

  en: {
    'signIn.title': 'Sign in',
    'signIn.phone': 'Phone number',
    'signIn.submit': 'Get a code',
    'signIn.loading': 'Sending...',
    'signIn.hint': 'We will text you a code',

    'signIn.codeTitle': 'Enter the code',
    'signIn.code': 'Code from SMS',
    'signIn.codeSent': 'Code sent to {phone}',
    'signIn.codeSubmit': 'Confirm',
    'signIn.codeLoading': 'Checking...',
    'signIn.resend': 'Send the code again',
    'signIn.resendIn': 'Send again in {seconds}s',
    'signIn.back': 'Change the number',

    'signIn.nameTitle': 'Your name',
    'signIn.name': 'Name',
    'signIn.nameSubmit': 'Continue',
    'signIn.nameHint': 'First time here — tell us your name',

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
    'error.credentials': 'The code is wrong or expired',
    'error.phone': 'That phone number is not valid',
    'error.tooMany': 'Too many attempts. Try again in a minute',
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
