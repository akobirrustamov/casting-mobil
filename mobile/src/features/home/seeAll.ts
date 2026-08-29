/**
 * Куда ведёт «Barchasi ›» с ряда главной.
 *
 * <h2>Зачем</h2>
 * Заказчик (28.08.2026): плитки категорий убрать, вместо них ряды видео,
 * и у каждого — переход в общий список. Открывается экран «Media» с уже
 * выбранной вкладкой, чтобы человек попал ровно в тот раздел, из которого
 * нажал.
 *
 * <h2>Почему не у каждого ряда</h2>
 * «Танланган» и «Машҳур» — это подборки, а не раздел каталога: у них нет
 * вкладки, в которую можно перейти. Ссылка туда вела бы в случайное место,
 * поэтому у таких рядов её просто нет. То же с ручным рядом админа.
 *
 * Ключ вкладки — строка, а не индекс: порядок вкладок на экране «Media»
 * ещё будет меняться, а от перестановки ссылки ломаться не должны.
 */
export type MediaTabKey =
  | 'series'
  | 'podcasts'
  | 'reels'
  | 'clips'
  | 'streams'
  | 'shows'
  | 'movies';

const BY_SECTION: Record<string, MediaTabKey> = {
  MINI_SERIES: 'series',
  REELS_SERIES: 'reels',
  PODCASTS: 'podcasts',
  SHOWS: 'shows',
  STREAMS: 'streams',
  CLIPS: 'clips',
};

export function seeAllTab(sectionType: string): MediaTabKey | null {
  return BY_SECTION[sectionType] ?? null;
}

/** Адрес экрана «Media» с выбранной вкладкой. */
export function seeAllRoute(tab: MediaTabKey): string {
  return `/(tabs)/premiere?tab=${tab}`;
}
