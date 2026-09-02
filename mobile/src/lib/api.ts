import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';

/**
 * Тот же бэкенд, что у сайта (frontend/src/config/index.js).
 * Прод: https://uzcasting.site
 */
export const BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? 'https://uzcasting.site';

/**
 * Режим только для чтения.
 *
 * Приложение сейчас смотрит в БОЕВУЮ базу сайта. Пока идёт разработка,
 * любая запись туда запрещена: интерцептор ниже роняет запрос до отправки,
 * если метод не GET/HEAD. Это защита от случайного POST при отладке.
 *
 * Снимать флаг только осознанно, когда появится тестовый контур.
 */
export const READ_ONLY = process.env.EXPO_PUBLIC_READ_ONLY !== 'false';

const SAFE_METHODS = ['get', 'head', 'options'];

/**
 * Исключения из read-only.
 *
 * Список открывается по одному адресу и только там, где запрет
 * ломает саму функцию. Всё остальное — заявки, покупки, донаты,
 * комментарии — под запретом, пока не появится тестовый контур:
 * это чужие деньги и чужой контент в боевой базе.
 *
 * ⚠️ Добавлять сюда — осознанное решение, а не способ «чтобы
 * заработало». Каждая строка означает запись в живую базу сайта из
 * отладочной сборки.
 */
const WRITE_ALLOWLIST = [
  '/api/v1/auth/',
  // ⚠️ OTP переехал в новое пространство (`AppAuthController`).
  //
  // Без этой строки вход по SMS ломался бы ТИХО: интерцептор
  // `READ_ONLY` роняет запрос ДО отправки, и пользователь видел бы
  // не ответ сервера, а внутреннюю ошибку клиента.
  '/api/v1/app/auth/',

  /*
   * Избранное.
   *
   * ⚠️ Почему исключение оправдано: это собственные предпочтения
   * вошедшего человека. Ни денег, ни чужих данных, ни контента —
   * худшее, что может случиться от отладочной сборки, это лишний
   * «лайк» в списке тестового аккаунта.
   *
   * Без этой строки функция мертва целиком: список остаётся на
   * телефоне ровно как до починки, и проверить синхронизацию нельзя
   * вообще никак.
   */
  '/api/v1/app/favorites',

  /*
   * «Продолжить просмотр» — позиция, на которой человек остановился.
   *
   * ⚠️ Почему исключение оправдано: та же категория, что избранное.
   * Собственные данные вошедшего человека, ни денег, ни чужого.
   * Худшее от отладочной сборки — лишняя строка «продолжить» у
   * тестового аккаунта.
   *
   * ⚠️ Без этой строки функция ломается ТИХО и наполовину: локальная
   * позиция сохраняется и видео продолжается на этом телефоне, а на
   * сервер не уходит ничего. То есть на другом устройстве всё
   * выглядит так, будто человек и не смотрел — и понять, почему,
   * по поведению приложения невозможно.
   */
  '/api/v1/app/watch-progress',
];

/**
 * Попадает ли адрес под разрешение.
 *
 * <h2>⚠️ Почему не простой `startsWith`</h2>
 * Запись, оканчивающаяся на `/`, — это ПРОСТРАНСТВО адресов
 * (`/app/auth/` открывает всё внутри). Запись без слэша — КОНКРЕТНЫЙ
 * адрес, и он не должен открывать соседей: с обычным `startsWith`
 * разрешение на `/app/favorites` заодно открыло бы, например,
 * `/app/favorites-import`, которого никто открывать не собирался.
 *
 * Тест поймал это на выдуманном адресе — раньше, чем такой адрес
 * успел появиться.
 */
function allows(entry: string, url: string): boolean {
  if (entry.endsWith('/')) {
    return url.startsWith(entry);
  }
  return url === entry || url.startsWith(`${entry}/`) || url.startsWith(`${entry}?`);
}

export const api = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
});

/**
 * Токен для заголовка `Authorization`.
 *
 * Живёт здесь, а не в сторе, по направлению зависимостей: `lib` не знает про
 * `features`. Значение сюда проталкивает `features/auth/store` — одной
 * подпиской, чтобы вход, выход и восстановление из хранилища не требовали
 * каждый своего вызова (пропущенный вызов дал бы молчаливый 401).
 */
let authToken: string | null = null;

export function setAuthToken(token: string | null): void {
  authToken = token;
}

/**
 * Обновление истёкшего токена.
 *
 * Возвращает новый access-токен либо `null`, если сессию продлить
 * нельзя. Саму политику (какой эндпоинт, что сохранить, когда
 * разлогинить) знает `features/auth` — сюда она передаёт готовую
 * функцию, чтобы `lib` по-прежнему не зависел от `features`.
 */
type TokenRefresher = () => Promise<string | null>;

let refresher: TokenRefresher | null = null;

export function setTokenRefresher(fn: TokenRefresher | null): void {
  refresher = fn;
}

/**
 * Запросы авторизации, которые НЕ надо повторять после обновления.
 *
 * ⚠️ Без этого списка 401 на `/otp/verify` (человек ошибся кодом)
 * запускал бы обновление токена, а сам эндпоинт обновления при
 * отказе дёргал бы себя же — бесконечный цикл на экране входа.
 */
