/**
 * Кнопка «Tomosha qilish» на странице контента.
 *
 * <h2>Что здесь ломается тихо</h2>
 * У фильма и у сериала кнопка ВЫГЛЯДИТ одинаково — на макете это одна и
 * та же надпись. Делает она при этом разное: фильму включает плеер,
 * сериалу открывает список серий. Перепутать их местами — значит либо
 * показать пустой плеер там, где серий десять, либо увести человека в
 * список из одной строки.
 *
 * Ни то, ни другое не даёт ошибки: экран просто ведёт себя не так.
 *
 * Отдельно проверяется, что у ЗАКРЫТОГО контента кнопки нет вовсе.
 * Она бы обещала то, чего не сделает: право на просмотр считает сервер,
 * а цена и путь к покупке живут в `LockedPanel`.
 */

const mockPush = jest.fn();

// ⚠️ Префикс `mock` обязателен: фабрика `jest.mock` поднимается выше
// объявлений, и babel пропускает в неё только такие имена.
let mockDetailData: Record<string, unknown> | undefined;
let mockWatchData: Record<string, unknown> | undefined;
let mockWatchError: unknown = null;

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-image', () => ({ Image: 'Image' }));
jest.mock('expo-linear-gradient', () => ({ LinearGradient: 'LinearGradient' }));
jest.mock('expo-linking', () => ({ createURL: (p: string) => `uzcasting:/${p}` }));
jest.mock('expo-router', () => ({
  router: { push: (...a: unknown[]) => mockPush(...a), back: jest.fn() },
  // Окно плеера видно только на странице в фокусе — в тесте она всегда в нём.
  useIsFocused: () => true,
}));

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

jest.mock('react-native-safe-area-context', () => ({
  useSafeAreaInsets: () => ({ top: 44, bottom: 34, left: 0, right: 0 }),
}));

// Нижние блоки и плитки к развилке отношения не имеют, а тянут за собой
// баланс, донаты и плеер — здесь это лишний шум.
jest.mock('../ContentExtras', () => ({
  CastRail: () => null,
  ScenesRail: () => null,
  TrailerCard: () => null,
}));
const mockStatsRow = jest.fn();
jest.mock('../StatsRow', () => ({
  StatsRow: (props: unknown) => {
    mockStatsRow(props);
    return null;
  },
}));
const mockSerialFallback = jest.fn();
let mockSerialCounters: Record<string, unknown> | undefined;
jest.mock('../serialCounters', () => ({
  useSerialCountersFallback: (...args: unknown[]) => {
    mockSerialFallback(...args);
    return mockSerialCounters;
  },
}));
jest.mock('../PlayerActions', () => ({ PlayerActions: () => null }));
jest.mock('../LockedPanel', () => ({ LockedPanel: () => null }));

jest.mock('@/features/watch/Player', () => ({
  Player: () => null,
  playbackSource: (s: { url: string }) => ({ uri: s.url }),
}));

jest.mock('@/components/states/ScreenState', () => ({ ScreenState: () => null }));
jest.mock('@/features/analytics/api', () => ({ trackContentView: jest.fn() }));
let mockCard: Record<string, unknown> | undefined;
jest.mock('@/features/home/api', () => ({
  useHomeFeed: () => ({ data: undefined }),
  contentCards: () => [],
  useContentCard: () => mockCard,
}));
jest.mock('@/lib/api', () => ({ mediaUrl: () => undefined }));
jest.mock('@/lib/network', () => ({ useIsOffline: () => false }));

jest.mock('@/features/auth/store', () => ({
  useAuthStore: (
    select: (s: { isAuthorized: boolean; token: string | null }) => unknown
  ) => select({ isAuthorized: false, token: null }),
}));

jest.mock('@/features/favorites/content', () => ({
  useIsContentSaved: () => false,
  useContentFavorites: (select: (s: Record<string, unknown>) => unknown) =>
    select({ toggle: jest.fn(), load: jest.fn(), isLoaded: true }),
}));

class ContentIsMultiPartError extends Error {}
class ContentNotFoundError extends Error {}
class WatchUnavailableError extends Error {}

jest.mock('@/features/watch/api', () => ({
  ContentIsMultiPartError: class extends Error {},
  ContentNotFoundError: class extends Error {},
  WatchUnavailableError: class extends Error {},
  useWatchContent: () => ({
    data: mockWatchData,
    error: mockWatchError,
    isPending: false,
    isRefetching: false,
    refetch: jest.fn().mockResolvedValue(undefined),
  }),
}));

let mockDetailError: unknown = null;
let mockDetailPending = false;
let mockServerLacksDetail = false;
jest.mock('../detail', () => ({
  ContentDetailUnavailableError: class extends Error {},
  serverLacksContentDetail: () => mockServerLacksDetail,
  useContentDetail: () => ({
    data: mockDetailData,
    error: mockDetailError,
    isPending: mockDetailPending,
    isRefetching: false,
    refetch: jest.fn().mockResolvedValue(undefined),
  }),
}));

