/**
 * Katta faylni yuklashni davom ettirish va bekor qilish (ТЗ §16).
 *
 * ⚠️ Nega bu muhim.
 *
 * Bo'laklab yuklash bir necha daqiqa davom etadi. Server seansni
 * saqlab turadi va qaysi bo'laklar yetganini aytadi, lekin klient
 * `uploadId` ni unutsa — bir gigabaytlik video BOSHIDAN yuklanadi.
 *
 * Bekor qilishda esa server bo'laklarni tozalashi kerak: aks holda
 * ular diskda qolib ketadi.
 */
const mockRequest = jest.fn();
const mockPost = jest.fn();
const mockPut = jest.fn();
const mockDelete = jest.fn();

jest.mock('axios', () => ({
  __esModule: true,
  default: {
    create: () => ({
      request: (...a) => mockRequest(...a),
      post: (...a) => mockPost(...a),
      put: (...a) => mockPut(...a),
      delete: (...a) => mockDelete(...a),
      interceptors: { request: { use: () => {} } },
    }),
  },
}));

// 12 MB — bo'laklash chegarasidan (8 MB) katta.
function bigFile(name = 'kino.mp4') {
  const blob = new Blob([new Uint8Array(12 * 1024 * 1024)]);
  return Object.assign(blob, {
    name,
    lastModified: 1700000000000,
    slice: () => new Blob(['x']),
  });
}

describe('Yuklashni davom ettirish', () => {
  let client;

  beforeEach(() => {
    jest.resetModules();
    [mockRequest, mockPost, mockPut, mockDelete].forEach((m) => m.mockReset());
    localStorage.clear();
    client = require('../client');
  });

  test('seans yuklash DAVOMIDA eslab qolinadi', async () => {
    mockRequest.mockImplementation(({ method, url }) => {
      if (method === 'post' && url.endsWith('/uploads')) {
        return Promise.resolve({ data: { uploadId: 'S1', chunkSize: 5 * 1024 * 1024, totalChunks: 2, receivedChunks: [] } });
      }
      return Promise.resolve({ data: { id: 7 } });
    });

    // ⚠️ Tekshiruv aynan YUKLASH PAYTIDA: brauzer shu daqiqada
    // yopilishi mumkin va aynan shunda `uploadId` saqlangan bo'lishi
    // kerak. Tugagandan keyin u ataylab o'chiriladi (pastdagi test).
    let savedDuringUpload = null;
    mockPut.mockImplementation(() => {
      savedDuringUpload = localStorage.getItem('uzpanel.uploads');
      return Promise.resolve({ data: {} });
    });

    await client.adminApi.uploadMedia(bigFile(), 'content');

    expect(savedDuringUpload).toContain('S1');
  });

  test('uzilgandan keyin yetib kelgan bo\'laklar QAYTA yuborilmaydi', async () => {
    localStorage.setItem('uzpanel.uploads',
      JSON.stringify({ 'kino.mp4|12582912|1700000000000': 'S1' }));

    mockRequest.mockImplementation(({ method, url }) => {
      // Yarim qolgan seans: birinchi bo'lak allaqachon serverda.
      if (method === 'get' && url.includes('/uploads/S1')) {
        return Promise.resolve({ data: { uploadId: 'S1', chunkSize: 5 * 1024 * 1024, totalChunks: 2, receivedChunks: [0] } });
      }
      return Promise.resolve({ data: { id: 7 } });
    });
    mockPut.mockResolvedValue({ data: {} });

    await client.adminApi.uploadMedia(bigFile(), 'content');

    // Yangi seans OCHILMAYDI va faqat qolgan bitta bo'lak yuboriladi.
    const opened = mockRequest.mock.calls.filter(
      ([c]) => c.method === 'post' && c.url.endsWith('/uploads')).length;
    expect(opened).toBe(0);
    expect(mockPut).toHaveBeenCalledTimes(1);
  });

  test('server seansni unutgan bo\'lsa boshidan boshlanadi', async () => {
    localStorage.setItem('uzpanel.uploads',
      JSON.stringify({ 'kino.mp4|12582912|1700000000000': 'ESKI' }));

    mockRequest.mockImplementation(({ method, url }) => {
      if (method === 'get' && url.includes('/uploads/ESKI')) {
        const err = new Error('404');
        err.response = { status: 404, data: {} };
        return Promise.reject(err);
      }
      if (method === 'post' && url.endsWith('/uploads')) {
        return Promise.resolve({ data: { uploadId: 'S2', chunkSize: 5 * 1024 * 1024, totalChunks: 1, receivedChunks: [] } });
      }
      return Promise.resolve({ data: { id: 7 } });
    });
    mockPut.mockResolvedValue({ data: {} });

    await client.adminApi.uploadMedia(bigFile(), 'content');

    // Eski yaroqsiz seans ishlatilmaydi, yangisi ochiladi.
    const opened = mockRequest.mock.calls.filter(
      ([c]) => c.method === 'post' && c.url.endsWith('/uploads')).length;
    expect(opened).toBe(1);
    expect(localStorage.getItem('uzpanel.uploads')).not.toContain('ESKI');
  });

  test('yuklash tugagach seans esdan chiqariladi', async () => {
    mockRequest.mockImplementation(({ method, url }) => {
      if (method === 'post' && url.endsWith('/uploads')) {
        return Promise.resolve({ data: { uploadId: 'S1', chunkSize: 5 * 1024 * 1024, totalChunks: 1, receivedChunks: [] } });
      }
      return Promise.resolve({ data: { id: 7 } });
    });
    mockPut.mockResolvedValue({ data: {} });

    await client.adminApi.uploadMedia(bigFile(), 'content');

    // Tugagan seansni davom ettirishga urinish 404 berardi.
    expect(localStorage.getItem('uzpanel.uploads')).not.toContain('S1');
  });
});

