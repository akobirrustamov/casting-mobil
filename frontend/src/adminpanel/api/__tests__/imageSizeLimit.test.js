/**
 * Rasm hajmi chegarasi — klient tomoni.
 *
 * <h2>⚠️ Nega bu kerak bo'ldi</h2>
 * Maydon tagidagi «≤10 MB» yozuvi HECH NIMANI tekshirmasdi:
 * `mediaSpecs.js` dagi `maxMb` faqat matnni chizardi. Ya'ni admin
 * 40 MB lik afishani bemalol yuklardi va u keyin har bir kartochkada
 * tomoshabinga yuborilardi.
 *
 * Endi tekshiruv IKKI joyda: bu yerda (tez javob, bekorga trafik
 * ketmasin) va serverda (haqiqiy qoida). Bu test faqat birinchisini
 * qo'riqlaydi.
 *
 * ⚠️ Chegara serverdagi `app.upload.max-image-bytes` bilan MOS
 * bo'lishi shart. Ajralib qolsa, panel faylni yuboradi-yu, server
 * rad etadi.
 */
jest.mock('axios', () => {
  const inst = {
    get: jest.fn(), post: jest.fn(), put: jest.fn(),
    patch: jest.fn(), delete: jest.fn(), request: jest.fn(),
    interceptors: { request: { use: jest.fn() }, response: { use: jest.fn() } },
    defaults: { headers: { common: {} } },
  };
  return { __esModule: true, default: { create: jest.fn(() => inst), ...inst } };
});

const { adminApi } = require('../client');

/** Berilgan hajmdagi soxta fayl. Haqiqiy bayt ajratilmaydi. */
function fakeFile(sizeMb, type = 'image/jpeg', name = 'afisha.jpg') {
  return { name, type, size: Math.round(sizeMb * 1024 * 1024) };
}

it('Chegaradan katta rasm YUBORILMAYDI', async () => {
  await expect(adminApi.uploadMedia(fakeFile(11), 'content'))
    .rejects.toMatchObject({ code: 'IMAGE_TOO_LARGE', status: 400 });
});

/**
 * ⚠️ Xabar aniq raqamlarni aytishi kerak.
 *
 * «Fayl juda katta» degan quruq gap odamga nima qilishni aytmaydi:
 * u faylni qanchaga kichraytirish kerakligini bilmaydi.
 */
it('Xabarda ham hajmi, ham chegara ko‘rsatiladi', async () => {
  await expect(adminApi.uploadMedia(fakeFile(25), 'content'))
    .rejects.toMatchObject({ message: expect.stringContaining('25 MB') });

  await expect(adminApi.uploadMedia(fakeFile(25), 'content'))
    .rejects.toMatchObject({ message: expect.stringContaining('10 MB') });
});

/**
 * ⚠️ VIDEO bu chegaraga bo'ysunmaydi.
 *
 * U tabiatan katta va bo'laklab yuboriladi. Bu yerga tushib qolsa,
 * hech qanday video yuklab bo'lmasdi.
 */
it('Video tekshirilmaydi', async () => {
  const rejected = await adminApi
    .uploadMedia(fakeFile(900, 'video/mp4', 'film.mp4'), 'content')
    .then(() => null, (e) => e);

  expect(rejected?.code).not.toBe('IMAGE_TOO_LARGE');
});

/** Chegaradan kichik rasm bu tekshiruvda to'xtamaydi. */
it('Kichik rasm o‘tadi', async () => {
  const rejected = await adminApi
    .uploadMedia(fakeFile(3), 'content')
    .then(() => null, (e) => e);

  expect(rejected?.code).not.toBe('IMAGE_TOO_LARGE');
});

/**
 * ⚠️ `type` bo'sh bo'lsa to'xtatilmaydi.
 *
 * Ba'zi brauzerlar noma'lum kengaytmada uni bo'sh qoldiradi. Bu
 * yerda taxmin qilib rad etish haqiqiy faylni to'sib qo'yardi;
 * serverda tur kengaytma bo'yicha ham aniqlanadi, ya'ni chegara
 * baribir qo'llanadi.
 */
it('Turi noma‘lum fayl bu yerda to‘xtatilmaydi', async () => {
  const rejected = await adminApi
    .uploadMedia(fakeFile(50, '', 'nomalum.bin'), 'content')
    .then(() => null, (e) => e);

  expect(rejected?.code).not.toBe('IMAGE_TOO_LARGE');
});
