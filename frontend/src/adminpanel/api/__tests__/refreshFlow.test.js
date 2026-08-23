/**
 * ТЗ §86 — kritik oqim testi: 401 da tokenni yangilash.
 *
 * Nega aynan shu: bu mantiq §61 da yozildi va u nozik. Uchta xato
 * qilish oson va uchalasi ham foydalanuvchini tizimdan chiqarib
 * yuboradi:
 *
 *   1. Cheksiz halqa — yangilash so'rovining o'zi 401 qaytarsa;
 *   2. Poyga — bir vaqtda ketgan besh so'rov beshta yangilash boshlasa,
 *      har biri boshqasining tokenini bekor qiladi va server buni
 *      «token o'g'irlandi» deb hisoblaydi;
 *   3. Qayta urinmaslik — token muddati tugagan har safar admin
 *      qaytadan kirishga majbur bo'ladi.
 *
 * ⚠️ Bu yerda `@testing-library` ishlatilmadi: u loyihada o'rnatilmagan
 * va faqat shu test uchun uchta yangi bog'liqlik qo'shish ТЗ §70 ga zid
 * bo'lardi. Mantiqni sinash uchun DOM kerak emas.
 */
// axios v1 ESM tarqatiladi, CRA jest esa node_modules ni o'girmaydi.
// Shuning uchun haqiqiy modul umuman yuklanmaydi - factory mock.
// `mock` prefiksi majburiy: jest faqat shunday nomlarga factory ichidan
// murojaat qilishga ruxsat beradi.
const mockRequest = jest.fn();
const mockPost = jest.fn();

jest.mock('axios', () => ({
  __esModule: true,
  default: {
    create: () => ({
      request: (...a) => mockRequest(...a),
      post: (...a) => mockPost(...a),
      interceptors: { request: { use: () => {} } },
    }),
  },
}));

describe('401 da tokenni yangilash', () => {
  let client;

  beforeEach(() => {
    jest.resetModules();
    mockRequest.mockReset();
    mockPost.mockReset();
    localStorage.clear();
    client = require('../client');
  });

  const unauthorized = () => {
    const err = new Error('401');
    err.response = { status: 401, data: {} };
    return err;
  };

  test('401 dan keyin yangilaydi va so\'rovni qaytaradi', async () => {
    mockRequest
      .mockRejectedValueOnce(unauthorized())
      .mockResolvedValueOnce({ data: { ok: true } });
    mockPost.mockResolvedValue({ data: { accessToken: 'yangi-token' } });

    const result = await client.api.get('/api/v1/app/admin/content');

    expect(mockPost).toHaveBeenCalledTimes(1);
    expect(result).toEqual({ ok: true });
    expect(client.tokenStore.get()).toBe('yangi-token');
  });

  test('bir vaqtda ketgan so\'rovlar BITTA yangilashni bo\'lishadi', async () => {
    // Har biri o'z rotatsiyasini boshlasa, ular bir-birining tokenini
    // bekor qiladi va server buni o'g'rilik deb hisoblaydi.
    mockRequest.mockImplementation(({ url }) =>
      mockRequest.mock.calls.filter((c) => c[0].url === url).length === 1
        ? Promise.reject(unauthorized())
        : Promise.resolve({ data: { url } })
    );
    mockPost.mockResolvedValue({ data: { accessToken: 'yagona-token' } });

    await Promise.all([
      client.api.get('/api/v1/app/admin/content'),
      client.api.get('/api/v1/app/admin/creators'),
      client.api.get('/api/v1/app/admin/genres'),
    ]);

    expect(mockPost).toHaveBeenCalledTimes(1);
  });

  test('yangilash ham 401 bersa cheksiz halqa bo\'lmaydi', async () => {
    mockRequest.mockRejectedValue(unauthorized());
    mockPost.mockRejectedValue(unauthorized());

    await expect(client.api.get('/api/v1/app/admin/content')).rejects.toBeDefined();

    // Bitta asosiy so'rov + bitta qayta urinish, boshqa emas.
    expect(mockRequest.mock.calls.length).toBeLessThanOrEqual(2);
  });

  test('auth endpointlari qayta urinmaydi', async () => {
    mockRequest.mockRejectedValue(unauthorized());

    await expect(
      client.api.post('/api/v1/app/admin/auth/login', {})
    ).rejects.toBeDefined();

    // Login 401 bersa - parol xato. Yangilashga urinish ma'nosiz.
    expect(mockPost).not.toHaveBeenCalled();
  });

  test('access token localStorage ga yozilmaydi', async () => {
    // §61: localStorage ni har qanday JavaScript o'qiy oladi.
    client.tokenStore.set('maxfiy-token');

    const stored = Object.keys(localStorage).map((k) => localStorage.getItem(k));
    expect(stored.join(' ')).not.toContain('maxfiy-token');
  });
});
