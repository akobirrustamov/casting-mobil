/**
 * «Скрыть автора» — личный мьют на телефоне.
 *
 * <h2>Зачем это есть</h2>
 * UGC-политика Google просит не только жалобу, но и возможность
 * заблокировать автора. Жалоба уходит модератору и работает на всех,
 * мьют — личная настройка владельца телефона, и сервера она не требует.
 *
 * <h2>Что ломается тихо</h2>
 * <ul>
 *   <li>Скрытие по имени вместо id: тёзки исчезают вместе с нарушителем,
 *       а сам он возвращается, сменив имя в профиле.</li>
 *   <li>Скрытые комментарии исчезают молча — человек думает, что лента
 *       не догрузилась, и снять мьют неоткуда.</li>
 *   <li>Список не переживает перезапуск, потому что его забыли записать
 *       на диск.</li>
 * </ul>
 */

let mockComments: Record<string, unknown>;
const store: Record<string, string> = {};

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-image', () => ({ Image: 'Image' }));
jest.mock('expo-router', () => ({ router: { push: jest.fn(), back: jest.fn() } }));
jest.mock('react-i18next', () => ({
  useTranslation: () => ({
    // Счётчик нужен в самом тексте — иначе не проверить, что он дошёл.
    t: (key: string, params?: { count?: number }) =>
      params?.count === undefined ? key : `${key}:${params.count}`,
  }),
}));
jest.mock('react-native-safe-area-context', () => ({
  useSafeAreaInsets: () => ({ top: 44, bottom: 34, left: 0, right: 0 }),
  // ⚠️ `useSafeAreaFrame` ham SHART: `useKeyboardInset` (`src/lib/keyboard.ts`)
  // oyna balandligini shundan oladi. Zaglushkada bo'lmasa yiqiladigan narsa
  // klaviatura hisobi emas — EKRANNING O'ZI umuman render bo'lmaydi
  // (`useSafeAreaFrame is not a function`), va sabab test nomidan
  // ko'rinmaydi.
  useSafeAreaFrame: () => ({ x: 0, y: 0, width: 400, height: 800 }),
}));
jest.mock('@/components/ui/Screen', () => ({
  Screen: ({ children }: { children: React.ReactNode }) => children,
}));
jest.mock('@/components/states/ScreenState', () => {
  const { Text } = jest.requireActual('react-native');
  return { ScreenState: ({ kind }: { kind: string }) => <Text>{`state:${kind}`}</Text> };
});
jest.mock('@/features/home/api', () => ({ useContentCard: () => ({ title: 'Film' }) }));
jest.mock('@/lib/network', () => ({ useIsOffline: () => false }));
jest.mock('@/features/auth/store', () => ({
  useAuthStore: (select: (s: { token: string | null }) => unknown) =>
    select({ token: 't' }),
}));
jest.mock('@/features/watch/api', () => ({ useViewerKey: () => 'viewer' }));
jest.mock('@/lib/api', () => ({
  api: { get: jest.fn(), post: jest.fn(), delete: jest.fn() },
}));
jest.mock('@/lib/storage', () => ({
  getItem: jest.fn(async (key: string) => store[key] ?? null),
  setItem: jest.fn(async (key: string, value: string) => {
    store[key] = value;
  }),
  removeItem: jest.fn(async (key: string) => {
    delete store[key];
  }),
}));

jest.mock('../api', () => {
  const actual = jest.requireActual('../api');
  return {
    ...actual,
    useComments: () => mockComments,
    useDeleteComment: () => ({ mutate: jest.fn() }),
    usePostComment: () => ({
      mutate: jest.fn(),
      reset: jest.fn(),
      isPending: false,
      isError: false,
      error: null,
    }),
    useReportComment: () => ({ mutateAsync: jest.fn(), isPending: false }),
  };
});

import { act, create, type ReactTestRenderer } from 'react-test-renderer';
import { Text } from 'react-native';

import { CommentsScreen } from '../CommentsScreen';
import { useMutedAuthors } from '../mutedAuthors';

