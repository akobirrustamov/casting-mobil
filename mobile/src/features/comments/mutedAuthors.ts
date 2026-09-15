import { create } from 'zustand';

import { getItem, setItem } from '@/lib/storage';

/**
 * Скрытые авторы комментариев — список живёт на телефоне.
 *
 * <h2>Зачем это есть</h2>
 * UGC-политика Google просит две вещи: пожаловаться на запись и
 * **заблокировать автора**. Жалоба уходит модератору и работает на всех
 * (`../api.ts`), а это — личная настройка: «я не хочу видеть этого
 * человека», и решения модератора она не ждёт.
 *
 * <h2>⚠️ Почему на телефоне, а не на сервере</h2>
 * Серверный мьют — это ещё одна таблица, эндпоинт и фильтр в выдаче,
 * причём фильтр должен работать на каждой странице ленты. Здесь же
 * список из десятка идентификаторов, и он никому, кроме владельца
 * телефона, не нужен. Цена решения: после переустановки список пуст —
 * это приемлемо, потому что мьют не средство модерации, а личное
 * удобство.
 *
 * <h2>⚠️ Скрываем по id, а не по имени</h2>
 * Имена не уникальны и меняются: «Ali» может быть у десяти человек, а
 * автор в любой момент правит имя в профиле. Идентификатор приходит с
 * бэкенда (`authorId`, 15.09.2026) и не меняется.
 */
const KEY = 'uzcasting.muted_authors';

type MutedState = {
  ids: Set<string>;
  /** Список уже поднят с диска — до этого фильтровать нечем. */
  isRestored: boolean;

  restore: () => Promise<void>;
  mute: (authorId: string) => Promise<void>;
  /** Показать всех обратно — единственный способ снять мьют. */
  clear: () => Promise<void>;
};

export const useMutedAuthors = create<MutedState>((set, get) => ({
  ids: new Set(),
  isRestored: false,

  restore: async () => {
    const raw = await getItem(KEY);
    set({ ids: parse(raw), isRestored: true });
  },

  mute: async (authorId) => {
    if (!authorId) return;

    const next = new Set(get().ids);
    next.add(authorId);
    set({ ids: next });
    await persist(next);
  },

  clear: async () => {
    set({ ids: new Set() });
    await persist(new Set());
  },
}));

/**
 * ⚠️ Битую запись не бросаем наружу: мьют — украшение, и падать из-за
 * него на экране комментариев нельзя.
 */
function parse(raw: string | null): Set<string> {
  if (!raw) return new Set();
  try {
    const list = JSON.parse(raw);
    return Array.isArray(list)
      ? new Set(list.filter((v) => typeof v === 'string'))
      : new Set();
  } catch {
    return new Set();
  }
}

async function persist(ids: Set<string>): Promise<void> {
  await setItem(KEY, JSON.stringify([...ids]));
}
