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

jest.mock('react-native-safe-area-context', () => ({
  useSafeAreaInsets: () => ({ top: 24, bottom: 48, left: 0, right: 0 }),
}));

import { useKeyboardInset } from '../keyboard';

type Handler = (event: { endCoordinates?: { height: number } }) => void;

const listeners: Record<string, Handler> = {};

beforeEach(() => {
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
  act(() => {
    TestRenderer.create(<Probe />);
  });
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
