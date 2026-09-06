import axios from 'axios';

/**
 * Tomoshabin uchun API klienti.
 *
 * <h2>⚠️ Nega panel klientidan ALOHIDA</h2>
 * `adminpanel/api/client.js` PANEL hisoblari bilan ishlaydi:
 * boshqa manzil (`/app/admin/auth/**`), boshqa saqlash kaliti va
 * refresh cookie orqali boradi.
 *
 * Bittasini ikkalasiga moslash uni ikki holatli qilardi va bitta
 * brauzerda ikkala hisobga birdan kirib bo'lmasdi — xodim o'z
 * panelini ochib turib, tomoshabin sifatida videoni sinab ko'ra
 * olmasdi.
 *
 * <h2>Token qayerda</h2>
 * `localStorage`. Refresh token ham shu yerda: panelning cookie'li
 * yo'li bu yerda ishlamaydi, chunki backend uni faqat panel manzili
 * uchun qo'yadi.
 */

export const BASE_URL = process.env.REACT_APP_API_URL ?? 'http://localhost:8080';

const ACCESS_KEY = 'uzcasting.viewer.access';
const REFRESH_KEY = 'uzcasting.viewer.refresh';
const USER_KEY = 'uzcasting.viewer.user';

export function getAccessToken() {
  try {
    return localStorage.getItem(ACCESS_KEY);
  } catch {
    // Brauzer saqlashni bloklagan (shaxsiy oyna, sozlama) — mehmon
    // sifatida ishlaymiz, bepul kontent baribir ochiladi.
    return null;
  }
}

export function getViewer() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function store(session) {
  try {
    localStorage.setItem(ACCESS_KEY, session.access_token);
    localStorage.setItem(REFRESH_KEY, session.refresh_token);
    if (session.user) localStorage.setItem(USER_KEY, JSON.stringify(session.user));
  } catch {
    // Saqlanmasa sessiya sahifa yangilangunga qadar yashaydi.
  }
}

export function signOut() {
  try {
    [ACCESS_KEY, REFRESH_KEY, USER_KEY].forEach((k) => localStorage.removeItem(k));
  } catch {
    // Bo'shatib bo'lmasa ham chiqish holati xotirada qoladi.
  }
}

const http = axios.create({ baseURL: BASE_URL, timeout: 20000 });

/**
 * ⚠️ Bir vaqtda BITTA yangilash.
 *
 * Backend refresh tokenni ROTATSIYA qiladi: eskisi darhol bekor
 * bo'ladi. Sahifada bir nechta so'rov birdan 401 olsa va har biri
 * o'zicha yangilashga ketsa, birinchisi o'tadi, qolganlari esa
 * allaqachon bekor qilingan token bilan boradi — va backend, mutlaqo
 * to'g'ri, odamni hamma joydan chiqarib yuboradi.
 *
 * Ya'ni yangilashning o'zi chiqib ketishga sabab bo'lardi. Tomosha
 * sahifasida bu ayniqsa oson: pleyer va progress bir vaqtda so'rov
 * yuboradi.
 */
let inFlight = null;

function refreshOnce() {
  if (!inFlight) {
    inFlight = doRefresh().finally(() => {
      inFlight = null;
    });
  }
  return inFlight;
}

async function doRefresh() {
  let refreshToken = null;
  try {
    refreshToken = localStorage.getItem(REFRESH_KEY);
  } catch {
    return null;
  }
  if (!refreshToken) return null;

  try {
    const { data } = await http.request({
      method: 'post',
      url: '/api/v1/app/auth/refresh',
      // ⚠️ `refresh_token`, `refreshToken` EMAS.
      //
      // Backend uni `@JsonProperty("refresh_token")` bilan oladi va
      // nomi kirish javobidagi maydon bilan bir xil. camelCase
      // yuborilsa maydon `null` bo'ladi va yangilash HAR DOIM
      // yiqiladi — lekin buni faqat 15 daqiqadan keyin, access token
      // muddati tugagach bilish mumkin. Shunda odam sahifa
      // o'rtasida jimgina «mehmon» ga aylanadi: video o'ynayveradi,
      // pozitsiya esa saqlanmay qo'yadi.
      data: { refresh_token: refreshToken },
    });
    store(data);
    return data.access_token;
  } catch {
    // Refresh ham amal qilmaydi — sessiya haqiqatan tugagan.
    signOut();
    return null;
  }
}

/**
 * Bitta so'rov: token qo'shadi, 401 da bir marta yangilab qaytadan
 * uradi.
 *
 * <h2>⚠️ Nega axios interceptori EMAS</h2>
 * Interceptor faqat haqiqiy axios ishlaganda ishlaydi. Axios v1 esa
 * ESM tarqatiladi va CRA jest `node_modules` ni o'girmaydi — ya'ni
 * testda axios butunlay almashtiriladi va interceptor HECH QACHON
 * chaqirilmasdi. Testlar yashil turardi, yangilash esa sinalmagan
 * qolardi.
 *
 * Aynan shu qatlamda maydon nomi xatosi bor edi va u faqat brauzerda,
 * 15 daqiqadan keyin chiqdi. Panel klienti ham shu sababdan aniq
 * o'ramdan foydalanadi.
 */
