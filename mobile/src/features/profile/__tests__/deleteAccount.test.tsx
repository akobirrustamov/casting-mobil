/**
 * Удаление аккаунта — экран и запрос.
 *
 * <h2>Зачем это тестами</h2>
 * Экран существует ради требования Google Play, и проверить его глазами
 * можно ровно один раз: после подтверждения аккаунт пропадает, и второй
 * попытки на том же телефоне уже нет.
 *
 * <h2>Что ломается тихо</h2>
 * <ul>
 *   <li>Разлогин раньше ответа сервера: при обрыве сети человек остаётся
 *       без сессии с целым аккаунтом и думает, что всё сломалось.</li>
 *   <li>Удаление без подтверждения — случайное нажатие стирает аккаунт.</li>
 *   <li>Ошибка сервера проглатывается, и человек уверен, что удалил.</li>
 * </ul>
 */

const mockDelete = jest.fn();
const mockSignOut = jest.fn();
const mockReplace = jest.fn();
let mockAuthorized = true;

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-router', () => ({
  router: {
    replace: (...args: unknown[]) => mockReplace(...args),
    push: jest.fn(),
    back: jest.fn(),
  },
}));
jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));
jest.mock('@/components/ui/Screen', () => ({
  Screen: ({ children }: { children: React.ReactNode }) => children,
}));
jest.mock('@/components/states/ScreenState', () => {
  const { Text } = jest.requireActual('react-native');
  return { ScreenState: ({ kind }: { kind: string }) => <Text>{`state:${kind}`}</Text> };
});
jest.mock('@/components/ui/FormMessage', () => {
  const { Text } = jest.requireActual('react-native');
  return {
    FormMessage: ({ message }: { message: string | null }) => (
      <Text>{message ?? ''}</Text>
    ),
  };
});
jest.mock('@/features/auth/store', () => ({
  useAuthStore: (select: (s: Record<string, unknown>) => unknown) =>
    select({ isAuthorized: mockAuthorized, signOut: mockSignOut }),
}));
jest.mock('@/features/profile/api', () => ({
  useDeleteAccount: () => ({ mutateAsync: mockDelete, isPending: false }),
}));

import { Alert, Text } from 'react-native';
import { act, create, type ReactTestRenderer } from 'react-test-renderer';

import DeleteAccountScreen from '../../../../app/settings/delete-account';

type AlertButton = { text?: string; style?: string; onPress?: () => void };
let lastButtons: AlertButton[] = [];

function render(): ReactTestRenderer {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(<DeleteAccountScreen />);
  });
  return tree;
}

const deleteButton = (tree: ReactTestRenderer) =>
  tree.root.find(
    (n) =>
      n.props?.accessibilityLabel === 'settings.deleteAction' &&
      typeof n.props?.onPress === 'function'
  );

const texts = (tree: ReactTestRenderer) =>
  tree.root.findAllByType(Text).map((n) => String([n.props.children].flat().join('')));

beforeEach(() => {
  mockAuthorized = true;
  mockDelete.mockReset().mockResolvedValue(undefined);
  mockSignOut.mockReset().mockResolvedValue(undefined);
  mockReplace.mockReset();
  lastButtons = [];
  jest.spyOn(Alert, 'alert').mockImplementation((_title, _body, buttons) => {
    lastButtons = (buttons ?? []) as AlertButton[];
  });
});

afterEach(() => {
  jest.restoreAllMocks();
});

/** Подтверждение из системного диалога — «удалить», а не «отмена». */
async function confirm(tree: ReactTestRenderer) {
  await act(async () => {
    deleteButton(tree).props.onPress();
  });
  const destructive = lastButtons.find((b) => b.style === 'destructive');
  await act(async () => {
    destructive?.onPress?.();
  });
}

describe('подтверждение', () => {
  it('одно нажатие ничего не удаляет — сначала диалог', async () => {
    const tree = render();

    await act(async () => {
      deleteButton(tree).props.onPress();
    });

    expect(mockDelete).not.toHaveBeenCalled();
    // Диалог предлагает и отмену, иначе промах пальцем стирает аккаунт.
    expect(lastButtons.some((b) => b.style === 'cancel')).toBe(true);
  });

  it('после подтверждения аккаунт удаляется и человек уходит на вход', async () => {
    const tree = render();
    await confirm(tree);

    expect(mockDelete).toHaveBeenCalledTimes(1);
    expect(mockSignOut).toHaveBeenCalledTimes(1);
    expect(mockReplace).toHaveBeenCalledWith('/(auth)/sign-in');
  });
});

describe('порядок и ошибки', () => {
  /**
   * ⚠️ Главная проверка файла: сессию гасим ТОЛЬКО после ответа сервера.
   */
  it('при отказе сервера сессия остаётся, экран объясняет причину', async () => {
    mockDelete.mockRejectedValue(new Error('network'));
    const tree = render();
    await confirm(tree);

    expect(mockSignOut).not.toHaveBeenCalled();
    expect(mockReplace).not.toHaveBeenCalled();
    expect(texts(tree)).toContain('settings.deleteFailed');
  });

  it('гостю показывается «войдите», а не кнопка удаления', () => {
    mockAuthorized = false;
    const tree = render();

    expect(texts(tree)).toContain('state:locked');
    expect(
      tree.root.findAll((n) => n.props?.accessibilityLabel === 'settings.deleteAction')
    ).toHaveLength(0);
  });
});
