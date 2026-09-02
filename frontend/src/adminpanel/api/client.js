/**
 * Admin panel uchun HTTP klienti.
 *
 * Mavjud src/config/index.js dan farqi:
 *   - alohida token kaliti (sayt admini bilan aralashmaydi);
 *   - 401 ni bitta joyda ushlaydi va chiqishga majbur qiladi;
 *   - backend'ning {code, message, errors} formatini tushunadi.
 *
 * Komponent ichida to'g'ridan-to'g'ri axios chaqirilmaydi (§69).
 */
import axios from 'axios';

/**
 * Backend manzili.
 *
 * <h2>⚠️ `??`, `||` EMAS</h2>
 * BO'SH qiymat — bu «xuddi shu domen» degani va u to'liq haqiqiy
 * sozlama. Panel jar ichidan beriladi, ya'ni API bilan bitta
 * domenda turadi: nisbiy manzil har qanday domenda ishlaydi va CORS
 * umuman kerak bo'lmaydi.
 *
 * `||` bilan bo'sh qiymat `localhost` ga tushib ketardi.
 *
 * ⚠️ O'zgaruvchi UMUMAN berilmasa — `localhost:8080`. Bu ishlab
 * chiqish uchun qulay, lekin ishlab chiqarish buildida FALOKAT:
 * qiymat bundle ichiga qotib qoladi va foydalanuvchi brauzeri o'z
 * kompyuteriga murojaat qiladi. Sayt «ishlamaydi», konsolda esa
 * localhost'ga so'rov ko'rinadi.
 *
 * Shuning uchun prod build'dan oldin `.env` da BO'SH qiymat
 * qo'yiladi: `REACT_APP_API_URL=`
 */
export const BASE_URL = process.env.REACT_APP_API_URL ?? 'http://localhost:8080';

const USER_KEY = 'uzpanel.user';

/**
 * ⚠️ Access token XOTIRADA saqlanadi, `localStorage` da emas (§61).
 *
 * `localStorage` ni sahifadagi har qanday JavaScript o'qiy oladi —
 * bitta XSS (masalan buzilgan npm paketi) tokenni o'g'irlaydi. Xotiradagi
 * qiymat esa sahifa yopilishi bilan yo'qoladi va boshqa vkladkaga
 * ko'chmaydi.
 *
 * Sahifa yangilanganda token yo'qoladi — bu muammo emas: refresh token
 * `httpOnly` cookie'da turadi va `/auth/refresh` yangi access token
 * beradi. Cookie'ni JavaScript umuman ko'rmaydi.
 *
 * Profil (`USER_KEY`) `localStorage` da qoladi: u maxfiy emas, faqat
 * sahifa yangilanishida menyuni darhol chizish uchun kerak. Haqiqiy
 * huquq baribir backendda tekshiriladi.
 */
let accessToken = null;

export const tokenStore = {
  get: () => accessToken,
  set: (token) => { accessToken = token; },
  clear: () => {
    accessToken = null;
    localStorage.removeItem(USER_KEY);
  },
  getUser: () => {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      // Buzilgan JSON butun panelni yiqitmasligi kerak
      return null;
    }
  },
  setUser: (user) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
};

// withCredentials: refresh cookie'si so'rovlarga qo'shilishi uchun.
const http = axios.create({ baseURL: BASE_URL, timeout: 20000, withCredentials: true });

http.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    // Backend JwtService.normalizeToken 'Bearer ' bilan ham, usiz ham qabul qiladi.
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** 401 da chiqarib yuborish uchun - AuthContext shu yerga ulanadi. */
let onUnauthorized = null;
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

/**
 * Backend xatosini bir xil shaklga keltiradi.
 * Har doim {code, message, errors, status} qaytaradi - komponentlar
 * turli shakllarni tekshirib o'tirmasin.
 */
function normalizeError(error) {
  if (error.response) {
    const { status, data } = error.response;
    return {
      status,
      code: data?.code || 'HTTP_ERROR',
      message: data?.message || `Xatolik (${status})`,
      errors: data?.errors || [],
    };
  }
  // ⚠️ Omborga to'g'ridan-to'g'ri yuborishdagi uzilish — ALOHIDA hol.
  //
  // Bu yerda umumiy «Server bilan aloqa yo'q» xabari CHALG'ITADI:
  // server bilan aloqa BOR, imzoni u hozirgina berdi. Uzilgani —
  // ombor, va eng ehtimolli sabab bucketda CORS sozlanmagani.
  //
  // Xabar `uploadChunked` da yasaladi, chunki kontekst faqat o'sha
  // yerda ma'lum. Bu yerda uni saqlab qolamiz — ilgari u shu qatorda
  // jimgina yo'qolardi va foydalanuvchi internetini tekshirib
  // o'tirardi.
  if (error.storageUnreachable) {
    return {
      status: 0,
      code: 'STORAGE_UNREACHABLE',
      message: error.message,
      errors: [],
    };
  }

  return {
    status: 0,
    code: 'NETWORK_ERROR',
    message: 'Server bilan aloqa yo\'q',
    errors: [],
  };
}

