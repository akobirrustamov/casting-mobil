/**
 * Ombor javob bermaganda XABAR SABABNI aytadi.
 *
 * <h2>⚠️ Haqiqiy nosozlik</h2>
 * Foydalanuvchi panelda video yuklamoqchi bo'ldi va shu xabarni oldi:
 *
 *     «Server bilan aloqa yo'q. Internetni tekshiring.»
 *
 * Internet joyida edi. Backend loglarida esa HECH NARSA yo'q edi —
 * chunki so'rov unga umuman yetib bormagan.
 *
 * Sabab: katta fayl bo'laklari SERVERNI CHETLAB, to'g'ridan-to'g'ri
 * omborga ketadi. Bucketda CORS sozlanmagani uchun brauzer PUT
 * so'rovini yubormasdan turib bloklagan.
 *
 * ⚠️ Brauzer CORS blokini va haqiqiy uzilishni BIR XIL `TypeError`
 * bilan beradi — farqni ataylab aytmaydi, aks holda sahifa boshqa
 * domenlarni skanerlay olardi.
 *
 * Lekin bu yerda kontekst bor: bo'lak omborga ketyapti, imzoni esa
 * server hozirgina berdi — demak server bilan aloqa BOR. Shuning
 * uchun eng ehtimolli sabab aytiladi.
 *
 * Xabar SABABNI DA'VO QILMAYDI, taxmin qiladi — «ko'p hollarda».
 */
const mockRequest = jest.fn();

jest.mock('axios', () => ({
  __esModule: true,
  default: {
    create: () => ({
      request: (...a) => mockRequest(...a),
      post: jest.fn(),
      put: jest.fn(),
      delete: jest.fn(),
      interceptors: { request: { use: () => {} } },
    }),
  },
}));

/** 12 MB — bo'laklash chegarasidan (8 MB) katta. */
function bigFile(name = 'kino.mp4') {
  const blob = new Blob([new Uint8Array(12 * 1024 * 1024)]);
  return Object.assign(blob, {
    name,
    lastModified: 1700000000000,
    slice: () => new Blob(['x']),
  });
}

/** S3 rejimidagi seans — bo'laklar omborga to'g'ridan-to'g'ri ketadi. */
function s3Session() {
  return {
    uploadId: 'S1',
    chunkSize: 5 * 1024 * 1024,
    totalChunks: 2,
    receivedChunks: [],
    uploadMode: 'S3_MULTIPART',
  };
}

describe('Ombor javob bermaganda', () => {
  let client;

  beforeEach(() => {
    jest.resetModules();
    mockRequest.mockReset();
    localStorage.clear();

    mockRequest.mockImplementation(({ method, url }) => {
      if (method === 'post' && url.endsWith('/uploads')) {
        return Promise.resolve({ data: s3Session() });
      }
      if (method === 'post' && url.includes('/parts')) {
        return Promise.resolve({
          data: { parts: [{ index: 0, url: 'https://s3.example/part0' },
                          { index: 1, url: 'https://s3.example/part1' }] },
        });
      }
      return Promise.resolve({ data: { id: 7 } });
    });

    client = require('../client');
  });

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV.
   *
   * `fetch` CORS blokidagidek yiqiladi — `response` yo'q, faqat
   * `TypeError`. Xabar CORS ni eslatishi kerak.
   */
  it('CORS bloki «Internetni tekshiring» deb ko\'rsatilmaydi', async () => {
    global.fetch = jest.fn(() => Promise.reject(new TypeError('Failed to fetch')));

    // ⚠️ `normalizeError` Error EMAS, oddiy obyekt qaytaradi
    // ({code, message, errors, status}) — komponentlar bir xil
    // shaklni kutadi. Shuning uchun `toThrow` emas, `rejects` ning
    // qiymati tekshiriladi.
    await expect(client.adminApi.uploadMedia(bigFile(), 'content'))
      .rejects.toMatchObject({
        code: 'STORAGE_UNREACHABLE',
        message: expect.stringMatching(/CORS/i),
      });
  });

  /** Sabab aniq nomlanadi — odam nimani tekshirishni bilsin. */
  it('Xabar ExposeHeaders: ETag ni eslatadi', async () => {
    global.fetch = jest.fn(() => Promise.reject(new TypeError('Failed to fetch')));

    await expect(client.adminApi.uploadMedia(bigFile(), 'content'))
      .rejects.toMatchObject({ message: expect.stringMatching(/ETag/) });
  });

  /**
   * ⚠️ Sabab DA'VO QILINMAYDI.
   *
   * Haqiqatan internet uzilgan bo'lishi ham mumkin. Xabar buni ham
   * aytadi — aks holda odam ombor sozlamasini bejiz titkilardi.
   */
  it('Internet uzilishi ham ehtimol sifatida aytiladi', async () => {
    global.fetch = jest.fn(() => Promise.reject(new TypeError('Failed to fetch')));

    await expect(client.adminApi.uploadMedia(bigFile(), 'content'))
      .rejects.toMatchObject({ message: expect.stringMatching(/[Ii]nternet/) });
  });

  /**
   * Ombor javob berib, lekin XATO qaytarsa — bu boshqa hol va
   * eski xabar to'g'ri. CORS haqida gapirish adashtirardi.
   */
  it('Ombor 403 qaytarsa CORS haqida gapirilmaydi', async () => {
    global.fetch = jest.fn(() => Promise.resolve({ ok: false, status: 403 }));

    await expect(client.adminApi.uploadMedia(bigFile(), 'content'))
      .rejects.toMatchObject({ message: expect.stringMatching(/403/) });
  });
});
