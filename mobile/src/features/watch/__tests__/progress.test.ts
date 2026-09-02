/**
 * «Продолжить просмотр» — правила возобновления.
 *
 * <h2>⚠️ Почему это нужно тестами, а не глазами</h2>
 * Ошибки здесь выглядят как ПОТЕРЯ ПРОСМОТРА: человек возвращается к
 * фильму и попадает не туда. Проверить руками почти невозможно —
 * нужны два устройства, отключённая сеть и полтора часа видео.
 *
 * Три места, где легко ошибиться и дорого ошибиться: выбор свежей
 * копии, порог возобновления и сохранение при закрытии.
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

jest.mock('../progressApi', () => ({
  fetchProgress: jest.fn(),
  saveProgress: jest.fn(),
}));

import { fetchProgress, saveProgress, type WatchProgress } from '../progressApi';
import {
  MIN_RESUME_SECONDS,
  newer,
  persist,
  readLocal,
  restore,
  resumePosition,
  storageKey,
  writeLocal,
} from '../progress';

const fetchMock = fetchProgress as jest.MockedFunction<typeof fetchProgress>;
const saveMock = saveProgress as jest.MockedFunction<typeof saveProgress>;

/** Полный ответ сервера — чтобы не повторять поля в каждом тесте. */
function server(over: Partial<WatchProgress>): WatchProgress {
  return {
    type: 'CONTENT',
    targetId: 1,
    position: 0,
    duration: 7200,
    quality: 'auto',
    completed: false,
    percent: 0,
    updatedAt: '2026-09-01T10:00:00',
    ...over,
  };
}

beforeEach(() => {
  mockStorage.clear();
  jest.clearAllMocks();
  fetchMock.mockReset();
  saveMock.mockReset();
  fetchMock.mockResolvedValue(null);
  saveMock.mockResolvedValue(null);
});

describe('resumePosition', () => {
  it('Возвращает сохранённую секунду', () => {
    expect(resumePosition({ position: 5565, duration: 7200 })).toBe(5565);
  });

  /**
   * ⚠️ `null`, а НЕ ноль.
   *
   * Ноль — это тоже позиция, и вызывающий не отличил бы «начать
   * сначала» от «перематывать не надо». Разница видна на экране:
   * перемотка играющего видео на ноль дёргает картинку.
   */
  it('Ничего не сохранено — null, а не 0', () => {
    expect(resumePosition(null)).toBeNull();
  });

  /**
   * ⚠️ Человек мог открыть видео и сразу закрыть. Прыжок на пятую
   * секунду выглядел бы как сбой плеера.
   */
  it('Первые секунды не возобновляются', () => {
    expect(resumePosition({ position: MIN_RESUME_SECONDS - 1, duration: 7200 })).toBeNull();
    expect(resumePosition({ position: MIN_RESUME_SECONDS, duration: 7200 })).toBe(
      MIN_RESUME_SECONDS
    );
  });

  /**
   * ⚠️ Досмотревший фильм попадал бы прямо на титры и не смог бы
   * начать сначала, не перемотав вручную.
   */
  it('Досмотренное до конца не возобновляется', () => {
    expect(resumePosition({ position: 7100, duration: 7200 })).toBeNull();
  });

  /**
   * ⚠️ Длительность неизвестна, пока не загрузились метаданные —
   * тогда порог «почти конец» посчитать не из чего, но позиция
   * по-прежнему верна.
   */
  it('Без длительности возобновление работает', () => {
    expect(resumePosition({ position: 4000, duration: null })).toBe(4000);
  });
});

describe('newer — какая копия свежее', () => {
  const local = { position: 100, duration: 7200, quality: 'auto', savedAt: 2000 };

  it('Есть только телефон', () => {
    expect(newer(local, null)?.position).toBe(100);
  });

  it('Есть только сервер', () => {
    expect(newer(null, server({ position: 500 }))?.position).toBe(500);
  });

  it('Нет ничего', () => {
    expect(newer(null, null)).toBeNull();
  });

  /**
   * ⚠️ Последний сеанс мог пройти без сети: на телефоне позиция
   * свежее, на сервере — вчерашняя. Правило «сервер прав» отбросило
   * бы час просмотра.
   */
  it('Телефон новее сервера — берётся телефон', () => {
    const result = newer(
      { ...local, position: 5000, savedAt: Date.parse('2026-09-02T10:00:00') },
      server({ position: 100, updatedAt: '2026-09-01T10:00:00' })
    );
    expect(result?.position).toBe(5000);
  });

  /**
   * ⚠️ Человек мог продолжить на другом устройстве. Правило
   * «телефон прав» потеряло бы тот сеанс.
   */
  it('Сервер новее телефона — берётся сервер', () => {
    const result = newer(
      { ...local, position: 100, savedAt: Date.parse('2026-09-01T10:00:00') },
      server({ position: 5000, updatedAt: '2026-09-02T10:00:00' })
    );
    expect(result?.position).toBe(5000);
  });

  /**
   * ⚠️ `Date.parse` на нечитаемой строке даёт `NaN`, а любое
   * сравнение с `NaN` ложно. Без защиты сервер молча выигрывал бы
   * всегда — даже когда телефон свежее.
   */
  it('Нечитаемая дата сервера не выигрывает у телефона', () => {
    const result = newer(
      { ...local, position: 5000, savedAt: 1 },
      server({ position: 100, updatedAt: 'не дата' })
    );
    expect(result?.position).toBe(5000);
  });
});

