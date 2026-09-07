/**
 * Tomoshabin API klienti.
 *
 * <h2>⚠️ Nega bu test yozildi</h2>
 * Bu yerda topilgan xato brauzerda 40 daqiqadan keyin chiqdi:
 * yangilash so'rovida maydon nomi `refreshToken` deb yozilgan edi,
 * backend esa `refresh_token` kutadi.
 *
 * Oqibati jimgina: kirish ishlaydi, video ochiladi, hammasi joyida
 * ko'rinadi. Access token muddati tugagach (15 daqiqa) yangilash
 * yiqiladi, odam sahifa o'rtasida «mehmon» ga aylanadi va pozitsiya
 * saqlanmay qo'yadi — hech qanday xato xabari chiqmasdan.
 *
 * Shuning uchun testlar SIM ustidagi shaklni tekshiradi: qaysi
 * manzilga, qaysi maydon nomi bilan.
 *
 * <h2>⚠️ Nega axios BUTUNLAY almashtiriladi</h2>
 * Axios v1 ESM tarqatiladi va CRA jest `node_modules` ni o'girmaydi —
 * haqiqiy modul umuman yuklanmaydi. Panel testlari ham shu sababdan
 * shunday qilgan.
 *
 * Aynan shuning uchun klientda yangilash mantiqi axios
 * INTERCEPTORIDA emas, aniq `request()` o'ramida turadi: interceptor
 * bu yerda hech qachon chaqirilmasdi va test uni sinamasdi.
 *
 * `mock` prefiksi majburiy: jest faqat shunday nomlarga fabrika
 * ichidan murojaat qilishga ruxsat beradi.
 */
const mockRequest = jest.fn();

jest.mock('axios', () => ({
  __esModule: true,
  default: {
    create: () => ({ request: (...a) => mockRequest(...a) }),
  },
}));

/** 401 — muddati tugagan token. */
function unauthorized() {
  const err = new Error('401');
  err.response = { status: 401, data: {} };
  return err;
}

/** Muvaffaqiyatli javob. */
const ok = (data) => ({ data });

let client;

beforeEach(() => {
  jest.resetModules();
  mockRequest.mockReset();
  localStorage.clear();
  client = require('../client');
});

/** Yuborilgan so'rovlar. */
const sent = () => mockRequest.mock.calls.map(([c]) => c);

const ACCESS = 'uzcasting.viewer.access';
const REFRESH = 'uzcasting.viewer.refresh';

describe('Kirish', () => {
  it('Kod tasdiqlangach sessiya saqlanadi', async () => {
    mockRequest.mockResolvedValue(ok({
      access_token: 'A1', refresh_token: 'R1', user: { id: 'u1' },
    }));

    await client.verifyCode('+998945434230', '123456');

    expect(client.getAccessToken()).toBe('A1');
    expect(localStorage.getItem(REFRESH)).toBe('R1');
  });

  it('Kod uch qadamli yo\'lga yuboriladi', async () => {
    mockRequest.mockResolvedValue(ok({ sent: true, expiresInSeconds: 120 }));

    const result = await client.sendCode('+998945434230');

    expect(sent()[0].url).toBe('/api/v1/app/auth/otp/send');
    expect(result.expiresInSeconds).toBe(120);
  });

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV.
   *
   * `name_required: true` javobida token YO'Q. Agar u ham sessiya
   * deb saqlansa, `access_token` `undefined` bo'lib yozilardi va
   * keyingi HAR BIR so'rov 401 berardi — odam esa «kirdim» deb
   * o'ylab turardi va nima uchun hech narsa ochilmayotganini
   * tushunmasdi.
   */
  it('Ism kerak bo\'lganda token saqlanmaydi', async () => {
    mockRequest.mockResolvedValue(ok({ name_required: true, expiresInSeconds: 900 }));

    const result = await client.verifyCode('+998900000001', '123456');

    expect(result.nameRequired).toBe(true);
    expect(client.getAccessToken()).toBeNull();
    expect(localStorage.getItem(REFRESH)).toBeNull();
  });

  it('Ism yuborilgach sessiya saqlanadi', async () => {
    mockRequest.mockResolvedValue(ok({
      access_token: 'A2', refresh_token: 'R2', user: { id: 'u2' },
    }));

    await client.completeSignUp('+998900000001', 'Yangi Odam');

    expect(sent()[0].url).toBe('/api/v1/app/auth/otp/complete');
    expect(client.getAccessToken()).toBe('A2');
  });

  it('Chiqishda hammasi tozalanadi', async () => {
    mockRequest.mockResolvedValue(ok({
      access_token: 'A1', refresh_token: 'R1', user: { id: 'u1' },
    }));
    await client.verifyCode('+998945434230', '123456');

    client.signOut();

    expect(client.getAccessToken()).toBeNull();
    expect(localStorage.getItem('uzcasting.viewer.user')).toBeNull();
  });

  /** ⚠️ Tokensiz so'rovda `Authorization` sarlavhasi BO'LMASLIGI kerak. */
  it('Mehmonda Authorization yuborilmaydi', async () => {
    mockRequest.mockResolvedValue(ok(null));

    await client.fetchProgress('content', 13);

    expect(sent()[0].headers.Authorization).toBeUndefined();
  });

  it('Token bo‘lsa yuboriladi', async () => {
    localStorage.setItem(ACCESS, 'A1');
    mockRequest.mockResolvedValue(ok(null));

    await client.fetchProgress('content', 13);

    expect(sent()[0].headers.Authorization).toBe('Bearer A1');
  });
});

