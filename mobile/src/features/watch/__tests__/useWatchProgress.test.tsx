/**
 * Подключение «продолжить просмотр» к плееру.
 *
 * <h2>⚠️ Что здесь можно сломать молча</h2>
 * `timeUpdateEventInterval` по умолчанию равен НУЛЮ, и это значит
 * «событие не отправлять вовсе» (docs.expo.dev, SDK 57). Забыли
 * строку — обработчик не вызовется ни разу, позиция не сохранится
 * НИКОГДА, и при этом не будет ни ошибки, ни предупреждения.
 *
 * Такую поломку не видно ни в типах, ни на экране: видео играет,
 * просто «продолжить» не работает. Поэтому она проверяется тестом.
 */

const mockStorage = new Map<string, string>();

jest.mock('@/lib/storage', () => ({
  getItem: jest.fn(async (key: string) => mockStorage.get(key) ?? null),
  setItem: jest.fn(async (key: string, value: string) => {
    mockStorage.set(key, value);
  }),
  removeItem: jest.fn(async () => undefined),
}));

jest.mock('../progressApi', () => ({
  fetchProgress: jest.fn(),
  saveProgress: jest.fn(),
}));

/**
 * `useEventListener` подменяется, чтобы ВЫЗВАТЬ обработчики вручную.
 *
 * Настоящий плеер здесь поднять нельзя — он нативный. Нам же нужны
 * не кадры, а решения: что происходит на очередном тике времени.
 */
// ⚠️ Имя обязано начинаться с `mock`: фабрика `jest.mock` поднимается
// выше объявлений, и Jest пропускает в неё только такие переменные.
const mockHandlers = new Map<string, (payload: unknown) => void>();

jest.mock('expo', () => ({
  useEventListener: (_player: unknown, event: string, handler: (p: unknown) => void) => {
    mockHandlers.set(event, handler);
  },
}));

import { act, create } from 'react-test-renderer';

import { fetchProgress, saveProgress } from '../progressApi';
import { readLocal, writeLocal } from '../progress';
import { useWatchProgress } from '../useWatchProgress';

const fetchMock = fetchProgress as jest.MockedFunction<typeof fetchProgress>;
const saveMock = saveProgress as jest.MockedFunction<typeof saveProgress>;

/** Заглушка плеера: только те поля, которых касается хук. */
function fakePlayer(duration = 7200) {
  return { timeUpdateEventInterval: 0, currentTime: 0, duration };
}

function Harness({ player, targetId }: { player: unknown; targetId: number | null }) {
  useWatchProgress({
    player: player as never,
    type: 'CONTENT',
    targetId,
  });
  return null;
}

async function mount(player: unknown, targetId: number | null = 5) {
  let tree: ReturnType<typeof create> | undefined;
  await act(async () => {
    tree = create(<Harness player={player} targetId={targetId} />);
  });
  return tree as ReturnType<typeof create>;
}

/** Тик времени от плеера. */
async function tick(seconds: number) {
  await act(async () => {
    mockHandlers.get('timeUpdate')?.({ currentTime: seconds });
  });
}

beforeEach(() => {
  mockStorage.clear();
  mockHandlers.clear();
  jest.clearAllMocks();
  fetchMock.mockReset();
  saveMock.mockReset();
  fetchMock.mockResolvedValue(null);
  saveMock.mockResolvedValue(null);
});

/**
 * ⚠️ САМАЯ ВАЖНАЯ ПРОВЕРКА ФАЙЛА.
 *
 * Ноль означает «не присылать событие». Без явной установки хук
 * выглядел бы рабочим, а позиция не сохранялась бы ни разу.
 */
it('Включает событие времени — иначе оно не придёт ни разу', async () => {
  const player = fakePlayer();
  await mount(player);

  expect(player.timeUpdateEventInterval).toBeGreaterThan(0);
});

it('На тике позиция уходит на телефон', async () => {
  await mount(fakePlayer());
  await tick(120);

  expect((await readLocal('CONTENT', 5))?.position).toBe(120);
});

/**
 * ⚠️ На сервер — РЕЖЕ, чем на телефон.
 *
 * Плеер шлёт время постоянно. Запрос на каждый тик означал бы
 * несколько обращений в секунду на каждого зрителя.
 */
it('На сервер уходит не на каждом тике', async () => {
  await mount(fakePlayer());

  await tick(5);
  await tick(10);
  expect(saveMock).not.toHaveBeenCalled();

  await tick(20);
  expect(saveMock).toHaveBeenCalledTimes(1);
});

/**
 * ⚠️ Сравнение по МОДУЛЮ разницы.
 *
 * Человек перемотал назад — разница отрицательная. Без модуля
 * отправка на сервер встала бы до тех пор, пока он снова не
 * досмотрит до прежнего места.
 */
it('После перемотки назад сохранение на сервер продолжается', async () => {
  await mount(fakePlayer());

  await tick(600);
  expect(saveMock).toHaveBeenCalledTimes(1);

  await tick(100);
  expect(saveMock).toHaveBeenCalledTimes(2);
});

/**
 * ⚠️ Пауза — самый вероятный момент ухода с экрана. Ждать
 * следующего тика нельзя: приложение могут закрыть раньше.
 */
