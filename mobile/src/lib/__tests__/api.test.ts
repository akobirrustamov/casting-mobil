import { AxiosError } from 'axios';
import type { AxiosRequestConfig, AxiosResponse } from 'axios';

import { READ_ONLY, api, setAuthToken, setTokenRefresher } from '../api';

/**
 * Обновление истёкшего токена.
 *
 * <h2>⚠️ Почему это первый тест мобильного приложения</h2>
 * Здесь ошибка не видна ни компилятору, ни на глаз: код выглядит
 * правильным, экран открывается, а ломается только при совпадении
 * во времени — когда несколько запросов получают 401 одновременно.
 *
 * И цена ошибки высокая. На бэкенде включена ротация: refresh-токен
 * срабатывает ровно один раз, а повторное использование погашенного
 * считается кражей и закрывает ВСЕ сессии пользователя. То есть
 * починка «человека выкидывает каждые 15 минут» при неверной
 * реализации сама бы его и выкидывала — причём совсем.
 *
 * <h2>Как устроен тест</h2>
 * Подменяется транспорт axios (`adapter`), а не сеть: интерцептор —
 * это и есть предмет проверки, и он должен отработать целиком,
 * включая повтор запроса.
 */

type Handler = (config: AxiosRequestConfig) => { status: number };

/** Ответы отдаёт этот обработчик — его задаёт каждый тест. */
let handler: Handler;

/** Все запросы, дошедшие до транспорта, — включая повторы. */
let sent: AxiosRequestConfig[] = [];

const originalAdapter = api.defaults.adapter;

beforeEach(() => {
  sent = [];
  setAuthToken('eski-token');

  api.defaults.adapter = async (config): Promise<AxiosResponse> => {
    sent.push(config);
    const { status } = handler(config);

    const response = {
      status,
      statusText: '',
      data: {},
      headers: {},
      config,
    } as AxiosResponse;

    // ⚠️ Превращать не-2xx в ошибку обязан САМ адаптер — в axios это
    // делает `settle()`. Если просто вернуть ответ, 401 придёт как
    // успех, интерцептор не сработает, и тест проверял бы не тот
    // путь, по которому идёт настоящий сбой.
    if (status >= 200 && status < 300) {
      return response;
    }
    throw new AxiosError(
      `Request failed with status code ${status}`,
      AxiosError.ERR_BAD_REQUEST,
      config as never,
      null,
      response,
    );
  };
});

afterEach(() => {
  api.defaults.adapter = originalAdapter;
  setTokenRefresher(null);
  setAuthToken(null);
});

/** Первый запрос — 401, все последующие — 200. */
function expiredOnce(): void {
  let first = true;
  handler = () => {
    if (first) {
      first = false;
      return { status: 401 };
    }
    return { status: 200 };
  };
}

describe('Продление сессии', () => {
  it('после 401 запрос повторяется и проходит', async () => {
    expiredOnce();
    setTokenRefresher(async () => 'yangi-token');

    const response = await api.get('/api/v1/app/home');

    expect(response.status).toBe(200);
    expect(sent).toHaveLength(2);
  });

  /**
   * ⚠️ Повтор должен уйти с НОВЫМ токеном.
   *
   * Со старым он получил бы тот же 401, и обновление оказалось бы
   * бессмысленным — человек всё равно увидел бы экран входа.
   */
  it('повтор уходит с новым токеном', async () => {
    expiredOnce();
    setTokenRefresher(async () => 'yangi-token');

    await api.get('/api/v1/app/home');

    expect(sent[1].headers?.Authorization).toBe('Bearer yangi-token');
    expect(sent[0].headers?.Authorization).toBe('Bearer eski-token');
  });

  /**
   * ⚠️ САМЫЙ ВАЖНЫЙ ТЕСТ.
   *
   * Экран открывает несколько запросов сразу. Если у каждого будет
   * своё обновление, первое пройдёт, а остальные придут с уже
   * погашенным токеном — бэкенд расценит это как кражу и закроет все
   * сессии пользователя.
   */
  it('параллельные 401 дают РОВНО ОДНО обновление', async () => {
    let expired = true;
    handler = () => ({ status: expired ? 401 : 200 });

    let refreshes = 0;
    setTokenRefresher(async () => {
      refreshes += 1;
      // Сеть отвечает не мгновенно — без этого гонка не воспроизводится.
      await new Promise((resolve) => setTimeout(resolve, 10));
      expired = false;
      return 'yangi-token';
    });

    await Promise.all([
      api.get('/api/v1/app/home'),
      api.get('/api/v1/app/content/1/episodes'),
      api.get('/api/v1/app/watch/1'),
      api.get('/api/v1/app/media/1/raw'),
    ]);

    expect(refreshes).toBe(1);
  });

  /** Следующий 401 (уже после успешного продления) обновляет снова. */
  it('обновление возможно повторно', async () => {
    let refreshes = 0;
    setTokenRefresher(async () => {
      refreshes += 1;
      return 'yangi-token';
    });

    expiredOnce();
    await api.get('/api/v1/app/home');

    expiredOnce();
    await api.get('/api/v1/app/home');

    expect(refreshes).toBe(2);
  });
});

