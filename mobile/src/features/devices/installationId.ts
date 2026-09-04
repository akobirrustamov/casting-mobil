import Constants from 'expo-constants';
import * as Crypto from 'expo-crypto';
import { Platform } from 'react-native';

import { getItem, setItem } from '@/lib/storage';

/**
 * Идентификатор УСТАНОВКИ приложения — то, что бэкенд считает «устройством».
 *
 * <h2>Зачем</h2>
 * Заказчик: с одного аккаунта не больше двух устройств. Считать их бэкенд
 * умеет (`cms_user_device`, настройка `account.device.limit`), но ему нужен
 * стабильный ключ, по которому телефон узнаётся между запусками.
 *
 * <h2>⚠️ Почему это НЕ `analytics/deviceKey`</h2>
 * Тот ключ существует ровно для одной вещи — считать уникальные показы
 * рекламы у НЕзалогиненных, и в его описании прямо записано: «не
 * идентификатор человека». Привязать его к аккаунту значило бы нарушить это
 * обещание и превратить анонимный счётчик в связку «человек ↔ устройство».
 *
 * Ключи разные и живут отдельно. Стоимость — один лишний UUID в хранилище.
 *
 * <h2>Что это не даёт</h2>
 * Переустановка приложения создаёт НОВЫЙ ключ: аппаратного идентификатора,
 * который переживает удаление, ни iOS, ни Android приложению не дают.
 * То есть человек, переустановив приложение, займёт второй слот, а старая
 * запись останется висеть. Поэтому список устройств и кнопка «выйти»
 * обязательны — без них он окажется заперт снаружи собственного аккаунта.
 */
const STORAGE_KEY = 'device.installationId';

/**
 * Одно обещание на весь процесс.
 *
 * Без него два параллельных вызова на старте прочитали бы пустое хранилище
 * каждый и записали РАЗНЫЕ ключи. Второй затёр бы первый — и одно устройство
 * заняло бы два слота из двух.
 */
let pending: Promise<string> | null = null;

async function load(): Promise<string> {
  const stored = await getItem(STORAGE_KEY);
  if (stored) return stored;

  const created = Crypto.randomUUID();
  await setItem(STORAGE_KEY, created);
  return created;
}

export function installationId(): Promise<string> {
  pending ??= load();
  return pending;
}

/**
 * Имя, по которому человек узнает телефон в списке.
 *
 * `Constants.deviceName` на телефоне отдаёт то, что владелец сам написал в
 * настройках («iPhone Али»). В браузере и на эмуляторе его нет — тогда
 * остаётся платформа, и это честнее выдуманной модели устройства.
 */
export function deviceLabel(): string {
  const named = Constants.deviceName?.trim();
  if (named) return named;

  return Platform.select({
    ios: 'iPhone',
    android: 'Android',
    web: 'Brauzer',
    default: 'Qurilma',
  }) as string;
}

/** `ios` / `android` / `web` — бэкенд хранит это как есть. */
export function devicePlatform(): string {
  return Platform.OS;
}