describe('Локальное хранилище', () => {
  it('Записывается и читается', async () => {
    await writeLocal('CONTENT', 5, { position: 1234, duration: 7200, quality: '720p' });

    const read = await readLocal('CONTENT', 5);
    expect(read?.position).toBe(1234);
    expect(read?.quality).toBe('720p');
    expect(read?.savedAt).toBeGreaterThan(0);
  });

  /** ⚠️ Серия и контент с одним номером — РАЗНЫЕ записи. */
  it('EPISODE и CONTENT не смешиваются', async () => {
    await writeLocal('EPISODE', 7, { position: 100, duration: null, quality: null });
    await writeLocal('CONTENT', 7, { position: 900, duration: null, quality: null });

    expect((await readLocal('EPISODE', 7))?.position).toBe(100);
    expect((await readLocal('CONTENT', 7))?.position).toBe(900);
  });

  /**
   * ⚠️ Испорченная строка в хранилище НЕ должна ронять просмотр —
   * человек не смог бы открыть видео вообще.
   */
  it('Испорченная запись читается как «ничего нет»', async () => {
    mockStorage.set(storageKey('CONTENT', 5), '{это не json');
    expect(await readLocal('CONTENT', 5)).toBeNull();
  });
});

describe('persist — сохранение', () => {
  it('Пишет на телефон и на сервер', async () => {
    await persist('CONTENT', 5, 1234, 7200, 'auto', true);

    expect((await readLocal('CONTENT', 5))?.position).toBe(1234);
    expect(saveMock).toHaveBeenCalledWith('CONTENT', 5, 1234, 7200, 'auto');
  });

  /** Промежуточный тик: телефон — да, сервер — нет. */
  it('Без флага на сервер не ходит', async () => {
    await persist('CONTENT', 5, 1234, 7200, 'auto', false);

    expect((await readLocal('CONTENT', 5))?.position).toBe(1234);
    expect(saveMock).not.toHaveBeenCalled();
  });

  /**
   * ⚠️ САМОЕ ВАЖНОЕ ЗДЕСЬ.
   *
   * Сеть отвалилась или запись запрещена `READ_ONLY` — просмотр
   * продолжается, а позиция остаётся на телефоне. Без этого одна
   * сетевая ошибка роняла бы воспроизведение целиком.
   */
  it('Ошибка сервера не мешает сохранить на телефон', async () => {
    saveMock.mockRejectedValue(new Error('нет сети'));

    await expect(persist('CONTENT', 5, 1234, 7200, 'auto', true)).resolves.toBeUndefined();
    expect((await readLocal('CONTENT', 5))?.position).toBe(1234);
  });
});

describe('restore — откуда продолжить', () => {
  it('Ничего не сохранено — не перематываем', async () => {
    expect(await restore('CONTENT', 5)).toBeNull();
  });

  it('Берётся сохранённая позиция', async () => {
    await writeLocal('CONTENT', 5, { position: 5565, duration: 7200, quality: 'auto' });
    expect(await restore('CONTENT', 5)).toBe(5565);
  });

  /**
   * ⚠️ Не вошёл, нет сети или старая сборка бэкенда — локальной
   * записи достаточно. Иначе возобновление на этом телефоне
   * ломалось бы из-за недоступности сервера.
   */
  it('Ошибка сервера не отменяет возобновление', async () => {
    fetchMock.mockRejectedValue(new Error('нет сети'));
    await writeLocal('CONTENT', 5, { position: 5565, duration: 7200, quality: 'auto' });

    expect(await restore('CONTENT', 5)).toBe(5565);
  });

  /** Другое устройство продолжило дальше — берём серверную. */
  it('Свежая серверная позиция побеждает старую локальную', async () => {
    await writeLocal('CONTENT', 5, { position: 100, duration: 7200, quality: 'auto' });
    fetchMock.mockResolvedValue(
      server({ position: 5565, updatedAt: '2099-01-01T00:00:00' })
    );

    expect(await restore('CONTENT', 5)).toBe(5565);
  });

  /**
   * ⚠️ Досмотренное до конца не возобновляется даже с сервера —
   * иначе человек попадал бы на титры.
   */
  it('Досмотренное с сервера не возобновляется', async () => {
    fetchMock.mockResolvedValue(server({ position: 7100, completed: true }));
    expect(await restore('CONTENT', 5)).toBeNull();
  });
});
