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
  url: string | null;
  durationSeconds: number | null;
};

export type WatchInfo = {
  episodeId: number | null;
  contentId: number | null;
  episodeNumber: number | null;
  durationSeconds: number | null;
  title: string | null;

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
