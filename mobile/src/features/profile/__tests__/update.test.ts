/**
 * Изменение профиля и язык на сервере.
 *
 * <h2>⚠️ Почему это нужно тестами</h2>
 * Язык уезжает на сервер САМ, из обработчика, который модуль
 * регистрирует при импорте — то есть ни один экран его не отправляет
 * явно, и увидеть поломку глазами нельзя. Ошибка проявилась бы только
 * после подключения FCM: человек, выбравший русский, получал бы
 * узбекский push, и причину искали бы в рассылке, а не здесь.
 *
 * Второе: гость не должен ничего слать. Запрос без токена вернёт 401, а
 * интерцептор попробует продлить сессию — на экране онбординга, где
 * сессии ещё нет.
 */

jest.mock('@/lib/api', () => ({
  api: { put: jest.fn(), get: jest.fn(), post: jest.fn() },
}));

jest.mock('@/features/auth/store', () => {
  const state = { isAuthorized: false };
  return {
    useAuthStore: Object.assign(jest.fn(), {
      getState: () => state,
      // Тесты меняют состояние через эту дверь: `getState` замкнут на
      // объект, созданный внутри фабрики, и снаружи его не достать.
      __state: state,
    }),
  };
});

jest.mock('@/i18n/storage', () => ({ setLanguageSync: jest.fn() }));

import { useAuthStore } from '@/features/auth/store';
import { setLanguageSync } from '@/i18n/storage';
import { api } from '@/lib/api';

import { updateProfile } from '../api';

const put = api.put as jest.Mock;
const state = (useAuthStore as unknown as { __state: { isAuthorized: boolean } }).__state;

/**
 * Обработчик, который модуль зарегистрировал при импорте.
 *
 * ⚠️ Берётся из вызова мока, а не из переменной теста: `import`
 * поднимается выше объявлений, и обычная переменная была бы затёрта
 * своим же инициализатором уже ПОСЛЕ регистрации.
 */
const registered = (setLanguageSync as jest.Mock).mock.calls[0]?.[0] as
  | ((language: string) => void)
  | undefined;

beforeEach(() => {
  put.mockReset();
  put.mockResolvedValue({ data: {} });
  state.isAuthorized = false;
});

describe('updateProfile', () => {
  it('язык интерфейса переводится в значение бэкенда', async () => {
    await updateProfile({ language: 'ru' });

    expect(put).toHaveBeenCalledWith('/api/v1/app/me', {
      name: undefined,
      language: 'RU',
    });
  });

  /** Не переданное поле не трогается — иначе смена языка стёрла бы имя. */
  it('имя без языка уходит одно', async () => {
    await updateProfile({ name: 'Ali Valiyev' });

    expect(put).toHaveBeenCalledWith('/api/v1/app/me', {
      name: 'Ali Valiyev',
      language: undefined,
    });
  });
});

describe('язык → сервер', () => {
  it('обработчик зарегистрирован при импорте модуля', () => {
    expect(registered).toBeInstanceOf(Function);
  });

  /**
   * ⚠️ У гостя языка на сервере нет. Запрос ушёл бы в 401 и разбудил
   * продление сессии там, где сессии не существует.
   */
  it('гость ничего не отправляет', () => {
    state.isAuthorized = false;

    registered?.('ru');

    expect(put).not.toHaveBeenCalled();
  });

  it('вошедший отправляет выбор', async () => {
    state.isAuthorized = true;

    registered?.('en');
    await Promise.resolve();

    expect(put).toHaveBeenCalledWith('/api/v1/app/me', {
      name: undefined,
      language: 'EN',
    });
  });

  /**
   * ⚠️ Сбой сети не должен всплывать наружу: язык интерфейса уже
   * переключился, и необработанный отказ промиса уронил бы экран.
   */
  it('ошибка сети не выбрасывается наружу', async () => {
    state.isAuthorized = true;
    put.mockRejectedValue(new Error('Network Error'));

    expect(() => registered?.('uz')).not.toThrow();
    await Promise.resolve();
    await Promise.resolve();
  });
});