function page(items: unknown[]) {
  return {
    data: {
      pages: [
        {
          items,
          page: 0,
          totalItems: items.length,
          hasMore: false,
          alreadyCommented: false,
        },
      ],
    },
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

const base = {
  createdAt: null,
  authorAvatarUrl: null,
  mine: false,
  hidden: false,
};

/** Два комментария одного автора и один — чужого. */
const first = { ...base, id: 1, text: 'Birinchi', authorId: 'aziz', authorName: 'Aziz' };
const second = { ...base, id: 2, text: 'Ikkinchi', authorId: 'aziz', authorName: 'Aziz' };
const other = {
  ...base,
  id: 3,
  text: 'Uchinchi',
  authorId: 'malika',
  authorName: 'Malika',
};

function render(): ReactTestRenderer {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(<CommentsScreen contentId={42} />);
  });
  return tree;
}

const byLabel = (tree: ReactTestRenderer, label: string) =>
  tree.root.findAll(
    (n) => n.props?.accessibilityLabel === label && typeof n.props?.onPress === 'function'
  );

const texts = (tree: ReactTestRenderer) =>
  tree.root.findAllByType(Text).map((n) => String([n.props.children].flat().join('')));

beforeEach(() => {
  for (const key of Object.keys(store)) delete store[key];
  act(() => {
    useMutedAuthors.setState({ ids: new Set(), isRestored: true });
  });
  mockComments = page([first, second, other]);
});

describe('скрытие автора', () => {
  it('убирает ВСЕ его комментарии, чужие остаются', async () => {
    const tree = render();
    expect(texts(tree)).toEqual(
      expect.arrayContaining(['Birinchi', 'Ikkinchi', 'Uchinchi'])
    );

    await act(async () => {
      byLabel(tree, 'comments.report')[0].props.onPress();
    });
    await act(async () => {
      byLabel(tree, 'comments.hideAuthor')[0].props.onPress();
    });

    const shown = texts(tree);
    expect(shown).not.toContain('Birinchi');
    expect(shown).not.toContain('Ikkinchi');
    expect(shown).toContain('Uchinchi');
  });

  /**
   * ⚠️ Без этой строки комментарии просто пропадают, и человек решает,
   * что лента не догрузилась. Снять мьют тоже было бы негде.
   */
  it('показывает, сколько скрыто, и возвращает по кнопке', async () => {
    const tree = render();

    await act(async () => {
      byLabel(tree, 'comments.report')[0].props.onPress();
    });
    await act(async () => {
      byLabel(tree, 'comments.hideAuthor')[0].props.onPress();
    });

    expect(texts(tree)).toContain('comments.mutedNotice:2');

    await act(async () => {
      byLabel(tree, 'comments.mutedUndo')[0].props.onPress();
    });

    expect(texts(tree)).toContain('Birinchi');
  });

  it('без authorId пункта нет — по имени скрывать нельзя', async () => {
    mockComments = page([{ ...other, authorId: null }]);
    const tree = render();

    await act(async () => {
      byLabel(tree, 'comments.report')[0].props.onPress();
    });

    // Шторка открыта — причины жалобы на месте, а «скрыть автора» нет.
    expect(byLabel(tree, 'comments.reasonSpam')).toHaveLength(1);
    expect(byLabel(tree, 'comments.hideAuthor')).toHaveLength(0);
  });
});

describe('хранилище', () => {
  it('переживает перезапуск приложения', async () => {
    await act(async () => {
      await useMutedAuthors.getState().mute('aziz');
    });

    // Новый запуск: состояние в памяти пустое, на диске — нет.
    act(() => {
      useMutedAuthors.setState({ ids: new Set(), isRestored: false });
    });
    await act(async () => {
      await useMutedAuthors.getState().restore();
    });

    expect([...useMutedAuthors.getState().ids]).toEqual(['aziz']);
  });

  it('битая запись не роняет экран — считается пустым списком', async () => {
    const { setItem } = jest.requireMock('@/lib/storage') as {
      setItem: (k: string, v: string) => Promise<void>;
    };
    await setItem('uzcasting.muted_authors', 'не json');

    await act(async () => {
      await useMutedAuthors.getState().restore();
    });

    expect(useMutedAuthors.getState().ids.size).toBe(0);
  });
});
