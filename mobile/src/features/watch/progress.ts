import { getItem, setItem } from '@/lib/storage';

import { fetchProgress, saveProgress, type WatchProgress, type WatchTarget } from './progressApi';

/**
 * «Продолжить просмотр»: где человек остановился.
 *
 * Здесь только ПРАВИЛА — без React и без плеера, чтобы каждое можно
 * было проверить отдельно. Подключение к плееру — в `useWatchProgress`.
 */

/**
 * Как часто позиция уходит на сервер, в секундах.
 *
 * ⚠️ Не «на каждое обновление времени»: плеер шлёт их постоянно, и
 * это был бы запрос несколько раз в секунду на каждого зрителя.
 *
 * Цена ошибки при потере — не больше этого интервала: человек
 * вернётся на 15 секунд раньше, чем ушёл. Это незаметно, а вот
 * поток запросов заметен сразу.
 */
export const SERVER_SAVE_SECONDS = 15;

/**
 * Раньше этой секунды возобновлять НЕ НАДО.
 *
 * Человек мог открыть видео и сразу закрыть. Перемотка на пятую
 * секунду выглядела бы как сбой: он нажал «играть», а плеер прыгнул.
 */
export const MIN_RESUME_SECONDS = 15;

/**
 * Ближе этой доли к концу возобновлять НЕ НАДО.
 *
 * ⚠️ Иначе человек, досмотревший фильм, при повторном открытии
 * попадал бы прямо на титры и не мог начать сначала, не перемотав
 * вручную.
 */
export const RESUME_MAX_RATIO = 0.95;

/** Ключ в локальном хранилище. */
export function storageKey(type: WatchTarget, targetId: number): string {
  return `uzcasting.progress.${type}.${targetId}`;
}

/** Что лежит на телефоне. `savedAt` — чтобы сравнить с сервером. */
export type LocalProgress = {
  position: number;
  duration: number | null;
  quality: string | null;
  /** Время записи, миллисекунды epoch. */
  savedAt: number;
};

/**
 * Надо ли перематывать на сохранённую позицию.
 *
 * ⚠️ Возвращает `null`, а не `0`: ноль — это ТОЖЕ позиция, и
 * вызывающий не отличил бы «начать сначала» от «перематывать не
 * надо». Разница видна: перемотка на ноль у уже играющего видео
 * дёргает картинку.
 */
export function resumePosition(progress: {
  position: number;
  duration: number | null;
  completed?: boolean;
} | null): number | null {
  if (progress === null) return null;
  if (progress.completed === true) return null;
  if (progress.position < MIN_RESUME_SECONDS) return null;

  const { duration } = progress;
  if (duration !== null && duration > 0 && progress.position >= duration * RESUME_MAX_RATIO) {
    return null;
  }
  return progress.position;
}

/**
 * Какая из двух записей новее — телефона или сервера.
 *
 * <h2>⚠️ Почему нельзя просто «сервер прав»</h2>
 * Последний сеанс мог пройти без сети: тогда на телефоне позиция
 * свежее, а на сервере — та, что была сутки назад. «Сервер прав»
 * отбросил бы час просмотра.
 *
 * <h2>⚠️ И почему нельзя «телефон прав»</h2>
 * Человек мог продолжить на другом устройстве. Тогда свежая позиция
 * как раз на сервере, а телефон помнит старую.
 *
 * Поэтому сравниваются ВРЕМЕНА записи, а не значения. Ради этого
 * сервер и отдаёт `updatedAt`.
 */
export function newer(
  local: LocalProgress | null,
  server: WatchProgress | null
): { position: number; duration: number | null; quality: string | null } | null {
  if (local === null && server === null) return null;
  if (server === null) return local;
  if (local === null) return server;

  const serverAt = server.updatedAt === null ? 0 : Date.parse(server.updatedAt);

  // ⚠️ `Date.parse` возвращает NaN на неразобранной строке. Без этой
  // проверки сравнение с NaN всегда ложно и сервер молча проигрывал
  // бы всегда — даже когда он и правда новее.
  const serverTime = Number.isFinite(serverAt) ? serverAt : 0;

  return local.savedAt >= serverTime ? local : server;
}

// ------------------------------------------------------------------ хранилище

export async function readLocal(
  type: WatchTarget,
  targetId: number
): Promise<LocalProgress | null> {
  const raw = await getItem(storageKey(type, targetId));
  if (raw === null) return null;

  try {
    const parsed = JSON.parse(raw) as Partial<LocalProgress>;
    if (typeof parsed.position !== 'number' || !Number.isFinite(parsed.position)) {
      return null;
    }
    return {
      position: parsed.position,
      duration: typeof parsed.duration === 'number' ? parsed.duration : null,
      quality: typeof parsed.quality === 'string' ? parsed.quality : null,
      savedAt: typeof parsed.savedAt === 'number' ? parsed.savedAt : 0,
    };
  } catch {
    // Испорченная запись — как будто её нет. Ронять просмотр из-за
    // строки в хранилище нельзя.
    return null;
  }
}

export async function writeLocal(
  type: WatchTarget,
  targetId: number,
  value: Omit<LocalProgress, 'savedAt'>
): Promise<void> {
  await setItem(
    storageKey(type, targetId),
    JSON.stringify({ ...value, savedAt: Date.now() } satisfies LocalProgress)
  );
}

/**
 * Сохраняет позицию: сначала телефон, затем сервер.
 *
 * <h2>⚠️ Порядок важен и ошибка сервера НЕ ронять просмотр</h2>
 * Локальная запись — то, что делает возобновление мгновенным, и она
 * не должна зависеть от сети. Если сервер недоступен, человек всё
 * равно продолжит с нужной секунды на этом телефоне.
 *
 * Поэтому сетевая ошибка здесь ГЛОТАЕТСЯ. Это единственное место,
 * где это оправдано: просмотр важнее синхронизации, а неотправленная
 * позиция уйдёт со следующим тиком.
 */
export async function persist(
  type: WatchTarget,
  targetId: number,
  position: number,
  duration: number | null,
  quality: string | null,
  toServer: boolean
): Promise<void> {
  await writeLocal(type, targetId, { position, duration, quality });

  if (!toServer) return;

  try {
    await saveProgress(type, targetId, position, duration, quality);
  } catch {
    // Сеть недоступна или запись запрещена — просмотр продолжается.
  }
}

/**
 * Откуда продолжить: телефон (мгновенно) и сервер (между устройствами).
 *
 * ⚠️ Ошибка сервера НЕ отменяет возобновление: локальной записи
 * достаточно, чтобы продолжить с нужной секунды.
 */
export async function restore(
  type: WatchTarget,
  targetId: number
): Promise<number | null> {
  const local = await readLocal(type, targetId);

  let server: WatchProgress | null = null;
  try {
    server = await fetchProgress(type, targetId);
  } catch {
    // Не вошёл, нет сети, старая сборка бэкенда — работаем по телефону.
  }

  // ⚠️ `completed` сюда НЕ передаётся намеренно.
  //
  // Досмотренное видео и так отсекается порогом `RESUME_MAX_RATIO`:
  // бэкенд ставит `completed` ровно тогда, когда длительность
  // известна и позиция near конца. Отдельная ветка была бы вторым
  // правилом о том же — и они разошлись бы при первом же изменении
  // порога.
  return resumePosition(newer(local, server));
}