async function request(method, url, { data, params, retried } = {}) {
  const headers = {};
  const token = getAccessToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  try {
    const res = await http.request({ method, url, data, params, headers });
    return res.data;
  } catch (error) {
    const status = error?.response?.status;

    // ⚠️ Bir marta. Yangilangan token ham 401 olsa, gap muddatda
    // emas — ikkinchi urinish cheksiz aylanma berardi.
    const isAuthCall = url.startsWith('/api/v1/app/auth/');
    if (status === 401 && !retried && !isAuthCall) {
      const fresh = await refreshOnce();
      if (fresh) {
        return request(method, url, { data, params, retried: true });
      }
    }
    throw error;
  }
}

// ------------------------------------------------------------------ so'rovlar

/**
 * Kirish — SMS kod orqali, uch qadamda.
 *
 * <h2>⚠️ Nega parol emas</h2>
 * Ilgari bu yerda {@code POST /api/v1/app/auth/login} turardi va u
 * telefon bilan parol yuborardi. O'sha endpoint backenddan OLIB
 * TASHLANGAN — butun ro'yxatdan o'tish OTP ga o'tkazilgan, sayt
 * tomoni esa yangilanmagan. Natijada saytdagi kirish umuman
 * ishlamasdi va nosozlik jimgina edi: server 401 qaytarardi, ya'ni
 * «parol xato» bo'lib ko'rinardi.
 *
 * ⚠️ Ilova foydalanuvchisi parol O'RNATA olmaydi: bunday oqim
 * umuman yo'q. Ya'ni parol bilan kirishni tiklash bitta ekilgan
 * sinov hisobiga xizmat qilardi, xolos.
 *
 * <h2>Qadamlar</h2>
 * <pre>
 *   1. sendCode(phone)          → SMS ketadi
 *   2. verifyCode(phone, code)  → sessiya YOKI «ism kerak»
 *   3. completeSignUp(phone, name) → sessiya
 * </pre>
 *
 * Uchinchi qadam faqat yangi odam uchun. Eski foydalanuvchi ikkinchi
 * qadamdayoq kiradi.
 */

/**
 * 1-qadam: raqamga kod yuborish.
 *
 * @returns {Promise<{expiresInSeconds: number}>} kod qancha yashaydi
 */
export async function sendCode(phone) {
  const data = await request('post', '/api/v1/app/auth/otp/send', {
    data: { phone },
  });
  return { expiresInSeconds: data?.expiresInSeconds ?? 0 };
}

/**
 * 2-qadam: kodni tekshirish.
 *
 * ⚠️ Ikki xil javob keladi va ularni ARALASHTIRIB bo'lmaydi:
 * <ul>
 *   <li>{@code name_required: false} — sessiya bor, saqlaymiz;</li>
 *   <li>{@code name_required: true} — token YO'Q, ism so'raladi.</li>
 * </ul>
 *
 * Ikkinchi holatda {@code store()} chaqirilsa saqlanadigan token
 * {@code undefined} bo'lardi va keyingi har bir so'rov 401 berardi —
 * odam esa «kirdim» deb o'ylab turardi.
 *
 * @returns {Promise<{nameRequired: boolean, expiresInSeconds: number}>}
 */
export async function verifyCode(phone, code) {
  const data = await request('post', '/api/v1/app/auth/otp/verify', {
    data: { phone, code },
  });

  if (data?.name_required) {
    return { nameRequired: true, expiresInSeconds: data?.expiresInSeconds ?? 0 };
  }

  store(data);
  return { nameRequired: false, expiresInSeconds: 0 };
}

/**
 * 3-qadam: yangi foydalanuvchining ismi.
 *
 * ⚠️ Bu qadam ikkinchi SMS SO'RAMAYDI: server raqamni 15 daqiqaga
 * «tasdiqlangan» deb belgilab qo'yadi.
 */
export async function completeSignUp(phone, name) {
  const data = await request('post', '/api/v1/app/auth/otp/complete', {
    data: { phone, name },
  });
  store(data);
  return data;
}

/**
 * Qismni yoki yaxlit kontentni ko'rish: huquq va manbalar.
 *
 * ⚠️ Huquqni FAQAT server hal qiladi. Klient obuna, xarid va
 * bepullikni o'zi qo'shib hisoblamaydi — aks holda qoida ikki joyda
 * yashardi va tarif o'zgargan kuni ular ajralib ketardi.
 */
export async function fetchWatch(type, id, locale = 'UZ') {
  const path = type === 'episode'
    ? `/api/v1/app/watch/${id}`
    : `/api/v1/app/watch/content/${id}`;
  return request('get', path, { params: { locale } });
}

/** Saqlangan pozitsiya. Yozuv yo'q bo'lsa `null` — bu xato emas. */
export async function fetchProgress(type, id) {
  const data = await request('get', `/api/v1/app/watch-progress/${target(type)}/${id}`);
  return data ?? null;
}

export async function saveProgress(type, id, position, duration, quality) {
  return request('put', `/api/v1/app/watch-progress/${target(type)}/${id}`, {
    data: {
      position: Math.round(position),
      duration: duration == null ? null : Math.round(duration),
      quality,
    },
  });
}

/** URL dagi `episode`/`content` → backend kutgan `EPISODE`/`CONTENT`. */
function target(type) {
  return type === 'episode' ? 'EPISODE' : 'CONTENT';
}

