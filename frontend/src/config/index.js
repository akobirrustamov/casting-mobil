import axios from "axios";

// Bazaviy manzil: .env dagi REACT_APP_API_URL bo'lsa o'sha, aks holda lokal server.
// Prod uchun: REACT_APP_API_URL=https://uzcasting.site
// ⚠️ `??`, `||` EMAS: bo'sh qiymat «xuddi shu domen» degani va u
// haqiqiy sozlama. `||` bilan u localhost'ga tushib ketardi va prod
// buildida foydalanuvchi brauzeri o'z kompyuteriga murojaat qilardi.
export let baseUrl = process.env.REACT_APP_API_URL ?? "http://localhost:8080";

/**
 * @param {string} url        endpoint (masalan "/api/v1/news")
 * @param {string} method     GET | POST | PUT | DELETE
 * @param {*}      data       body
 * @param {*}      param      query params
 * @param {boolean} isMultipart  FormData yuborilayotgan bo'lsa true
 * @param {function} onUploadProgress  yuklanish foizi uchun callback
 */
export default function (url, method, data, param, isMultipart, onUploadProgress) {
    const token = localStorage.getItem("access_token");

    const headers = {};
    if (token) {
        headers["Authorization"] = token;
    }
    if (isMultipart) {
        // axios FormData uchun boundary bilan Content-Type ni o'zi qo'yadi
        delete headers["Content-Type"];
    }

    return axios({
        url: baseUrl + url,
        method: method,
        data: data,
        headers: headers,
        params: param,
        onUploadProgress: typeof onUploadProgress === "function" ? onUploadProgress : undefined
    }).then((res) => {
        return {
            error: false,
            data: res.data
        };
    }).catch((err) => {
        const status = err?.response?.status;

        // Avvalgi xulq saqlanadi: refresh_token yo'q 401 -> {error:true, data:401}
        if (status === 401 && localStorage.getItem("refresh_token") === null) {
            return {
                error: true,
                data: 401
            };
        }

        // Qolgan barcha xatolar: chaqiruvchi tomondagi try/catch ishlashi uchun
        // xatoni oshkora tashlaymiz (avval bu yerda undefined qaytib,
        // chaqiruvchida tushunarsiz TypeError yuzaga kelardi).
        throw err;
    });
}
