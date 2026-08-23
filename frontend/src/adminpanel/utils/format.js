/**
 * Raqam va pul formatlash (ТЗ §103, §104).
 *
 * ⚠️ Nega markazlashtirildi.
 *
 * Bu mantiq to'rtta sahifada qo'lda takrorlangan edi va har biri
 * `null` bilan BOSHQACHA ishlardi: ba'zilari uni nolga aylantirardi,
 * bittasi esa chiziqcha ko'rsatardi.
 *
 * Farq bezak emas. Sovg'a obunada `paidAmount` — `null`, ya'ni
 * «sotilmagan». Uni «0 so'm» deb ko'rsatish «bepul sotildi» degan
 * boshqa ma'noni beradi va hisobotni chalkashtiradi (§45, §71).
 */

/**
 * Pul summasi.
 *
 * @param value son yoki `null`
 * @returns null/undefined uchun `'—'`, aks holda ajratilgan raqam
 */
export function money(value) {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  const n = Number(value);
  return Number.isNaN(n) ? '—' : n.toLocaleString();
}

/**
 * Sanoq (ko'rishlar, yulduzlar, hodisalar).
 *
 * Bu yerda nol HAQIQIY qiymat: «hech kim ko'rmagan» — bu ma'lumot,
 * noaniqlik emas. Shuning uchun pul formatidan farqli o'laroq
 * bo'sh qiymat nol sifatida ko'rsatiladi.
 */
export function count(value) {
  return Number(value || 0).toLocaleString();
}