describe('Bekor qilish', () => {
  let client;

  beforeEach(() => {
    jest.resetModules();
    [mockRequest, mockPost, mockPut, mockDelete].forEach((m) => m.mockReset());
    localStorage.clear();
    client = require('../client');
  });

  test('serverga bekor qilish yuboriladi va seans unutiladi', async () => {
    localStorage.setItem('uzpanel.uploads',
      JSON.stringify({ 'kino.mp4|12582912|1700000000000': 'S1' }));
    mockRequest.mockResolvedValue({ data: null });

    await client.adminApi.cancelUpload(bigFile(), 'S1');

    const deleteCall = mockRequest.mock.calls.find(([c]) => c.method === 'delete');
    expect(deleteCall[0].url).toContain('/uploads/S1');
    expect(localStorage.getItem('uzpanel.uploads')).not.toContain('S1');
  });

  test('server xato bersa ham klient seansni unutadi', async () => {
    localStorage.setItem('uzpanel.uploads',
      JSON.stringify({ 'kino.mp4|12582912|1700000000000': 'S1' }));
    const err = new Error('500');
    err.response = { status: 500, data: {} };
    mockRequest.mockRejectedValue(err);

    await expect(client.adminApi.cancelUpload(bigFile(), 'S1')).rejects.toBeDefined();

    // Seans baribir yaroqsiz - uni davom ettirishga urinish xato berardi.
    expect(localStorage.getItem('uzpanel.uploads')).not.toContain('S1');
  });
});

/**
 * Access token 15 daqiqada tugaydi (`app.jwt.access-token-ms`).
 * Bir gigabaytlik video 10 Mbit/s kanalda ~14 daqiqa yuklanadi — ya'ni
 * KATTA video yuklashda tokenning tugashi istisno emas, ODATIY hol.
 */
