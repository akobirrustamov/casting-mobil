/**
 * Синхронизация избранного.
 *
 * <h2>⚠️ Почему это нужно тестами, а не глазами</h2>
 * Ошибки здесь выглядят как ПОТЕРЯ ДАННЫХ, и человек замечает их
 * позже — когда открывает второе устройство и не находит того, что
 * «точно сохранял». Ни компилятор, ни ручная проверка на одном
 * телефоне такого не покажут.
 *
 * Три места, где легко ошибиться и дорого ошибиться:
 * слияние при входе, откат при сбое сети, очистка при выходе.
 */

const mockStorage = new Map<string, string>();

jest.mock('@/lib/storage', () => ({
  getItem: jest.fn(async (key: string) => mockStorage.get(key) ?? null),
  setItem: jest.fn(async (key: string, value: string) => {
    mockStorage.set(key, value);
  }),
  removeItem: jest.fn(async (key: string) => {
    mockStorage.delete(key);
  }),
}));

// ⚠️ READ_ONLY выключаем: с ним синхронизация намеренно не работает
// (приложение смотрит в боевую базу), и все проверки ниже были бы
// бессмысленно зелёными.
jest.mock('@/lib/api', () => ({
  READ_ONLY: false,
  setAuthToken: jest.fn(),
  setTokenRefresher: jest.fn(),
  api: {},
}));

jest.mock('../api', () => ({
  MAX_BATCH: 200,
  fetchFavorites: jest.fn(),
  addFavorites: jest.fn(),
  removeFavorite: jest.fn(),
}));

import { useAuthStore } from '@/features/auth/store';

import { addFavorites, fetchFavorites, removeFavorite } from '../api';
import { useFavoritesStore } from '../store';

const mockAdd = addFavorites as jest.MockedFunction<typeof addFavorites>;
const mockFetch = fetchFavorites as jest.MockedFunction<typeof fetchFavorites>;
const mockRemove = removeFavorite as jest.MockedFunction<typeof removeFavorite>;

const KEY = 'uzcasting.favorites';

function saved(): number[] {
  const raw = mockStorage.get(KEY);
  return raw ? JSON.parse(raw) : [];
}

/**
 * ⚠️ Порядок здесь важен.
 *
 * Смена `isAuthorized` дёргает НАСТОЯЩУЮ подписку стора — ту самую,
 * что запускает синхронизацию. Поэтому сначала переводим авторизацию
 * в нужное состояние и даём её последствиям отработать, и только
 * потом расставляем данные теста. Иначе фоновая синхронизация из
 * `beforeEach` дописывала бы своё поверх и тесты «плавали» бы.
 */
beforeEach(async () => {
  jest.clearAllMocks();
  mockAdd.mockReset();
  mockFetch.mockReset();
  mockRemove.mockReset();

  // Большинство проверок — про вошедшего человека.
  useAuthStore.setState({ isAuthorized: true });
  await flush();

  mockStorage.clear();
  mockAdd.mockReset();
  mockFetch.mockReset();
  mockRemove.mockReset();
  useFavoritesStore.setState({ ids: new Set(), isRestoring: true, isSynced: false });
});

describe('Слияние при входе', () => {
  /**
   * ⚠️ САМАЯ ДОРОГАЯ ОШИБКА.
   *
   * Человек отмечает избранное ещё до регистрации. Если вход
   * ЗАМЕНЯЕТ список серверным, всё отмеченное исчезает в момент,
   * когда он наконец решился войти.
   */
  it('локальный список УХОДИТ на сервер, а не затирается', async () => {
    mockStorage.set(KEY, JSON.stringify([1, 2]));
    mockAdd.mockResolvedValue([1, 2, 3]);

    await useFavoritesStore.getState().restore();
    await useFavoritesStore.getState().sync();

    expect(mockAdd).toHaveBeenCalledWith([1, 2]);
    expect([...useFavoritesStore.getState().ids]).toEqual([1, 2, 3]);
  });

  /** Отмеченное на другом устройстве тоже должно вернуться. */
  it('результат слияния сохраняется на диск', async () => {
    mockStorage.set(KEY, JSON.stringify([1]));
    mockAdd.mockResolvedValue([1, 9]);

    await useFavoritesStore.getState().restore();
    await useFavoritesStore.getState().sync();

    expect(saved()).toEqual([1, 9]);
  });

  it('пустой локальный список — просто читаем серверный', async () => {
    mockFetch.mockResolvedValue([5]);

    await useFavoritesStore.getState().restore();
    await useFavoritesStore.getState().sync();

    expect(mockAdd).not.toHaveBeenCalled();
    expect([...useFavoritesStore.getState().ids]).toEqual([5]);
  });

  /**
   * ⚠️ ГОНКА.
   *
   * Восстановление профиля и восстановление избранного стартуют
   * параллельно (`app/_layout.tsx`). Если вход успеет раньше диска,
   * слияние уйдёт с ПУСТЫМ списком — и всё отмеченное до входа
   * пропадёт, хотя на диске оно лежит.
   */
  it('ждёт локальный список, если он ещё не поднят с диска', async () => {
    mockStorage.set(KEY, JSON.stringify([7]));
    mockAdd.mockResolvedValue([7]);

    // `restore()` НЕ вызван — состояние ещё `isRestoring`.
    await useFavoritesStore.getState().sync();

    expect(mockAdd).toHaveBeenCalledWith([7]);
  });

  it('сбой сети оставляет локальный список рабочим', async () => {
    mockStorage.set(KEY, JSON.stringify([1, 2]));
    mockAdd.mockRejectedValue(new Error('сеть'));

    await useFavoritesStore.getState().restore();
    await useFavoritesStore.getState().sync();

    expect([...useFavoritesStore.getState().ids]).toEqual([1, 2]);
  });
});

