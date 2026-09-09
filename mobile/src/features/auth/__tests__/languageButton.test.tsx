const mockSetLanguage = jest.fn();

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key, i18n: { language: 'uz' } }),
  // ⚠️ Заглушка обязана отдать и `initReactI18next`: `src/i18n` ставит его
  // в `i18n.use(...)` на импорте, и без него падает не тест, а сам модуль.
  initReactI18next: { type: '3rdParty', init: () => {} },
}));

jest.mock('@/i18n/storage', () => ({
  setLanguage: (...a: unknown[]) => mockSetLanguage(...a),
}));

/**
 * ⚠️ В jest-expo `Modal` не рендерит СОДЕРЖИМОЕ вообще: дерево выходит
 * пустым, и любая проверка того, что внутри всплывашки, находит ноль
 * узлов — сколько бы правильно ни работал сам компонент.
 *
 * Подменяем его прозрачной обёрткой. Тогда проверяется НАША логика —
 * открылось, переключило, закрылось, — а не реализация RN, которую мы
 * всё равно не чиним.
 */
jest.mock('react-native/Libraries/Modal/Modal', () => {
  const react = jest.requireActual('react');
  const { View } = jest.requireActual('react-native');

  const FakeModal = ({ visible, children }: { visible: boolean; children: unknown }) =>
    visible ? react.createElement(View, null, children) : null;

  // ⚠️ `displayName` обязателен: NativeWind оборачивает компоненты и
  // читает его на импорте — без него падает не тест, а сам модуль.
  FakeModal.displayName = 'Modal';

  return { __esModule: true, default: FakeModal };
});

import { act, create, type ReactTestRenderer } from 'react-test-renderer';

import { AuthLanguageButton } from '../AuthLanguageButton';

/**
 * Шестерёнка выбора языка на экранах входа.
 *
 * <h2>Что здесь ломается тихо</h2>
 * 1. Раскладка. Кнопка стоит поверх знака; стоит ей попасть в поток —
 *    и вся колонка входа съедет вниз на её высоту, а весь смысл
 *    `AuthScaffold` в том, что поле НЕ шевелится между шагами.
 * 2. Мёртвая кнопка. Карточка открывается по замеру координат
 *    (`measureInWindow`), а в тестовом рендерере замера нет. Если бы
 *    запасного пути не было, нажатие просто ничего не делало бы — ровно
 *    то, что человек читает как «приложение сломалось».
 *
 * ⚠️ Узлы ищутся по роли, а не по типу: NativeWind подменяет `Pressable`
 * своей обёрткой, и `findByType(Pressable)` не находит ничего.
 */
function render(): ReactTestRenderer {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(<AuthLanguageButton top={128} />);
  });
  return tree;
}

function gear(tree: ReactTestRenderer) {
  return tree.root.find((node) => node.props?.accessibilityLabel === 'profile.language');
}

/**
 * Все нажимаемые сегменты выбора языка — по одному на язык.
 *
 * ⚠️ Дедупликация обязательна. NativeWind оборачивает каждый `Pressable`
 * в несколько слоёв, и один сегмент находится трижды: три кнопки давали
 * девять совпадений. Отбирать по «настоящим» узлам (`typeof type ===
 * 'string'`) нельзя — у них уже нет `onPress`, только внутренние
 * обработчики RN, и нажать такой узел не получится.
 *
 * Поэтому берём узлы С обработчиком и схлопываем по его тождеству:
 * слои передают одну и ту же функцию вниз.
 */
function segments(tree: ReactTestRenderer) {
  const found = tree.root.findAll(
    (node) =>
      node.props?.accessibilityRole === 'button' &&
      node.props?.accessibilityState?.selected !== undefined &&
      typeof node.props?.onPress === 'function'
  );

  const seen = new Set<unknown>();
  return found.filter((node) => {
    if (seen.has(node.props.onPress)) return false;
    seen.add(node.props.onPress);
    return true;
  });
}

beforeEach(() => {
  mockSetLanguage.mockClear();
});

describe('AuthLanguageButton', () => {
  it('не участвует в раскладке экрана', () => {
    const style = gear(render()).props.style;

    expect(style.position).toBe('absolute');
    expect(style.top).toBe(128);
  });

  it('закрыта, пока её не нажали', () => {
    expect(segments(render())).toHaveLength(0);
  });

  it('открывает выбор языка даже без замера координат', () => {
    const tree = render();

    act(() => gear(tree).props.onPress());

    expect(segments(tree)).toHaveLength(3);
  });

  it('переключает язык и закрывается', () => {
    const tree = render();
    act(() => gear(tree).props.onPress());

    act(() => segments(tree)[1].props.onPress());

    expect(mockSetLanguage).toHaveBeenCalledWith('ru');
    expect(segments(tree)).toHaveLength(0);
  });
});
