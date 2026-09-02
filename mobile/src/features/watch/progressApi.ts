import type { ContentCard } from '@/features/home/types';
import { api } from '@/lib/api';

/**
 * «Продолжить просмотр» — `PUT/GET/DELETE /api/v1/app/watch-progress`.
 *
 * <h2>⚠️ Что чинится</h2>
 * Позиция не сохранялась НИГДЕ. Человек останавливал двухчасовой фильм
 * на 1:32:45, а на следующий день начинал с 0:00 и сам искал, где
 * остановился.
 *
 * <h2>Зачем и сервер, и телефон</h2>
 * <pre>
 *   телефон → читается МГНОВЕННО, работает без сети
 *   сервер  → переживает переустановку, виден на другом устройстве
 * </pre>
 *
 * Плеер не должен ждать сеть, чтобы перемотать на нужную секунду:
 * ожидание видно как «видео началось сначала, а потом дёрнулось».
 * Поэтому локальная копия — не кэш, а основной путь для перемотки, а
 * сервер — источник правды между устройствами.
 *
 * <h2>Про `WRITE_ALLOWLIST`</h2>
 * Приложение смотрит в боевую базу, запись по умолчанию запрещена.
 * Этот адрес — точечное исключение в `lib/api.ts`, рядом с избранным:
 * собственные данные вошедшего человека.
 */

/** Что смотрят. Зеркало `WatchTargetType` на бэкенде. */
export type WatchTarget = 'EPISODE' | 'CONTENT';

export type WatchProgress = {
  type: WatchTarget;
  targetId: number;
  position: number;
  duration: number | null;
  quality: string | null;
  completed: boolean;
  /** 0–100, либо `null` — длительность неизвестна. */
  percent: number | null;

  /**
   * Когда сервер записал это, ISO-строка.
   *
   * ⚠️ Нужна, чтобы понять, какая из двух копий свежее — на телефоне
   * или на сервере. Без неё пришлось бы гадать, и любой из вариантов
   * терял бы данные: «сервер прав» отбрасывал бы просмотр без сети,
   * «телефон прав» — просмотр на другом устройстве.
   */
  updatedAt: string | null;
};

function num(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function str(value: unknown): string | null {
  return typeof value === 'string' && value.trim() !== '' ? value : null;
}

/**
 * Ответ сервера → наш тип.
 *
 * ⚠️ `null` — законный ответ: «это ещё не смотрели». Старая сборка
 * бэкенда на этот адрес отдаёт index.html со статусом 200, и без
 * проверки `position` строка HTML молча превратилась бы в позицию
 * `undefined`, а плеер перемотал бы в никуда.
 */
function mapProgress(raw: unknown): WatchProgress | null {
  const r = raw as Record<string, unknown> | null;
  if (!r || typeof r !== 'object') return null;

  const position = num(r.position);
  const targetId = num(r.targetId);
  const type = str(r.type);
  if (position === null || targetId === null || (type !== 'EPISODE' && type !== 'CONTENT')) {
    return null;
  }

  return {
    type,
    targetId,
    position,
    duration: num(r.duration),
    quality: str(r.quality),
    completed: r.completed === true,
    percent: num(r.percent),
    updatedAt: str(r.updatedAt),
  };
}

export async function fetchProgress(
  type: WatchTarget,
  targetId: number
): Promise<WatchProgress | null> {
  const { data } = await api.get<unknown>(`/api/v1/app/watch-progress/${type}/${targetId}`);
  return mapProgress(data);
}

/**
 * Сохраняет позицию.
 *
 * ⚠️ `PUT`, а не `POST`: на одно видео одна запись, повтор запроса
 * ничего не создаёт. На плохой связи клиент повторяет запросы
 * постоянно.
 */
export async function saveProgress(
  type: WatchTarget,
  targetId: number,
  position: number,
  duration: number | null,
  quality: string | null
): Promise<WatchProgress | null> {
  const { data } = await api.put<unknown>(`/api/v1/app/watch-progress/${type}/${targetId}`, {
    position: Math.round(position),
    duration: duration === null ? null : Math.round(duration),
    quality,
  });
  return mapProgress(data);
}

/**
 * Один элемент ленты «Продолжить просмотр».
 *
 * ⚠️ Карточка приходит С СЕРВЕРА, в том же виде, что и остальные ряды
 * главной. Собирать её на клиенте значило бы запрашивать двадцать
 * контентов по одному — в момент открытия главной.
 */
export type ContinueItem = {
  progress: WatchProgress;
  /** `HomeFeedDto.ContentCard` — та же форма, что в ленте главной. */
  content: ContentCard;
  /** Номер серии; у фильма `null` — серий там нет. */
  episodeNumber: number | null;
};

function mapItem(raw: unknown): ContinueItem | null {
  const r = raw as Record<string, unknown> | null;
  if (!r || typeof r !== 'object') return null;

  const progress = mapProgress(r.progress);
  const content = r.content as ContentCard | null;

  // ⚠️ Без карточки рисовать нечего: получился бы пустой прямоугольник
  // без обложки и названия. Такой элемент лучше не показывать вовсе.
  if (progress === null || !content || typeof content.id !== 'number') {
    return null;
  }
  return { progress, content, episodeNumber: num(r.episodeNumber) };
}

export async function fetchContinueWatching(locale: string): Promise<ContinueItem[]> {
  const { data } = await api.get<unknown>('/api/v1/app/watch-progress/continue', {
    params: { locale },
  });
  const items = (data as { items?: unknown } | null)?.items;

  // ⚠️ Пустой список законен, ОТСУТСТВИЕ массива — нет: ответил не
  // тот сервер. Иначе HTML-заглушка стала бы «нечего продолжать».
  if (!Array.isArray(items)) return [];

  return items.map(mapItem).filter((i): i is ContinueItem => i !== null);
}

export async function forgetProgress(type: WatchTarget, targetId: number): Promise<void> {
  await api.delete(`/api/v1/app/watch-progress/${type}/${targetId}`);
}
