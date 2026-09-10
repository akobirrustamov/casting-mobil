/**
 * `pushOnce` — одна страница на серию быстрых нажатий.
 *
 * ⚠️ Что чинилось: десять быстрых нажатий на «Izohlar» (и на плитки
 * «Yulduzlar» / «Uzcasting») открывали десять одинаковых страниц.
 */
const mockPush = jest.fn();

jest.mock('expo-router', () => ({
  router: { push: (...a: unknown[]) => mockPush(...a) },
}));

import { PUSH_COOLDOWN_MS, pushOnce, resetPushOnceForTests } from '../navigation';

beforeEach(() => {
  resetPushOnceForTests();
  mockPush.mockClear();
});

it('десять нажатий подряд — один переход', () => {
  const t0 = 1_000_000;
  for (let i = 0; i < 10; i++) pushOnce('/comments/42', t0 + i * 30);

  expect(mockPush).toHaveBeenCalledTimes(1);
  expect(mockPush).toHaveBeenCalledWith('/comments/42');
});

it('замок общий: «Izohlar» и сразу «Yulduzlar» — тоже одна страница', () => {
  const t0 = 1_000_000;
  pushOnce('/comments/42', t0);
  pushOnce('/donors/42?currency=STARS', t0 + 100);

  expect(mockPush).toHaveBeenCalledTimes(1);
});

it('после паузы переход снова работает', () => {
  const t0 = 1_000_000;
  expect(pushOnce('/comments/42', t0)).toBe(true);
  expect(pushOnce('/comments/42', t0 + PUSH_COOLDOWN_MS)).toBe(true);

  expect(mockPush).toHaveBeenCalledTimes(2);
});
