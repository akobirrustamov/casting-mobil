/**
 * Лента «Izohlar» и разбор ответа сервера.
 *
 * <h2>Что здесь ломается тихо</h2>
 * <ul>
 *   <li>Гостю показывается поле ввода — он пишет абзац, жмёт «отправить»
 *       и получает 401 вместо комментария.</li>
 *   <li>Кнопка «удалить» появляется у ЧУЖОГО комментария.</li>
 *   <li>Старый сервер отвечает 401/HTML — экран пишет «комментариев нет»,
 *       хотя на самом деле их просто негде взять.</li>
 * </ul>
 */

let mockSignedIn = false;
let mockComments: Record<string, unknown>;
const mockMutate = jest.fn();

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-image', () => ({ Image: 'Image' }));
jest.mock('expo-router', () => ({ router: { push: jest.fn(), back: jest.fn() } }));
jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));
jest.mock('react-native-safe-area-context', () => ({
  useSafeAreaInsets: () => ({ top: 44, bottom: 34, left: 0, right: 0 }),
}));
jest.mock('@/components/ui/Screen', () => ({
  Screen: ({ children }: { children: React.ReactNode }) => children,
}));
jest.mock('@/components/states/ScreenState', () => {
  const { Text } = jest.requireActual('react-native');
  return {
    ScreenState: ({ kind, body }: { kind: string; body?: string }) => (
      <Text>{`state:${kind}:${body ?? ''}`}</Text>
    ),
  };
});
jest.mock('@/features/home/api', () => ({ useContentCard: () => ({ title: 'Film' }) }));
jest.mock('@/lib/network', () => ({ useIsOffline: () => false }));
jest.mock('@/features/auth/store', () => ({
  useAuthStore: (select: (s: { token: string | null }) => unknown) =>
    select({ token: mockSignedIn ? 't' : null }),
}));

// Настоящий `../api` нужен ради разборщиков ответа, а его зависимости —
// нет: `watch/api` тянет за собой инициализацию i18n, `lib/api` — axios.
jest.mock('@/features/watch/api', () => ({ useViewerKey: () => 'guest' }));
jest.mock('@/lib/api', () => ({
  api: { get: jest.fn(), post: jest.fn(), delete: jest.fn() },
}));

jest.mock('../api', () => {
  const actual = jest.requireActual('../api');
  return {
    ...actual,
    useComments: () => mockComments,
    useDeleteComment: () => ({ mutate: mockMutate }),
    usePostComment: () => ({
      mutate: jest.fn(),
      reset: jest.fn(),
      isPending: false,
      isError: false,
      error: null,
    }),
  };
});

import { act, create, type ReactTestRenderer } from 'react-test-renderer';
import { Text, TextInput } from 'react-native';

import { CommentsUnavailableError, mapComment, mapCommentPage } from '../api';
import { CommentsScreen, openComments } from '../CommentsScreen';

function page(items: unknown[]) {
  return {
    data: { pages: [{ items, page: 0, totalItems: items.length, hasMore: false }] },
    error: null,
    isPending: false,
    isError: false,
    isRefetching: false,
    isFetchingNextPage: false,
    hasNextPage: false,
    refetch: jest.fn(),
    fetchNextPage: jest.fn(),
  };
}

const mine = {
  id: 1,
  text: 'Mening izohim',
  createdAt: null,
  authorName: 'Men',
  authorAvatarUrl: null,
  mine: true,
  hidden: true,
};
const theirs = {
  ...mine,
  id: 2,
  text: 'Begona izoh',
  authorName: 'Aziz',
  mine: false,
  hidden: false,
};

function render(): ReactTestRenderer {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(<CommentsScreen contentId={42} />);
  });
  return tree;
}

const texts = (tree: ReactTestRenderer) =>
  tree.root.findAllByType(Text).map((n) => String([n.props.children].flat().join('')));

const deleteButtons = (tree: ReactTestRenderer) =>
  tree.root.findAll(
    (n) =>
      n.props?.accessibilityLabel === 'comments.delete' &&
      typeof n.props?.onPress === 'function'
  );

beforeEach(() => {
  mockSignedIn = false;
  mockMutate.mockClear();
  mockComments = page([mine, theirs]);
});

describe('разбор ответа', () => {
  it('комментарий без id или текста выбрасывается, флаги читаются строго', () => {
    const list = mapCommentPage({
      items: [{ id: 1, text: 'ok', mine: 'yes' }, { id: 2 }, { text: 'без id' }],
      page: 0,
      totalItems: 3,
      hasMore: false,
    });

    expect(list.items).toHaveLength(1);
    // Строка «yes» — не true: «удалить» у чужого не должно появиться.
    expect(list.items[0].mine).toBe(false);
  });

  it('index.html старой сборки — «недоступно», а не пустая лента', () => {
    expect(() => mapCommentPage('<!doctype html>')).toThrow(CommentsUnavailableError);
    expect(() => mapCommentPage({ totalItems: 0 })).toThrow(CommentsUnavailableError);
  });

  it('mapComment не падает на null', () => {
    expect(mapComment(null)).toBeNull();
  });
});

describe('лента', () => {
  it('адрес страницы — идентификатор контента', () => {
    const { router } = jest.requireMock('expo-router') as { router: { push: jest.Mock } };
    openComments(42);
    expect(router.push).toHaveBeenCalledWith('/comments/42');
  });

  it('гость читает, но вместо поля ввода — кнопка входа', () => {
    const tree = render();

    expect(texts(tree)).toContain('Begona izoh');
    expect(texts(tree)).toContain('comments.signInToComment');
    expect(tree.root.findAllByType(TextInput)).toHaveLength(0);
  });

  it('вошедший видит поле ввода', () => {
    mockSignedIn = true;
    const tree = render();

    expect(tree.root.findAllByType(TextInput)).toHaveLength(1);
  });

  it('«удалить» только у своего, скрытый модератором помечен', () => {
    const tree = render();

    expect(deleteButtons(tree)).toHaveLength(1);
    expect(texts(tree)).toContain('comments.hidden');
  });

  it('старый сервер — честное «ещё не включено»', () => {
    mockComments = {
      ...page([]),
      data: undefined,
      isError: true,
      error: new CommentsUnavailableError(),
    };

    expect(texts(render())).toContain('state:empty:comments.unavailable');
  });

  it('старый сервер — поля ввода нет: писать абзац, который не уйдёт, незачем', () => {
    mockSignedIn = true;
    mockComments = {
      ...page([]),
      data: undefined,
      isError: true,
      error: new CommentsUnavailableError(),
    };

    expect(render().root.findAllByType(TextInput)).toHaveLength(0);
  });

  it('пустая лента — приглашение написать первым', () => {
    mockComments = page([]);

    expect(texts(render())).toContain('comments.empty');
  });
});