it('Пауза отправляет позицию сразу', async () => {
  await mount(fakePlayer());
  await tick(300);          // ушло на сервер
  await tick(305);          // меньше интервала — на сервер НЕ ушло
  saveMock.mockClear();

  await act(async () => {
    mockHandlers.get('playingChange')?.({ isPlaying: false });
  });

  // ⚠️ Именно 305: пауза досылает то, что интервал ещё не отправил.
  // В этом весь смысл — человек может закрыть приложение через
  // секунду, и следующего тика не будет.
  expect(saveMock).toHaveBeenCalledWith('CONTENT', 5, 305, 7200, 'auto');
});

/**
 * ⚠️ Открыть и закрыть, ничего не посмотрев, НЕ должно обновлять
 * запись.
 *
 * Значение то же, но `updatedAt` обновился бы — и видео всплыло бы в
 * начало списка «Продолжить просмотр». То есть список сортировался бы
 * по тому, что ОТКРЫВАЛИ, а не по тому, что смотрели.
 */
it('Пауза без движения ничего не отправляет', async () => {
  await mount(fakePlayer());
  await tick(300);
  saveMock.mockClear();

  await act(async () => {
    mockHandlers.get('playingChange')?.({ isPlaying: false });
  });

  expect(saveMock).not.toHaveBeenCalled();
});

it('Возобновляет с сохранённой секунды', async () => {
  await writeLocal('CONTENT', 5, { position: 5565, duration: 7200, quality: 'auto' });

  const player = fakePlayer();
  await mount(player);

  expect(player.currentTime).toBe(5565);
});

/**
 * ⚠️ Ноль — это НЕ «перемотать в начало», а «нечего возобновлять».
 * Перемотка играющего видео на ноль дёргает картинку.
 */
it('Без сохранённой позиции не перематывает', async () => {
  const player = fakePlayer();
  player.currentTime = 42;

  await mount(player);

  expect(player.currentTime).toBe(42);
});

/**
 * ⚠️ Закрытие экрана — последний шанс сохранить. Значение берётся
 * из ref: обычная переменная дала бы позицию первого рендера, то
 * есть ноль, и человек вернулся бы в начало.
 */
it('Закрытие экрана отправляет последнюю позицию', async () => {
  const tree = await mount(fakePlayer());
  await tick(1800);         // ушло на сервер
  await tick(1805);         // меньше интервала — не ушло
  saveMock.mockClear();

  await act(async () => {
    tree.unmount();
  });

  expect(saveMock).toHaveBeenCalledWith('CONTENT', 5, 1805, 7200, 'auto');
});

/**
 * ⚠️ САМЫЙ ВАЖНЫЙ случай — ровно он и был замечен в браузере.
 *
 * Человек открывает видео, плеер перематывает на сохранённую секунду,
 * человек закрывает экран, ничего не посмотрев. Записывать нечего:
 * позиция та же, что и была.
 *
 * Без этой проверки удалённая позиция появлялась заново просто от
 * открытия страницы — и видео поднималось в начало «Продолжить
 * просмотр».
 */
it('Открыть с сохранённой позицией и закрыть — ничего не отправляет', async () => {
  await writeLocal('CONTENT', 5, { position: 5565, duration: 7200, quality: 'auto' });

  const player = fakePlayer();
  const tree = await mount(player);

  expect(player.currentTime).toBe(5565);
  saveMock.mockClear();

  await act(async () => {
    tree.unmount();
  });

  expect(saveMock).not.toHaveBeenCalled();
});

/**
 * ⚠️ И при закрытии — то же правило: неизменившаяся позиция не
 * переписывается.
 *
 * Проверено в браузере на веб-плеере: удалённая позиция появлялась
 * заново просто от открытия страницы.
 */
it('Закрытие без движения ничего не отправляет', async () => {
  const tree = await mount(fakePlayer());
  await tick(1800);
  saveMock.mockClear();

  await act(async () => {
    tree.unmount();
  });

  expect(saveMock).not.toHaveBeenCalled();
});

/**
 * ⚠️ Нулевую позицию сохранять НЕЛЬЗЯ: видео могли открыть и сразу
 * закрыть. Запись затёрла бы настоящую позицию прошлого сеанса, и
 * «продолжить» отправляло бы человека в начало.
 */
it('Закрытие без просмотра ничего не затирает', async () => {
  const tree = await mount(fakePlayer());

  await act(async () => {
    tree.unmount();
  });

  expect(saveMock).not.toHaveBeenCalled();
});

/** Сохранять некуда — хук не должен ни писать, ни падать. */
it('Без идентификатора ничего не сохраняет', async () => {
  await mount(fakePlayer(), null);
  await tick(300);

  expect(saveMock).not.toHaveBeenCalled();
  expect(mockStorage.size).toBe(0);
});

/**
 * ⚠️ `duration` до загрузки метаданных равен нулю — это «ещё не
 * знаю», а не «видео нулевой длины». Ноль, ушедший на сервер, дал
 * бы 100% просмотра на первой же секунде и видео пропало бы из
 * «продолжить».
 */
it('Нулевая длительность уходит как «неизвестно»', async () => {
  await mount(fakePlayer(0));
  await tick(300);

  expect(saveMock).toHaveBeenCalledWith('CONTENT', 5, 300, null, 'auto');
});
