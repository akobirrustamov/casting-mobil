/**
 * «Top 100 donatchilar» — отдельная страница рейтинга (10.09.2026).
 *
 * <h2>Что здесь ломается тихо</h2>
 * <ul>
 *   <li>Плитка ведёт не в ту валюту — человек смотрел на монеты, а
 *       открылся звёздный рейтинг. Ошибки нет, просто чужие цифры.</li>
 *   <li>Старый сервер отвечает на рейтинг 401. Страница, показавшая на это
 *       «ошибка, повторите», обещала бы то, что повтор не исправит.</li>
 *   <li>Кнопка «Donat qilish» пропадает вместе с пустым или сломанным
 *       рейтингом — а поддержать можно и первым.</li>
 * </ul>
 */

const mockPush = jest.fn();

let mockDonors: {
  data?: { total: number; donors: unknown[] };
  error: unknown;
  isPending: boolean;
  isError: boolean;
  isRefetching: boolean;
  refetch: jest.Mock;
};
const mockUseDonors = jest.fn();

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-image', () => ({ Image: 'Image' }));
jest.mock('expo-linear-gradient', () => ({ LinearGradient: 'LinearGradient' }));
jest.mock('expo-router', () => ({
  router: { push: (...a: unknown[]) => mockPush(...a), back: jest.fn() },
}));
jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));
jest.mock('react-native-safe-area-context', () => ({
  useSafeAreaInsets: () => ({ top: 44, bottom: 34, left: 0, right: 0 }),
}));

// Каркас страницы и заглушки состояний к рейтингу отношения не имеют.
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
jest.mock('@/features/home/api', () => ({
  useContentCard: () => ({ title: 'Qalbing egasi' }),
}));
jest.mock('@/lib/network', () => ({ useIsOffline: () => false }));

// Окно доната — своё, у него свои зависимости (баланс, вход).
jest.mock('../DonateSheet', () => {
  const { Text } = jest.requireActual('react-native');
  return {
    DonateSheet: ({ open, currency }: { open: boolean; currency: string }) =>
      open ? <Text>{`donate-sheet:${currency}`}</Text> : null,
  };
});

jest.mock('../detail', () => {
  class ContentDetailUnavailableError extends Error {}
  return {
    ContentDetailUnavailableError,
    useContentDonors: (...args: unknown[]) => {
      mockUseDonors(...args);
      return mockDonors;
    },
  };
});

import { act, create, type ReactTestRenderer } from 'react-test-renderer';
import { Text } from 'react-native';

import { DonorsScreen, openDonors, parseCurrency } from '../DonorsScreen';

const detailMock = jest.requireMock('../detail') as {
  ContentDetailUnavailableError: new () => Error;
};

function answered(donors: unknown[], total = 1000) {
  return {
    data: { total, donors },
    error: null,
    isPending: false,
    isError: false,
    isRefetching: false,
    refetch: jest.fn(),
  };
}

function render(currency: 'STARS' | 'UZCASTING_COIN' = 'STARS'): ReactTestRenderer {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(<DonorsScreen contentId={42} initialCurrency={currency} />);
  });
  return tree;
}

const texts = (tree: ReactTestRenderer) =>
  tree.root.findAllByType(Text).map((n) => String([n.props.children].flat().join('')));

/** Нажимаемый элемент, внутри которого есть текст `label`. */
function pressable(tree: ReactTestRenderer, label: string) {
  return tree.root.find(
    (node) =>
      typeof node.props?.onPress === 'function' &&
      node.findAllByType(Text).some((n) => String(n.props.children) === label)
  );
}

beforeEach(() => {
  mockPush.mockClear();
  mockUseDonors.mockClear();
  mockDonors = answered([
    { rank: 1, name: 'Abdulloh_88', avatarUrl: null, stars: 1250000 },
    { rank: 2, name: null, avatarUrl: null, stars: 950000 },
  ]);
});

describe('адрес страницы', () => {
  it('плитка ведёт в рейтинг СВОЕЙ валюты', () => {
    openDonors(42, 'UZCASTING_COIN');
    expect(mockPush).toHaveBeenCalledWith('/donors/42?currency=UZCASTING_COIN');
  });

  it('незнакомая валюта в адресе — звёзды, а не пустая страница', () => {
    expect(parseCurrency('UZCASTING_COIN')).toBe('UZCASTING_COIN');
    expect(parseCurrency('STARS')).toBe('STARS');
    expect(parseCurrency(undefined)).toBe('STARS');
    expect(parseCurrency('gems')).toBe('STARS');
  });
});

describe('рейтинг', () => {
  it('строки и итог рисуются, безымянный остаётся в списке', () => {
    const all = texts(render());

    expect(all).toContain('Abdulloh_88');
    // Удалённый аккаунт — не повод прятать его звёзды.
    expect(all).toContain('content.anonymousDonor');
    expect(all).toContain('content.starsTotal');
  });

  it('открывается в валюте из адреса, переключатель меняет запрос', () => {
    const tree = render('UZCASTING_COIN');
    expect(mockUseDonors).toHaveBeenLastCalledWith(42, 'UZCASTING_COIN');

    act(() => pressable(tree, 'content.stars').props.onPress());

    expect(mockUseDonors).toHaveBeenLastCalledWith(42, 'STARS');
  });

  it('старый сервер — честное «ещё не включено», кнопка доната остаётся', () => {
    mockDonors = {
      data: undefined,
      error: new detailMock.ContentDetailUnavailableError(),
      isPending: false,
      isError: true,
      isRefetching: false,
      refetch: jest.fn(),
    };

    const tree = render();

    expect(texts(tree)).toContain('state:empty:content.donorsUnavailable');
    expect(pressable(tree, 'content.donate')).toBeTruthy();
  });

  it('«Donat qilish» открывает окно доната в валюте страницы', () => {
    const tree = render('UZCASTING_COIN');

    act(() => pressable(tree, 'content.donate').props.onPress());

    expect(texts(tree)).toContain('donate-sheet:UZCASTING_COIN');
  });
});
