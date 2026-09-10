/**
 * Вход ЖДЁТ регистрацию устройства и сообщает её итог.
 *
 * <h2>⚠️ Что охраняет этот тест</h2>
 * Разбор 10.09.2026: после ввода кода из SMS в собранной APK человек
 * получал ЧЁРНЫЙ экран. Ошибки не было ни одной — ни `ErrorBoundary`,
 * ни глобальный обработчик не срабатывали, а при следующем запуске
 * приложение работало.
 *
 * Причина: `signIn` запускал регистрацию устройства в фоне (`void`).
 * Экран входа тут же уходил на `(tabs)`, а ответ `409` приходил
 * следом и поднимал ВТОРОЙ переход — на `/devices`. Два `replace` по
 * корневому стеку в один кадр, и навигатор не показывал ничего.
 *
 * Починка держится на двух свойствах, и оба ломаются молча:
 * `signIn` должен ДОЖДАТЬСЯ регистрации и ВЕРНУТЬ её статус. Верни он
 * снова `void` — экран потеряет развилку, снова уйдёт на `(tabs)`, и
 * чёрный экран вернётся ровно там же, где был.
 */

jest.mock('@/lib/storage', () => ({
  getItem: jest.fn(async () => null),
  setItem: jest.fn(async () => undefined),
  removeItem: jest.fn(async () => undefined),
}));

jest.mock('@/lib/api', () => ({
  setAuthToken: jest.fn(),
  setTokenRefresher: jest.fn(),
}));

jest.mock('../api', () => ({
  fetchMe: jest.fn(),
  refreshSession: jest.fn(),
}));

/**
 * ⚠️ Префикс `mock` обязателен: фабрика `jest.mock` поднимается выше
 * объявлений, и без него jest запрещает обращение к переменной.
 */
const mockEnsureRegistered = jest.fn();

/** `prime()` — тот самый вызов, которого не было ниоткуда. */
const mockPrime = jest.fn(async () => undefined);

jest.mock('@/features/devices/store', () => ({
  useDeviceStore: { getState: () => ({ ensureRegistered: mockEnsureRegistered, prime: mockPrime, reset: jest.fn() }) },
}));

import type { AuthUser } from '../store';
import { useAuthStore } from '../store';

const USER: AuthUser = {
  id: 'u-1',
  name: 'Diyor',
  phone: '+998900000000',
  email: null,
  avatarUrl: null,
  role: 'user',
};

beforeEach(() => {
  mockEnsureRegistered.mockReset();
  mockPrime.mockClear();
  useAuthStore.setState({ token: null, refreshToken: null, user: null, isAuthorized: false });
});

describe('Вход и регистрация устройства', () => {
  it('Мест нет — вход возвращает limit, и экран знает, куда вести', async () => {
    mockEnsureRegistered.mockResolvedValue('limit');

    const status = await useAuthStore.getState().signIn('t', USER, 'r');

    expect(status).toBe('limit');
    // Сессия при этом сохранена: токен нужен, чтобы показать список
    // устройств и освободить место.
    expect(useAuthStore.getState().isAuthorized).toBe(true);
  });

  it('Место есть — limit не возвращается, человек идёт на главную', async () => {
    mockEnsureRegistered.mockResolvedValue('registered');

    await expect(useAuthStore.getState().signIn('t', USER)).resolves.toBe('registered');
  });

  it('⚠️ Регистрация ЗАВЕРШЕНА до того, как вход вернул управление', async () => {
    let finished = false;
    mockEnsureRegistered.mockImplementation(
      () =>
        new Promise((resolve) => {
          setTimeout(() => {
            finished = true;
            resolve('registered');
          }, 10);
        })
    );

    await useAuthStore.getState().signIn('t', USER);

    // Именно это и было сломано: раньше `signIn` возвращался раньше
    // ответа, и второй переход прилетал уже после первого.
    expect(finished).toBe(true);
  });

  it('Сеть отвалилась — вход не срывается, статус error', async () => {
    mockEnsureRegistered.mockResolvedValue('error');

    await expect(useAuthStore.getState().signIn('t', USER)).resolves.toBe('error');
    expect(useAuthStore.getState().isAuthorized).toBe(true);
  });
});

describe('Ключ устройства уходит в заголовке', () => {
  /**
   * ⚠️ Этот тест держит ВЫЗОВ, а не саму функцию.
   *
   * `prime()` был написан правильно и не вызывался ниоткуда — поймать
   * такое можно только проверкой «его действительно зовут». Без
   * заголовка `X-Device-Id` сервер не отмечает своё устройство
   * (`current`), поэтому «чиqarish» собственного телефона не выводило
   * человека из аккаунта, а refresh-токен оставался не привязан ни к
   * какому устройству.
   */
  it('restore зовёт prime до всего остального', async () => {
    mockEnsureRegistered.mockResolvedValue('registered');

    await useAuthStore.getState().restore();

    expect(mockPrime).toHaveBeenCalledTimes(1);
  });
});
