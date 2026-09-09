/**
 * Трейлер на закрытом экране.
 *
 * <h2>Что здесь ломается тихо</h2>
 * Ролик показывается ТЕМ ЖЕ плеером, что и фильм. Значит по недосмотру он
 * унесёт с собой и позицию просмотра, и счётчик запусков контента: человек
 * посмотрит 90 секунд рекламы — и «продолжить просмотр» отправит его на
 * 1:30 фильма, которого он не покупал, а в отчётах появится просмотр.
 *
 * Ни то, ни другое не видно на экране. Поэтому проверяется не картинка, а
 * то, ЧТО передано плееру.
 */

const playerProps: Record<string, unknown>[] = [];

jest.mock('../Player', () => ({
  Player: (props: Record<string, unknown>) => {
    playerProps.push(props);
    return null;
  },
  // Экран зовёт её, чтобы понять, сменился ли адрес видео.
  playbackSource: (s: { hlsUrl: string | null; url: string }) => ({
    uri: s.hlsUrl ?? s.url,
  }),
}));

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-image', () => ({ Image: 'Image' }));
jest.mock('expo-router', () => ({ router: { back: jest.fn(), push: jest.fn() } }));

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: '3rdParty', init: () => undefined },
}));

jest.mock('@/components/ui/Screen', () => ({ Screen: 'Screen' }));
jest.mock('@/components/ui/Badge', () => ({ Badge: 'Badge' }));
jest.mock('@/components/ui/Button', () => ({ Button: 'Button' }));
jest.mock('@/components/states/ScreenState', () => ({ ScreenState: 'ScreenState' }));

jest.mock('@/features/analytics/api', () => ({ trackContentView: jest.fn() }));
jest.mock('@/features/content/railLayout', () => ({ CARD_RATIO: 2 / 3 }));
jest.mock('@/features/home/api', () => ({ useHomeFeed: () => ({ data: undefined }), contentCards: () => [] }));
jest.mock('@/lib/api', () => ({ mediaUrl: () => undefined }));
jest.mock('@/lib/network', () => ({ useIsOffline: () => false }));
jest.mock('@/lib/money', () => ({
  formatSum: (n: number) => String(n),
  groupDigits: (n: number) => String(n),
}));

// StatChips спрашивает, вошёл ли человек: без мока сюда приезжает
// expo-secure-store, которого в jest нет.
jest.mock('@/features/auth/store', () => ({
  useAuthStore: (select: (s: { token: string | null }) => unknown) =>
    select({ token: null }),
}));

jest.mock('../api', () => ({
  ContentNotFoundError: class ContentNotFoundError extends Error {},
  WatchUnavailableError: class WatchUnavailableError extends Error {},
  setLike: jest.fn(),
}));

import { act, create } from 'react-test-renderer';

import { WatchDetail } from '../WatchDetail';

import type { VideoSource, WatchInfo } from '../types';

function trailer(over: Partial<VideoSource> = {}): VideoSource {
  return {
    partNumber: null,
    mediaId: 42,
    url: '/api/v1/app/media/42/raw',
    hlsUrl: '/api/v1/app/media/42/hls/master.m3u8?t=abc',
    durationSeconds: 90,
    ...over,
  };
}