/**
 * Bir vaqtda ketayotgan bir nechta so'rov 401 olsa, faqat BITTA
 * yangilash bo'lishi kerak. Aks holda har biri o'z rotatsiyasini
 * boshlab, bir-birining tokenini bekor qilardi va foydalanuvchi
 * «o'g'rilik aniqlandi» degan sababdan tizimdan chiqib ketardi.
 */
let refreshing = null;

function refreshAccessToken() {
  if (!refreshing) {
    refreshing = http
      .post('/api/v1/app/admin/auth/refresh')
      .then((res) => {
        tokenStore.set(res.data.accessToken);
        if (res.data.user) tokenStore.setUser(res.data.user);
        return res.data.accessToken;
      })
      .finally(() => { refreshing = null; });
  }
  return refreshing;
}

async function request(method, url, { data, params, retried, timeout } = {}) {
  try {
    const res = await http.request({ method, url, data, params, timeout });
    return res.data;
  } catch (error) {
    const normalized = normalizeError(error);

    // Access token muddati tugagan bo'lsa — bir marta yangilab ko'ramiz.
    // Yangilash oqimining o'zi qayta urinmaydi (`retried`), aks holda
    // cheksiz halqa hosil bo'lardi.
    const isAuthCall = url.startsWith('/api/v1/app/admin/auth/');
    if (normalized.status === 401 && !retried && !isAuthCall) {
      try {
        await refreshAccessToken();
        return await request(method, url, { data, params, timeout, retried: true });
      } catch {
        // Yangilash ham o'tmadi — sessiya haqiqatan tugagan.
      }
    }

    if (normalized.status === 401 && onUnauthorized) {
      onUnauthorized();
    }
    throw normalized;
  }
}

export const api = {
  get: (url, params) => request('get', url, { params }),
  /**
   * ⚠️ Uchinchi argument — QUERY parametrlari.
   *
   * Backend ba'zi amallarda ma'lumotni tanada emas, so'rov satrida
   * kutadi: `POST /staff/{id}/block?reason=...` va
   * `PUT /staff/{id}/role?role=...`. Ularni qo'lda URL'ga yopishtirish
   * har chaqiruvda `encodeURIComponent` ni unutish xavfini tug'dirardi —
   * sabab matnida `&` bo'lsa so'rov jimgina buzilardi.
   */
  post: (url, data, params) => request('post', url, { data, params }),
  put: (url, data, params) => request('put', url, { data, params }),
  del: (url) => request('delete', url),
  /** Chiqish — token serverda ham bekor qilinadi (§61). */
  logout: () => request('post', '/api/v1/app/admin/auth/logout'),
  /** Sahifa yangilangach sessiyani tiklash. */
  refresh: () => refreshAccessToken(),
};

/** Media faylining to'liq manzili. */
export const mediaUrl = (id) => (id ? `${BASE_URL}/api/v1/app/media/${id}/raw` : null);

/**
 * Bo'laklab yuklashga o'tish chegarasi.
 *
 * Kichik rasm uchun bo'laklash ortiqcha - uch marta so'rov o'rniga bitta
 * multipart yetarli. Katta video esa bitta so'rovga sig'maydi: serverda
 * multipart chegarasi 50 MB.
 */
const CHUNKED_THRESHOLD = 8 * 1024 * 1024;

/** Fayl so'rovlari uchun alohida, uzoqroq kutish - 5 MB sekin internetda vaqt oladi. */
const FILE_TIMEOUT_MS = 120000;

/**
 * Yig'ish uchun kutish.
 *
 * ⚠️ `complete` — bu «bir necha bayt qaytar» emas: server BUTUN faylni
 * bo'laklardan qayta yozadi. Lokal SSD'da 500 MB 0.7 soniya oldi, lekin
 * prod'da saqlash tarmoq diskida bo'lishi mumkin. 5 GB video 100 MB/s
 * da 50 soniya oladi — standart 20 soniya buni ko'tarmaydi va klient
 * server ISHNI TUGATGANIDA uzilib ketardi (fayl saqlanadi, admin esa
 * xato ko'radi va boshidan yuklaydi).
 */
const COMPLETE_TIMEOUT_MS = 300000;

