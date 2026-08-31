/**
 * Backend manzilining aniqlanishi.
 *
 * <h2>⚠️ Nima uchun bu testga arziydi</h2>
 * Qiymat build paytida bundle ICHIGA qotib qoladi. Noto'g'ri bo'lsa
 * xato build paytida emas, foydalanuvchi brauzerida chiqadi — va u
 * yerda «sayt ishlamaydi» bo'lib ko'rinadi, konsolda esa
 * `localhost:8080` ga so'rov turadi.
 *
 * Ya'ni bu xatoni deploy'dan keyin, foydalanuvchi aytganda bilib
 * olardik.
 */

// axios v1 ESM tarqatiladi, CRA jest esa `node_modules` ni o'girmaydi —
// qo'shni testlardagi kabi factory mock (`refreshFlow.test.js`).
jest.mock('axios', () => ({
  __esModule: true,
  default: {
    create: () => ({
      request: () => {},
      interceptors: { request: { use: () => {} } },
    }),
  },
}));

const KEY = 'REACT_APP_API_URL';

/** Har chaqiruv modulni QAYTA yuklaydi — `BASE_URL` modul yuklanganda hisoblanadi. */
function baseUrlWith(value) {
  let result;
  jest.isolateModules(() => {
    const previous = process.env[KEY];
    if (value === undefined) {
      delete process.env[KEY];
    } else {
      process.env[KEY] = value;
    }

    // eslint-disable-next-line global-require
    result = require('../client').BASE_URL;

    if (previous === undefined) {
      delete process.env[KEY];
    } else {
      process.env[KEY] = previous;
    }
  });
  return result;
}

describe('Backend manzili', () => {
  it('berilgan manzil ishlatiladi', () => {
    expect(baseUrlWith('https://uzcasting.site')).toBe('https://uzcasting.site');
  });

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV.
   *
   * Bo'sh qiymat — «xuddi shu domen» degani va u to'liq haqiqiy
   * sozlama: panel jar ichidan, API bilan bitta domendan beriladi.
   * Nisbiy manzil har qanday domenda ishlaydi va CORS kerak
   * bo'lmaydi.
   *
   * Ilgari bu yerda `||` turardi va bo'sh qiymat JIMGINA
   * `localhost:8080` ga tushib ketardi. Prod buildida bu foydalanuvchi
   * brauzerini o'z kompyuteriga yuborardi.
   */
  it('BO\'SH qiymat localhost\'ga TUSHMAYDI', () => {
    expect(baseUrlWith('')).toBe('');
  });

  /**
   * O'zgaruvchi umuman berilmasa — ishlab chiqish uchun qulay sukut.
   * Bu ataylab: lokal stendda hech narsa sozlamasdan ishga tushadi.
   */
  it('umuman berilmasa — lokal server', () => {
    expect(baseUrlWith(undefined)).toBe('http://localhost:8080');
  });
});
