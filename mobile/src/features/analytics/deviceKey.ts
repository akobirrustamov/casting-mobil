import * as Crypto from 'expo-crypto';

import { getItem, setItem } from '@/lib/storage';

/**
 * Ключ устройства для аналитики.
 *
 * <h2>Зачем он нужен</h2>
 * Бэкенд считает не только «сколько показов», но и «сколько РАЗНЫХ зрителей»
 * (`AdDailyStatistic.uniqueImpressions`). У гостя нет id пользователя, и без
 * этого ключа все незалогиненные слились бы в одного: уникальные показы
 * рекламы всегда равнялись бы единице.
 *
 * <h2>Что это НЕ</h2>
 * Не идентификатор человека и не рекламный ID устройства. Случайный UUID,
 * созданный самим приложением, живёт только в его хранилище и исчезает
 * вместе с приложением. Ничего из профиля в него не попадает — это прямо
 * записано и в контракте бэкенда («Shaxsni aniqlamaydi»).
 */
const STORAGE_KEY = 'analytics.deviceKey';

/**
 * Одно обещание на весь процесс.
 *
 * Без него два одновременных события на старте прочитали бы пустое
 * хранилище каждый и записали РАЗНЫЕ ключи — второй затёр бы первый, и одно
 * устройство считалось бы двумя.
 */
let pending: Promise<string> | null = null;

async function load(): Promise<string> {
  const stored = await getItem(STORAGE_KEY);
  if (stored) return stored;

  const created = Crypto.randomUUID();
  await setItem(STORAGE_KEY, created);
  return created;
}

export function deviceKey(): Promise<string> {
  pending ??= load();
  return pending;
}
