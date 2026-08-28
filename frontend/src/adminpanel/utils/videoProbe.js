/**
 * Yuklashdan OLDIN video o'lchamini aniqlaydi.
 *
 * <h2>⚠️ Nima uchun brauzerda, serverda emas</h2>
 * O'lcham server tomonda faqat transcoding paytida, ya'ni fayl to'liq
 * yuklab bo'lingandan KEYIN ma'lum bo'ladi. 4 GB lik faylni yuklab
 * bo'lgach «bu 4K ekan» deyish kech: mehnat allaqachon sarflangan.
 *
 * Brauzer esa faylni SERVERGA YUBORMASDAN o'qiy oladi — ogohlantirish
 * yuklash boshlanishidan oldin keladi va u haqiqatan foyda beradi.
 *
 * <h2>Nima uchun ogohlantirish, rad etish emas</h2>
 * Chiqish allaqachon 1080p bilan cheklangan (`VideoProfileSelector`
 * manbadan yuqori variant yasamaydi). Ya'ni 4K manba SIFAT bermaydi —
 * u faqat qayta ishlashni ~2.4 barobar uzaytiradi, chunki har kadr
 * baribir dekodlanishi kerak.
 *
 * Bu tezlik masalasi, to'g'ri-noto'g'ri masalasi emas. Shuning uchun
 * qaror admin qo'lida qoladi.
 */

/**
 * Tavsiya etilgan eng katta sifat darajasi.
 *
 * ⚠️ `VideoProfileSelector` dagi eng yuqori profil bilan bir xil.
 * Undan katta manba yangi sifat qo'shmaydi.
 */
export const MAX_RECOMMENDED_HEIGHT = 1080;

/**
 * Fayl o'lchamini o'qiydi.
 *
 * ⚠️ Aniqlab bo'lmasa `null` qaytaradi va bu ODATIY holat: brauzer
 * `.mkv` va `.avi` ni odatda ocholmaydi, kodek qo'llab-quvvatlanmasligi
 * mumkin, fayl buzuq bo'lishi mumkin.
 *
 * Bunday holatda yuklash TO'XTATILMAYDI. «Bilmayman» sababli ishni
 * to'xtatish adminni mutlaqo yaroqli faylni yuklay olmaydigan holga
 * qo'yardi — va sabab hech qayerda ko'rinmasdi.
 *
 * @param {File} file
 * @returns {Promise<{width: number, height: number} | null>}
 */
export function probeVideoSize(file) {
  return new Promise((resolve) => {
    if (!file || typeof URL === 'undefined' || !URL.createObjectURL) {
      resolve(null);
      return;
    }

    const url = URL.createObjectURL(file);
    const video = document.createElement('video');

    // ⚠️ Faqat metama'lumot kerak — butun fayl yuklanmasin. U bir
    // necha gigabayt bo'lishi mumkin va uni xotiraga tortish
    // brauzerni qotirardi.
    video.preload = 'metadata';
    video.muted = true;

    let settled = false;
    const finish = (result) => {
      if (settled) return;
      settled = true;
      URL.revokeObjectURL(url);
      video.removeAttribute('src');
      resolve(result);
    };

    video.onloadedmetadata = () => {
      const width = video.videoWidth;
      const height = video.videoHeight;
      // 0×0 — «o'qildi, lekin video yo'q». Uni haqiqiy o'lcham deb
      // qabul qilish «0p video» degan ma'nosiz holatni berardi.
      finish(width > 0 && height > 0 ? { width, height } : null);
    };

    video.onerror = () => finish(null);

    // ⚠️ Ba'zi fayllarda `<video>` na `loadedmetadata`, na `error`
    // beradi — u jimgina osilib qoladi. Chegara bo'lmasa yuklash
    // tugmasi abadiy javobsiz qolardi.
    setTimeout(() => finish(null), PROBE_TIMEOUT_MS);

    video.src = url;
  });
}

/** Metama'lumot shuncha vaqtda o'qilmasa — o'qilmaydi. */
const PROBE_TIMEOUT_MS = 5000;

/**
 * Ogohlantirish kerakmi.
 *
 * <h2>⚠️ QISQA tomon bo'yicha — balandlik bo'yicha EMAS</h2>
 * «1080p» — bu kadr balandligi emas, sifat DARAJASI. Odamlar
 * 1080×1920 lik rolikni «1080p vertikal» deyishadi, «1920p» emas.
 *
 * Balandlik bo'yicha hisoblansa, HAR BIR oddiy vertikal rolik
 * (1080×1920) ogohlantirish oynasini ochardi. Loyihada vertikal
 * kontent birinchi darajali (ТЗ §19 — Reels), ya'ni bu chekka holat
 * emas — u har kuni takrorlanardi.
 *
 * Va aynan shu narsa ogohlantirishni o'ldirardi: har safar chiqadigan
 * oyna o'qilmasdan yopiladigan bo'lib qoladi, keyin esa HAQIQIY 4K
 * fayl kelganda ham u ishlamaydi.
 *
 * Backend ham AYNAN shunday hisoblaydi (`VideoProfileSelector`
 * `Math.min(width, height)` oladi) — ikkalasi bir xil bo'lishi shart,
 * aks holda panel manba tushiriladi deb ogohlantirar, backend esa
 * uni tushirmasdi.
 *
 * ⚠️ `null` (aniqlanmagan) — ogohlantirilmaydi. Faqat ANIQ bilingan
 * katta o'lcham uchun so'raladi.
 */
export function needsDownscaleWarning(size) {
  if (!size) return false;
  const quality = Math.min(size.width, size.height);
  return quality > MAX_RECOMMENDED_HEIGHT;
}
