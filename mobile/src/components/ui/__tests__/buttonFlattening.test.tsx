/**
 * Ряд с подписью кнопки НЕ сплющивается при смене `loading`.
 *
 * <h2>⚠️ Что охраняет этот тест</h2>
 * Разбор 15.09.2026 по `adb logcat` с телефона тестировщика: чёрный экран
 * после кода из SMS — это не навигация и не лимит устройств, а падение
 * монтирования в Fabric:
 *
 *   addViewAt: cannot insert view [2194] into parent [2202]:
 *   View already has a parent: [2200]  Parent: ReactViewGroup View: ReactTextView
 *   → ReactHost.handleHostException → Destroying ReactInstance
 *
 * [2202] — `Pressable` кнопки «Davom etish», [2200] — ряд с подписью,
 * [2194] — сама подпись, рядом кружок ожидания. Пока идёт запрос, у ряда
 * `opacity: 0`, и Fabric держит его настоящим view. Ответ пришёл —
 * `opacity: 1`, ряд становится «чисто раскладочным», Fabric его сплющивает
 * и переносит подпись в `Pressable`. Экран кода в этот момент уже уходит
 * со стека (`router.replace`), перенос срывается, и React Native гасит
 * ВЕСЬ инстанс: ни `ErrorBoundary`, ни глобальный обработчик JS этого не
 * видят, остаётся пустое окно.
 *
 * `collapsable={false}` запрещает сплющивание — ряд остаётся тем же view
 * при любом `loading`, и переносить подпись некуда.
 */
jest.mock('react-native/Libraries/Components/ActivityIndicator/ActivityIndicator', () => {
  const { View } = jest.requireActual('react-native');
  return { __esModule: true, default: View };
});

import { View } from 'react-native';
import { act, create, type ReactTestRenderer } from 'react-test-renderer';

import { Button } from '../Button';

/** Ряд с подписью — тот `View`, у которого прозрачность зависит от `loading`. */
function labelRow(tree: ReactTestRenderer) {
  return tree.root.find(
    (n) =>
      n.type === View &&
      n.props.style !== undefined &&
      typeof (n.props.style as { opacity?: unknown }).opacity === 'number'
  );
}

function render(loading: boolean): ReactTestRenderer {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(<Button loading={loading}>Davom etish</Button>);
  });
  return tree;
}

describe('Button: ряд с подписью', () => {
  it('⚠️ во время ожидания запрещено сплющивание', () => {
    expect(labelRow(render(true)).props.collapsable).toBe(false);
  });

  it('⚠️ после ожидания — тоже: иначе подпись переедет в Pressable', () => {
    expect(labelRow(render(false)).props.collapsable).toBe(false);
  });

  it('смена loading → false не меняет запрет на сплющивание', () => {
    const tree = render(true);
    act(() => {
      tree.update(<Button loading={false}>Davom etish</Button>);
    });

    expect(labelRow(tree).props.collapsable).toBe(false);
    expect((labelRow(tree).props.style as { opacity: number }).opacity).toBe(1);
  });
});