/** Bitta bo'lak necha marta qayta urinadi. */
const CHUNK_RETRIES = 3;

/**
 * Omborga to'g'ridan-to'g'ri yuborish uzilganda ko'rsatiladigan xabar.
 *
 * ⚠️ Sabab TAXMIN qilib aytiladi, da'vo qilinmaydi: brauzer CORS
 * blokini ham, haqiqiy uzilishni ham bir xil `TypeError` bilan beradi.
 * Lekin birinchisi ancha ehtimolliroq va uni tekshirish oson —
 * shuning uchun aynan u ko'rsatiladi.
 */
const STORAGE_UNREACHABLE = "Ombor javob bermadi. Ko'p hollarda sabab — "
  + "bucketda CORS sozlanmagani (`ExposeHeaders: ETag` bilan). "
  + "Internet uzilgan bo'lishi ham mumkin.";

/**
 * Fayl so'rovi — 401 da tokenni yangilab QAYTA uradi.
 *
 * ⚠️ Nega alohida. Fayl so'rovlari `request()` dan o'tmaydi: ular xom
 * tana (bo'lak) yoki `FormData` yuboradi va progress hodisasi kerak.
 * Shu sababli ular `request()` dagi 401→yangilash mantig'idan CHETDA
 * qolgan edi.
 *
 * Oqibati og'ir edi: access token 15 daqiqada tugaydi
 * (`app.jwt.access-token-ms`), bir gigabaytlik video esa 10 Mbit/s
 * kanalda ~14 daqiqa yuklanadi. Ya'ni katta video yuklashda tokenning
 * tugashi ISTISNO EMAS. Tugagan zahoti bo'lak 401 olardi, yuklash
 * to'xtardi va admin tizimdan CHIQARIB yuborilardi — 40 daqiqalik ish
 * bir zumda yo'qolardi.
 */
async function fileRequest(send, refreshed = false) {
  try {
    return await send();
  } catch (error) {
    if (error.response?.status === 401 && !refreshed) {
      // Yangilash ham o'tmasa xato yuqoriga ketadi va `uploadFile`
      // uni sessiya tugagani deb qabul qiladi — bu to'g'ri.
      await refreshAccessToken();
      return fileRequest(send, true);
    }
    throw error;
  }
}

/** Kichik fayl - bitta multipart so'rov. */
async function uploadSingle(file, folder, onProgress) {
  const form = new FormData();
  form.append('file', file);
  form.append('folder', folder);
  const res = await fileRequest(() => http.post('/api/v1/app/admin/media', form, {
    // ⚠️ Standart 20 soniya bu yerda YETMAYDI: chegaraga yaqin 8 MB
    // fayl uchun u 3.4 Mbit/s barqaror tezlik talab qiladi. Sekinroq
    // kanalda yuklash «Server bilan aloqa yo'q» bilan uzilardi.
    timeout: FILE_TIMEOUT_MS,
    onUploadProgress: (e) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded * 100) / e.total));
      }
    },
  }));
  return res.data;
}

/**
 * Yarim qolgan yuklashlar ro'yxati.
 *
 * ⚠️ Nega `localStorage`. Bo'laklab yuklash bir necha daqiqa davom
 * etadi va shu orada sahifa yangilanishi yoki brauzer yopilishi
 * mumkin. Server seansni saqlab turadi va qaysi bo'laklar yetganini
 * aytadi, lekin klient `uploadId` ni unutsa - butun fayl BOSHIDAN
 * yuklanadi. Bir gigabaytlik video uchun bu bir necha daqiqa.
 *
 * Bu maxfiy ma'lumot emas: shunchaki seans identifikatori. Ruxsat
 * baribir serverda tekshiriladi - begona `uploadId` ga tegib bo'lmaydi.
 */
const RESUME_KEY = 'uzpanel.uploads';

/** Fayl imzosi: nom + o'lcham + o'zgartirilgan vaqt. */
function fileSignature(file) {
  return `${file.name}|${file.size}|${file.lastModified || 0}`;
}

function readResumable() {
  try {
    return JSON.parse(localStorage.getItem(RESUME_KEY) || '{}');
  } catch {
    // Buzilgan JSON butun yuklashni to'xtatmasin.
    return {};
  }
}

function rememberUpload(file, uploadId) {
  try {
    const all = readResumable();
    all[fileSignature(file)] = uploadId;
    localStorage.setItem(RESUME_KEY, JSON.stringify(all));
  } catch {
    // Saqlash imkoni bo'lmasa yuklash baribir ishlaydi, faqat
    // uzilganda davom ettirib bo'lmaydi.
  }
}

