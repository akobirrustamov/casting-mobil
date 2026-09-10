/**
 * «Нравится» переживает уход с экрана.
 *
 * <h2>Что здесь ломалось тихо</h2>
 * Ответ сервера ложился только в состояние компонента. Карточка контента
 * при этом лежит в кэше пять минут (`useContentDetail`), и при
 * возвращении на экран показывалась ОНА — со старым `liked: false`.
 *
 * То есть человек ставил лайк, уходил, возвращался — и сердце снова
 * серое. На сервере при этом всё правильно, поэтому баг выглядит как
 * «лайк не работает», хотя он работает. Ни одна проверка сервера этого
 * не поймала бы: ошибка целиком на стороне кэша клиента.
 */

const mockSetLike = jest.fn();

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-image', () => ({ Image: 'Image' }));
jest.mock('expo-router', () => ({ router: { push: jest.fn() } }));

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: '3rdParty', init: () => undefined },
}));

jest.mock('@/features/auth/store', () => ({
  useAuthStore: (select: (s: { token: string | null }) => unknown) =>
    select({ token: 'jwt' }),
}));

jest.mock('@/features/watch/api', () => ({
  setLike: (...a: unknown[]) => mockSetLike(...a),
}));

jest.mock('@/lib/money', () => ({ groupDigits: (n: number) => String(n) }));

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, create, type ReactTestRenderer } from 'react-test-renderer';

import { StatsRow } from '../StatsRow';

import type { ContentDetail } from '../detail';

/** Ключ кэша карточки — язык и зритель в нём есть, и это важно. */
const KEY = ['content-detail', 13, 'uz', 'guest'];

function detail(over: Partial<ContentDetail> = {}): ContentDetail {
  return {
    liked: false,
    likeCount: 7,
    commentCount: null,
    starsReceived: null,
    coinsReceived: null,
    viewCount: null,
  } as ContentDetail;
}

function render(client: QueryClient): ReactTestRenderer {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(
      <QueryClientProvider client={client}>
        <StatsRow contentId={13} detail={detail()} info={undefined} />
      </QueryClientProvider>
    );
  });
  return tree;
}

function heart(tree: ReactTestRenderer) {
  return tree.root.find(
    (node) =>
      node.props?.accessibilityLabel === 'content.likes' &&
      typeof node.props?.onPress === 'function'
  );
}

let client: QueryClient;

beforeEach(() => {
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  mockSetLike.mockReset();
  mockSetLike.mockResolvedValue({ liked: true, likeCount: 8 });
});

// ⚠️ Без очистки QueryClient держит таймеры сборки мусора, и jest
// не завершается после последнего теста.
afterEach(() => client.clear());

/**
 * ⚠️ Главная проверка файла.
 *
 * Кэш обязан узнать про нажатие. Иначе экран, открытый повторно в
 * ближайшие пять минут, покажет ответ, снятый ДО него.
 */
it('Ответ сервера попадает в кэш карточки', async () => {
  client.setQueryData(KEY, detail());

  const tree = render(client);

  await act(async () => {
    await heart(tree).props.onPress();
  });

  expect(mockSetLike).toHaveBeenCalledWith(13, true);
  expect(client.getQueryData(KEY)).toMatchObject({ liked: true, likeCount: 8 });
});

/**
 * ⚠️ Ключ в кэше неполный (`['content-detail', 13]`), и это осознанно:
 * в настоящем есть ещё язык и зритель. Проверка следит, что снимки
 * находятся по префиксу — иначе правка прошла бы мимо и баг вернулся бы
 * молча.
 */
it('Находит снимок, снятый на другом языке', async () => {
  const otherLanguage = ['content-detail', 13, 'ru', 'guest'];
  client.setQueryData(otherLanguage, detail());

  const tree = render(client);

  await act(async () => {
    await heart(tree).props.onPress();
  });

  expect(client.getQueryData(otherLanguage)).toMatchObject({ liked: true, likeCount: 8 });
});

/** Чужой контент трогать нельзя: у него свой счётчик. */
it('Карточку другого контента не трогает', async () => {
  const other = ['content-detail', 99, 'uz', 'guest'];
  client.setQueryData(other, detail());

  const tree = render(client);

  await act(async () => {
    await heart(tree).props.onPress();
  });

  expect(client.getQueryData(other)).toMatchObject({ liked: false, likeCount: 7 });
});

/** Сбой сети не должен оставлять в кэше выдуманное число. */
it('Сбой не портит кэш', async () => {
  mockSetLike.mockRejectedValue(new Error('нет сети'));

  client.setQueryData(KEY, detail());

  const tree = render(client);

  await act(async () => {
    await heart(tree).props.onPress();
  });

  expect(client.getQueryData(KEY)).toMatchObject({ liked: false, likeCount: 7 });
});