const NO_REFRESH_PATHS = ['/api/v1/app/auth/', '/api/v1/auth/'];

/**
 * Обновление в ОДНОМ экземпляре.
 *
 * ⚠️ Это не оптимизация, а обязательное условие. На бэкенде включена
 * ротация: каждый refresh-токен срабатывает ровно один раз, а
 * повторное использование уже погашенного токена считается кражей и
 * закрывает ВСЕ сессии пользователя.
 *
 * Экран открывает несколько запросов сразу. Если у каждого будет
 * своё обновление, первый пройдёт, остальные придут со старым
 * токеном — и бэкенд, совершенно правильно, разлогинит человека
 * везде. То есть починка выхода из аккаунта сама бы его и вызывала.
 */
let inFlight: Promise<string | null> | null = null;

function refreshOnce(): Promise<string | null> {
  if (!inFlight) {
    const run = refresher ? refresher() : Promise.resolve(null);
    inFlight = run.finally(() => {
      inFlight = null;
    });
  }
  return inFlight;
}

/** Помечаем повтор, чтобы он не ушёл на второй круг. */
type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

api.interceptors.response.use(undefined, async (error: unknown) => {
  if (!axios.isAxiosError(error) || error.response?.status !== 401) {
    throw error;
  }

  const config = error.config as RetriableConfig | undefined;
  const url = config?.url ?? '';

  // ⚠️ Повторяем ровно один раз. Если и обновлённый токен получил
  // 401 — дело не в сроке жизни, и второй заход дал бы бесконечный
  // цикл запросов вместо честной ошибки на экране.
  if (!config || config._retried || NO_REFRESH_PATHS.some((p) => url.startsWith(p))) {
    throw error;
  }

  config._retried = true;

  const token = await refreshOnce();
  if (!token) {
    throw error;
  }

  // ⚠️ Новый токен кладём ЗДЕСЬ, а не полагаемся на то, что его
  // проставит кто-то снаружи.
  //
  // Повтор проходит через тот же request-интерцептор, и заголовок
  // ему ставится из `authToken`. Если оставить это на совести
  // обновляющей функции, любой её вариант, не трогающий стор,
  // отправлял бы повтор со СТАРЫМ токеном — тот же 401, но уже
  // без второй попытки. Молча и без единой ошибки в коде.
  setAuthToken(token);
  return api.request(config);
});

api.interceptors.request.use((config) => {
  const method = (config.method ?? 'get').toLowerCase();

  const url = config.url ?? '';
  const isAllowed = WRITE_ALLOWLIST.some((entry) => allows(entry, url));

  if (READ_ONLY && !SAFE_METHODS.includes(method) && !isAllowed) {
    throw new Error(
      `[READ_ONLY] Запрос ${method.toUpperCase()} ${url} заблокирован. ` +
        'Приложение подключено к боевой базе сайта, запись запрещена.'
    );
  }

  if (authToken) {
    config.headers.set('Authorization', `Bearer ${authToken}`);
  }

  return config;
});

/**
 * Заголовки для плеера и любых запросов мимо axios.
 *
 * Картинки бэкенд отдаёт всем, а видео проверяет право доступа
 * (`AccessService.canReadMedia`) и без токена вернёт отказ — поэтому
 * `expo-video` обязан идти с тем же заголовком, что и остальные запросы.
 */
export function authHeaders(): Record<string, string> {
  return authToken ? { Authorization: `Bearer ${authToken}` } : {};
}

/** Файлы отдаются по id вложения. */
export function fileUrl(id: string): string {
  return `${BASE_URL}/api/v1/file/getFile/${id}`;
}

/**
 * Метка запуска — только для локального бэкенда.
 *
 * <h2>Зачем</h2>
 * `expo-image` кэширует картинку по URL, и это правильно: в проде id медиа
 * постоянен, а его содержимое неизменно — новая загрузка получает новый id.
 *
 * На локальном стенде наоборот: база H2 живёт в памяти и пересоздаётся при
 * каждом рестарте, раздавая те же самые id ДРУГИМ файлам. Адрес
 * `/media/90/raw` не меняется, содержимое меняется — и телефон продолжает
 * показывать вчерашнюю заглушку, сколько ни перезапускай бэкенд.
 *
 * Поэтому в дев-режиме к адресу добавляется метка запуска приложения.
 * В проде она пустая и на кэш никак не влияет.
 */
const MEDIA_CACHE_BUST = READ_ONLY ? '' : `?v=${Date.now()}`;

/**
 * Медиа новой платформы — другой namespace, чем `fileUrl`.
 *
 * `fileUrl` — вложения старого кастингового модуля (`Attachment`, UUID),
 * здесь — `MediaAsset` нового бэкенда (числовой id). Это разные хранилища,
 * перепутать нельзя: id из фида в `fileUrl` даст 404.
 *
 * Картинки отдаются без токена, видео проверяет entitlement и на отказ
 * возвращает 404 — поэтому в постерах достаточно обычного URL.
 */
export function mediaUrl(id: number | null | undefined): string | undefined {
  return id == null
    ? undefined
    : `${BASE_URL}/api/v1/app/media/${id}/raw${MEDIA_CACHE_BUST}`;
}