describe('Token yangilash', () => {
  beforeEach(() => {
    localStorage.setItem(ACCESS, 'ESKI');
    localStorage.setItem(REFRESH, 'R1');
  });

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV — AYNAN shu xato bo'lgan.
   *
   * Backend maydonni `@JsonProperty("refresh_token")` bilan oladi.
   * camelCase yuborilsa u `null` bo'ladi va yangilash HAR DOIM
   * yiqiladi — lekin faqat access token muddati tugagach bilinadi.
   */
  it('Maydon nomi refresh_token bo‘ladi, refreshToken emas', async () => {
    mockRequest
      .mockRejectedValueOnce(unauthorized())
      .mockResolvedValueOnce(ok({ access_token: 'A2', refresh_token: 'R2' }))
      .mockResolvedValueOnce(ok({ position: 95 }));

    await client.fetchProgress('content', 13);

    const refresh = sent().find((r) => r.url.includes('/auth/refresh'));
    expect(refresh.url).toBe('/api/v1/app/auth/refresh');
    expect(Object.keys(refresh.data)).toEqual(['refresh_token']);
    expect(refresh.data.refresh_token).toBe('R1');
  });

  it('401 dan keyin so‘rov YANGI token bilan takrorlanadi', async () => {
    mockRequest
      .mockRejectedValueOnce(unauthorized())
      .mockResolvedValueOnce(ok({ access_token: 'A2', refresh_token: 'R2' }))
      .mockResolvedValueOnce(ok({ position: 95 }));

    const out = await client.fetchProgress('content', 13);

    expect(out.position).toBe(95);
    expect(client.getAccessToken()).toBe('A2');

    const takror = sent()[sent().length - 1];
    expect(takror.headers.Authorization).toBe('Bearer A2');
  });

  /**
   * ⚠️ Yangilangan token ham 401 olsa — TO'XTAYMIZ.
   *
   * Gap muddatda emas, va ikkinchi urinish cheksiz aylanma berardi:
   * har javob yana yangilashni chaqirardi.
   */
  it('Ikkinchi 401 da takrorlanmaydi', async () => {
    // ⚠️ Progress HAR DOIM 401, yangilash HAR DOIM muvaffaqiyatli.
    //
    // Sanoqli moklar bilan yozilsa test tasodifan o'tardi: ular
    // tugagach chaqiruv `undefined` qaytarib, halqa o'z-o'zidan
    // uzilardi — ya'ni qo'riqchi olib tashlansa ham test yashil
    // turardi. Bu yerda esa uzilishning YAGONA sababi qo'riqchi.
    mockRequest.mockImplementation((config) => {
      if (config.url.includes('/auth/refresh')) {
        return Promise.resolve(ok({ access_token: 'A2', refresh_token: 'R2' }));
      }
      // ⚠️ Chegara: qo'riqchisiz bu halqa CHEKSIZ bo'ladi va test
      // yiqilish o'rniga OSILIB qolardi — sabab ko'rinmasdi.
      // Chegaradan oshsa 401 EMAS, boshqa xato: u yangilashni
      // chaqirmaydi va halqa aniq to'xtaydi.
      const urinishlar = sent().filter((r) => r.url.includes('watch-progress')).length;
      return urinishlar > 3
        ? Promise.reject(new Error('CHEKSIZ HALQA'))
        : Promise.reject(unauthorized());
    });

    await expect(client.fetchProgress('content', 13)).rejects.toBeDefined();

    // Progress so'rovi ikki marta: asl va bitta takror.
    expect(sent().filter((r) => r.url.includes('watch-progress'))).toHaveLength(2);
  });

  /**
   * ⚠️ Yangilash muvaffaqiyatsiz bo'lsa sessiya TOZALANADI.
   *
   * Aks holda amal qilmaydigan token qolib, har so'rov yana
   * yangilashga urinardi — va odam «kirganman» deb o'ylab yurardi.
   */
  it('Yangilash yiqilsa sessiya tozalanadi', async () => {
    mockRequest
      .mockRejectedValueOnce(unauthorized())
      .mockRejectedValueOnce(unauthorized());

    await expect(client.fetchProgress('content', 13)).rejects.toBeDefined();
    expect(client.getAccessToken()).toBeNull();
  });

  /**
   * ⚠️ Kirish so'rovining O'ZI yangilashni chaqirmasin.
   *
   * Noto'g'ri parolda ham 401 keladi. Yangilashga urinish odamga
   * «parol noto'g'ri» o'rniga boshqa xato ko'rsatardi, va eng
   * yomoni — mavjud sessiyani o'chirib yuborardi.
   */
  it('Kirishdagi 401 yangilashni chaqirmaydi', async () => {
    mockRequest.mockRejectedValue(unauthorized());

    await expect(client.verifyCode('+998900000000', '000000')).rejects.toBeDefined();

    expect(sent().filter((r) => r.url.includes('/auth/refresh'))).toHaveLength(0);
  });

  /**
   * ⚠️ Bir vaqtda BITTA yangilash.
   *
   * Backend refresh tokenni rotatsiya qiladi: eskisi darhol bekor
   * bo'ladi. Ikki so'rov birdan yangilashga ketsa, ikkinchisi
   * allaqachon bekor qilingan token bilan borardi va backend odamni
   * hamma joydan chiqarib yuborardi — ya'ni yangilashning O'ZI
   * chiqib ketishga sabab bo'lardi.
   */
  it('Parallel 401 larda yangilash BIR MARTA ketadi', async () => {
    mockRequest.mockImplementation((config) => {
      if (config.url.includes('/auth/refresh')) {
        return Promise.resolve(ok({ access_token: 'A2', refresh_token: 'R2' }));
      }
      // Har manzil BIRINCHI marta 401, keyin muvaffaqiyat.
      const oldingi = sent().filter((r) => r.url === config.url).length;
      return oldingi === 1
        ? Promise.reject(unauthorized())
        : Promise.resolve(ok({ position: 10 }));
    });

    await Promise.all([
      client.fetchProgress('content', 1),
      client.fetchProgress('content', 2),
    ]);

    expect(sent().filter((r) => r.url.includes('/auth/refresh'))).toHaveLength(1);
  });
});

