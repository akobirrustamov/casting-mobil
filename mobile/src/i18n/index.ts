import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { getLocales } from 'expo-localization';

import ru from './locales/ru.json';
import uz from './locales/uz.json';

/**
 * ТЗ, 1-этап: только UZ и RU. EN добавляется на 2-м этапе вместе с iOS.
 * Узбекский ведём латиницей — так подписаны экраны в мокапах ТЗ.
 */
export const SUPPORTED_LANGUAGES = ['uz', 'ru'] as const;
export type Language = (typeof SUPPORTED_LANGUAGES)[number];

export const DEFAULT_LANGUAGE: Language = 'uz';

function detectLanguage(): Language {
  const code = getLocales()[0]?.languageCode;
  return SUPPORTED_LANGUAGES.includes(code as Language)
    ? (code as Language)
    : DEFAULT_LANGUAGE;
}

i18n.use(initReactI18next).init({
  resources: {
    uz: { translation: uz },
    ru: { translation: ru },
  },
  lng: detectLanguage(),
  fallbackLng: DEFAULT_LANGUAGE,
  interpolation: { escapeValue: false },
});

export default i18n;
