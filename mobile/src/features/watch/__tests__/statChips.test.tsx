/**
 * Просмотры и «нравится» под названием.
 *
 * <h2>Что здесь ломается тихо</h2>
 * Две вещи, и обе не видны на скриншоте:
 *
 * 1. Разница между «сервер прислал 0» и «сервер не прислал ничего».
 *    Свести их к нулю — значит написать «0 просмотров» на КАЖДОМ фильме,
 *    пока на сервере старая сборка. Выглядит как рабочий экран, а на
 *    деле выдуманная цифра.
 * 2. Гость. Если его нажатие уйдёт на сервер, оно вернётся 401, сердце
 *    молча отскочит назад — и человек решит, что приложение сломано.
 */

const mockPush = jest.fn();
const mockSetLike = jest.fn();
let mockToken: string | null = null;

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-router', () => ({ router: { push: (...a: unknown[]) => mockPush(...a) } }));

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

jest.mock('@/features/auth/store', () => ({
  useAuthStore: (select: (s: { token: string | null }) => unknown) => select({ token: mockToken }),
}));

jest.mock('../api', () => ({ setLike: (...a: unknown[]) => mockSetLike(...a) }));

import { act, create, type ReactTestRenderer } from 'react-test-renderer';
import { Text } from 'react-native';

import { StatChips } from '../StatChips';

import type { WatchInfo } from '../types';

function info(over: Partial<WatchInfo> = {}): WatchInfo {
  return {
    episodeId: null,
    contentId: 13,
    episodeNumber: null,
    durationSeconds: 5400,
    title: 'Film',
    orientation: 'LANDSCAPE',
    allowed: true,
    reason: 'FREE',
    requiredAction: 'NONE',
    episodePrice: null,
    premierePrice: null,
    showAds: false,
    trailer: null,
    viewCount: 1200,
    likeCount: 7,
    liked: false,
    sources: [],
    ...over,
  };
}

function render(data: WatchInfo): ReactTestRenderer {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(<StatChips info={data} />);
  });
  return tree;
}

/**
 * Кнопка «нравится».
 *
 * ⚠️ Ищется по роли, а не по типу `Pressable`: NativeWind подменяет
 * компонент своей обёрткой, и `findByType(Pressable)` не находит ничего.
 */
function heart(tree: ReactTestRenderer) {
  return tree.root.find(
    (node) =>
      node.props?.accessibilityRole === 'button' &&
      typeof node.props?.onPress === 'function'
  );
}

/** Все подписи на экране. */
function texts(tree: ReactTestRenderer): string[] {
  return tree.root
    .findAllByType(Text)
    .map((node) => String(node.props.children))
    .filter(Boolean);
}

beforeEach(() => {
  mockPush.mockClear();
  mockSetLike.mockClear();
  mockSetLike.mockResolvedValue({ liked: true, likeCount: 8 });
  mockToken = null;
});

describe('чего сервер не сказал — того не рисуем', () => {
  it('без обоих счётчиков строки нет совсем', () => {
    const tree = render(info({ viewCount: null, likeCount: null }));

    expect(tree.toJSON()).toBeNull();
  });

  it('ноль от сервера — это факт, его показываем', () => {
    // ⚠️ Именно этим ноль отличается от `null`: «никто не смотрел» —
    // правда о контенте, и скрывать её незачем.
    const tree = render(info({ viewCount: 0, likeCount: 0 }));

    expect(texts(tree)).toEqual(['0', '0']);
  });

  it('пришло одно из двух — рисуется одно', () => {
    const tree = render(info({ viewCount: 340, likeCount: null }));

    expect(texts(tree)).toEqual(['340']);
  });

  it('разряды разделены пробелом', () => {
    const tree = render(info({ viewCount: 1200, likeCount: 12345 }));

    expect(texts(tree)).toEqual(['1 200', '12 345']);
  });
});

describe('гость', () => {
  it('видит счётчик, но нажатие ведёт на вход', () => {
    const tree = render(info());

    act(() => {
      heart(tree).props.onPress();
    });

    expect(mockPush).toHaveBeenCalledWith('/(auth)/sign-in');
    // ⚠️ Главное: запрос НЕ ушёл. Иначе сервер ответит 401, сердце
    // отскочит обратно, и виноватым будет выглядеть приложение.
    expect(mockSetLike).not.toHaveBeenCalled();
  });
});

describe('вошедший человек', () => {
  beforeEach(() => {
    mockToken = 'jwt';
  });

  it('нажатие ставит «нравится» и увеличивает счёт сразу', async () => {
    const tree = render(info({ likeCount: 7, liked: false }));

    await act(async () => {
      await heart(tree).props.onPress();
    });

    expect(mockSetLike).toHaveBeenCalledWith(13, true);
    // Число из ОТВЕТА сервера, а не наше предположение.
    expect(texts(tree)).toContain('8');
  });

  it('повторное нажатие снимает — DELETE, а не второй PUT', async () => {
    mockSetLike.mockResolvedValue({ liked: false, likeCount: 6 });
    const tree = render(info({ likeCount: 7, liked: true }));

    await act(async () => {
      await heart(tree).props.onPress();
    });

    expect(mockSetLike).toHaveBeenCalledWith(13, false);
    expect(texts(tree)).toContain('6');
  });

  it('сбой сети возвращает как было', async () => {
    mockSetLike.mockRejectedValue(new Error('нет сети'));
    const tree = render(info({ likeCount: 7, liked: false }));

    await act(async () => {
      await heart(tree).props.onPress();
    });

    // ⚠️ Не «7 после оптимистичных 8», а именно исходное состояние:
    // иначе человек ушёл бы с экрана уверенным, что «нравится»
    // сохранилось.
    expect(texts(tree)).toContain('7');
  });
});
