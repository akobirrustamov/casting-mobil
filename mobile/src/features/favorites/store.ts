import { create } from 'zustand';

import { useAuthStore } from '@/features/auth/store';
import { getItem, removeItem, setItem } from '@/lib/storage';

import { addFavorites, fetchFavorites, removeFavorite } from './api';

/**
 * Избранное («Sevimli»).
 *
 * <h2>Два хранилища, одно поведение</h2>
 * <pre>
 *   не вошёл  → только телефон
 *   вошёл     → сервер, телефон остаётся кэшем на первый кадр
 * </pre>
 *
 * ⚠️ Локальное хранилище НЕ убрано. Список нужен до входа (человек
 * листает каталог и отмечает, ещё не зарегистрировавшись) и нужен
 * без сети. Убрать его — значит показать пустой экран там, где
 * раньше всё работало.
 *
 * <h2>⚠️ Вход СЛИВАЕТ списки, а не заменяет</h2>
 * Отмеченное до входа уходит на сервер и объединяется с тем, что
 * там уже есть. Замена в любую сторону молча теряла бы часть: либо
 * то, что отмечено на этом телефоне, либо то, что на другом.
 *
 * <h2>Про `READ_ONLY`</h2>
 * Приложение смотрит в боевую базу сайта, и запись туда по умолчанию
 * запрещена. Избранное — точечное исключение в `WRITE_ALLOWLIST`
 * (`lib/api.ts`): это собственные предпочтения вошедшего человека,
 * без денег и чужих данных.
 *
 * ⚠️ Решение о разрешении принимается ТАМ, а не здесь. Второй
 * выключатель в этом файле означал бы, что функцию можно случайно
 * выключить наполовину: запрос уходит, а состояние не меняется —
 * или наоборот.
 */
const KEY = 'uzcasting.favorites';

type FavoritesState = {
  ids: Set<number>;
  isRestoring: boolean;
  /** true — список уже сверен с сервером в этой сессии. */
  isSynced: boolean;

  restore: () => Promise<void>;
  toggle: (id: number) => Promise<void>;
  /** Слить локальный список с серверным. Вызывается после входа. */
  sync: () => Promise<void>;
};

export const useFavoritesStore = create<FavoritesState>((set, get) => ({
  ids: new Set(),
  isRestoring: true,
  isSynced: false,

  restore: async () => {
    const raw = await getItem(KEY);
    set({ ids: parseIds(raw), isRestoring: false });
  },

  /**
   * Слияние локального списка с серверным.
   *
   * ⚠️ Порядок важен: сначала ОТДАЁМ своё, потом берём итог. Если
   * сначала прочитать сервер и записать его поверх локального,
   * отмеченное до входа исчезнет ещё до того, как уйдёт наверх.
   */
  sync: async () => {
    // ⚠️ Локальный список мог ещё не подняться с диска: восстановление
    // профиля и восстановление избранного стартуют параллельно. Без
    // этой строки слияние ушло бы с пустым списком и всё отмеченное
    // до входа пропало бы.
    if (get().isRestoring) {
      await get().restore();
    }

    const local = [...get().ids];
    try {
      const merged = local.length > 0 ? await addFavorites(local) : await fetchFavorites();

      // ⚠️ За время запроса человек мог ВЫЙТИ из аккаунта.
      //
      // Без этой проверки ответ ложился на диск уже после очистки
      // при выходе — и на телефоне оставалось избранное предыдущего
      // владельца сессии. Ровно то, что очистка и должна была
      // предотвратить.
      if (!useAuthStore.getState().isAuthorized) {
        return;
      }

      await persist(merged);
      set({ ids: new Set(merged), isSynced: true });
    } catch {
      // Сеть или старая сборка бэкенда — остаёмся на локальном
      // списке. Он рабочий, просто не общий между устройствами.
    }
  },

  toggle: async (id) => {
    const previous = get().ids;
    const next = new Set(previous);
    const adding = !next.has(id);

    if (adding) {
      next.add(id);
    } else {
      next.delete(id);
    }

    // ⚠️ Сначала состояние, потом всё остальное: сердечко должно
    // откликаться мгновенно, а не ждать диск и тем более сеть.
    set({ ids: next });
    await persist([...next]);

    if (!get().isSynced) return;

    try {
      const server = adding ? await addFavorites([id]) : await removeFavorite(id);
      await persist(server);
      set({ ids: new Set(server) });
    } catch {
      // ⚠️ Возвращаем как было.
      //
      // Иначе сердечко осталось бы закрашенным, а на сервере ничего
      // не изменилось — и на другом устройстве человек не нашёл бы
      // то, что «точно сохранял». Молчаливое расхождение хуже, чем
      // невыполненное нажатие.
      set({ ids: previous });
      await persist([...previous]);
    }
  },
}));

async function persist(ids: number[]): Promise<void> {
  await setItem(KEY, JSON.stringify(ids));
}

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

/**
 * Вход и выход из аккаунта.
 *
 * <h2>Вход — слияние</h2>
 * Отмеченное до авторизации уходит на сервер и объединяется с тем,
 * что там уже есть.
 *
 * <h2>⚠️ Выход — ОЧИСТКА локального списка</h2>
 * Иначе следующий вошедший на этом телефоне человек влил бы чужое
 * избранное в свой аккаунт. Терять тут нечего: список лежит на
 * сервере и вернётся при следующем входе.
 */
useAuthStore.subscribe((state, previous) => {
  if (state.isAuthorized && !previous.isAuthorized) {
    void useFavoritesStore.getState().sync();
    return;
  }

  if (!state.isAuthorized && previous.isAuthorized) {
    void (async () => {
      await removeItem(KEY);
      useFavoritesStore.setState({ ids: new Set(), isSynced: false });
    })();
  }
});