function forgetUpload(file) {
  try {
    const all = readResumable();
    delete all[fileSignature(file)];
    localStorage.setItem(RESUME_KEY, JSON.stringify(all));
  } catch {
    /* e'tiborsiz */
  }
}

/**
 * Yarim qolgan seansni topadi.
 *
 * Server seansni o'chirgan yoki muddati o'tgan bo'lsa `null` qaytadi
 * va yuklash boshidan boshlanadi — bu to'g'ri xatti-harakat, chunki
 * eski `uploadId` ga bo'lak yuborish 404 berardi.
 */
async function findResumable(file) {
  const uploadId = readResumable()[fileSignature(file)];
  if (!uploadId) {
    return null;
  }
  try {
    const session = await request('get', `/api/v1/app/admin/uploads/${uploadId}`);
    return session?.uploadId ? session : null;
  } catch {
    forgetUpload(file);
    return null;
  }
}

/**
 * Bitta bo'lak uchun imzolangan havola.
 *
 * ⚠️ Havolalar guruh bilan olinadi va KESHLANADI. Har bo'lak uchun
 * alohida so'rov 20 GB lik faylda 2048 ta ortiqcha murojaat bo'lardi.
 *
 * Kesh sessiyaga bog'liq: `uploadId` o'zgarsa eski havolalar yaroqsiz.
 */
const PART_URL_BATCH = 20;
const partUrlCache = new Map();

async function partUrl(uploadId, index) {
  const cached = partUrlCache.get(`${uploadId}:${index}`);
  if (cached) return cached;

  const batch = await request('post', `/api/v1/app/admin/uploads/${uploadId}/parts`, {
    data: { from: index, count: PART_URL_BATCH },
  });

  (batch.parts || []).forEach((part) => {
    partUrlCache.set(`${uploadId}:${part.index}`, part.url);
  });

  const url = partUrlCache.get(`${uploadId}:${index}`);
  if (!url) {
    throw normalizeError(new Error(`Bo'lak ${index} uchun havola berilmadi`));
  }
  return url;
}

/**
 * Katta fayl - bo'laklab yuklash.
 *
 * Har bir bo'lak alohida so'rov, ya'ni bittasi uzilsa faqat o'sha qayta
 * yuboriladi - butun fayl emas. Server allaqachon qabul qilgan bo'laklarni
 * aytadi, shuning uchun qayta urinishda ular o'tkazib yuboriladi.
 *
 * Sahifa yangilansa ham yuklash davom etadi: `uploadId` saqlanadi va
 * server yetib kelgan bo'laklarni aytadi.
 */
async function uploadChunked(file, folder, onProgress, options = {}) {
  const resumed = await findResumable(file);
  const session = resumed || await request('post', '/api/v1/app/admin/uploads', {
    data: {
      filename: file.name,
      sizeBytes: file.size,
      mimeType: file.type || 'application/octet-stream',
      folder,
    },
  });
  rememberUpload(file, session.uploadId);

  const { uploadId, chunkSize, totalChunks } = session;
  const alreadyHave = new Set(session.receivedChunks || []);

  for (let index = 0; index < totalChunks; index += 1) {
    if (alreadyHave.has(index)) continue;

    // Bekor qilingan bo'lsa keyingi bo'lakni yubormaymiz.
    if (options.signal?.aborted) {
      throw normalizeError(new Error('Yuklash bekor qilindi'));
    }

    const blob = file.slice(index * chunkSize, (index + 1) * chunkSize);

    let lastError = null;
    for (let attempt = 1; attempt <= CHUNK_RETRIES; attempt += 1) {
      try {
        if (session.uploadMode === 'S3_MULTIPART') {
          // ⚠️ Bo'lak SERVER ORQALI O'TMAYDI — imzolangan havola
          // orqali to'g'ridan-to'g'ri omborga ketadi.
          //
          // `axios` emas, `fetch`: `axios` bizning interceptor'imizga
          // ega va u har bir so'rovga `Authorization` sarlavhasini
          // qo'shadi. Ombor esa imzoni tekshiradi va begona
          // sarlavhani ko'rib so'rovni RAD ETADI.
          const url = await partUrl(uploadId, index);

          // ⚠️ CORS bloklaganida `fetch` `TypeError` tashlaydi va
          // internet uzilganidan FARQ QILMAYDI — brauzer sababni
          // ataylab aytmaydi (aks holda sahifa boshqa domenlarni
          // skanerlay olardi).
          //
          // Lekin bu yerda kontekst bor: bo'lak OMBORGA ketyapti,
          // imzoni esa server hozirgina berdi — demak server bilan
          // aloqa BOR. Shu sababli eng ehtimolli sabab bucketda CORS
          // sozlanmagani.
          //
          // Buni aytmaslik qimmatga tushdi: xabar «Internetni
          // tekshiring» derdi va odam soatlab tarmoqni tekshirardi,
          // ayb esa ombor sozlamasida edi.
          let response;
          try {
            response = await fetch(url, { method: 'PUT', body: blob });
          } catch (cause) {
            const error = new Error(STORAGE_UNREACHABLE);
            error.storageUnreachable = true;
            error.cause = cause;
            throw error;
          }

          if (!response.ok) {
            const error = new Error(`Bo'lak yuborilmadi (${response.status})`);
            error.response = { status: response.status };
            throw error;
          }
        } else {
          await fileRequest(() => http.put(
            `/api/v1/app/admin/uploads/${uploadId}/chunks/${index}`, blob, {
              headers: { 'Content-Type': 'application/octet-stream' },
              timeout: FILE_TIMEOUT_MS,
            },
          ));
        }
        lastError = null;
        break;
      } catch (error) {
        lastError = error;
        // Serverning "yo'q" javobini qayta urinish bilan yengib bo'lmaydi -
        // faqat tarmoq va server xatolarida qayta urinamiz.
        const status = error.response?.status;
        if (status && status < 500) break;
      }
    }

    if (lastError) {
      throw normalizeError(lastError);
    }

    if (onProgress) {
      // 100% ni yig'ish tugagach beramiz - aks holda progress to'lgach
      // foydalanuvchi kutib qolardi.
      onProgress(Math.round(((index + 1) * 99) / totalChunks));
    }
  }

  const media = await request('post', `/api/v1/app/admin/uploads/${uploadId}/complete`,
    { data: {}, timeout: COMPLETE_TIMEOUT_MS });

  // Havolalar endi keraksiz va ular xotirada joy egallaydi.
  [...partUrlCache.keys()]
    .filter((key) => key.startsWith(`${uploadId}:`))
    .forEach((key) => partUrlCache.delete(key));

  forgetUpload(file);
  if (onProgress) onProgress(100);
  return media;
}

