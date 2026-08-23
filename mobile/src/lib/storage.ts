import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

/**
 * Небольшое key-value хранилище поверх SecureStore.
 *
 * В браузере SecureStore недоступен — нативного модуля там нет. Веб нам нужен
 * только как площадка для просмотра вёрстки, но без запасного пути превью
 * ломалось: флаг «онбординг показан» некуда записать, а вход через Google
 * падал на `setValueWithKeyAsync is not a function` уже после ответа Google.
 * Поэтому на web уходим в localStorage.
 *
 * Через это же хранилище идёт и токен сессии. На телефоне он по-прежнему
 * в Keystore/Keychain — ветка с localStorage там не выполняется. Ослабление
 * касается только браузерного превью, которое в магазины не публикуется.
 */
const isWeb = Platform.OS === 'web';

export async function getItem(key: string): Promise<string | null> {
  try {
    if (isWeb) {
      return globalThis.localStorage?.getItem(key) ?? null;
    }
    return await SecureStore.getItemAsync(key);
  } catch {
    return null;
  }
}

export async function setItem(key: string, value: string): Promise<void> {
  try {
    if (isWeb) {
      globalThis.localStorage?.setItem(key, value);
      return;
    }
    await SecureStore.setItemAsync(key, value);
  } catch {
    // Хранилище недоступно — состояние просто не переживёт перезапуск.
  }
}

export async function removeItem(key: string): Promise<void> {
  try {
    if (isWeb) {
      globalThis.localStorage?.removeItem(key);
      return;
    }
    await SecureStore.deleteItemAsync(key);
  } catch {
    // ignore
  }
}
