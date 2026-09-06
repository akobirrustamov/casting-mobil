/**
 * Контракт `GET /api/v1/app/watch/**` (ТЗ §37).
 *
 * Зеркало `WatchController.WatchResponse` из
 * `backend/src/main/java/com/example/backend/Cms/Controller/WatchController.java`.
 *
 * ⚠️ Право на просмотр считает ТОЛЬКО сервер (`AccessService`). Клиент не
 * складывает подписку, покупку и бесплатность сам — он показывает то, что
 * решил сервер. Иначе правило доступа существовало бы в двух местах и они
 * разъехались бы при первом же изменении тарифов.
 */

import type { Orientation } from '@/features/content/orientation';

/** Почему доступ дан или не дан. Список бэкенда может вырасти. */
export type WatchReason =
  | 'FREE'
  | 'PREMIUM'
  | 'EPISODE_PURCHASE'
  | 'PREMIERE_PURCHASE'
  | 'NOT_PUBLISHED'
  | 'USER_BLOCKED'
  | 'NOT_AUTHENTICATED'
  | 'PAYMENT_REQUIRED'
  | (string & {});

/**
 * Что человек должен сделать, чтобы получить доступ.
 * Это ЕДИНСТВЕННЫЙ источник CTA на экране: подставлять свою кнопку по
 * `accessPolicy` из фида нельзя — фид не знает ни о подписке, ни о покупке.
 */
export type RequiredAction =
  | 'NONE'
  | 'SIGN_IN'
  | 'BUY_EPISODE'
  | 'BUY_PREMIERE'
  | 'SUBSCRIBE'
  | 'BUY_OR_SUBSCRIBE'
  | (string & {});

/**
 * Один видеофайл.
 *
 * Их может быть несколько: в формате Reels (ТЗ §19) серия нарезана на части,
 * `partNumber` — их порядок. `url` приходит относительным
 * (`/api/v1/app/media/{id}/raw`), базу подставляет клиент.
 */
export type VideoSource = {
  partNumber: number | null;
  mediaId: number | null;

  /**
   * ОТНОСИТЕЛЬНЫЙ путь — `/api/v1/app/media/{id}/raw`.
   *
   * ⚠️ Перед ним подставляется `BASE_URL`. Абсолютный адрес здесь дал
   * бы `https://uzcasting.sitehttps://cdn…` — тихая поломка без
   * единого сообщения об ошибке.
   *
   * Это ЗАПАСНОЙ путь: он работает всегда, но видео идёт через сервер
   * приложения, а не через CDN.
   */
  url: string | null;

  /**
   * HLS master playlist. `null` — «HLS ещё нет», тогда играем по `url`.
   *
   * <h2>⚠️ Приходит в ДВУХ видах, и различать их обязан клиент</h2>
   * <pre>
   *   «/api/v1/…»  → защищённый плейлист нашего сервера, нужен BASE_URL
   *   «https://…»  → CDN напрямую, BASE_URL добавлять НЕЛЬЗЯ
   * </pre>
   *
   * Первый вид — обычный для боевого контура: плейлист отдаёт наш сервер
   * (в адресе билет `?t=`), а сегменты идут в хранилище по подписанной
   * ссылке. Второй остался для окружения без S3.
   *
   * Различает первый символ — см. `playbackSource()` в `Player.tsx`.
   * Слепая склейка с `BASE_URL` дала бы `https://uzcasting.comhttps://cdn…`
   * — тихую поломку без единого сообщения.
   *
   * ⚠️ Адрес живёт ограниченное время: билет 6 часов, подпись сегмента —
   * около трёх. Просроченный лечится перезапросом `/watch`, а не повтором
   * проигрывания.
   */
  hlsUrl: string | null;

  durationSeconds: number | null;
};

export type WatchInfo = {
  episodeId: number | null;
  contentId: number | null;
  episodeNumber: number | null;
  durationSeconds: number | null;
  title: string | null;

  /**
   * Формат кадра — `LANDSCAPE` или `VERTICAL` (см. `features/content/orientation`).
   *
   * Приходит и при отказе: у закрытого контента видео не выдаётся, а
   * афишу под замком рисовать надо — и рилс под ней вертикальный.
   */
  orientation: Orientation | null;

  allowed: boolean;
  reason: WatchReason;
  requiredAction: RequiredAction;

  /** Цены приходят от сервера. Захардкоженных цен в приложении быть не должно. */
  episodePrice: number | null;
  premierePrice: number | null;

  /** У подписчика реклама не показывается. */
  showAds: boolean;


  /** При отказе — всегда пустой список, ссылок на файлы в отказе нет. */
  sources: VideoSource[];
};
