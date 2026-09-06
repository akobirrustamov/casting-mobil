/**
 * Выбор качества видео.
 *
 * <h2>Зачем он вообще нужен</h2>
 * Лестница у нас три ступени: 1080p ≈ 5,5 Мбит/с, 720p ≈ 3,2, 480p ≈ 1,4.
 * Плеер переключает их сам — но по СКОРОСТИ сети, а не по её цене. На
 * хорошем 4G он честно возьмёт 1080p и съест мобильный трафик там, где
 * человек предпочёл бы 480p.
 *
 * <h2>⚠️ Ручной выбор ОТКЛЮЧАЕТ подстройку</h2>
 * Ступень — это отдельный плейлист. Указав плееру его, мы отдаём ему один
 * вариант, и при просадке сети он не спустится ниже, а начнёт буферизацию.
 * Поэтому «Авто» остаётся значением по умолчанию, а выбор ступени — это
 * осознанный размен: предсказуемый трафик вместо предсказуемой картинки.
 *
 * Правильное решение — отдавать урезанный мастер-плейлист с сервера
 * (`master.m3u8?max=720`), тогда подстройка сохраняется внутри лимита.
 * Это работа на бэкенде, здесь её нет.
 *
 * <h2>Откуда берётся список</h2>
 * Из самого плеера: событие `sourceLoad` приносит `availableVideoTracks`
 * с готовыми адресами. Разбирать плейлист руками не нужно — и не надо:
 * плеер уже сделал это и учёл, какие кодеки поддерживает устройство.
 */

/** Что выбрал человек: `auto` или высота кадра в пикселях. */
export type Quality = 'auto' | number;

/** Одна ступень лестницы. */
export type Rung = {
  /** Высота кадра — по ней ступень и называется: 720 → «720p». */
  height: number;
  /** Адрес плейлиста этой ступени. */
  url: string;
  /** Пиковый битрейт, бит/с. `null` — плеер не сообщил. */
  peakBitrate: number | null;
};

/**
 * То, что нам нужно от `VideoTrack` из expo-video.
 *
 * Свой тип, а не импорт: так модуль можно проверить тестом, не поднимая
 * нативный плеер, и видно, на какие ровно поля мы опираемся.
 */
export type TrackLike = {
  url?: string | null;
  size?: { width: number; height: number } | null;
  peakBitrate?: number | null;
  bitrate?: number | null;
  isSupported?: boolean;
};

/**
 * Ступени, между которыми есть смысл выбирать.
 *
 * Отбрасываются:
 * <ul>
 *   <li>дорожки без адреса — переключаться некуда (не-HLS источник);</li>
 *   <li>неподдерживаемые устройством — выбор привёл бы к чёрному кадру;</li>
 *   <li>повторы по высоте — на экране две одинаковые кнопки «720p».</li>
 * </ul>
 *
 * ⚠️ Меньше двух ступеней — пустой список. Выбирать не из чего, и меню
 * «Авто / 480p» только запутывает: оно обещает выбор, которого нет.
 */
export function rungs(tracks: readonly TrackLike[] | null | undefined): Rung[] {
  if (!tracks || tracks.length === 0) return [];

  const byHeight = new Map<number, Rung>();

  for (const t of tracks) {
    const url = typeof t.url === 'string' && t.url !== '' ? t.url : null;
    const height = t.size?.height ?? 0;

    if (url === null || height <= 0 || t.isSupported === false) continue;
    if (byHeight.has(height)) continue;

    byHeight.set(height, {
      height,
      url,
      peakBitrate: t.peakBitrate ?? t.bitrate ?? null,
    });
  }

  if (byHeight.size < 2) return [];

  return [...byHeight.values()].sort((a, b) => b.height - a.height);
}

/**
 * Адрес выбранной ступени или `null` — играем мастер-плейлист.
 *
 * ⚠️ Неизвестная высота тоже даёт `null`, а не ближайшую ступень. Такое
 * бывает: человек выбрал 1080p на одном фильме, а у следующего её нет.
 * Молча подставить 720p — значит соврать подписи под кнопкой; честнее
 * вернуться к «Авто», где качество и так подберётся.
 */
export function variantUrl(list: readonly Rung[], quality: Quality): string | null {
  if (quality === 'auto') return null;
  return list.find((r) => r.height === quality)?.url ?? null;
}

/**
 * Осталась ли выбранная ступень доступной.
 *
 * Нужна экрану, чтобы подсветить «Авто», когда выбранной высоты у этого
 * видео нет — иначе кнопка «1080p» выглядела бы нажатой, а играло бы
 * автоматическое качество.
 */
export function effective(list: readonly Rung[], quality: Quality): Quality {
  if (quality === 'auto') return 'auto';
  return list.some((r) => r.height === quality) ? quality : 'auto';
}

/** Подпись ступени: 720 → «720p». Для «Авто» подпись берётся из переводов. */
export function rungLabel(height: number): string {
  return `${height}p`;
}

/**
 * Значение для журнала просмотра.
 *
 * Сервер хранит это поле у записи «продолжить просмотр»
 * (`app/watch-progress`), и до сих пор туда всегда уходило `auto` —
 * выбирать было нечем.
 */
export function qualityTag(quality: Quality): string {
  return quality === 'auto' ? 'auto' : rungLabel(quality);
}

/**
 * Разбор сохранённого выбора.
 *
 * ⚠️ Всё, что не похоже на высоту кадра, — «Авто». В хранилище может
 * лежать что угодно: значение из старой версии приложения, обрезанная
 * запись, чужой ключ. Строгий разбор здесь дешевле, чем плеер, которому
 * подсунули `NaN`.
 */
export function parseQuality(raw: string | null): Quality {
  if (raw === null || raw === 'auto') return 'auto';

  const height = Number.parseInt(raw, 10);
  return Number.isFinite(height) && height > 0 ? height : 'auto';
}
