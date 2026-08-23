/**
 * ТЗ §103, §104 — pul formatlash markazlashtirilgan.
 *
 * ⚠️ Asosiy tekshiruv: `null` va `0` FARQLANADI.
 *
 * Sovg'a obunada to'lov summasi `null` — «sotilmagan». Uni «0 so'm»
 * deb ko'rsatish «bepul sotildi» degan boshqa ma'noni beradi va
 * hisobotni chalkashtiradi (§45, §71). Ilgari bu mantiq to'rtta
 * sahifada takrorlangan edi va har biri `null` bilan boshqacha
 * ishlardi.
 */
import { count, money } from '../format';

describe('money', () => {
  test('null — noma\'lum, chiziqcha', () => {
    expect(money(null)).toBe('—');
    expect(money(undefined)).toBe('—');
    expect(money('')).toBe('—');
  });

  test('nol — haqiqiy qiymat, nol', () => {
    // ⚠️ Bu `null` dan boshqa narsa: «bepul sotildi».
    expect(money(0)).toBe('0');
  });

  test('son ajratgichlar bilan ko\'rsatiladi', () => {
    expect(money(1234567)).toBe(Number(1234567).toLocaleString());
  });

  test('son bo\'lmagan qiymat chiziqchaga aylanadi', () => {
    // Backend kutilmagan narsa qaytarsa, ekranda «NaN» chiqmasin.
    expect(money('salom')).toBe('—');
  });
});

describe('count', () => {
  test('bo\'sh qiymat nol', () => {
    // Sanoqda nol haqiqiy: «hech kim ko'rmagan» — bu ma'lumot.
    expect(count(null)).toBe('0');
    expect(count(undefined)).toBe('0');
  });

  test('son ajratgichlar bilan', () => {
    expect(count(9876)).toBe(Number(9876).toLocaleString());
  });
});
