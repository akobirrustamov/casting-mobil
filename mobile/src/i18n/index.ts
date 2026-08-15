import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import en from './locales/en.json';
import ru from './locales/ru.json';
import uz from './locales/uz.json';

/**
 * Три языка. Узбекский латиницей — так подписаны экраны в мокапах ТЗ.
 *
 * ТЗ относило английский ко 2-му этапу вместе с iOS, но заказчик попросил
 * добавить его сразу — держим все три с первого релиза.
 */
export const SUPPORTED_LANGUAGES = ['uz', 'ru', 'en'] as const;
export type Language = (typeof SUPPORTED_LANGUAGES)[number];

/** Названия для переключателя языка — каждый на своём языке. */
export const LANGUAGE_LABELS: Record<Language, string> = {
  uz: "O'zbekcha",
  ru: 'Русский',
  en: 'English',
};

/**
 * Приложение в основном узбекоязычное, поэтому стартуем всегда с UZ —
 * независимо от локали устройства. Переключение появится
 * в Настройках (экран 24 по ТЗ).
 */
export const DEFAULT_LANGUAGE: Language = 'uz';

export function isSupportedLanguage(value: string): value is Language {
  return (SUPPORTED_LANGUAGES as readonly string[]).includes(value);
}

i18n.use(initReactI18next).init({
  resources: {
    uz: { translation: uz },
    ru: { translation: ru },
    en: { translation: en },
  },
  lng: DEFAULT_LANGUAGE,
  // Недостающий ключ берём из узбекского: он самый полный по определению
  fallbackLng: DEFAULT_LANGUAGE,
  interpolation: { escapeValue: false },
});

export default i18n;
