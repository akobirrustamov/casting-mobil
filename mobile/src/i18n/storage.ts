import { getItem, setItem } from '@/lib/storage';

import i18n, { DEFAULT_LANGUAGE, isSupportedLanguage, type Language } from './index';

/** Выбранный язык переживает перезапуск. */
const KEY = 'uzcasting.language';

export async function loadLanguage(): Promise<Language> {
  const saved = await getItem(KEY);
  return saved && isSupportedLanguage(saved) ? saved : DEFAULT_LANGUAGE;
}

/**
 * Язык → сервер.
 *
 * <h2>⚠️ Зачем это понадобилось</h2>
 * Выбор языка хранился ТОЛЬКО на телефоне, а `cms_user_account.language`
 * не менялся никогда. Push-уведомление берёт язык именно оттуда — то
 * есть человек, выбравший русский, получил бы узбекский текст. Заметить
 * это можно было бы лишь после подключения FCM, когда искать причину уже
 * поздно.
 *
 * <h2>Почему обработчик ставится извне</h2>
 * Направление зависимостей: `features` знает про `i18n`, но не наоборот.
 * Так же сюда попадает токен в `lib/api`. Пока обработчик не поставлен,
 * выбор просто не уезжает на сервер — и это рабочее состояние, а не
 * поломка (у гостя языка на сервере нет).
 */
type LanguageSync = (language: Language) => void;

let sync: LanguageSync | null = null;

export function setLanguageSync(fn: LanguageSync | null): void {
  sync = fn;
}

/** Меняет язык в интерфейсе, запоминает выбор и сообщает его серверу. */
export async function setLanguage(language: Language): Promise<void> {
  await i18n.changeLanguage(language);
  await setItem(KEY, language);

  /**
   * ⚠️ Без `await` и без броска: выбор языка не должен зависеть от сети.
   * Человек переключил язык — интерфейс меняется сразу, а сервер
   * догоняет. Если запрос не дошёл, потеряется только язык push-
   * уведомлений, и он поправится при следующем переключении.
   */
  sync?.(language);
}
