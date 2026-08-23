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

export const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

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

async function request(method, url, { data, params, retried } = {}) {
  try {
    const res = await http.request({ method, url, data, params });
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
        return await request(method, url, { data, params, retried: true });
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
  post: (url, data) => request('post', url, { data }),
  put: (url, data) => request('put', url, { data }),
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

/** Bo'lak yuborish uchun alohida, uzoqroq kutish - 5 MB sekin internetda vaqt oladi. */
const CHUNK_TIMEOUT_MS = 120000;

/** Bitta bo'lak necha marta qayta urinadi. */
const CHUNK_RETRIES = 3;

/** Kichik fayl - bitta multipart so'rov. */
async function uploadSingle(file, folder, onProgress) {
  const form = new FormData();
  form.append('file', file);
  form.append('folder', folder);
  const res = await http.post('/api/v1/app/admin/media', form, {
    onUploadProgress: (e) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded * 100) / e.total));
      }
    },
  });
  return res.data;
}

/**
 * Katta fayl - bo'laklab yuklash.
 *
 * Har bir bo'lak alohida so'rov, ya'ni bittasi uzilsa faqat o'sha qayta
 * yuboriladi - butun fayl emas. Server allaqachon qabul qilgan bo'laklarni
 * aytadi, shuning uchun qayta urinishda ular o'tkazib yuboriladi.
 */
async function uploadChunked(file, folder, onProgress) {
  const session = await request('post', '/api/v1/app/admin/uploads', {
    data: {
      filename: file.name,
      sizeBytes: file.size,
      mimeType: file.type || 'application/octet-stream',
      folder,
    },
  });

  const { uploadId, chunkSize, totalChunks } = session;
  const alreadyHave = new Set(session.receivedChunks || []);

  for (let index = 0; index < totalChunks; index += 1) {
    if (alreadyHave.has(index)) continue;

    const blob = file.slice(index * chunkSize, (index + 1) * chunkSize);

    let lastError = null;
    for (let attempt = 1; attempt <= CHUNK_RETRIES; attempt += 1) {
      try {
        await http.put(`/api/v1/app/admin/uploads/${uploadId}/chunks/${index}`, blob, {
          headers: { 'Content-Type': 'application/octet-stream' },
          timeout: CHUNK_TIMEOUT_MS,
        });
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

  const media = await request('post', `/api/v1/app/admin/uploads/${uploadId}/complete`, { data: {} });
  if (onProgress) onProgress(100);
  return media;
}

/**
 * Fayl yuklash.
 *
 * O'lchamga qarab o'zi tanlaydi: kichik bo'lsa bitta so'rov, katta bo'lsa
 * bo'laklab. Chaqiruvchi uchun farqi yo'q.
 */
async function uploadFile(file, folder = 'content', onProgress) {
  try {
    return file.size > CHUNKED_THRESHOLD
      ? await uploadChunked(file, folder, onProgress)
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

  genres: (params) => api.get('/api/v1/app/admin/genres', params),
  createGenre: (body) => api.post('/api/v1/app/admin/genres', body),
  updateGenre: (id, body) => api.put(`/api/v1/app/admin/genres/${id}`, body),

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

  media: (params) => api.get('/api/v1/app/admin/media', params),
  uploadMedia: uploadFile,

  comments: (params) => api.get('/api/v1/app/admin/comments', params),
  setCommentStatus: (id, status) => api.put(`/api/v1/app/admin/comments/${id}/status/${status}`),

  notifications: (params) => api.get('/api/v1/app/admin/notifications', params),
  createNotification: (body) => api.post('/api/v1/app/admin/notifications', body),
  updateNotification: (id, body) => api.put(`/api/v1/app/admin/notifications/${id}`, body),
  sendNotification: (id) => api.post(`/api/v1/app/admin/notifications/${id}/send`),
  cancelNotification: (id) => api.post(`/api/v1/app/admin/notifications/${id}/cancel`),

  users: (params) => api.get('/api/v1/app/admin/users', params),
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

  auditLogs: (params) => api.get('/api/v1/app/admin/audit-logs', params),

  staff: (params) => api.get('/api/v1/app/admin/staff', params),
};