/**
 * Fayl yuklash.
 *
 * O'lchamga qarab o'zi tanlaydi: kichik bo'lsa bitta so'rov, katta bo'lsa
 * bo'laklab. Chaqiruvchi uchun farqi yo'q.
 */
async function uploadFile(file, folder = 'content', onProgress, options = {}) {
  try {
    return file.size > CHUNKED_THRESHOLD
      ? await uploadChunked(file, folder, onProgress, options)
      : await uploadSingle(file, folder, onProgress);
  } catch (error) {
    // uploadChunked allaqachon normalizatsiya qilgan bo'lishi mumkin.
    const normalized = error.code && error.status !== undefined
      ? error
      : normalizeError(error);
    if (normalized.status === 401 && onUnauthorized) onUnauthorized();
    throw normalized;
  }
}

export const adminApi = {
  login: (phone, password) => api.post('/api/v1/app/admin/auth/login', { phone, password }),
  logout: () => api.logout(),
  refreshSession: () => api.refresh(),
  me: () => api.get('/api/v1/app/admin/auth/me'),
  dashboard: () => api.get('/api/v1/app/admin/dashboard/summary'),
  // --- Dashboard grafiklari va jadvallari (ТЗ §48 · BOSQICH F3) ---
  //
  // ⚠️ `summary` dan ATAYLAB alohida: kartochkalar yengil va tez keladi,
  // grafiklar esa og'irroq. Ularni bitta javobga qo'shish sahifaning
  // BIRINCHI ko'rinishini sekinlashtirardi.
  dashboardCharts: (days) => api.get('/api/v1/app/admin/dashboard/charts', { days }),
  dashboardTables: (limit) => api.get('/api/v1/app/admin/dashboard/tables', { limit }),

  content: (params) => api.get('/api/v1/app/admin/content', params),
  contentById: (id) => api.get(`/api/v1/app/admin/content/${id}`),
  createContent: (body) => api.post('/api/v1/app/admin/content', body),
  updateContent: (id, body) => api.put(`/api/v1/app/admin/content/${id}`, body),
  archiveContent: (id) => api.del(`/api/v1/app/admin/content/${id}`),

  seasons: (contentId) => api.get(`/api/v1/app/admin/content/${contentId}/seasons`),
  createSeason: (contentId, body) => api.post(`/api/v1/app/admin/content/${contentId}/seasons`, body),
  updateSeason: (contentId, id, body) => api.put(`/api/v1/app/admin/content/${contentId}/seasons/${id}`, body),
  deleteSeason: (contentId, id) => api.del(`/api/v1/app/admin/content/${contentId}/seasons/${id}`),

  episodes: (contentId) => api.get(`/api/v1/app/admin/content/${contentId}/episodes`),
  createEpisode: (contentId, body) => api.post(`/api/v1/app/admin/content/${contentId}/episodes`, body),
  updateEpisode: (contentId, id, body) => api.put(`/api/v1/app/admin/content/${contentId}/episodes/${id}`, body),
  deleteEpisode: (contentId, id) => api.del(`/api/v1/app/admin/content/${contentId}/episodes/${id}`),

  creators: (params) => api.get('/api/v1/app/admin/creators', params),
  createCreator: (body) => api.post('/api/v1/app/admin/creators', body),
  updateCreator: (id, body) => api.put(`/api/v1/app/admin/creators/${id}`, body),

  categories: (params) => api.get('/api/v1/app/admin/categories', params),
  createCategory: (body) => api.post('/api/v1/app/admin/categories', body),
  updateCategory: (id, body) => api.put(`/api/v1/app/admin/categories/${id}`, body),
  /** ⚠️ Bog'langan kontenti bo'lsa backend 409 `CATEGORY_IN_USE` qaytaradi (ТЗ §16). */
  deleteCategory: (id) => api.del(`/api/v1/app/admin/categories/${id}`),

  genres: (params) => api.get('/api/v1/app/admin/genres', params),
  createGenre: (body) => api.post('/api/v1/app/admin/genres', body),
  updateGenre: (id, body) => api.put(`/api/v1/app/admin/genres/${id}`, body),
  /** ⚠️ Bog'langan kontenti bo'lsa backend 409 `GENRE_IN_USE` qaytaradi (ТЗ §17). */
  deleteGenre: (id) => api.del(`/api/v1/app/admin/genres/${id}`),

  advertisements: () => api.get('/api/v1/app/admin/advertisements'),
  createAd: (body) => api.post('/api/v1/app/admin/advertisements', body),
  updateAd: (id, body) => api.put(`/api/v1/app/admin/advertisements/${id}`, body),
  deleteAd: (id) => api.del(`/api/v1/app/admin/advertisements/${id}`),

  premieres: () => api.get('/api/v1/app/admin/premieres'),
  createPremiere: (body) => api.post('/api/v1/app/admin/premieres', body),
  updatePremiere: (id, body) => api.put(`/api/v1/app/admin/premieres/${id}`, body),
  deletePremiere: (id) => api.del(`/api/v1/app/admin/premieres/${id}`),

  homepageSections: () => api.get('/api/v1/app/admin/homepage/sections'),
  updateHomepageSection: (id, body) => api.put(`/api/v1/app/admin/homepage/sections/${id}`, body),

  // --- Bosh sahifa: tartib, qo'lda tanlash, ijodkorlar (ТЗ §31 · F4) ---
  //
  // ⚠️ Tartib BITTA atomar so'rovda yuboriladi. Bittalab o'zgartirishda
  // oraliq holat yuzaga kelardi va o'sha lahzada `/app/home` ni so'ragan
  // foydalanuvchi aralashib ketgan bosh sahifani ko'rardi.
  reorderHomepageSections: (sectionIds) =>
    api.put('/api/v1/app/admin/homepage/sections/order', { sectionIds }),
  homepageSectionItems: (id) =>
    api.get(`/api/v1/app/admin/homepage/sections/${id}/items`),
  /** ⚠️ Bo'sh ro'yxat — «qatorni tozalash», ya'ni avtomatik qoidaga qaytarish. */
  replaceHomepageSectionItems: (id, contentIds) =>
    api.put(`/api/v1/app/admin/homepage/sections/${id}/items`, { contentIds }),
  homepageCreators: (limit) =>
    api.get('/api/v1/app/admin/homepage/creators', { limit }),

  // --- Media kutubxonasi (ТЗ §26 · BOSQICH F2) ---
  media: (params) => api.get('/api/v1/app/admin/media', params),
  /** Bitta fayl — media maydonida faqat `mediaId` bo'lgani uchun kerak. */
  mediaAsset: (id) => api.get(`/api/v1/app/admin/media/${id}`),

  /**
   * Videoni panelda ko'rish uchun havola.
   *
   * ⚠️ Nega alohida so'rov: `<video src>` `Authorization` sarlavhasini
   * YUBORMAYDI. Server shu sababli manzilning o'ziga imzo qo'yib
   * beradi — S3 da to'g'ridan-to'g'ri omborga, lokalda esa chipta
   * bilan `/raw` ga.
   */
  mediaPreview: (id) => api.get(`/api/v1/app/admin/media/${id}/preview`),

  /**
   * Ombor holati — oxirgi skanerlash natijasi.
   *
   * ⚠️ Hech qachon skanerlanmagan bo'lsa server 204 qaytaradi va bu
   * XATO EMAS. `null` bilan farqlanadi.
   */
  storage: () => api.get('/api/v1/app/admin/storage'),

  /** ⚠️ QIMMAT: butun omborni qayta sanaydi. Faqat admin so'raganda. */
  storageScan: () => api.post('/api/v1/app/admin/storage/scan'),

  /**
   * Yetim faylni o'chiradi.
   *
   * ⚠️ QAYTARIB BO'LMAYDI. Server keshga ISHONMAYDI: fayl hali ham
   * yetim ekanini bazadan qayta hisoblaydi va biriktirilgan bo'lsa
   * `409` qaytaradi.
   *
   * ⚠️ Kalit tanada: u `/` belgilarini o'z ichiga oladi va manzilga
   * solinganda yo'l sifatida talqin qilinardi.
   */
  /**
   * Papkani ochadi.
   *
   * ⚠️ Bu `storageScan` dan tubdan ARZON va keshlanmaydi: S3 bitta
   * darajani qaytaradi, ichkariga kirmaydi.
   */
  storageBrowse: (prefix) =>
    api.get('/api/v1/app/admin/storage/browse', { prefix: prefix || '' }),

  storageDeleteOrphan: (key) =>
    api.post('/api/v1/app/admin/storage/orphan/delete', { key }),
  /** Yiqilgan transcoding'ni navbatga qaytaradi (MEDIA_UPLOAD). */
  retryTranscoding: (id) => api.post(`/api/v1/app/admin/media/${id}/retry-transcoding`),
  /** Navbat holati — panel yangilashni qachon to'xtatishni biladi. */
  transcodingQueue: () => api.get('/api/v1/app/admin/media/transcoding-queue'),
  uploadMedia: uploadFile,

  /**
   * Yarim qolgan yuklashni bekor qiladi.
   *
   * ⚠️ Server bo'laklarni tozalaydi. Chaqirilmasa ular diskda qolib
   * ketardi: bir gigabaytlik video bekor qilinsa ham joy egallardi.
   */
  cancelUpload: async (file, uploadId) => {
    try {
      await api.del(`/api/v1/app/admin/uploads/${uploadId}`);
    } finally {
      // Server javobidan qat'i nazar klient eslab qolmasin: seans
      // baribir yaroqsiz va uni davom ettirishga urinish 404 berardi.
      if (file) forgetUpload(file);
    }
  },
  /** Fayl qayerda ishlatilyapti — o'chirishdan OLDIN ko'rsatiladi. */
  mediaUsage: (id) => api.get(`/api/v1/app/admin/media/${id}/usage`),
  archiveMedia: (id) => api.post(`/api/v1/app/admin/media/${id}/archive`),
  restoreMedia: (id) => api.post(`/api/v1/app/admin/media/${id}/restore`),
  /** ⚠️ Ishlatilayotgan fayl uchun backend 409 `MEDIA_IN_USE` qaytaradi. */
  deleteMedia: (id) => api.del(`/api/v1/app/admin/media/${id}`),

  comments: (params) => api.get('/api/v1/app/admin/comments', params),
  setCommentStatus: (id, status) => api.put(`/api/v1/app/admin/comments/${id}/status/${status}`),

  notificationAudience: () => api.get('/api/v1/app/admin/notifications/audience'),
  notifications: (params) => api.get('/api/v1/app/admin/notifications', params),
  createNotification: (body) => api.post('/api/v1/app/admin/notifications', body),
  updateNotification: (id, body) => api.put(`/api/v1/app/admin/notifications/${id}`, body),
  sendNotification: (id) => api.post(`/api/v1/app/admin/notifications/${id}/send`),
  cancelNotification: (id) => api.post(`/api/v1/app/admin/notifications/${id}/cancel`),

  users: (params) => api.get('/api/v1/app/admin/users', params),
  /** Bitta foydalanuvchi (ТЗ §35 · BOSQICH F6). */
  userById: (id) => api.get(`/api/v1/app/admin/users/${id}`),
  blockUser: (id, reason) => api.post(`/api/v1/app/admin/users/${id}/block`, { reason }),
  unblockUser: (id) => api.post(`/api/v1/app/admin/users/${id}/unblock`),
  grantPremium: (id, body) => api.post(`/api/v1/app/admin/users/${id}/premium`, body),

  // --- Donatlar (ТЗ §42) ---
  donationReport: (params) => api.get('/api/v1/app/admin/donations/report', params),
  donationTransactions: (params) =>
    api.get('/api/v1/app/admin/donations/transactions', params),

  // --- Eski casting moduli (ТЗ §49) ---
  // ⚠️ Yo'l ATAYLAB eski: `/api/v1/casting-user/web`. Eski tizim
  // o'zgartirilmaydi, panel unga shunchaki murojaat qiladi.
  castingApplications: (params) => api.get('/api/v1/casting-user/web', params),
  revokePremium: (id) => api.del(`/api/v1/app/admin/users/${id}/premium`),
  userDevices: (id) => api.get(`/api/v1/app/admin/users/${id}/devices`),
  revokeDevice: (id, rowId) => api.del(`/api/v1/app/admin/users/${id}/devices/${rowId}`),

  subscriptions: (params) => api.get('/api/v1/app/admin/subscriptions', params),
  /**
   * Obunalar jamlanmasi — grafiklar uchun.
   *
   * ⚠️ Son va daromad ALOHIDA keladi: sovg'a obunalar sanaladi, lekin
   * pul keltirmaydi. Ularni bitta grafikda birlashtirib bo'lmaydi.
   */
  subscriptionSummary: (params) =>
    api.get('/api/v1/app/admin/subscriptions/summary', params),
  tariffs: () => api.get('/api/v1/app/admin/tariffs'),
  createTariff: (body) => api.post('/api/v1/app/admin/tariffs', body),
  updateTariff: (id, body) => api.put(`/api/v1/app/admin/tariffs/${id}`, body),

  currencyPackages: () => api.get('/api/v1/app/admin/currency-packages'),
  savePackage: (id, body) => (id
    ? api.put(`/api/v1/app/admin/currency-packages/${id}`, body)
    : api.post('/api/v1/app/admin/currency-packages', body)),
  deletePackage: (id) => api.del(`/api/v1/app/admin/currency-packages/${id}`),

  settings: () => api.get('/api/v1/app/admin/settings'),
  updateSetting: (key, value) => api.put(`/api/v1/app/admin/settings/${key}`, { value }),

  reportOverview: (params) => api.get('/api/v1/app/admin/reports/overview', params),

  // --- Bitta obyekt bo'yicha hisobotlar (ТЗ §81, §33, §46 · F5) ---
  //
  // ⚠️ Umumiy hisobotda faqat TOP-10 chiqadi. 30 ta banneri bor admin
  // 25-chisining natijasini umuman ko'ra olmasdi — shuning uchun har bir
  // obyekt uchun alohida endpoint bor.
  adStatistics: (id, days) =>
    api.get(`/api/v1/app/admin/advertisements/${id}/statistics`, { days }),
  contentStatistics: (id, days) =>
    api.get(`/api/v1/app/admin/reports/content/${id}/statistics`, { days }),
  /** ⚠️ O'lchanmaydigan ko'rsatkich `available: false` bilan keladi — nol EMAS. */
  notificationReport: (id) =>
    api.get(`/api/v1/app/admin/notifications/${id}/report`),

  auditLogs: (params) => api.get('/api/v1/app/admin/audit-logs', params),

  // --- Xodimlar (ТЗ §12, §78 · BOSQICH F1) ---
  //
  // ⚠️ Rol, ruxsat va parol ATAYLAB alohida endpointlarda: har birining
  // o'z xavfsizlik qoidasi bor (ierarxiya, «o'zida bo'lmaganini bera
  // olmaydi») va ularni oddiy tahrirlash bilan qo'shib yuborish
  // tekshiruvlarni yashirib qo'yardi.
  staff: (params) => api.get('/api/v1/app/admin/staff', params),
  createStaff: (body) => api.post('/api/v1/app/admin/staff', body),
  updateStaff: (id, body) => api.put(`/api/v1/app/admin/staff/${id}`, body),
  changeStaffRole: (id, role) =>
    api.put(`/api/v1/app/admin/staff/${id}/role`, null, { role }),
  setStaffPermissions: (id, permissions) =>
    api.put(`/api/v1/app/admin/staff/${id}/permissions`, permissions),
  resetStaffPassword: (id, password) =>
    api.put(`/api/v1/app/admin/staff/${id}/password`, { password }),
  activateStaff: (id) => api.post(`/api/v1/app/admin/staff/${id}/activate`),
  deactivateStaff: (id, reason) =>
    api.post(`/api/v1/app/admin/staff/${id}/deactivate`, null, { reason: reason || undefined }),
  blockStaff: (id, reason) =>
    api.post(`/api/v1/app/admin/staff/${id}/block`, null, { reason: reason || undefined }),
  unblockStaff: (id) => api.post(`/api/v1/app/admin/staff/${id}/unblock`),
};