describe('Переключение', () => {
  beforeEach(() => {
    useFavoritesStore.setState({ isRestoring: false, isSynced: true });
  });

  it('добавляет мгновенно, до ответа сервера', async () => {
    let resolve: (v: number[]) => void = () => {};
    mockAdd.mockReturnValue(new Promise((r) => { resolve = r; }));

    const pending = useFavoritesStore.getState().toggle(4);
    // Ответа ещё нет, а сердечко уже закрашено.
    expect(useFavoritesStore.getState().ids.has(4)).toBe(true);

    resolve([4]);
    await pending;
  });

  it('удаляет через сервер', async () => {
    useFavoritesStore.setState({ ids: new Set([4]) });
    mockRemove.mockResolvedValue([]);

    await useFavoritesStore.getState().toggle(4);

    expect(mockRemove).toHaveBeenCalledWith(4);
    expect([...useFavoritesStore.getState().ids]).toEqual([]);
  });

  /**
   * ⚠️ Без отката сердечко осталось бы закрашенным, а на сервере
   * ничего не изменилось. На другом устройстве человек не нашёл бы
   * то, что «точно сохранял».
   *
   * Молчаливое расхождение хуже, чем невыполненное нажатие.
   */
  it('при сбое сервера возвращает прежнее состояние', async () => {
    mockAdd.mockRejectedValue(new Error('сеть'));

    await useFavoritesStore.getState().toggle(4);

    expect(useFavoritesStore.getState().ids.has(4)).toBe(false);
    expect(saved()).toEqual([]);
  });

  it('откат работает и для удаления', async () => {
    useFavoritesStore.setState({ ids: new Set([4]) });
    await persistDirect([4]);
    mockRemove.mockRejectedValue(new Error('сеть'));

    await useFavoritesStore.getState().toggle(4);

    expect(useFavoritesStore.getState().ids.has(4)).toBe(true);
    expect(saved()).toEqual([4]);
  });

  /**
   * До сверки с сервером трогать его нельзя: локальный список ещё
   * не отдан, и одиночное добавление ушло бы раньше слияния.
   */
  it('до синхронизации сервер не дёргается', async () => {
    useFavoritesStore.setState({ isSynced: false });

    await useFavoritesStore.getState().toggle(4);

    expect(mockAdd).not.toHaveBeenCalled();
    expect(useFavoritesStore.getState().ids.has(4)).toBe(true);
  });
});

describe('Вход и выход', () => {
  it('вход запускает синхронизацию', async () => {
    mockFetch.mockResolvedValue([]);
    useFavoritesStore.setState({ isRestoring: false });

    useAuthStore.setState({ isAuthorized: false });
    await flush();
    useAuthStore.setState({ isAuthorized: true });
    await flush();

    expect(mockFetch).toHaveBeenCalled();
  });

  /**
   * ⚠️ Иначе следующий вошедший на этом телефоне человек влил бы
   * ЧУЖОЕ избранное в свой аккаунт.
   */
  it('выход очищает локальный список', async () => {
    useFavoritesStore.setState({ ids: new Set([1, 2]), isSynced: true });
    await persistDirect([1, 2]);

    useAuthStore.setState({ isAuthorized: false });
    await flush();

    expect([...useFavoritesStore.getState().ids]).toEqual([]);
    expect(mockStorage.has(KEY)).toBe(false);
  });

  /**
   * ⚠️ ГОНКА, найденная тестом.
   *
   * Синхронизация уходит на сервер при входе. Если человек выйдет,
   * пока запрос в пути, ответ вернётся УЖЕ ПОСЛЕ очистки — и ляжет
   * на диск. На телефоне осталось бы избранное предыдущего
   * владельца сессии: ровно то, что очистка и должна предотвратить.
   */
  it('ответ, пришедший ПОСЛЕ выхода, не возвращает список', async () => {
    mockStorage.set(KEY, JSON.stringify([1]));
    useFavoritesStore.setState({ ids: new Set([1]), isRestoring: false });

    let resolve: (v: number[]) => void = () => {};
    mockAdd.mockReturnValue(new Promise((r) => { resolve = r; }));

    const pending = useFavoritesStore.getState().sync();

    // Человек вышел, пока запрос был в пути.
    useAuthStore.setState({ isAuthorized: false });
    await flush();

    // И только теперь ответил сервер.
    resolve([1, 2]);
    await pending;

    // Чужое избранное не должно вернуться на устройство.
    expect([...useFavoritesStore.getState().ids]).toEqual([]);
    expect(mockStorage.has(KEY)).toBe(false);
  });

  it('выход сбрасывает признак синхронизации', async () => {
    useFavoritesStore.setState({ isSynced: true });

    useAuthStore.setState({ isAuthorized: false });
    await flush();

    expect(useFavoritesStore.getState().isSynced).toBe(false);
  });
});

/** Подписка на стор асинхронная — даём микрозадачам отработать. */
async function flush(): Promise<void> {
  await new Promise((r) => setTimeout(r, 0));
}

async function persistDirect(ids: number[]): Promise<void> {
  mockStorage.set(KEY, JSON.stringify(ids));
}
