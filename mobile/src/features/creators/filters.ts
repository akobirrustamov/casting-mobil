import type { Creator, Gender } from './types';

/**
 * Фильтры каталога — состав по ТЗ (V3, экраны 06–12):
 * направление, город, пол, возраст.
 *
 * Считаются на клиенте: API отдаёт весь список одним запросом и не умеет
 * фильтровать. Пока анкет пара сотен это дешевле, чем ходить на сервер
 * на каждое нажатие. Когда список вырастет — переносить на бэкенд,
 * сигнатура хука при этом не изменится.
 */
export type AgeBucket = 'any' | '18-24' | '25-34' | '35+';

export type CreatorFilters = {
  /** Типы из API. Пусто — не фильтруем по направлению. */
  apiTypes: string[];
  region: string | null;
  gender: Gender | null;
  age: AgeBucket;
};

export const EMPTY_FILTERS: CreatorFilters = {
  apiTypes: [],
  region: null,
  gender: null,
  age: 'any',
};

const AGE_RANGES: Record<Exclude<AgeBucket, 'any'>, [number, number]> = {
  '18-24': [18, 24],
  '25-34': [25, 34],
  '35+': [35, 200],
};

export function applyFilters(
  creators: Creator[],
  filters: CreatorFilters
): Creator[] {
  return creators.filter((c) => {
    if (filters.apiTypes.length > 0) {
      if (!c.castingType || !filters.apiTypes.includes(c.castingType)) {
        return false;
      }
    }

    if (filters.region && c.region !== filters.region) return false;
    if (filters.gender && c.gender !== filters.gender) return false;

    if (filters.age !== 'any') {
      // Возраст известен не у всех — без него в возрастной фильтр не попадаем
      if (c.age === null) return false;
      const [min, max] = AGE_RANGES[filters.age];
      if (c.age < min || c.age > max) return false;
    }

    return true;
  });
}

/** Города, которые реально встречаются в данных — фильтр не должен врать. */
export function collectRegions(creators: Creator[]): string[] {
  const set = new Set<string>();
  for (const c of creators) {
    if (c.region) set.add(c.region);
  }
  return [...set].sort((a, b) => a.localeCompare(b));
}

export function countActive(filters: CreatorFilters): number {
  let n = 0;
  if (filters.region) n++;
  if (filters.gender) n++;
  if (filters.age !== 'any') n++;
  return n;
}