describe("Token muddati yuklash o'rtasida tugasa", () => {
  let client;

  beforeEach(() => {
    jest.resetModules();
    [mockRequest, mockPost, mockPut, mockDelete].forEach((m) => m.mockReset());
    localStorage.clear();
    client = require('../client');
  });

  function beginTwoChunks() {
    mockRequest.mockImplementation(({ method, url }) => {
      if (method === 'post' && url.endsWith('/uploads')) {
        return Promise.resolve({
          data: { uploadId: 'S9', chunkSize: 5 * 1024 * 1024, totalChunks: 2, receivedChunks: [] },
        });
      }
      return Promise.resolve({ data: { id: 77 } });   // complete
    });
  }

  test('token yangilanib, yuklash DAVOM etadi', async () => {
    beginTwoChunks();

    const refreshes = [];
    mockPost.mockImplementation((url) => {
      if (url.endsWith('/auth/refresh')) {
        refreshes.push(url);
        return Promise.resolve({ data: { accessToken: 'YANGI' } });
      }
      return Promise.resolve({ data: {} });
    });

    let puts = 0;
    mockPut.mockImplementation(() => {
      puts += 1;
      // Birinchi bo'lak ketdi; ikkinchisida token muddati tugadi.
      if (puts === 2) {
        return Promise.reject({ response: { status: 401, data: { code: 'UNAUTHORIZED' } } });
      }
      return Promise.resolve({ data: {} });
    });

    const kicked = jest.fn();
    client.setUnauthorizedHandler(kicked);

    const media = await client.adminApi.uploadMedia(bigFile(), 'content');

    expect(refreshes).toHaveLength(1);
    expect(media.id).toBe(77);
    // ⚠️ Eng muhimi: admin tizimdan CHIQARIB yuborilmasin. 40 daqiqa
    // yuklangan video shu sababdan yo'qolsa — bu eng og'ir nosozlik.
    expect(kicked).not.toHaveBeenCalled();
  });
});

/**
 * S3 rejimi — bo'lak SERVER ORQALI O'TMAYDI.
 *
 * ⚠️ Butun ishning maqsadi shu. Agar klient S3 rejimida ham eski
 * `PUT .../chunks/{n}` yo'lidan yuborsa, 10 GB baribir server orqali
 * oqardi va hech qanday xato ko'rinmasdi — server uni qabul qilardi.
 */
describe('S3 multipart rejimi', () => {
  let client;
  let fetchCalls;

  beforeEach(() => {
    jest.resetModules();
    [mockRequest, mockPost, mockPut, mockDelete].forEach((m) => m.mockReset());
    localStorage.clear();
    fetchCalls = [];
    global.fetch = jest.fn((url, init) => {
      fetchCalls.push({ url, method: init.method });
      return Promise.resolve({ ok: true, status: 200 });
    });
    client = require('../client');
  });

  afterEach(() => { delete global.fetch; });

  function s3Session() {
    mockRequest.mockImplementation(({ method, url }) => {
      if (method === 'post' && url.endsWith('/uploads')) {
        return Promise.resolve({
          data: {
            uploadId: 'S3-1',
            uploadMode: 'S3_MULTIPART',
            chunkSize: 10 * 1024 * 1024,
            totalChunks: 2,
            receivedChunks: [],
          },
        });
      }
      if (method === 'post' && url.endsWith('/parts')) {
        return Promise.resolve({
          data: {
            parts: [
              { index: 0, url: 'https://s3.example.invalid/part0' },
              { index: 1, url: 'https://s3.example.invalid/part1' },
            ],
          },
        });
      }
      return Promise.resolve({ data: { id: 99 } });   // complete
    });
  }

  test("bo'laklar TO'G'RIDAN-TO'G'RI omborga ketadi", async () => {
    s3Session();

    const media = await client.adminApi.uploadMedia(bigFile(), 'content');

    expect(media.id).toBe(99);
    // Ikkala bo'lak ham omborga.
    expect(fetchCalls).toHaveLength(2);
    expect(fetchCalls[0].url).toContain('s3.example.invalid');
    expect(fetchCalls[0].method).toBe('PUT');
    // ⚠️ Eng muhimi: server orqali BITTA ham bo'lak ketmadi.
    expect(mockPut).not.toHaveBeenCalled();
  });

  test("havolalar GURUH bilan olinadi — har bo'lak uchun alohida so'rov emas", async () => {
    s3Session();

    await client.adminApi.uploadMedia(bigFile(), 'content');

    const partRequests = mockRequest.mock.calls
      .filter(([{ url }]) => url.endsWith('/parts'));
    // Ikki bo'lak, lekin havola so'rovi BITTA: birinchi javob
    // ikkalasini ham qamragan.
    expect(partRequests).toHaveLength(1);
  });

  test("ombor rad etsa xato yashirilmaydi", async () => {
    s3Session();
    global.fetch = jest.fn(() => Promise.resolve({ ok: false, status: 403 }));

    await expect(client.adminApi.uploadMedia(bigFile(), 'content')).rejects.toBeDefined();
  });
});