describe('Когда продлевать нельзя', () => {
  /** Сессия закончилась — вызывающий должен увидеть честный 401. */
  it('без refresh-токена ошибка доходит до вызывающего', async () => {
    handler = () => ({ status: 401 });
    setTokenRefresher(async () => null);

    await expect(api.get('/api/v1/app/home')).rejects.toMatchObject({
      response: { status: 401 },
    });
    expect(sent).toHaveLength(1);
  });

  it('без зарегистрированного обработчика ничего не ломается', async () => {
    handler = () => ({ status: 401 });

    await expect(api.get('/api/v1/app/home')).rejects.toMatchObject({
      response: { status: 401 },
    });
  });

  /**
   * ⚠️ Повторяем ровно один раз.
   *
   * Если и обновлённый токен получает 401 — дело не в сроке жизни.
   * Без этого ограничения запрос уходил бы на второй круг и дальше,
   * бесконечно, вместо честной ошибки на экране.
   */
  it('повторный 401 НЕ уходит на второй круг', async () => {
    handler = () => ({ status: 401 });

    let refreshes = 0;
    setTokenRefresher(async () => {
      refreshes += 1;
      return 'yangi-token';
    });

    await expect(api.get('/api/v1/app/home')).rejects.toMatchObject({
      response: { status: 401 },
    });

    expect(refreshes).toBe(1);
    expect(sent).toHaveLength(2);
  });

  /**
   * ⚠️ 401 на входе — это «неверный код», а не «истёк токен».
   *
   * Без исключения этих путей ошибка в SMS-коде запускала бы
   * обновление, а сам эндпоинт обновления при отказе дёргал бы себя
   * же — бесконечный цикл на экране входа.
   */
  it.each([
    '/api/v1/app/auth/otp/verify',
    '/api/v1/app/auth/refresh',
    '/api/v1/auth/google',
  ])('401 на %s не запускает обновление', async (url) => {
    handler = () => ({ status: 401 });

    let refreshes = 0;
    setTokenRefresher(async () => {
      refreshes += 1;
      return 'yangi-token';
    });

    await expect(api.post(url, {})).rejects.toMatchObject({
      response: { status: 401 },
    });

    expect(refreshes).toBe(0);
    expect(sent).toHaveLength(1);
  });
});

describe('Другие ответы не трогаем', () => {
  /**
   * ⚠️ 403 — «прав нет», и обновление токена этого не изменит.
   * Лишний обмен сжигал бы refresh-токен на пустом месте.
   */
  it.each([403, 404, 500])('%i не запускает обновление', async (status) => {
    handler = () => ({ status });

    let refreshes = 0;
    setTokenRefresher(async () => {
      refreshes += 1;
      return 'yangi-token';
    });

    await expect(api.get('/api/v1/app/home')).rejects.toBeDefined();

    expect(refreshes).toBe(0);
  });

  it('успешный ответ проходит как обычно', async () => {
    handler = () => ({ status: 200 });

    await expect(api.get('/api/v1/app/home')).resolves.toMatchObject({
      status: 200,
    });
    expect(sent).toHaveLength(1);
  });
});

/**
 * Защита боевой базы от записи из отладочной сборки.
 *
 * <h2>⚠️ Что здесь легко сломать незаметно</h2>
 * Интерцептор роняет запрос ДО отправки. Значит ошибка выглядит не
 * как отказ сервера, а как внутренний сбой клиента — и разбираться
 * в ней будут не там, где она возникла. Ровно это уже случалось с
 * OTP, когда он переехал в новое пространство адресов.
 *
 * Проверки идут при `READ_ONLY = true` — это состояние по умолчанию
 * и именно оно работает у всех, кто не трогал `.env`.
 */
describe('Read-only режим', () => {
  beforeEach(() => {
    handler = () => ({ status: 200 });
  });

  it('по умолчанию включён', () => {
    expect(READ_ONLY).toBe(true);
  });

  it('чтение проходит всегда', async () => {
    await expect(api.get('/api/v1/app/home')).resolves.toMatchObject({ status: 200 });
  });

  /**
   * ⚠️ Это и есть смысл режима: деньги и чужой контент в живой базе.
   */
  it.each([
    '/api/v1/app/donations',
    '/api/v1/app/analytics/events',
    '/api/v1/app/purchases',
  ])('запись на %s заблокирована', async (url) => {
    await expect(api.post(url, {})).rejects.toThrow(/READ_ONLY/);
    expect(sent).toHaveLength(0);
  });

  it('вход разрешён — иначе в приложение не попасть', async () => {
    await expect(api.post('/api/v1/app/auth/otp/send', {})).resolves.toBeDefined();
    await expect(api.post('/api/v1/auth/google', {})).resolves.toBeDefined();
  });

  /**
   * ⚠️ Точечное исключение: собственные предпочтения вошедшего
   * человека, без денег и чужих данных.
   *
   * Без него синхронизация избранного мертва целиком — список
   * остаётся на телефоне ровно как до починки.
   */
  it.each(['post', 'delete'] as const)(
    'избранное разрешено (%s)',
    async (method) => {
      const response = method === 'post'
        ? await api.post('/api/v1/app/favorites', {})
        : await api.delete('/api/v1/app/favorites');

      expect(response.status).toBe(200);
      expect(sent).toHaveLength(1);
    },
  );

  /**
   * ⚠️ Разрешение НЕ должно расползаться на соседние адреса.
   *
   * Список сравнивается по началу строки, поэтому слишком короткий
   * префикс открыл бы и то, что открывать не собирались.
   */
  it('разрешение не распространяется на соседние адреса', async () => {
    await expect(api.post('/api/v1/app/favorites-import', {})).rejects.toThrow(/READ_ONLY/);
  });
});
