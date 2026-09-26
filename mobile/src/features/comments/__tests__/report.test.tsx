/**
 * Жалоба на чужой комментарий.
 *
 * <h2>Зачем это вообще есть</h2>
 * Политика Google Play для приложений с пользовательским контентом:
 * должен быть способ пожаловаться на чужую запись. Ревьюер открывает
 * список комментариев и ищет эту кнопку — не находит, отказ.
 *
 * <h2>Что ломается тихо</h2>
 * <ul>
 *   <li>Кнопка жалобы появляется у СВОЕГО комментария — сервер ответит
 *       422, а человек решит, что приложение сломано.</li>
 *   <li>Жалоба уходит без причины или с чужой причиной — модератор
 *       получает бесполезную очередь.</li>
 *   <li>После отправки ничего не происходит: комментарий остаётся на
 *       месте (так и задумано), и без ответного сообщения человек жмёт
 *       ещё раз, получая «вы уже жаловались».</li>
 * </ul>
 */

let mockComments: Record<string, unknown>;
const mockReport = jest.fn();
const mockAlert = jest.fn();

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-image', () => ({ Image: 'Image' }));
jest.mock('expo-router', () => ({ router: { push: jest.fn(), back: jest.fn() } }));
jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
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
  return {
    ScreenState: ({ kind }: { kind: string }) => <Text>{`state:${kind}`}</Text>,
  };
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
    useReportComment: () => ({ mutateAsync: mockReport, isPending: false }),
  };
});

import { Alert } from 'react-native';
import { act, create, type ReactTestRenderer } from 'react-test-renderer';

import { ReportRejectedError, reportComment } from '../api';
import { CommentsScreen } from '../CommentsScreen';

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

const mine = {
  id: 1,
  text: 'Mening izohim',
  createdAt: null,
  authorName: 'Men',
  authorAvatarUrl: null,
  mine: true,
  hidden: false,
};
const theirs = { ...mine, id: 2, text: 'Begona izoh', authorName: 'Aziz', mine: false };

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

beforeEach(() => {
  mockReport.mockReset().mockResolvedValue(undefined);
  mockAlert.mockClear();
  jest.spyOn(Alert, 'alert').mockImplementation(mockAlert);
  mockComments = page([mine, theirs]);
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('кнопка', () => {
  it('есть у чужого комментария и нет у своего', () => {
    const tree = render();

    // Один чужой комментарий — одна кнопка жалобы.
    expect(byLabel(tree, 'comments.report')).toHaveLength(1);
    // И ровно одна кнопка удаления — у своего.
    expect(byLabel(tree, 'comments.delete')).toHaveLength(1);
  });

  it('у списка из одних своих комментариев жалобы нет вовсе', () => {
    mockComments = page([mine]);
    const tree = render();

    expect(byLabel(tree, 'comments.report')).toHaveLength(0);
  });
});

describe('отправка', () => {
  it('уходит с выбранной причиной и подтверждается сообщением', async () => {
    const tree = render();

    await act(async () => {
      byLabel(tree, 'comments.report')[0].props.onPress();
    });

    // Шторка открылась — в ней четыре причины.
    expect(byLabel(tree, 'comments.reasonSpam')).toHaveLength(1);
    expect(byLabel(tree, 'comments.reasonOther')).toHaveLength(1);

    await act(async () => {
      byLabel(tree, 'comments.reasonInsult')[0].props.onPress();
    });

    expect(mockReport).toHaveBeenCalledWith({ commentId: 2, reason: 'INSULT' });
    expect(mockAlert).toHaveBeenCalledWith('comments.reportSent');
  });

  /**
   * ⚠️ Отказ тоже должен быть виден. Жалоба ничего не меняет на экране,
   * поэтому молчание в ответ неотличимо от «кнопка не работает».
   */
  it('повторная жалоба объясняет, что она уже была', async () => {
    mockReport.mockRejectedValue(new ReportRejectedError('already', 'server'));
    const tree = render();

    await act(async () => {
      byLabel(tree, 'comments.report')[0].props.onPress();
    });
    await act(async () => {
      byLabel(tree, 'comments.reasonSpam')[0].props.onPress();
    });

    expect(mockAlert).toHaveBeenCalledWith('comments.reportAlready');
  });

  it('шторка закрывается после выбора причины', async () => {
    const tree = render();

    await act(async () => {
      byLabel(tree, 'comments.report')[0].props.onPress();
    });
    await act(async () => {
      byLabel(tree, 'comments.reasonSpam')[0].props.onPress();
    });

    expect(byLabel(tree, 'comments.reasonSpam')).toHaveLength(0);
  });
});

describe('разбор ответа сервера', () => {
  it('409 превращается в «уже жаловались», а не в общую ошибку', async () => {
    const { api } = jest.requireMock('@/lib/api') as {
      api: { post: jest.Mock };
    };
    api.post.mockRejectedValue({
      isAxiosError: true,
      response: { status: 409, data: { message: 'bor' } },
    });

    await expect(reportComment(2, 'SPAM')).rejects.toMatchObject({
      name: 'ReportRejectedError',
      reason: 'already',
    });
  });

  it('401 просит войти — это не «попробуйте ещё раз»', async () => {
    const { api } = jest.requireMock('@/lib/api') as {
      api: { post: jest.Mock };
    };
    api.post.mockRejectedValue({
      isAxiosError: true,
      response: { status: 401, data: {} },
    });

    await expect(reportComment(2, 'SPAM')).rejects.toMatchObject({ reason: 'signIn' });
  });
});
