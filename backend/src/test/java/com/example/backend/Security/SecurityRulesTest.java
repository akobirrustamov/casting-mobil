package com.example.backend.Security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * SecurityConfig kirish qoidalarining regressiya testi.
 *
 * Bu test ikki tomonlama qo'riqchi:
 *   1. Ochiq qolishi SHART bo'lgan endpointlar yopilib qolmasin — aks holda sayt,
 *      Telegram bot yoki mobil ilova ishlamay qoladi;
 *   2. Yopilishi shart bo'lganlar ochilib ketmasin.
 *
 * Biznes natijasi tekshirilmaydi — faqat kirish nazorati. Shuning uchun ochiq
 * endpointlar uchun "401/403 EMAS" deb tekshiriladi: 400 yoki 404 ham to'g'ri
 * javob, muhimi so'rov himoya devoridan o'tgani.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Xavfsizlik qatlami so'rovni to'sganini bildiruvchi kodlar.
     *
     * Bu {@link RestAuthErrorHandler} qaytaradigan kodlar. Ular biznes darajasidagi
     * 401 dan (masalan, login'da noto'g'ri parol -> INVALID_CREDENTIALS) farq qiladi:
     * u yerda so'rov himoya devoridan O'TGAN va handler'ga yetib borgan.
     */
    private static final String BLOCKED_BY_SECURITY = "\"code\":\"UNAUTHORIZED\"";
    private static final String DENIED_BY_SECURITY = "\"code\":\"ACCESS_DENIED\"";

    /**
     * So'rov himoya devoridan o'tganini tekshiradi.
     *
     * Status 400, 404, hatto 401 ham bo'lishi mumkin — muhimi, 401 xavfsizlik
     * qatlamidan emas, handler'ning o'zidan kelgan bo'lsin.
     */
    private void assertReachable(MvcResult result, String what) throws Exception {
        String body = result.getResponse().getContentAsString();
        int status = result.getResponse().getStatus();
        assertThat(body)
                .as("%s ochiq bo'lishi kerak, lekin xavfsizlik qatlami to'sdi (status %d)",
                        what, status)
                .doesNotContain(BLOCKED_BY_SECURITY)
                .doesNotContain(DENIED_BY_SECURITY);
    }

    /** So'rov aynan xavfsizlik qatlami tomonidan to'silganini tekshiradi. */
    private void assertBlocked(MvcResult result, String what) throws Exception {
        int status = result.getResponse().getStatus();
        assertThat(status)
                .as("%s tokensiz 401 qaytarishi kerak, lekin %d qaytdi", what, status)
                .isEqualTo(401);
        assertThat(result.getResponse().getContentAsString())
                .as("%s 401 ni xavfsizlik qatlami qaytarishi kerak", what)
                .contains(BLOCKED_BY_SECURITY);
    }

    @Nested
    @DisplayName("Ochiq qolishi shart - aks holda klientlar sinadi")
    class PublicEndpoints {

        @Test
        @DisplayName("Sayt katalogi va mobil ilova: GET /casting-user/web")
        void catalogIsPublic() throws Exception {
            assertReachable(mockMvc.perform(get("/api/v1/casting-user/web")).andReturn(),
                    "GET /api/v1/casting-user/web");
        }

        @Test
        @DisplayName("Bot bosh sahifasi: GET /news")
        void newsListIsPublic() throws Exception {
            assertReachable(mockMvc.perform(get("/api/v1/news")).andReturn(),
                    "GET /api/v1/news");
        }

        @Test
        @DisplayName("Bot: anonim foydalanuvchi anketa yuboradi - POST /casting-user")
        void applicationSubmitIsPublic() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/casting-user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"photos\":[]}"))
                    .andReturn();
            assertReachable(result, "POST /api/v1/casting-user (bot anketasi)");
        }

        @Test
        @DisplayName("Bot: anonim foydalanuvchi rasm yuklaydi - POST /file/upload")
        void fileUploadIsPublic() throws Exception {
            // Parametrlarsiz yuborilyapti: 400 kutiladi, lekin 401 EMAS.
            assertReachable(mockMvc.perform(multipart("/api/v1/file/upload")).andReturn(),
                    "POST /api/v1/file/upload (bot rasmi)");
        }

        @Test
        @DisplayName("Bot: mening arizalarim - GET /casting-user/my/{telegramId}")
        void myApplicationsIsPublic() throws Exception {
            assertReachable(mockMvc.perform(get("/api/v1/casting-user/my/123456")).andReturn(),
                    "GET /api/v1/casting-user/my/**");
        }

        @Test
        @DisplayName("Bot: murojaat - GET /casting-user/appeal/{id}")
        void appealIsPublic() throws Exception {
            assertReachable(mockMvc.perform(get("/api/v1/casting-user/appeal/1")).andReturn(),
                    "GET /api/v1/casting-user/appeal/**");
        }

        @Test
        @DisplayName("Rasmlar: GET /file/getFile/{id}")
        void fileDownloadIsPublic() throws Exception {
            assertReachable(mockMvc.perform(
                            get("/api/v1/file/getFile/00000000-0000-0000-0000-000000000000"))
                            .andReturn(),
                    "GET /api/v1/file/getFile/**");
        }

        @Test
        @DisplayName("Kirish: POST /auth/login va /auth/google")
        void authEndpointsArePublic() throws Exception {
            // Soxta parol bilan 401 kutiladi - bu TO'G'RI javob va endpoint ochiq
            // ekanini bildiradi. Muhimi, 401 xavfsizlik qatlamidan emas, login
            // mantiqidan kelishi (INVALID_CREDENTIALS, UNAUTHORIZED emas).
            assertReachable(mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\":\"x\",\"password\":\"y\"}"))
                    .andReturn(), "POST /api/v1/auth/login");

            assertReachable(mockMvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"probe\"}"))
                    .andReturn(), "POST /api/v1/auth/google");
        }

        @Test
        @DisplayName("Analitika hodisalari ochiq — anonim foydalanuvchi ham yuboradi")
        void analyticsIngestIsPublic() throws Exception {
            assertReachable(mockMvc.perform(post("/api/v1/app/analytics/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"deviceKey\":\"d1\",\"events\":[{\"type\":\"CONTENT_VIEW\",\"targetId\":1}]}"))
                    .andReturn(), "POST /api/v1/app/analytics/events");
        }

        @Test
        @DisplayName("Admin panelga kirish: POST /api/v1/app/admin/auth/login")
        void adminLoginIsPublic() throws Exception {
            // Bu qoida unutilgan edi va barcha admin login'lari bloklangan edi.
            assertReachable(mockMvc.perform(post("/api/v1/app/admin/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\":\"+998900000000\",\"password\":\"whatever1\"}"))
                    .andReturn(), "POST /api/v1/app/admin/auth/login");
        }

        @Test
        @DisplayName("Media fayli ochiq: GET /api/v1/app/media/{id}/raw")
        void mediaRawIsPublic() throws Exception {
            assertReachable(mockMvc.perform(get("/api/v1/app/media/1/raw")).andReturn(),
                    "GET /api/v1/app/media/*/raw");
        }

        @Test
        @DisplayName("React marshrutlari (SPA) yopilmagan")
        void spaRoutesArePublic() throws Exception {
            assertReachable(mockMvc.perform(get("/models")).andReturn(), "SPA /models");
            assertReachable(mockMvc.perform(get("/aadmin/login")).andReturn(), "SPA /aadmin/login");
        }
    }

    @Nested
    @DisplayName("Tokensiz yopiq bo'lishi shart")
    class ProtectedEndpoints {

        @Test
        @DisplayName("To'liq anketa ro'yxati - shaxsiy ma'lumot bilan")
        void fullCastingListIsProtected() throws Exception {
            assertBlocked(mockMvc.perform(get("/api/v1/casting-user")).andReturn(),
                    "GET /api/v1/casting-user");
        }

        @Test
        @DisplayName("Anketa o'chirish")
        void deleteIsProtected() throws Exception {
            assertBlocked(mockMvc.perform(delete("/api/v1/casting-user/1")).andReturn(),
                    "DELETE /api/v1/casting-user/1");
        }

        @Test
        @DisplayName("Anketa statusi, narxi va web-show")
        void castingMutationsAreProtected() throws Exception {
            assertBlocked(mockMvc.perform(put("/api/v1/casting-user/status/1/1/100")).andReturn(),
                    "PUT /casting-user/status/**");
            assertBlocked(mockMvc.perform(put("/api/v1/casting-user/price/1/100")).andReturn(),
                    "PUT /casting-user/price/**");
            assertBlocked(mockMvc.perform(put("/api/v1/casting-user/web-show/1")).andReturn(),
                    "PUT /casting-user/web-show/**");
        }

        @Test
        @DisplayName("Admin statistikasi")
        void adminStatisticIsProtected() throws Exception {
            assertBlocked(mockMvc.perform(get("/api/v1/admin/statistic")).andReturn(),
                    "GET /api/v1/admin/statistic");
        }

        @Test
        @DisplayName("Admin parolini o'zgartirish - hisob egallab olishning oldini oladi")
        void passwordChangeIsProtected() throws Exception {
            assertBlocked(mockMvc.perform(
                            put("/api/v1/auth/password/00000000-0000-0000-0000-000000000000")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"password\":\"hacked\"}"))
                            .andReturn(),
                    "PUT /api/v1/auth/password/**");
        }

        @Test
        @DisplayName("Yangilik yaratish, tahrirlash, o'chirish")
        void newsMutationsAreProtected() throws Exception {
            assertBlocked(mockMvc.perform(post("/api/v1/news")
                            .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "POST /api/v1/news");
            assertBlocked(mockMvc.perform(delete("/api/v1/news/1")).andReturn(),
                    "DELETE /api/v1/news/1");
        }

        @Test
        @DisplayName("Fayl ko'rinishini o'zgartirish")
        void attachmentToggleIsProtected() throws Exception {
            assertBlocked(mockMvc.perform(
                            put("/api/v1/file/00000000-0000-0000-0000-000000000000")).andReturn(),
                    "PUT /api/v1/file/**");
        }

        @Test
        @DisplayName("Security va decode")
        void securityEndpointsAreProtected() throws Exception {
            assertBlocked(mockMvc.perform(get("/api/v1/security")
                    .header("Authorization", "")).andReturn(), "GET /api/v1/security");
        }

        @Test
        @DisplayName("Admin profili tokensiz yopiq: /auth/me")
        void adminMeIsProtected() throws Exception {
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/auth/me")).andReturn(),
                    "GET /api/v1/app/admin/auth/me");
        }

        @Test
        @DisplayName("Yangi admin API sukut bo'yicha yopiq")
        void newAdminApiIsProtectedByDefault() throws Exception {
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/staff")).andReturn(),
                    "GET /api/v1/app/admin/**");
        }

        @Test
        @DisplayName("CMS yozish amallari tokensiz yopiq")
        void cmsWriteEndpointsAreProtected() throws Exception {
            assertBlocked(mockMvc.perform(post("/api/v1/app/admin/content")
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "POST /api/v1/app/admin/content");
            assertBlocked(mockMvc.perform(put("/api/v1/app/admin/content/1")
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "PUT /api/v1/app/admin/content/1");
            assertBlocked(mockMvc.perform(delete("/api/v1/app/admin/content/1")).andReturn(),
                    "DELETE /api/v1/app/admin/content/1");
            assertBlocked(mockMvc.perform(post("/api/v1/app/admin/categories")
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "POST /api/v1/app/admin/categories");
            assertBlocked(mockMvc.perform(post("/api/v1/app/admin/creators")
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "POST /api/v1/app/admin/creators");
        }

        @Test
        @DisplayName("Fasl va qism endpointlari tokensiz yopiq")
        void seasonEpisodeEndpointsAreProtected() throws Exception {
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/content/1/seasons")).andReturn(),
                    "GET /content/1/seasons");
            assertBlocked(mockMvc.perform(post("/api/v1/app/admin/content/1/seasons")
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "POST /content/1/seasons");
            assertBlocked(mockMvc.perform(delete("/api/v1/app/admin/content/1/seasons/1")).andReturn(),
                    "DELETE /content/1/seasons/1");
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/content/1/episodes")).andReturn(),
                    "GET /content/1/episodes");
            assertBlocked(mockMvc.perform(post("/api/v1/app/admin/content/1/episodes")
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "POST /content/1/episodes");
            assertBlocked(mockMvc.perform(delete("/api/v1/app/admin/content/1/episodes/1")).andReturn(),
                    "DELETE /content/1/episodes/1");
        }

        @Test
        @DisplayName("Bosh sahifa, reklama va premyera endpointlari tokensiz yopiq")
        void homepageEndpointsAreProtected() throws Exception {
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/homepage/sections")).andReturn(),
                    "GET /homepage/sections");
            assertBlocked(mockMvc.perform(put("/api/v1/app/admin/homepage/sections/1")
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "PUT /homepage/sections/1");
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/advertisements")).andReturn(),
                    "GET /advertisements");
            assertBlocked(mockMvc.perform(post("/api/v1/app/admin/advertisements")
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "POST /advertisements");
            assertBlocked(mockMvc.perform(delete("/api/v1/app/admin/advertisements/1")).andReturn(),
                    "DELETE /advertisements/1");
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/premieres")).andReturn(),
                    "GET /premieres");
            assertBlocked(mockMvc.perform(post("/api/v1/app/admin/premieres")
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "POST /premieres");
        }

        @Test
        @DisplayName("Izoh, bildirishnoma, foydalanuvchi va monetizatsiya yopiq")
        void phase67EndpointsAreProtected() throws Exception {
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/comments")).andReturn(),
                    "GET /comments");
            assertBlocked(mockMvc.perform(put("/api/v1/app/admin/comments/1/status/HIDDEN")).andReturn(),
                    "PUT /comments/1/status/HIDDEN");
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/notifications")).andReturn(),
                    "GET /notifications");
            assertBlocked(mockMvc.perform(post("/api/v1/app/admin/notifications/1/send")).andReturn(),
                    "POST /notifications/1/send");
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/users")).andReturn(),
                    "GET /users");
            assertBlocked(mockMvc.perform(post(
                    "/api/v1/app/admin/users/00000000-0000-0000-0000-000000000000/premium")
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "POST /users/{id}/premium");
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/tariffs")).andReturn(),
                    "GET /tariffs");
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/settings")).andReturn(),
                    "GET /settings");
            assertBlocked(mockMvc.perform(put("/api/v1/app/admin/settings/pricing.episode.default")
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                    "PUT /settings/{key}");
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/audit-logs")).andReturn(),
                    "GET /audit-logs");
        }

        @Test
        @DisplayName("Hisobotlar tokensiz yopiq")
        void reportsAreProtected() throws Exception {
            assertBlocked(mockMvc.perform(get("/api/v1/app/admin/reports/overview")).andReturn(),
                    "GET /reports/overview");
        }

        @Test
        @DisplayName("Media yuklash tokensiz yopiq (ko'rish esa ochiq)")
        void mediaUploadIsProtected() throws Exception {
            assertBlocked(mockMvc.perform(multipart("/api/v1/app/admin/media")).andReturn(),
                    "POST /api/v1/app/admin/media");
        }

        @Test
        @DisplayName("Noma'lum yangi endpoint ham sukut bo'yicha yopiq")
        void unknownApiPathIsProtectedByDefault() throws Exception {
            assertBlocked(mockMvc.perform(get("/api/v1/whatever-new-endpoint")).andReturn(),
                    "GET /api/v1/whatever-new-endpoint");
        }
    }
}