describe('Manzillar', () => {
  beforeEach(() => {
    localStorage.setItem(ACCESS, 'A1');
    mockRequest.mockResolvedValue(ok({}));
  });

  /**
   * ⚠️ Yo'ldagi `content`/`episode` backenddagi `CONTENT`/`EPISODE`
   * ga aylanadi. Aks holda server turni tanimay xato qaytarardi.
   */
  it('Tur katta harfga aylantiriladi', async () => {
    await client.saveProgress('episode', 42, 100, 7200, 'auto');
    await client.saveProgress('content', 13, 100, 7200, 'auto');

    expect(sent()[0].url).toBe('/api/v1/app/watch-progress/EPISODE/42');
    expect(sent()[1].url).toBe('/api/v1/app/watch-progress/CONTENT/13');
  });

  /**
   * ⚠️ Ko'rish IKKI xil endpointdan boradi va ular almashtirilsa
   * server boshqa videoni qaytarardi — qism o'rniga butun kontentni.
   */
  it('Qism va kontent uchun boshqa manzil', async () => {
    await client.fetchWatch('episode', 42);
    await client.fetchWatch('content', 13);

    expect(sent()[0].url).toBe('/api/v1/app/watch/42');
    expect(sent()[1].url).toBe('/api/v1/app/watch/content/13');
  });

  /** ⚠️ Soniyalar butun bo'ladi — pleyer kasr beradi. */
  it('Pozitsiya yaxlitlanadi', async () => {
    await client.saveProgress('content', 13, 95.837, 165.4, 'auto');

    expect(sent()[0].data.position).toBe(96);
    expect(sent()[0].data.duration).toBe(165);
  });

  /** Davomiylik noma'lum — `null` ketadi, nol emas. */
  it('Noma‘lum davomiylik null bo‘lib ketadi', async () => {
    await client.saveProgress('content', 13, 100, null, 'auto');

    expect(sent()[0].data.duration).toBeNull();
  });
});
