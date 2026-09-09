/**
 * Баннер поверх экрана.
 *
 * <h2>Что здесь ломается тихо</h2>
 * 1. Повторный показ. Вкладка «Media» размонтируется при переходе на
 *    другую и монтируется обратно — держи счётчик в состоянии экрана, и
 *    человек получал бы баннер при каждом возвращении.
 * 2. Показ без содержимого. Сервер отвечает 204, когда показывать
 *    нечего; принять это за «пустой баннер» значит накрыть экран пустым
 *    чёрным прямоугольником с крестиком.
 * 3. Показ, который нечем закрыть, — приложение выглядит зависшим.
 */

const mockTrackImpression = jest.fn();
const mockTrackClick = jest.fn();
let mockBanner: unknown = null;

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-image', () => ({ Image: 'Image' }));
jest.mock('expo-linking', () => ({ openURL: jest.fn() }));
jest.mock('expo-router', () => ({ router: { push: jest.fn() } }));

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: '3rdParty', init: () => undefined },
}));

jest.mock('@/features/analytics/api', () => ({
  trackAdImpression: (...a: unknown[]) => mockTrackImpression(...a),
  trackAdClick: (...a: unknown[]) => mockTrackClick(...a),
}));

jest.mock('@/lib/api', () => ({ mediaUrl: () => undefined }));

jest.mock('../api', () => ({
  useInterstitialAd: () => ({ data: mockBanner }),
}));

/**
 * ⚠️ В jest-expo `Modal` не рендерит содержимое вообще — дерево выходит
 * пустым. Подменяем прозрачной обёрткой: проверяется наша логика, а не
 * реализация RN.
 */
jest.mock('react-native/Libraries/Modal/Modal', () => {
  const react = jest.requireActual('react');
  const { View } = jest.requireActual('react-native');

  const FakeModal = ({ children }: { children: unknown }) =>
    react.createElement(View, null, children);
  FakeModal.displayName = 'Modal';

  return { __esModule: true, default: FakeModal };
});

import { act, create, type ReactTestRenderer } from 'react-test-renderer';

import { InterstitialAd, resetInterstitialForTests } from '../InterstitialAd';

const BANNER = {
  id: 7,
  audience: 'ADVERTISEMENT',
  title: 'Yangi avlod bank ilovasi',
  subtitle: null,
  description: null,
  buttonText: 'Ilovani yuklab olish',
  buttonEnabled: true,
  imageMediaId: 42,
  videoMediaId: null,
  linkType: 'EXTERNAL',
  linkUrl: 'https://example.uz',
  internalTargetType: null,
  internalTargetId: null,
};

function render(): ReactTestRenderer {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(<InterstitialAd />);
  });
  return tree;
}

/** Кнопка по подписи: NativeWind оборачивает `Pressable`, тип не найти. */
function button(tree: ReactTestRenderer, label: string) {
  return tree.root.find(
    (node) =>
      node.props?.accessibilityLabel === label &&
      typeof node.props?.onPress === 'function'
  );
}

beforeEach(() => {
  resetInterstitialForTests();
  mockTrackImpression.mockClear();
  mockTrackClick.mockClear();
  mockBanner = null;
});

it('Показывать нечего — на экране ничего нет', () => {
  const tree = render();

  expect(tree.toJSON()).toBeNull();
  expect(mockTrackImpression).not.toHaveBeenCalled();
});

it('Баннер есть — показан и засчитан один показ', () => {
  mockBanner = BANNER;

  const tree = render();

  expect(tree.toJSON()).not.toBeNull();
  expect(mockTrackImpression).toHaveBeenCalledWith(7);
});

/**
 * ⚠️ Главная проверка файла.
 *
 * Вкладка монтируется заново при каждом возвращении. Без флага на
 * уровне модуля человек получал бы баннер по десятку раз за сеанс — и
 * это выглядело бы как поломка, а не как реклама.
 */
it('Второй раз за сеанс не показывается', () => {
  mockBanner = BANNER;

  render();
  const second = render();

  expect(second.toJSON()).toBeNull();
  expect(mockTrackImpression).toHaveBeenCalledTimes(1);
});

it('Крестик закрывает', () => {
  mockBanner = BANNER;
  const tree = render();

  act(() => button(tree, 'common.close').props.onPress());

  expect(tree.toJSON()).toBeNull();
});

it('Нажатие на кнопку считается кликом', () => {
  mockBanner = BANNER;
  const tree = render();

  act(() => button(tree, 'Ilovani yuklab olish').props.onPress());

  expect(mockTrackClick).toHaveBeenCalledWith(7);
});
