import { getItem, setItem } from '@/lib/storage';

import i18n, { DEFAULT_LANGUAGE, isSupportedLanguage, type Language } from './index';

/** Выбранный язык переживает перезапуск. */
const KEY = 'uzcasting.language';

export async function loadLanguage(): Promise<Language> {
  const saved = await getItem(KEY);
  return saved && isSupportedLanguage(saved) ? saved : DEFAULT_LANGUAGE;
}

/** Меняет язык в интерфейсе и запоминает выбор. */
export async function setLanguage(language: Language): Promise<void> {
  await i18n.changeLanguage(language);
  await setItem(KEY, language);
}
