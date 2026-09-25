/**
 * Отступ под клавиатуру.
 *
 * <h2>Что здесь ломается тихо</h2>
 * Android отдаёт высоту клавиатуры БЕЗ полосы навигации, а окно в режиме
 * edge-to-edge занимает весь экран вместе с полосой. Забыть её — и поле
 * ввода встанет ровно на её высоту ниже, то есть частично под
 * клавиатурой. На глаз в симуляторе это почти незаметно, на телефоне с
 * тремя кнопками — сразу.
 */

import { Keyboard, Platform } from 'react-native';
import TestRenderer, { act } from 'react-test-renderer';

/** Высота корневого вида — тест меняет её, изображая сжатое системой окно. */
let mockFrameHeight = 800;

jest.mock('react-native-safe-area-context', () => ({
  useSafeAreaInsets: () => ({ top: 24, bottom: 48, left: 0, right: 0 }),
  useSafeAreaFrame: () => ({ x: 0, y: 0, width: 400, height: mockFrameHeight }),
}));

import { useKeyboardInset } from '../keyboard';

type Handler = (event: { endCoordinates?: { height: number } }) => void;

const listeners: Record<string, Handler> = {};

beforeEach(() => {
  mockFrameHeight = 800;
  for (const key of Object.keys(listeners)) delete listeners[key];

  jest.spyOn(Keyboard, 'addListener').mockImplementation(((
    name: string,
    handler: Handler
  ) => {
    listeners[name] = handler;
    return {
      remove: () => {
        delete listeners[name];
      },
    };
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  }) as any);
});

afterEach(() => {
  jest.restoreAllMocks();
});

/** Текущее значение хука — держим снаружи, чтобы читать без рендера. */
let inset = -1;

function Probe() {
  inset = useKeyboardInset();
  return null;
}

function renderOn(os: 'ios' | 'android') {
  Object.defineProperty(Platform, 'OS', { value: os, configurable: true });
  let tree!: TestRenderer.ReactTestRenderer;
  act(() => {
    tree = TestRenderer.create(<Probe />);
  });
  return tree;
}

test('без клавиатуры отступа нет', () => {
  renderOn('android');

  expect(inset).toBe(0);
});

test('Android: к высоте клавиатуры прибавляется полоса навигации', () => {
  renderOn('android');

  act(() => listeners.keyboardDidShow?.({ endCoordinates: { height: 300 } }));

  expect(inset).toBe(348);
});

test('iOS: высота клавиатуры уже от низа экрана', () => {
  renderOn('ios');

  act(() => listeners.keyboardWillShow?.({ endCoordinates: { height: 300 } }));

  expect(inset).toBe(300);
});

test('клавиатура закрылась — отступ снова нулевой', () => {
  renderOn('ios');

  act(() => listeners.keyboardWillShow?.({ endCoordinates: { height: 300 } }));
  act(() => listeners.keyboardWillHide?.({}));

  expect(inset).toBe(0);
});

test('Android сам сжал окно под клавиатуру — второй раз не отступаем', () => {
  const tree = renderOn('android');

  mockFrameHeight = 452; // окно сжалось на всю клавиатуру с полосой навигации
  act(() => listeners.keyboardDidShow?.({ endCoordinates: { height: 300 } }));
  act(() => tree.update(<Probe />));

  expect(inset).toBe(0);
});
