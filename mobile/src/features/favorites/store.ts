import { create } from 'zustand';

import { getItem, setItem } from '@/lib/storage';

/**
 * Избранное («Sevimli»).
 *
 * ⚠️ Хранится **только на устройстве**. Эндпоинта на бэкенде нет
 * (docs/API.md §5), поэтому между телефоном и планшетом список не разъедется —
 * его там просто не будет. Когда эндпоинт появится, меняется одна эта прослойка:
 * экраны работают через хуки ниже и о хранилище ничего не знают.
 *
 * Держим Set в памяти, а на диск пишем массив — так проверка «в избранном ли»
 * остаётся O(1) на каждой карточке списка.
 */
const KEY = 'uzcasting.favorites';

type FavoritesState = {
  ids: Set<number>;
  isRestoring: boolean;

  restore: () => Promise<void>;
  toggle: (id: number) => Promise<void>;
};

export const useFavoritesStore = create<FavoritesState>((set, get) => ({
  ids: new Set(),
  isRestoring: true,

  restore: async () => {
    const raw = await getItem(KEY);
    set({ ids: parseIds(raw), isRestoring: false });
  },

  toggle: async (id) => {
    const next = new Set(get().ids);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }

    // Сначала состояние, потом запись: сердечко должно откликаться мгновенно,
    // а не ждать диск.
    set({ ids: next });
    await setItem(KEY, JSON.stringify([...next]));
  },
}));

/** Подписка на одну карточку — перерисуется только она, а не весь список. */
export function useIsFavorite(id: number): boolean {
  return useFavoritesStore((s) => s.ids.has(id));
}

function parseIds(raw: string | null): Set<number> {
  if (!raw) return new Set();
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return new Set();
    return new Set(parsed.filter((v): v is number => typeof v === 'number'));
  } catch {
    return new Set();
  }
}
