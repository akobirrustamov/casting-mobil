/**
 * Плеер: выбор адреса и сообщение о сбое.
 *
 * <h2>Почему эти два решения проверяются тестом</h2>
 * Оба ломаются ТИХО. Неверно склеенный адрес даёт чёрный прямоугольник без
 * единой ошибки; необработанный сбой — тот же чёрный прямоугольник. Ни то,
 * ни другое не видно ни в логах, ни в типах.
 *
 * Раньше контракт стерёг тест бэкенда, читающий исходник `Player.tsx`
 * (`CdnUrlTest.MobileContract`). Он и сейчас нужен — ловит потерю правки при
 * слиянии, — но проверяет ТЕКСТ, а не поведение. С появлением jest в мобильном
 * проекте поведение можно проверить здесь.
 */

/** Держим адрес, с которым создали плеер, и обработчики его событий. */
const created: { source: unknown } = { source: null };
const handlers: Record<string, (payload: never) => void> = {};

jest.mock('expo', () => ({
  useEventListener: (_player: unknown, event: string, handler: (p: never) => void) => {
    handlers[event] = handler;
  },
}));

jest.mock('expo-video', () => ({
  VideoView: 'VideoView',
  useVideoPlayer: (source: unknown) => {
    created.source = source;
    return { loop: false, duration: 0, currentTime: 0 };
  },
}));

jest.mock('@/features/analytics/api', () => ({
  trackContentPlay: jest.fn(),
  trackContentComplete: jest.fn(),
}));

// Позиция просмотра — своя история со своими тестами, здесь она только шумит.
jest.mock('../useWatchProgress', () => ({ useWatchProgress: () => undefined }));

jest.mock('@/lib/api', () => ({
  BASE_URL: 'https://uzcasting.com',
  authHeaders: () => ({ Authorization: 'Bearer t0ken' }),
}));

import { act, create } from 'react-test-renderer';

import { Player, playbackSource } from '../Player';

import type { VideoSource } from '../types';

function source(over: Partial<VideoSource> = {}): VideoSource {
  return {
    partNumber: 1,
    mediaId: 7,
    url: '/api/v1/app/media/7/raw',
    hlsUrl: null,
    durationSeconds: 120,
    ...over,
  };
}

beforeEach(() => {
  created.source = null;
  for (const key of Object.keys(handlers)) delete handlers[key];
});

describe('playbackSource — какой из двух путей открыт', () => {
  /**
   * ⚠️ Боевой контур приходит именно так: плейлист отдаёт наш сервер, билет
   * лежит в адресе. Без `BASE_URL` получился бы путь без домена.
   */
  it('Относительный hlsUrl склеивается с BASE_URL', () => {
    const s = playbackSource(
      source({ hlsUrl: '/api/v1/app/media/7/hls/master.m3u8?t=abc' })
    );

    expect(s.uri).toBe('https://uzcasting.com/api/v1/app/media/7/hls/master.m3u8?t=abc');
  });

  /**
   * ⚠️ Ровно та тихая поломка, ради которой различие вообще существует:
   * `https://uzcasting.comhttps://cdn…` не откроется и ничего не скажет.
   */
  it('Абсолютный hlsUrl остаётся как есть', () => {
    const s = playbackSource(
      source({ hlsUrl: 'https://cdn.uzcasting.com/videos/7/hls/master.m3u8' })
    );

    expect(s.uri).toBe('https://cdn.uzcasting.com/videos/7/hls/master.m3u8');
  });

  /**
   * ⚠️ Заголовок на пути HLS означал бы, что видео не откроется ВОВСЕ:
   * плеер добавил бы его и к запросу сегмента, а хранилище отвергает запрос
   * сразу с `Authorization` и подписью в адресе.
   */
  it('На путь HLS токен не уходит', () => {
    const s = playbackSource(
      source({ hlsUrl: '/api/v1/app/media/7/hls/master.m3u8?t=abc' })
    );

    expect('headers' in s).toBe(false);
  });

  /** А на старом пути он обязателен: право проверяет сервер по нему. */
  it('Без hlsUrl играем через сервер и с токеном', () => {
    const s = playbackSource(source({ hlsUrl: null }));

    expect(s).toEqual({
      uri: 'https://uzcasting.com/api/v1/app/media/7/raw',
      headers: { Authorization: 'Bearer t0ken' },
    });
  });
});

describe('Сбой воспроизведения виден снаружи', () => {
  async function render(onError: (m: string | null) => void) {
    await act(async () => {
      create(
        <Player
          source={source({ hlsUrl: '/api/v1/app/media/7/hls/master.m3u8?t=abc' })}
          orientation="LANDSCAPE"
          contentId={1}
          episodeId={null}
          onError={onError}
        />
      );
    });
  }

  /**
   * ⚠️ Главный случай: подпись сегмента живёт около трёх часов, билет — шесть.
   * Фильм на паузе с вечера упирается ровно в это, и без сообщения наружу
   * экран показал бы чёрный прямоугольник навсегда.
   */
  it('Статус error поднимается на экран', async () => {
    const onError = jest.fn();
    await render(onError);

    act(() =>
      handlers.statusChange({
        status: 'error',
        error: { message: 'Source error' },
      } as never)
    );

    expect(onError).toHaveBeenCalledWith('Source error');
  });

  /**
   * ⚠️ Поле `error` необязательное — часть сбоев платформа присылает без него.
   * Условие «есть error» пропускало бы их молча, поэтому смотрим на статус.
   */
  it('Ошибка без текста — всё равно ошибка', async () => {
    const onError = jest.fn();
    await render(onError);

    act(() => handlers.statusChange({ status: 'error' } as never));

    expect(onError).toHaveBeenCalledWith(null);
  });

  /** Обычная загрузка не должна выглядеть как сбой. */
  it('Готовность и загрузка сбоем не считаются', async () => {
    const onError = jest.fn();
    await render(onError);

    act(() => handlers.statusChange({ status: 'loading' } as never));
    act(() => handlers.statusChange({ status: 'readyToPlay' } as never));

    expect(onError).not.toHaveBeenCalled();
  });
});
