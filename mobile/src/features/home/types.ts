/**
 * Контракт `GET /api/v1/app/home` (ТЗ §31).
 *
 * Зеркало `HomeFeedDto` из
 * `backend/src/main/java/com/example/backend/Cms/Dto/HomeFeedDto.java`.
 * При изменении DTO на бэкенде править здесь же.
 *
 * ⚠️ Главная НЕ зашита в клиенте: какие блоки есть, в каком порядке и как
 * называются — решает бэкенд. Поэтому здесь нет ни одного захардкоженного
 * заголовка секции, а неизвестный тип секции не ломает экран (см. `sections.tsx`).
 */

import type { Orientation } from '@/features/content/orientation';

/** Типы, которые сегодня отдаёт бэкенд. Список может вырасти без релиза приложения. */
export type HomeSectionType =
  | 'ADVERTISEMENT_CAROUSEL'
  | 'NEW_PREMIERES'
  | 'CATEGORIES'
  | 'MINI_SERIES'
  | 'REELS_SERIES'
  | 'PODCASTS'
  | 'SHOWS'
  | 'STREAMS'
  | 'CLIPS'
  | 'FEATURED_CONTENT'
  | 'POPULAR_CONTENT'
  | 'POPULAR_CREATORS'
  | 'CUSTOM_ROW'
  // Бэкенд может добавить новый тип раньше, чем выйдет новая версия приложения.
  | (string & {});

/**
 * Политика доступа к контенту.
 * Цены здесь НЕТ намеренно — она приходит из `/api/v1/app/watch/{episodeId}`
 * вместе с entitlement. Показывать на главной выдуманную цену нельзя.
 */
export type AccessPolicy =
  | 'FREE'
  | 'PREMIUM_ONLY'
  | 'PURCHASE_ONLY'
  | 'PREMIUM_OR_PURCHASE'
  | (string & {});

export type ContentCard = {
  id: number;
  slug: string | null;
  title: string | null;
  shortDescription: string | null;
  contentType: string | null;
  /** `LANDSCAPE` или `VERTICAL` — форма карточки и плеера. */
  orientation: Orientation | null;
  accessPolicy: AccessPolicy | null;
  ageRating: string | null;
  posterMediaId: number | null;

  /** Своя длительность контента. У многосерийного — `null`. */
  durationSeconds: number | null;
  /** Сколько ОПУБЛИКОВАННЫХ серий. У цельного — `null`. */
  episodeCount: number | null;
  /** Первый жанр на языке интерфейса. Полный список — на карточке контента. */
  genre: string | null;
};

/** Реклама и премьера приходят одной формой — широкий баннер. */
export type BannerCard = {
  id: number;
  /**
   * `ADVERTISEMENT` — платное размещение, `ADMIN_ANNOUNCEMENT` — собственный
   * анонс платформы. У премьер — `null`.
   *
   * Оба вида приходят одним массивом, но это разные вещи, и подпись у них
   * тоже должна быть разной.
   */
  audience: string | null;
  title: string | null;
  subtitle: string | null;
  description: string | null;
  buttonText: string | null;
  buttonEnabled: boolean;
  imageMediaId: number | null;
  videoMediaId: number | null;
  linkType: string | null;
  linkUrl: string | null;
  internalTargetType: string | null;
  internalTargetId: number | null;
};

export type CategoryCard = {
  id: number;
  slug: string | null;
  name: string | null;
  iconMediaId: number | null;
};

/**
 * Креатор ИЗ КАТАЛОГА КОНТЕНТА (актёр, режиссёр фильма).
 *
 * ⚠️ Это не анкета кастинга из `features/creators` — там старый бэкенд,
 * другой id и другая сущность. Открывать `/creator/{id}` с этим id нельзя.
 */
export type FeedCreatorCard = {
  id: number;
  slug: string | null;
  displayName: string | null;
  photoMediaId: number | null;
};

export type HomeSection = {
  id: number;
  type: HomeSectionType;
  title: string | null;
  sortOrder: number | null;
  content: ContentCard[];
  banners: BannerCard[];
  categories: CategoryCard[];
  creators: FeedCreatorCard[];
};

export type HomeFeed = {
  locale: string;
  /** Реклама скрыта у тех, у кого активна подписка. */
  showAds: boolean;
  sections: HomeSection[];
};
