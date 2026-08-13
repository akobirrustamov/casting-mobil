import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import ru from './locales/ru.json';
import uz from './locales/uz.json';

/**
 * ТЗ, 1-этап: только UZ и RU. EN добавляется на 2-м этапе вместе с iOS.
 * Узбекский ведём латиницей — так подписаны экраны в мокапах ТЗ.
 */
export const SUPPORTED_LANGUAGES = ['uz', 'ru'] as const;
export type Language = (typeof SUPPORTED_LANGUAGES)[number];

/**
 * Приложение в основном узбекоязычное, поэтому стартуем всегда с UZ —
 * независимо от локали устройства. Русский остаётся доступным,
 * переключение появится в Настройках (экран 24 по ТЗ).
 */
export const DEFAULT_LANGUAGE: Language = 'uz';

i18n.use(initReactI18next).init({
  resources: {
    uz: { translation: uz },
    ru: { translation: ru },
  },
  lng: DEFAULT_LANGUAGE,
  fallbackLng: DEFAULT_LANGUAGE,
  interpolation: { escapeValue: false },
});

export default i18n;
