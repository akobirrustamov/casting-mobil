/**
 * Лестница качества.
 *
 * Модуль чистый, и это единственная причина, по которой решения о качестве
 * вынесены из плеера: внутри компонента их нельзя ни прочитать, ни проверить
 * — список ступеней приходит от нативного плеера, и в jest его нет.
 */

import {
  type TrackLike,
  effective,
  parseQuality,
  qualityTag,
  rungs,
  variantUrl,
} from '../quality';

function track(over: Partial<TrackLike> & { height: number }): TrackLike {
  const { height, ...rest } = over;
  return {
    url: `https://uzcasting.com/hls/${height}p/index.m3u8?t=abc`,
    size: { width: Math.round((height * 16) / 9), height },
    peakBitrate: height * 5000,
    isSupported: true,
    ...rest,
  };
}

/** Та самая лестница, что лежит на боевом CDN. */
const LADDER = [track({ height: 480 }), track({ height: 1080 }), track({ height: 720 })];

describe('Из чего собирается меню', () => {
  it('Ступени идут сверху вниз', () => {
    expect(rungs(LADDER).map((r) => r.height)).toEqual([1080, 720, 480]);
  });

  /**
   * ⚠️ Меньше двух ступеней — меню не показываем совсем.
   *
   * «Авто / 480p» обещает выбор, которого нет: обе кнопки играют одно и то
   * же. Это как раз тот случай, где интерфейс врёт, а код формально работает.
   */
  it('Одна ступень — выбирать нечего', () => {
    expect(rungs([track({ height: 720 })])).toEqual([]);
  });

  it('Пусто и не пришло — тоже пусто', () => {
    expect(rungs([])).toEqual([]);
    expect(rungs(null)).toEqual([]);
    expect(rungs(undefined)).toEqual([]);
  });

  /**
   * ⚠️ Без адреса переключаться некуда.
   *
   * Так приходит не-HLS источник: дорожка есть, отдельного плейлиста у неё
   * нет. Кнопка была бы, нажатие не делало бы ничего.
   */
  it('Дорожки без адреса не в счёт', () => {
    const list = rungs([track({ height: 1080, url: null }), track({ height: 720 })]);

    expect(list).toEqual([]);
  });

  /**
   * ⚠️ Неподдерживаемая устройством ступень — чёрный кадр вместо видео.
   * Плеер сам её не выберет, а человек по кнопке — выберет.
   */
  it('Неподдерживаемая ступень скрыта', () => {
    const list = rungs([
      track({ height: 1080, isSupported: false }),
      track({ height: 720 }),
      track({ height: 480 }),
    ]);

    expect(list.map((r) => r.height)).toEqual([720, 480]);
  });

  /** Две дорожки одной высоты дали бы две одинаковые кнопки «720p». */
  it('Повторы по высоте схлопываются', () => {
    const list = rungs([track({ height: 720 }), track({ height: 720 }), track({ height: 480 })]);

    expect(list.map((r) => r.height)).toEqual([720, 480]);
  });
});

describe('Что играем', () => {
  it('Авто — мастер-плейлист, адрес ступени не подставляется', () => {
    expect(variantUrl(rungs(LADDER), 'auto')).toBeNull();
  });

  it('Выбранная ступень — её собственный плейлист', () => {
    expect(variantUrl(rungs(LADDER), 720)).toContain('720p/index.m3u8');
  });

  /**
   * ⚠️ У следующего фильма выбранной высоты может не быть.
   *
   * Молча подставить ближайшую — значит соврать подписи под кнопкой:
   * горит «1080p», играет 720p. Возвращаемся к «Авто», где качество и так
   * подберётся, и кнопка это показывает.
   */
  it('Пропавшая ступень возвращает к «Авто»', () => {
    const shortLadder = rungs([track({ height: 720 }), track({ height: 480 })]);

    expect(variantUrl(shortLadder, 1080)).toBeNull();
    expect(effective(shortLadder, 1080)).toBe('auto');
    expect(effective(shortLadder, 720)).toBe(720);
  });
});

describe('Сохранение выбора', () => {
  /** Запись и чтение должны сходиться — иначе выбор теряется молча. */
  it('Что записали, то и прочитали', () => {
    expect(parseQuality(qualityTag('auto'))).toBe('auto');
    expect(parseQuality(qualityTag(720))).toBe(720);
  });

  /**
   * ⚠️ В хранилище может лежать что угодно: значение от прошлой версии,
   * обрезанная запись, чужой ключ. Плеер, которому подсунули `NaN`,
   * не откроется — а причина будет в строке из SecureStore.
   */
  it('Мусор в хранилище — это «Авто»', () => {
    expect(parseQuality(null)).toBe('auto');
    expect(parseQuality('')).toBe('auto');
    expect(parseQuality('лучшее')).toBe('auto');
    expect(parseQuality('-720p')).toBe('auto');
  });
});
