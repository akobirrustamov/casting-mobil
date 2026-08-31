import { api } from '@/lib/api';

/**
 * Избранное на сервере — `GET/POST/DELETE /api/v1/app/favorites`.
 *
 * <h2>⚠️ Что чинится</h2>
 * Список хранился ТОЛЬКО на телефоне. Переустановил приложение —
 * потерял; открыл на планшете — пусто; сменил телефон — всё ушло.
 * Человек воспринимает это как потерю данных, а не как особенность.
 *
 * <h2>Каждый ответ — ВЕСЬ список</h2>
 * И добавление, и удаление возвращают актуальный список целиком.
 * Клиент одним запросом приводит своё состояние к серверному и не
 * пересчитывает его сам: чтобы две стороны разошлись, достаточно
 * одного потерянного ответа.
 */

/** Столько идентификаторов принимает один POST (бэкенд: `FavoriteService.MAX_BATCH`). */
export const MAX_BATCH = 200;

type FavoritesResponse = { type: string; targetIds: number[] };

/** Старая сборка бэкенда отдаёт на этот адрес index.html со статусом 200. */
export class FavoritesUnavailableError extends Error {
  constructor() {
    super('/api/v1/app/favorites недоступен на этом сервере');
    this.name = 'FavoritesUnavailableError';
  }
}

function idsOf(raw: unknown): number[] {
  const list = (raw as Partial<FavoritesResponse> | null)?.targetIds;

  // ⚠️ Пустой список — это законный ответ, а вот ОТСУТСТВИЕ массива
  // означает, что ответил не тот сервер. Без этой проверки HTML-
  // заглушка молча превратилась бы в «избранного нет».
  if (!Array.isArray(list)) {
    throw new FavoritesUnavailableError();
  }
  return list.filter((v): v is number => typeof v === 'number');
}

export async function fetchFavorites(): Promise<number[]> {
  const { data } = await api.get<unknown>('/api/v1/app/favorites');
  return idsOf(data);
}

/**
 * Добавляет и возвращает обновлённый список.
 *
 * ⚠️ Принимает СПИСОК, а не один элемент: после входа сюда уходит
 * всё, что человек отметил до авторизации. По одному это были бы
 * десятки запросов, часть которых на плохой связи потерялась бы.
 *
 * Длинный список режется на части — бэкенд ограничивает размер
 * пачки, и запрос целиком получил бы отказ.
 */
export async function addFavorites(targetIds: number[]): Promise<number[]> {
  let last: number[] = [];
  for (let i = 0; i < targetIds.length; i += MAX_BATCH) {
    const { data } = await api.post<unknown>('/api/v1/app/favorites', {
      type: 'CREATOR',
      targetIds: targetIds.slice(i, i + MAX_BATCH),
    });
    last = idsOf(data);
  }
  return last;
}

export async function removeFavorite(targetId: number): Promise<number[]> {
  const { data } = await api.delete<unknown>('/api/v1/app/favorites', {
    params: { type: 'CREATOR', targetId },
  });
  return idsOf(data);
}
