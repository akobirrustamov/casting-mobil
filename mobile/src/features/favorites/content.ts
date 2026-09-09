import { create } from 'zustand';

import { useAuthStore } from '@/features/auth/store';
import { api } from '@/lib/api';

/**
 * Сохранённый КОНТЕНТ — закладка на странице фильма или сериала.
 *
 * <h2>Почему отдельно от `./store`</h2>
 * Тот стор хранит анкеты КАСТИНГА (`FavoriteType.CREATOR`) — другой
 * модуль бэкенда, другие идентификаторы, другой экран. Общий стор
 * означал бы одно множество id на две несвязанные сущности: контент №7 и
 * креатор №7 гасили бы закладки друг друга.
 *
 * Сервер это уже различает: у `/api/v1/app/favorites` есть параметр
 * `type`, и `CONTENT` в нём предусмотрен с самого начала.
 *
 * <h2>⚠️ Закладка требует входа — в отличие от «сердечка» у креаторов</h2>
 * Там список живёт и на телефоне: человек листает каталог кастинга ещё до
 * регистрации, и терять отмеченное нельзя. Здесь такого сценария нет —
 * закладка ставится на странице контента, куда человек приходит уже
 * осознанно, и вход всё равно нужен, чтобы список пережил переустановку.
 *
 * Локальное хранилище дало бы третье место, где список может разъехаться
 * с сервером, — ради удобства, которого никто не просил.
 */

type FavoritesResponse = { targetIds: number[] };

/** Старая сборка бэкенда отдаёт на этот адрес index.html со статусом 200. */
export class ContentFavoritesUnavailableError extends Error {
  constructor() {
    super('/api/v1/app/favorites?type=CONTENT недоступен на этом сервере');
    this.name = 'ContentFavoritesUnavailableError';
  }
}

function idsOf(raw: unknown): number[] {
  const list = (raw as Partial<FavoritesResponse> | null)?.targetIds;

  // ⚠️ Пустой список — законный ответ, а вот ОТСУТСТВИЕ массива значит,
  // что ответил не тот сервер. Без проверки HTML-заглушка молча
  // превратилась бы в «ничего не сохранено».
  if (!Array.isArray(list)) {
    throw new ContentFavoritesUnavailableError();
  }
  return list.filter((v): v is number => typeof v === 'number');
}

export async function fetchContentFavorites(): Promise<number[]> {
  const { data } = await api.get<unknown>('/api/v1/app/favorites', {
    params: { type: 'CONTENT' },
  });
  return idsOf(data);
}

export async function addContentFavorite(contentId: number): Promise<number[]> {
  const { data } = await api.post<unknown>('/api/v1/app/favorites', {
    type: 'CONTENT',
    targetIds: [contentId],
  });
  return idsOf(data);
}

export async function removeContentFavorite(contentId: number): Promise<number[]> {
  const { data } = await api.delete<unknown>('/api/v1/app/favorites', {
    params: { type: 'CONTENT', targetId: contentId },
  });
  return idsOf(data);
}

type State = {
  ids: Set<number>;
  /** true — список уже сверен с сервером в этой сессии. */
  isLoaded: boolean;

  load: () => Promise<void>;
  toggle: (contentId: number) => Promise<void>;
  clear: () => void;
};

export const useContentFavorites = create<State>((set, get) => ({
  ids: new Set(),
  isLoaded: false,

  load: async () => {
    if (!useAuthStore.getState().isAuthorized) return;
    try {
      set({ ids: new Set(await fetchContentFavorites()), isLoaded: true });
    } catch {
      // Сеть или старая сборка бэкенда. Закладка просто не отметится —
      // страница контента от этого не ломается.
    }
  },

  toggle: async (contentId) => {
    const previous = get().ids;
    const adding = !previous.has(contentId);

    const next = new Set(previous);
    if (adding) {
      next.add(contentId);
    } else {
      next.delete(contentId);
    }

    // ⚠️ Сначала состояние: закладка должна откликаться мгновенно, а не
    // ждать сеть. Иначе нажатие выглядит непринятым.
    set({ ids: next });

    try {
      const server = adding
        ? await addContentFavorite(contentId)
        : await removeContentFavorite(contentId);
      set({ ids: new Set(server), isLoaded: true });
    } catch {
      // ⚠️ Возвращаем как было. Закрашенная закладка при пустом сервере —
      // молчаливое расхождение: на другом устройстве человек не нашёл бы
      // то, что «точно сохранял».
      set({ ids: previous });
    }
  },

  clear: () => set({ ids: new Set(), isLoaded: false }),
}));

/** Подписка на одну карточку — перерисуется только она. */
export function useIsContentSaved(contentId: number | null): boolean {
  return useContentFavorites((s) => (contentId === null ? false : s.ids.has(contentId)));
}

/**
 * Вход и выход из аккаунта.
 *
 * ⚠️ Выход ОЧИЩАЕТ список. Он ничей после выхода: следующий вошедший на
 * этом телефоне человек увидел бы чужие закладки. Терять нечего — список
 * лежит на сервере и вернётся при следующем входе.
 */
useAuthStore.subscribe((state, previous) => {
  if (state.isAuthorized && !previous.isAuthorized) {
    void useContentFavorites.getState().load();
    return;
  }
  if (!state.isAuthorized && previous.isAuthorized) {
    useContentFavorites.getState().clear();
  }
});