import { act, create, type ReactTestRenderer } from 'react-test-renderer';
import { Text } from 'react-native';

import { resetPushOnceForTests } from '@/lib/navigation';

import { ContentScreen } from '../ContentScreen';

// ⚠️ Классы ошибок берём ИЗ мока: `instanceof` в экране сравнивает с
// тем, что импортировал он, а не с объявленным здесь.
const watchApi = jest.requireMock('@/features/watch/api') as {
  ContentIsMultiPartError: typeof ContentIsMultiPartError;
  ContentNotFoundError: typeof ContentNotFoundError;
  WatchUnavailableError: typeof WatchUnavailableError;
};

function detail(over: Record<string, unknown> = {}) {
  return {
    id: 42,
    slug: null,
    title: 'Qalbing egasi',
    shortDescription: null,
    description: null,
    contentType: 'MOVIE',
    structureType: 'SINGLE',
    orientation: 'LANDSCAPE',
    accessPolicy: 'FREE',
    ageRating: '16+',
    year: 2024,
    language: 'uz',
    durationSeconds: 5400,
    episodeCount: null,
    seasonCount: null,
    posterMediaId: null,
    coverMediaId: null,
    trailerMediaId: null,
    galleryMediaIds: [],
    genres: [],
    viewCount: 0,
    likeCount: 0,
    commentCount: 0,
    starsReceived: 0,
    liked: false,
    cast: [],
    ...over,
  };
}

function watch(over: Record<string, unknown> = {}) {
  return {
    episodeId: null,
    contentId: 42,
    episodeNumber: null,
    durationSeconds: 5400,
    title: 'Qalbing egasi',
    orientation: 'LANDSCAPE',
    allowed: true,
    reason: 'FREE',
    requiredAction: 'NONE',
    episodePrice: null,
    premierePrice: null,
    showAds: false,
    trailer: null,
    viewCount: 0,
    likeCount: 0,
    liked: false,
    sources: [
      {
        partNumber: null,
        mediaId: 5,
        url: '/api/v1/app/media/5/raw',
        hlsUrl: null,
        durationSeconds: 5400,
      },
    ],
    ...over,
  };
}

function render(): ReactTestRenderer {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(<ContentScreen contentId={42} />);
  });
  return tree;
}

/**
 * Кнопка с надписью «content.watch».
 *
 * ⚠️ Ищется по подписи, а не по типу: NativeWind подменяет `Pressable`
 * своей обёрткой, и `findByType` не находит ничего.
 */
function watchButton(tree: ReactTestRenderer) {
  const labelled = tree.root
    .findAllByType(Text)
    .filter((n) => String(n.props.children) === 'content.watch');

  if (labelled.length === 0) return null;

  return tree.root.find(
    (node) =>
      node.props?.accessibilityRole === 'button' &&
      typeof node.props?.onPress === 'function' &&
      node.findAllByType(Text).some((n) => String(n.props.children) === 'content.watch')
  );
}

/**
 * Окно полноэкранного плеера.
 *
 * Ищется по `onRequestClose` + `supportedOrientations`: это единственное
 * окно страницы, которому разрешён ландшафт, — окна донатов живут только
 * в портрете.
 */
function playerWindow(tree: ReactTestRenderer) {
  return (
    tree.root.findAll(
      (node) =>
        typeof node.props?.onRequestClose === 'function' &&
        Array.isArray(node.props?.supportedOrientations)
    )[0] ?? null
  );
}

beforeEach(() => {
  // Замок от повторных нажатий общий на модуль — без сброса каждый тест
  // после первого упирался бы в «только что уже переходили».
  resetPushOnceForTests();
  mockPush.mockClear();
  mockStatsRow.mockClear();
  mockSerialFallback.mockClear();
  mockSerialCounters = undefined;
  mockDetailError = null;
  mockDetailPending = false;
  mockServerLacksDetail = false;
  mockCard = undefined;
  mockDetailData = undefined;
  mockWatchData = undefined;
  mockWatchError = null;
});

