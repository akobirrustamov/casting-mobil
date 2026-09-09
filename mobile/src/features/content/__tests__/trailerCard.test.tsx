/**
 * Трейлер на карточке контента.
 *
 * <h2>Что здесь ломается тихо</h2>
 * Ролик играет ТЕМ ЖЕ плеером, что и фильм. Значит по недосмотру он
 * унесёт с собой и позицию просмотра, и счётчик запусков: человек
 * посмотрит 90 секунд рекламы — и «продолжить просмотр» отправит его на
 * 1:30 фильма, которого он не покупал, а в отчётах появится просмотр.
 *
 * Ни то, ни другое не видно на экране, поэтому проверяется не картинка,
 * а то, ЧТО передано плееру.
 *
 * ⚠️ Тест переехал сюда из `watch/__tests__/lockedTrailer`. Пока мы с
 * коллегой шли параллельно, карточка контента появилась дважды; живой
 * осталась его — вместе с трейлером. Проверка нужна там, где код, иначе
 * она сторожила бы пустое место.
 */

const playerProps: Record<string, unknown>[] = [];

jest.mock('@/features/watch/Player', () => ({
  Player: (props: Record<string, unknown>) => {
    playerProps.push(props);
    return null;
  },
  playbackSource: (s: { hlsUrl: string | null; url: string }) => ({
    uri: s.hlsUrl ?? s.url,
  }),
}));

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-image', () => ({ Image: 'Image' }));
jest.mock('expo-linking', () => ({ openURL: jest.fn() }));
jest.mock('expo-router', () => ({ router: { push: jest.fn(), back: jest.fn() } }));

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  // ⚠️ Заглушка обязана отдать и `initReactI18next`: `src/i18n` ставит
  // его в `i18n.use(...)` на импорте, и без него падает сам модуль.
  initReactI18next: { type: '3rdParty', init: () => undefined },
}));

// ⚠️ `@/lib/api` тянет за собой обновление токена и стор авторизации:
// без заглушек падает не тест, а импорт модуля.
jest.mock('@/lib/api', () => ({
  mediaUrl: () => undefined,
  setTokenRefresher: () => undefined,
  api: { get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn() },
  READ_ONLY: false,
}));

jest.mock('@/features/auth/store', () => ({
  useAuthStore: (select: (s: { token: string | null }) => unknown) =>
    select({ token: null }),
}));
jest.mock('@/lib/money', () => ({
  formatSum: (n: number) => String(n),
  groupDigits: (n: number) => String(n),
}));

import { act, create, type ReactTestRenderer } from 'react-test-renderer';

import { TrailerCard } from '../ContentExtras';

import type { WatchInfo } from '@/features/watch/types';

const TRAILER = {
  partNumber: null,
  mediaId: 42,
  url: '/api/v1/app/media/42/raw',
  hlsUrl: '/api/v1/app/media/42/hls/master.m3u8?t=abc',
  durationSeconds: 90,
};

function watch(over: Partial<WatchInfo> = {}): WatchInfo {
  return {
    episodeId: null,
    contentId: 13,
    episodeNumber: null,
    durationSeconds: 5400,
    title: 'Film',
    orientation: 'LANDSCAPE',
    allowed: false,
    reason: 'PAYMENT_REQUIRED',
    requiredAction: 'BUY_PREMIERE',
    episodePrice: null,
    premierePrice: 5000,
    showAds: false,
    trailer: null,
    viewCount: null,
    likeCount: null,
    liked: false,
    starsReceived: null,
    coinsReceived: null,
    commentCount: null,
    credits: [],
    sources: [],
    ...over,
  };
}

function render(info: WatchInfo | undefined): ReactTestRenderer {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(<TrailerCard detail={undefined} info={info} />);
  });
  return tree;
}

/**
 * ⚠️ Ищем по роли и обработчику, а не по типу: NativeWind подменяет
 * `Pressable` своей обёрткой, и `findByType` не находит ничего.
 */
function play(tree: ReactTestRenderer) {
  const button = tree.root.find(
    (node) =>
      node.props?.accessibilityRole === 'button' &&
      typeof node.props?.onPress === 'function'
  );
  act(() => button.props.onPress());
}

beforeEach(() => {
  playerProps.length = 0;
});

it('Ролика нет — блока нет совсем', () => {
  const tree = render(watch({ trailer: null }));

  expect(tree.toJSON()).toBeNull();
});

/**
 * ⚠️ Ролик не грузится сам.
 *
 * Плеер, поднятый сразу, тянет видео у каждого, кто просто открыл
 * карточку, — а он там ещё ничего не выбрал.
 */
it('Не грузится, пока не нажали', () => {
  render(watch({ trailer: TRAILER }));

  expect(playerProps).toHaveLength(0);
});

/**
 * ⚠️ Главная проверка файла.
 *
 * Позиция и аналитика привязаны к этим двум полям. Подставь сюда
 * настоящие идентификаторы — и реклама начнёт считаться просмотром
 * фильма, а «продолжить просмотр» станет показывать место, на котором
 * закончился ролик.
 */
it('Трейлер не пишет позицию и не считается просмотром', () => {
  const tree = render(watch({ trailer: TRAILER }));

  play(tree);

  expect(playerProps).toHaveLength(1);
  expect(playerProps[0].source).toMatchObject({ mediaId: 42 });
  expect(playerProps[0].contentId).toBeNull();
  expect(playerProps[0].episodeId).toBeNull();
});
