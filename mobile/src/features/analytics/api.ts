import { READ_ONLY, api } from '@/lib/api';

import { deviceKey } from './deviceKey';

/**
 * Отправка событий аналитики (`POST /api/v1/app/analytics/events`).
 *
 * <h2>Зачем это в приложении</h2>
 * В админ-панели есть отчёты по рекламе (показы, клики, CTR) и по контенту
 * (просмотры, досмотры). Считаются они ИСКЛЮЧИТЕЛЬНО из событий, которые
 * присылает клиент: `AnalyticsService.aggregate()` раз в 5 минут сворачивает
 * их в дневные строки. Пока приложение молчит, все эти отчёты показывают
 * нули — не «мало показов», а «никто не сообщил».
 *
 * <h2>Почему события копятся, а не летят по одному</h2>
 * Пролистав главную, человек порождает десяток показов подряд. Отдельный
 * HTTP-запрос на каждый — это батарея и трафик ради телеметрии. Бэкенд сам
 * предлагает пачку: до 50 событий за раз (`MAX_BATCH`).
 *
 * <h2>Аналитика не имеет права ломать экран</h2>
 * Любая ошибка здесь гасится. Ни одно из этих событий не нужно человеку,
 * который смотрит кино, — а вот упавший экран он заметит.
 */

/** Типы, которые понимает бэкенд (`AnalyticsEventType`). */
export type AnalyticsEventType =
  | 'AD_IMPRESSION'
  | 'AD_CLICK'
  | 'CONTENT_VIEW'
  | 'CONTENT_PLAY'
  | 'CONTENT_COMPLETE'
  | 'NOTIFICATION_OPEN'
  | 'NOTIFICATION_CLICK';

type Event = {
  type: AnalyticsEventType;
  /** Реклама — id баннера, контент — id контента. */
  targetId?: number;
  episodeId?: number;
};

/** Столько бэкенд принимает за один запрос — больше он отвергает целиком. */
const MAX_BATCH = 50;

/** Задержка перед отправкой: столько ждём, чтобы собрать пачку. */
const FLUSH_DELAY_MS = 4_000;

/**
 * Предел очереди.
 *
 * Если сети нет долго, события копятся. Без предела список рос бы, пока
 * приложение открыто, и утащил бы память ради телеметрии. Переполнение
 * теряет САМЫЕ СТАРЫЕ: свежие показы полезнее.
 */
const MAX_QUEUE = 200;

/**
 * Выключено на боевой базе.
 *
 * `READ_ONLY` защищает боевую БД сайта от записи во время разработки
 * (`lib/api.ts`). Показы, придуманные отладкой, попали бы в отчёты
 * рекламодателей — то есть аналитика показывала бы фальшивые цифры и никто
 * бы не догадался, что они наши. Против тестового контура события уходят.
 */
export const ANALYTICS_ENABLED = !READ_ONLY;

let queue: Event[] = [];
let timer: ReturnType<typeof setTimeout> | null = null;

/** Отправить накопленное. Вызывается по таймеру и при переполнении пачки. */
export async function flushAnalytics(): Promise<void> {
  if (timer !== null) {
    clearTimeout(timer);
    timer = null;
  }
  if (queue.length === 0) return;

  const batch = queue.slice(0, MAX_BATCH);
  queue = queue.slice(batch.length);

  try {
    await api.post('/api/v1/app/analytics/events', {
      deviceKey: await deviceKey(),
      events: batch,
    });
  } catch {
    // Не достучались — вернём в начало очереди и попробуем со следующей
    // пачкой. Немедленный повтор в цикле при пропавшей сети только грел бы
    // телефон.
    queue = [...batch, ...queue].slice(-MAX_QUEUE);
  }
}

/**
 * Поставить событие в очередь.
 *
 * Ничего не ждёт и ничего не бросает: вызывающий код — это обработчик
 * нажатия или эффект плеера, он не должен знать про сеть.
 */
export function track(event: Event): void {
  if (!ANALYTICS_ENABLED) return;

  queue.push(event);
  if (queue.length > MAX_QUEUE) {
    queue = queue.slice(-MAX_QUEUE);
  }

  if (queue.length >= MAX_BATCH) {
    void flushAnalytics();
    return;
  }

  timer ??= setTimeout(() => {
    void flushAnalytics();
  }, FLUSH_DELAY_MS);
}

/**
 * Показы, уже отправленные в этом запуске.
 *
 * Бэкенд ничего не дедуплицирует — сколько прислали, столько и запишет.
 * Карусель перерисовывается при каждом ререндере главной, и без этой памяти
 * один баннер насчитал бы себе сотни показов за сессию: CTR рекламодателя
 * упал бы в пол на ровном месте.
 */
const seenImpressions = new Set<number>();

/** Баннер показался человеку — не чаще одного раза за запуск приложения. */
export function trackAdImpression(adId: number): void {
  if (seenImpressions.has(adId)) return;
  seenImpressions.add(adId);
  track({ type: 'AD_IMPRESSION', targetId: adId });
}

/** По баннеру нажали. Клики НЕ схлопываются: второй клик — тоже интерес. */
export function trackAdClick(adId: number): void {
  track({ type: 'AD_CLICK', targetId: adId });
}

/** Открыта карточка контента. */
export function trackContentView(contentId: number, episodeId?: number | null): void {
  track({ type: 'CONTENT_VIEW', targetId: contentId, episodeId: episodeId ?? undefined });
}

/** Видео начало играть — это не то же самое, что «открыл карточку». */
export function trackContentPlay(contentId: number, episodeId?: number | null): void {
  track({ type: 'CONTENT_PLAY', targetId: contentId, episodeId: episodeId ?? undefined });
}

/** Досмотрено до конца. */
export function trackContentComplete(contentId: number, episodeId?: number | null): void {
  track({
    type: 'CONTENT_COMPLETE',
    targetId: contentId,
    episodeId: episodeId ?? undefined,
  });
}

/** Только для тестов: очередь и память о показах — состояние модуля. */
export function resetAnalyticsForTests(): void {
  queue = [];
  seenImpressions.clear();
  if (timer !== null) {
    clearTimeout(timer);
    timer = null;
  }
}
