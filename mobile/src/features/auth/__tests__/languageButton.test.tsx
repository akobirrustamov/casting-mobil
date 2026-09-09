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

/** Все нажимаемые сегменты выбора языка. */
function segments(tree: ReactTestRenderer) {
  return tree.root.findAll(
    (node) =>
      node.props?.accessibilityRole === 'button' &&
      node.props?.accessibilityState?.selected !== undefined
  );
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
