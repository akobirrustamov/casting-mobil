/**
 * Ряд «Продолжить просмотр» на главной.
 *
 * <h2>⚠️ Что здесь легко сломать незаметно</h2>
 * Ряд необязательный: он либо есть, либо его нет. Именно поэтому
 * поломка не бросается в глаза — главная выглядит рабочей и без него.
 *
 * Проверяются три решения: когда блока быть НЕ должно, что запрос не
 * уходит гостем, и что карточки не схлопываются в одну.
 */

const mockQueryResult: { data: unknown; isPending: boolean } = {
  data: undefined,
  isPending: false,
};

/**
 * ⚠️ Запоминаем аргументы `useQuery`, чтобы проверить `enabled`.
 *
 * Иначе «гостю не запрашиваем» пришлось бы проверять по отсутствию
 * сетевого вызова — а его в этих тестах и так нет, и проверка
 * проходила бы всегда, ничего не проверяя.
 */
const mockQueryCalls: { enabled?: boolean; queryKey?: unknown }[] = [];

jest.mock('@tanstack/react-query', () => ({
  useQuery: (options: { enabled?: boolean; queryKey?: unknown }) => {
    mockQueryCalls.push(options);
    return mockQueryResult;
  },
}));

/**
 * ⚠️ Иконки и картинки подменяются: `@expo/vector-icons` тянет за
 * собой загрузчик шрифтов, а он в jest не разрешается. Нам от них
 * нужна только разметка, а не отрисовка.
 */
jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-image', () => ({ Image: 'Image' }));

const mockPush = jest.fn();
jest.mock('expo-router', () => ({ router: { push: (p: string) => mockPush(p) } }));

jest.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, vars?: Record<string, unknown>) =>
      vars ? `${key}:${JSON.stringify(vars)}` : key,
  }),

  // ⚠️ Обязателен: `@/i18n` вызывает `i18n.use(initReactI18next)` при
  // импорте, и без этого поля падает весь файл — до первого теста.
  initReactI18next: { type: '3rdParty', init: () => undefined },
}));

const mockViewer = { value: 'guest' };
jest.mock('../api', () => ({ useViewerKey: () => mockViewer.value }));

jest.mock('@/features/home/api', () => ({ useFeedLanguage: () => 'uz' }));
jest.mock('@/features/content/railLayout', () => ({
  CARD_RATIO: 2 / 3,
  useRailCardWidth: () => 132,
}));
jest.mock('@/lib/api', () => ({ mediaUrl: (id: number | null) => (id ? `/media/${id}` : undefined) }));

import { act, create } from 'react-test-renderer';

import { ContinueRail } from '../ContinueRail';

/** Готовый элемент ленты — поля, которых касается компонент. */
function item(over: Record<string, unknown> = {}) {
  return {
    progress: {
      type: 'CONTENT',
      targetId: 1,
      position: 5565,
      duration: 7200,
      quality: 'auto',
      completed: false,
      percent: 77,
      updatedAt: null,
    },
    content: {
      id: 1,
      title: 'Film',
      shortDescription: null,
      genre: null,
      posterMediaId: 10,
    },
    episodeNumber: null,
    ...over,
  };
}

async function render() {
  let tree: ReturnType<typeof create> | undefined;
  await act(async () => {
    tree = create(<ContinueRail />);
  });
  return tree as ReturnType<typeof create>;
}

beforeEach(() => {
  jest.clearAllMocks();
  mockQueryCalls.length = 0;
  mockQueryResult.data = undefined;
  mockViewer.value = 'user-1';
});

/**
 * ⚠️ Пустого ряда быть НЕ должно.
 *
 * У нового человека он занял бы верх главной и не сообщил бы ничего.
 * Заголовок без карточек выглядит как сломанная загрузка.
 */
it('Нечего продолжать — блока нет совсем', async () => {
  mockQueryResult.data = [];
  const tree = await render();

  expect(tree.toJSON()).toBeNull();
});

/** Ответа ещё нет или он не пришёл — тоже ничего не рисуем. */
it('Без ответа блока нет', async () => {
  mockQueryResult.data = undefined;
  const tree = await render();

  expect(tree.toJSON()).toBeNull();
});

it('С данными ряд появляется', async () => {
  mockQueryResult.data = [item()];
  const tree = await render();

  expect(tree.toJSON()).not.toBeNull();
});

/**
 * ⚠️ Гостю запрос не уходит: позиция привязана к человеку, и сервер
 * ответил бы отказом — на каждой загрузке главной.
 */
it('Гостю запрос не отправляется', async () => {
  mockViewer.value = 'guest';
  await render();

  expect(mockQueryCalls[0]?.enabled).toBe(false);
});

it('Вошедшему запрос отправляется', async () => {
  mockViewer.value = 'user-1';
  await render();

  expect(mockQueryCalls[0]?.enabled).toBe(true);
});

/**
 * ⚠️ Ключ из ТИПА и номера.
 *
 * Идентификаторы серий и контента нумеруются независимо: `EPISODE 7`
 * и `CONTENT 7` — разные видео. По одному номеру React счёл бы их
 * одной карточкой и показал бы только одну.
 */
it('Серия и контент с одним номером — разные ключи', async () => {
  mockQueryResult.data = [
    item({ progress: { ...item().progress, type: 'EPISODE', targetId: 7 } }),
    item({ progress: { ...item().progress, type: 'CONTENT', targetId: 7 } }),
  ];

  // ⚠️ Проверять по числу отрисованных карточек НЕЛЬЗЯ: при
  // одинаковых ключах React всё равно рисует обе и только пишет
  // предупреждение. Такая проверка проходила бы и со сломанным
  // ключом — то есть не проверяла бы ничего.
  //
  // Ломается это позже и незаметнее: при обновлении списка React
  // сопоставляет элементы по ключу и переносит состояние не на ту
  // карточку. Единственный надёжный признак здесь — предупреждение.
  const warnings: unknown[][] = [];
  const spy = jest.spyOn(console, 'error').mockImplementation((...args) => {
    warnings.push(args);
  });

  const tree = await render();
  spy.mockRestore();

  expect(warnings.some((w) => String(w[0]).includes('same key'))).toBe(false);
  expect(JSON.stringify(tree.toJSON()).split('/media/10').length - 1).toBe(2);
});

/**
 * ⚠️ Полоса досмотра — это и есть смысл ряда: она отличает его от
 * обычной подборки. Без неё человек не видит, сколько осталось.
 */
it('Процент досмотра доходит до карточки', async () => {
  mockQueryResult.data = [item()];
  const tree = await render();

  expect(JSON.stringify(tree.toJSON())).toContain('77%');
});
