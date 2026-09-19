/**
 * Админка кастинга: порядок запросов при решении, цена, фильтр списка.
 *
 * <h2>⚠️ Почему это нужно тестами</h2>
 * Одобрение — ДВА запроса, и порядок у них не косметический: статус
 * создаёт сообщение кандидату и делает заявку «одобренной». Статус без
 * цены — это «Qabul qilindi» без суммы у человека, которому дальше
 * платить. Перестановку строк в `approveApplication` глазами не заметить.
 */
const mockPut = jest.fn();

jest.mock('../client', () => ({
  adminApi: { put: (...args: unknown[]) => mockPut(...args), get: jest.fn() },
}));

import {
  approveApplication,
  filterAdminList,
  mapAdminUser,
  parsePrice,
  rejectApplication,
  toggleCatalog,
  togglePhoto,
  toAdminStatus,
} from '../api';

beforeEach(() => {
  mockPut.mockReset();
  mockPut.mockResolvedValue({ status: 200 });
});

describe('approveApplication', () => {
  it('сначала цена, потом статус — как в старой админке', async () => {
    await approveApplication(42, 150000);
    expect(mockPut.mock.calls.map((c) => c[0])).toEqual([
      '/api/v1/casting-user/price/42/150000',
      '/api/v1/casting-user/status/42/1/150000',
    ]);
  });

  /** Цена не дошла — статус не ставится, заявка остаётся новой. */
  it('если цена не сохранилась, статус не отправляется', async () => {
    mockPut.mockRejectedValueOnce(new Error('network'));
    await expect(approveApplication(42, 150000)).rejects.toThrow('network');
    expect(mockPut).toHaveBeenCalledTimes(1);
  });
});

describe('остальные действия', () => {
  it('отказ — статус 2 с ценой 0', async () => {
    await rejectApplication(7);
    expect(mockPut).toHaveBeenCalledWith('/api/v1/casting-user/status/7/2/0');
  });

  it('каталог и фото — переключатели на сервере', async () => {
    await toggleCatalog(7);
    await togglePhoto('3f2c0000-0000-4000-8000-000000000000');
    expect(mockPut.mock.calls.map((c) => c[0])).toEqual([
      '/api/v1/casting-user/web-show/7',
      '/api/v1/file/3f2c0000-0000-4000-8000-000000000000',
    ]);
  });
});

describe('parsePrice', () => {
  it.each([
    ['150000', 150000],
    ['150 000', 150000],
    [' 99 000 ', 99000],
  ])('%s → %s', (text, value) => {
    expect(parsePrice(text)).toBe(value);
  });

  /** Пустое поле и «0» — забытый ввод, а не цена (старая админка: «Iltimos, narx kiriting!»). */
  it.each(['', '0', '-5', '12.5', 'abc', '99999999999'])('«%s» — не цена', (text) => {
    expect(parsePrice(text)).toBeNull();
  });
});

describe('список', () => {
  const rows = [
    mapAdminUser({ id: 1, name: 'Madina', phone: '+998 90 123-45-67', status: 0, castingType: 'Model', photos: [] }),
    mapAdminUser({ id: 2, name: 'Aziz', phone: '901112233', status: 1, price: 100000, isWebShow: true }),
    mapAdminUser({ id: 3, name: 'Kamola', phone: null, status: 2 }),
    mapAdminUser({ id: 4, name: 'Bot anketa', status: null }),
  ];

  it('статусы 0/1/2 и null старого бота', () => {
    expect(rows.map((r) => r.status)).toEqual(['new', 'accepted', 'rejected', 'new']);
    expect(toAdminStatus('1')).toBe('accepted');
  });

  it('тип приводится к нижнему регистру, фото — к {id, isWebShow}', () => {
    expect(rows[0].castingType).toBe('model');
    const withPhotos = mapAdminUser({
      id: 5,
      photos: [{ id: 'a', prefix: '/x', name: 'n', isWebShow: true }, { id: 'b', isWebShow: null }, null],
    });
    expect(withPhotos.photos).toEqual([
      { id: 'a', isWebShow: true },
      { id: 'b', isWebShow: false },
    ]);
  });

  it('раздел + поиск по имени', () => {
    expect(filterAdminList(rows, 'new', '').map((r) => r.id)).toEqual([1, 4]);
    expect(filterAdminList(rows, 'new', 'madi').map((r) => r.id)).toEqual([1]);
  });

  /** Телефон в базе записан как придётся — ищем по цифрам. */
  it('поиск по телефону — по цифрам', () => {
    expect(filterAdminList(rows, 'new', '90 123').map((r) => r.id)).toEqual([1]);
    expect(filterAdminList(rows, 'accepted', '1112').map((r) => r.id)).toEqual([2]);
    expect(filterAdminList(rows, 'new', '12').map((r) => r.id)).toEqual([]);
  });
});