describe('развилка «фильм или сериал»', () => {
  it('у сериала кнопка ведёт в список серий, а не в плеер', () => {
    mockDetailData = detail({ structureType: 'SEASONAL', contentType: 'SERIES' });
    // У многосерийного `/watch/content/{id}` отвечает «спрашивай серию».
    mockWatchError = new watchApi.ContentIsMultiPartError();

    const tree = render();
    act(() => watchButton(tree)?.props.onPress());

    expect(mockPush).toHaveBeenCalledWith('/episodes/42');
  });

  /**
   * Сериал, у которого ещё нет ни одной опубликованной серии.
   *
   * ⚠️ Раньше кнопка вела в пустой список — и со стороны это выглядело
   * как «видео загружено, но не открывается» (10.09.2026).
   */
  it('у сериала без серий вместо кнопки — «скоро», и никуда не ведёт', () => {
    mockDetailData = detail({
      structureType: 'EPISODIC',
      contentType: 'SERIES',
      episodeCount: 0,
    });
    mockWatchError = new watchApi.ContentIsMultiPartError();

    const tree = render();

    expect(watchButton(tree)).toBeNull();
    expect(
      tree.root
        .findAllByType(Text)
        .some((n) => String(n.props.children) === 'content.episodesSoon')
    ).toBe(true);
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('число серий неизвестно (старый сервер) — кнопка ведёт в список, как раньше', () => {
    mockDetailData = detail({
      structureType: 'EPISODIC',
      contentType: 'SERIES',
      episodeCount: null,
    });
    mockWatchError = new watchApi.ContentIsMultiPartError();

    const tree = render();
    act(() => watchButton(tree)?.props.onPress());

    expect(mockPush).toHaveBeenCalledWith('/episodes/42');
  });

  it('старый сервер без structureType — развилку решает отказ /watch', () => {
    // Карточки нет вовсе: на старой сборке адреса `/content/{id}` нет.
    mockDetailData = undefined;
    mockWatchError = new watchApi.ContentIsMultiPartError();

    const tree = render();
    act(() => watchButton(tree)?.props.onPress());

    expect(mockPush).toHaveBeenCalledWith('/episodes/42');
  });

  it('у фильма кнопка никуда не уводит — открывает плеер во весь экран', () => {
    mockDetailData = detail();
    mockWatchData = watch();

    const tree = render();
    const button = watchButton(tree);
    expect(button).not.toBeNull();

    // До нажатия окно плеера закрыто: иначе видео начинало бы грузиться
    // от одного открытия страницы.
    expect(playerWindow(tree)?.props.visible).toBe(false);

    act(() => button?.props.onPress());

    expect(mockPush).not.toHaveBeenCalled();
    // Плеер открылся ОКНОМ поверх страницы (требование от 10.09.2026), а
    // не отдельным маршрутом: `/watch` уже получен, спрашивать его заново
    // незачем.
    expect(playerWindow(tree)?.props.visible).toBe(true);
  });
});

describe('закрытый контент', () => {
  it('кнопки «смотреть» нет — она обещала бы то, чего не сделает', () => {
    mockDetailData = detail({ accessPolicy: 'PREMIUM_ONLY' });
    mockWatchData = watch({
      allowed: false,
      reason: 'PAYMENT_REQUIRED',
      requiredAction: 'BUY_OR_SUBSCRIBE',
      sources: [],
      premierePrice: 15000,
    });

    expect(watchButton(render())).toBeNull();
  });
});

/**
 * Сериал на сборке бэкенда без `/content/{id}` (боевой сервер, 10.09.2026).
 *
 * ⚠️ «Нравится» не было видно, пока его не нажмёшь: у сериала не
 * оставалось ни одного источника числа. Теперь оно берётся из ответа
 * первой серии — но ТОЛЬКО для плиток: доступ к первой серии ничего не
 * говорит о сериале целиком.
 */
describe('счётчики сериала на старом сервере', () => {
  const detailMock = jest.requireMock('../detail') as {
    ContentDetailUnavailableError: new () => Error;
  };

  it('плитки получают «нравится» из серии, а замок страницы — нет', () => {
    mockDetailData = undefined;
    mockDetailError = new detailMock.ContentDetailUnavailableError();
    mockWatchError = new watchApi.ContentIsMultiPartError();
    // Первая серия платная: для неё `allowed: false`.
    mockSerialCounters = watch({
      likeCount: 7,
      liked: true,
      allowed: false,
      sources: [],
    });

    const tree = render();

    expect(mockSerialFallback).toHaveBeenLastCalledWith(42, true);
    const props = mockStatsRow.mock.calls.at(-1)?.[0] as { info?: { likeCount: number } };
    expect(props.info?.likeCount).toBe(7);
    // Кнопка на месте: закрытая первая серия не закрывает весь сериал.
    expect(watchButton(tree)).not.toBeNull();
  });

  /**
   * ⚠️ Скорость: сервер уже отказал в карточке на прошлом сериале. Ждать
   * отказа ещё раз незачем — обходная дорога стартует сразу, пока
   * карточка и `/watch` ещё в пути.
   */
  it('сервер уже известен как старый — обходная дорога стартует сразу', () => {
    mockServerLacksDetail = true;
    mockDetailPending = true;
    mockCard = { id: 42, title: 'Serial', episodeCount: 12 };
    mockWatchData = watch();

    render();

    expect(mockSerialFallback).toHaveBeenLastCalledWith(42, true);
  });

  it('фильм на старом сервере — обходной дороги нет: у него есть /watch', () => {
    mockServerLacksDetail = true;
    mockCard = { id: 42, title: 'Film', episodeCount: null };
    mockWatchData = watch();

    render();

    expect(mockSerialFallback).toHaveBeenLastCalledWith(42, false);
  });

  it('на новом сервере обходной путь не включается вовсе', () => {
    mockDetailData = detail({ structureType: 'EPISODIC', likeCount: 3 });
    mockWatchError = new watchApi.ContentIsMultiPartError();

    render();

    expect(mockSerialFallback).toHaveBeenLastCalledWith(42, false);
  });
});