/** Закрытый платный фильм — то состояние, ради которого всё и делалось. */
function locked(over: Partial<WatchInfo> = {}): WatchInfo {
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

async function render(info: WatchInfo) {
  const query = {
    isPending: false,
    isError: false,
    isRefetching: false,
    data: info,
    error: null,
    refetch: jest.fn().mockResolvedValue({}),
  };

  let tree!: ReturnType<typeof create>;
  await act(async () => {
    tree = create(<WatchDetail query={query as never} />);
  });
  return tree;
}

/**
 * Нажать «Tomosha qilish».
 *
 * ⚠️ Ищем по подписи, а не по типу: NativeWind оборачивает `Pressable`
 * своей обёрткой, и `findByType` не находит ничего. Перевод в тестах
 * подменён на сам ключ, поэтому в подписи именно `content.watch`.
 */
function press(tree: ReturnType<typeof create>, label: string) {
  const button = tree.root.find(
    (node) =>
      node.props?.accessibilityRole === 'button' &&
      typeof node.props?.onPress === 'function' &&
      node.props?.accessibilityLabel === label
  );
  act(() => button.props.onPress());
}

const pressWatch = (tree: ReturnType<typeof create>) => press(tree, 'content.watch');
const pressTrailer = (tree: ReturnType<typeof create>) => press(tree, 'content.trailer');

beforeEach(() => {
  playerProps.length = 0;
  jest.clearAllMocks();
});

/**
 * ⚠️ Ролик тоже НЕ грузится сам.
 *
 * Пока в блоке трейлера сразу стоял плеер, он тянул видео при каждом
 * открытии экрана — ровно то, от чего избавились у основного видео.
 * Два видео на одной карточке — это двойной трафик у человека, который
 * ещё ничего не выбрал.
 */
it('Трейлер не грузится, пока не нажали', async () => {
  await render(locked({ trailer: trailer() }));

  expect(playerProps).toHaveLength(0);
});

it('После нажатия играет ролик', async () => {
  const tree = await render(locked({ trailer: trailer() }));

  pressTrailer(tree);

  expect(playerProps).toHaveLength(1);
  expect(playerProps[0].source).toMatchObject({ mediaId: 42, durationSeconds: 90 });
});

/**
 * ⚠️ Главная проверка файла.
 *
 * Позиция и аналитика привязаны к этим двум полям. Подставь сюда настоящие
 * идентификаторы — и реклама начнёт считаться просмотром фильма, а «продолжить
 * просмотр» станет показывать место, на котором закончился ролик.
 */
it('Трейлер не пишет позицию и не считается просмотром', async () => {
  const tree = await render(locked({ trailer: trailer() }));
  pressTrailer(tree);

  expect(playerProps[0].contentId).toBeNull();
  expect(playerProps[0].episodeId).toBeNull();
});

/** Без ролика плеера нет вовсе — только афиша. */
it('Нет трейлера — плеер не создаётся', async () => {
  await render(locked({ trailer: null }));

  expect(playerProps).toHaveLength(0);
});

/**
 * ⚠️ Открытый контент НЕ начинает играть сам.
 *
 * Заказчик (09.09.2026): «только нажав на кнопку можно получить доступ к
 * видео». Плеер, поднятый сразу, тянет видео у каждого, кто просто
 * заглянул на карточку.
 */
it('Доступ открыт, но кнопку не нажали — фильм не играет', async () => {
  const film: VideoSource = {
    partNumber: 1,
    mediaId: 7,
    url: '/api/v1/app/media/7/raw',
    hlsUrl: '/api/v1/app/media/7/hls/master.m3u8?t=xyz',
    durationSeconds: 5400,
  };

  await render(locked({ allowed: true, reason: 'FREE', trailer: null, sources: [film] }));

  expect(playerProps).toHaveLength(0);
});

/**
 * ⚠️ Открытый контент играет ФИЛЬМ, а не ролик.
 *
 * Проверка на случай, когда трейлер подставят вместо источника: тогда
 * купивший человек получил бы 90 секунд рекламы вместо фильма — и это
 * худший из возможных исходов.
 */
it('После кнопки играет фильм, а не трейлер', async () => {
  const film: VideoSource = {
    partNumber: 1,
    mediaId: 7,
    url: '/api/v1/app/media/7/raw',
    hlsUrl: '/api/v1/app/media/7/hls/master.m3u8?t=xyz',
    durationSeconds: 5400,
  };

  const tree = await render(
    locked({ allowed: true, reason: 'PREMIERE_PURCHASE', trailer: trailer(), sources: [film] })
  );

  // До нажатия не играет ничего: ни фильм, ни ролик.
  expect(playerProps).toHaveLength(0);

  pressWatch(tree);

  // ⚠️ Фильм, а не ролик: подставь сюда трейлер — и купивший человек
  // получил бы 90 секунд рекламы вместо фильма.
  expect(playerProps.map((p) => p.source)).toContainEqual(
    expect.objectContaining({ mediaId: 7 })
  );
  // ⚠️ А вот у ФИЛЬМА идентификаторы настоящие: по ним пишется позиция
  // и считается просмотр. Ролик и фильм различаются именно этим.
  const movie = playerProps.find((p) => (p.source as { mediaId: number }).mediaId === 7);
  expect(movie?.contentId).toBe(13);
});
