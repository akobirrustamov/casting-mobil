package com.example.backend.Cms;

import com.example.backend.Entity.RefreshToken;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RefreshTokenRepo;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.Services.AuthService.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mobil ilova uchun token yangilash.
 *
 * <h2>⚠️ Qaysi nosozlik tuzatilyapti</h2>
 * Access token 15 daqiqa yashaydi. Mobil ilova refresh tokenni
 * OLARDI, lekin saqlamasdi va ishlatmasdi — odam har 15 daqiqada
 * tizimdan chiqib ketardi, hatto film o'rtasida ham.
 *
 * <h2>⚠️ Undan ham chuqurroq nosozlik</h2>
 * {@link RefreshTokenService} rotatsiya va bekor qilish bilan
 * allaqachon yozilgan edi, LEKIN kirish oqimlari uni chetlab o'tib
 * {@code jti} siz token berardi. Ya'ni:
 *
 * <ul>
 *   <li>token bazada yo'q edi — o'g'irlansa bekor qilib bo'lmasdi;</li>
 *   <li>rotatsiya uni «eski formatda» deb rad etardi — yangilash
 *       oqimi mobil uchun umuman ishlamasdi.</li>
 * </ul>
 *
 * Infratuzilma bor edi, undan foydalanuvchi yo'q edi. Bu test aynan
 * shu bog'lanishni qo'riqlaydi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MobileRefreshTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String REFRESH_URL = "/api/v1/app/auth/refresh";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private RefreshTokenRepo refreshTokenRepo;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------- yordamchi

    private User user() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99890" + (9600000 + SEQ.incrementAndGet()));
        u.setPassword(passwordEncoder.encode("Parol123!"));
        u.setName("Tomoshabin " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    private String body(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(
                refreshToken == null ? Map.of() : Map.of("refresh_token", refreshToken));
    }

    private String refresh(String token) throws Exception {
        return mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String field(String json, String name) throws Exception {
        return objectMapper.readTree(json).get(name).asText();
    }

    // ------------------------------------------------------------- yangilash

    @Nested
    @DisplayName("Yangilash")
    class Refreshing {

        @Test
        @DisplayName("Yangi access token beriladi")
        void issuesNewAccessToken() throws Exception {
            String refreshToken = refreshTokenService.issue(user(), null);

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").isNotEmpty());
        }

        /**
         * ⚠️ Javob {@code /otp/verify} va {@code /auth/google} bilan
         * BIR XIL shaklda — klient uchta oqim uchun bitta ishlov
         * yozadi.
         */
        @Test
        @DisplayName("Berilgan access token HAQIQIY kirish tokeni")
        void accessTokenIsUsable() throws Exception {
            User u = user();
            String json = refresh(refreshTokenService.issue(u, null));

            String access = field(json, "access_token");
            assertThat(jwtService.validateToken(access)).isTrue();
            assertThat(jwtService.typeOf(access)).isEqualTo(JwtService.TYPE_ACCESS);
            assertThat(jwtService.extractSubjectFromJwt(access)).isEqualTo(u.getId().toString());
        }

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV — bu bo'lmasa nosozlik QAYTADI.
         *
         * Rotatsiyada eski token bekor qilinadi. Klientga yangisi
         * berilmasa, u eskisini saqlab qolardi va keyingi yangilash
         * «bekor qilingan token» deb rad etilardi — odam yana
         * tizimdan chiqib ketardi.
         */
        @Test
        @DisplayName("YANGI refresh token ham qaytariladi va u ISHLAYDI")
        void returnsUsableRotatedToken() throws Exception {
            String first = refreshTokenService.issue(user(), null);

            String rotated = field(refresh(first), "refresh_token");
            assertThat(rotated).isNotEqualTo(first);

            // Ikkinchi yangilash — yangisi bilan.
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(rotated)))
                    .andExpect(status().isOk());
        }

        /** Uzoq sessiya: ketma-ket yangilashlar uzilmasligi kerak. */
        @Test
        @DisplayName("Ketma-ket yangilash uzilmaydi")
        void chainOfRefreshesWorks() throws Exception {
            String token = refreshTokenService.issue(user(), null);

            for (int i = 0; i < 4; i++) {
                token = field(refresh(token), "refresh_token");
            }
            assertThat(token).isNotBlank();
        }
    }

    // -------------------------------------------------------------- himoya

    @Nested
    @DisplayName("Himoya")
    class Protection {

        /**
         * ⚠️ Rotatsiyaning butun ma'nosi shunda: nusxa ko'chirilgan
         * token ikkinchi marta ishlatilsa bilinadi.
         */
        @Test
        @DisplayName("Bitta token IKKI marta ishlatilmaydi")
        void tokenWorksOnlyOnce() throws Exception {
            String first = refreshTokenService.issue(user(), null);
            refresh(first);

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(first)))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * ⚠️ Qayta ishlatish o'g'rilik belgisi bo'lishi mumkin, va
         * uni tarmoq uzilishidan ajratib bo'lmaydi. Xavfsiz tomon
         * tanlanadi: BARCHA sessiyalar yopiladi.
         */
        @Test
        @DisplayName("Qayta ishlatilsa butun zanjir yopiladi")
        void reuseClosesEverySession() throws Exception {
            User u = user();
            String first = refreshTokenService.issue(u, null);
            String second = field(refresh(first), "refresh_token");

            // O'g'irlangan eski token qayta keldi.
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(first)))
                    .andExpect(status().isUnauthorized());

            // Haqiqiy klientning yangi tokeni ham endi ishlamaydi.
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(second)))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * ⚠️ Aks holda o'g'irlangan access token cheksiz yangilanib
         * turardi va qisqa muddatning butun ma'nosi yo'qolardi.
         *
         * Eski {@code /api/v1/auth/refresh} da bu tekshiruv YO'Q.
         */
        @Test
        @DisplayName("Access token bilan yangilab bo'lmaydi")
        void accessTokenCannotRefresh() throws Exception {
            String access = jwtService.generateJwtToken(user());

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(access)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Soxta token o'tmaydi")
        void forgedTokenRejected() throws Exception {
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("soxta.token.imzo")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token yuborilmasa 401")
        void missingTokenRejected() throws Exception {
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(null)))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * ⚠️ Chiqishdan keyin token DARHOL kuchini yo'qotishi kerak.
         *
         * Aks holda «chiqish» faqat klient tomonida bo'lardi va
         * o'g'irlangan token muddati tugaguncha ishlayverardi.
         */
        @Test
        @DisplayName("Bekor qilingan token o'tmaydi")
        void revokedTokenRejected() throws Exception {
            String token = refreshTokenService.issue(user(), null);
            refreshTokenService.revoke(token);

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(token)))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * ⚠️ Token URL da EMAS, tanada qabul qilinadi.
         *
         * Eski endpoint uni {@code @RequestParam} bilan oladi va u
         * server, proksi hamda CDN jurnallariga ochiq tushadi —
         * bir kunlik kirish huquqi bir nechta jurnalda yotardi.
         */
        @Test
        @DisplayName("So'rov qatoridagi token QABUL QILINMAYDI")
        void queryParameterIsNotAccepted() throws Exception {
            String token = refreshTokenService.issue(user(), null);

            mockMvc.perform(post(REFRESH_URL + "?refresh_token=" + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(null)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------- kirish oqimi bilan bog'liq

    @Nested
    @DisplayName("⚠️ Kirish oqimi ro'yxatga olingan token beradi")
    class LoginIssuesTrackedTokens {

        /**
         * ⚠️ ENG CHUQUR TEKSHIRUV.
         *
         * {@code AuthServiceImpl} ilgari {@code jti} siz token
         * berardi. Bunday token bazada yo'q, ya'ni:
         *
         * <ul>
         *   <li>o'g'irlansa bekor qilib bo'lmasdi;</li>
         *   <li>rotatsiya uni «eski formatda» deb rad etardi va
         *       yangilash oqimi mobil uchun UMUMAN ishlamasdi.</li>
         * </ul>
         *
         * Bu test manba matnini o'qiydi, chunki nosozlik aynan
         * shu chaqiruvda edi — natijani tekshirish uchun esa
         * haqiqiy Google yoki SMS kerak bo'lardi.
         */
        @Test
        @DisplayName("Kirish `RefreshTokenService` orqali token beradi")
        void loginUsesTheRegistry() throws Exception {
            String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "src/main/java/com/example/backend/Services/AuthService/"
                            + "AuthServiceImpl.java"));

            // ⚠️ Faqat KOD qatorlari. Izohlarda eski chaqiruv nomi
            // ataylab eslatilgan (nima o'zgargani yozilgan) va uni
            // matn bo'yicha qidirish testni yolg'on yiqitardi.
            List<String> callSites = source.lines()
                    .map(String::trim)
                    .filter(line -> !line.startsWith("*") && !line.startsWith("//"))
                    .filter(line -> line.contains("\"refresh_token\""))
                    .toList();

            assertThat(callSites)
                    .as("`refresh_token` beradigan joy topilmadi — test eskirgan")
                    .isNotEmpty();

            assertThat(callSites)
                    .as("kirish yana ro'yxatdan tashqari token bermoqda — "
                            + "yangilash oqimi jimgina ishlamay qolardi")
                    .allMatch(line -> line.contains("issueRefreshToken("));

            assertThat(source).contains("refreshTokenService.issue(");
        }

        /**
         * Ro'yxatga olingan token — bazada qatori bor va u
         * yangilashdan keyin bekor qilinadi.
         */
        @Test
        @DisplayName("Token bazada qayd etiladi va yangilashda bekor bo'ladi")
        void tokenIsRecordedThenRevoked() throws Exception {
            String token = refreshTokenService.issue(user(), null);

            UUID jti = jwtService.jtiOf(token);
            assertThat(jti).as("token `jti` siz — bazada topib bo'lmaydi").isNotNull();

            RefreshToken row = refreshTokenRepo.findById(jti).orElseThrow();
            assertThat(row.getRevokedAt()).isNull();

            refresh(token);

            assertThat(refreshTokenRepo.findById(jti).orElseThrow().getRevokedAt())
                    .as("eski token bekor qilinmadi — u qayta ishlatilaverardi")
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("⚠️ Mobil bilan shartnoma")
    class MobileContract {

        private static final java.nio.file.Path STORE = java.nio.file.Path.of(
                "../mobile/src/features/auth/store.ts");
        private static final java.nio.file.Path API = java.nio.file.Path.of(
                "../mobile/src/lib/api.ts");
        private static final java.nio.file.Path AUTH_API = java.nio.file.Path.of(
                "../mobile/src/features/auth/api.ts");

        private String read(java.nio.file.Path path) throws java.io.IOException {
            return java.nio.file.Files.readString(path);
        }

        private boolean mobileMissing() {
            return !java.nio.file.Files.isRegularFile(STORE);
        }

        /**
         * ⚠️ AYNAN SHU nosozlik tuzatildi.
         *
         * Backend `refresh_token` ni har kirishda qaytarardi, mobil esa
         * uni TASHLAB YUBORARDI — faqat 15 daqiqalik access token
         * saqlanardi.
         */
        @Test
        @DisplayName("Mobil refresh tokenni SAQLAYDI")
        void mobileStoresTheRefreshToken() throws Exception {
            if (mobileMissing()) {
                return;
            }
            assertThat(read(STORE))
                    .as("refresh token yana saqlanmayapti — odam har 15 "
                            + "daqiqada tizimdan chiqib ketardi")
                    .contains("REFRESH_KEY");
        }

        /**
         * ⚠️ Saqlash yetarli emas — uni kirish oqimlari UZATISHI kerak.
         *
         * Uzatilmasa `signIn` uni `null` deb yozardi va natija
         * tuzatishdan oldingi holat bilan bir xil bo'lardi.
         */
        @Test
        @DisplayName("Ikkala kirish oqimi ham tokenni uzatadi")
        void bothLoginFlowsPassIt() throws Exception {
            if (mobileMissing()) {
                return;
            }
            assertThat(read(AUTH_API))
                    .as("kirish javobidan refresh token o'qilmayapti")
                    .contains("refreshToken: data.refresh_token");

            for (String screen : List.of("../mobile/app/(auth)/otp.tsx",
                    "../mobile/app/(auth)/sign-in.tsx")) {
                assertThat(read(java.nio.file.Path.of(screen)))
                        .as("%s refresh tokenni `signIn` ga bermayapti", screen)
                        .contains("signIn(token, user, refreshToken)");
            }
        }

        /**
         * ⚠️ ENG XAVFLI JOY.
         *
         * Backendda rotatsiya bor: har token BIR marta ishlaydi, va
         * bekor qilingan tokenni qayta ishlatish O'G'RILIK deb
         * baholanib, BARCHA sessiyalarni yopadi.
         *
         * Ekran bir vaqtda bir nechta so'rov yuboradi. Har biri o'z
         * yangilashini boshlasa, birinchisi o'tadi, qolganlari eski
         * token bilan keladi — va backend, mutlaqo to'g'ri, odamni
         * hamma joydan chiqarib yuboradi.
         *
         * Ya'ni tuzatishning o'zi aynan tuzatilayotgan nosozlikni
         * keltirib chiqarardi.
         */
        @Test
        @DisplayName("Yangilash BITTA nusxada bajariladi")
        void refreshIsSingleFlight() throws Exception {
            if (mobileMissing()) {
                return;
            }
            String source = read(API);

            assertThat(source)
                    .as("parallel yangilash — rotatsiya ularni o'g'rilik deb "
                            + "baholab, barcha sessiyalarni yopardi")
                    .contains("inFlight");

            assertThat(source)
                    .as("qayta urinish belgisi yo'q — 401 cheksiz aylanardi")
                    .contains("_retried");
        }

        /**
         * ⚠️ Eski endpoint tokenni URL da oladi, rotatsiya qilmaydi va
         * muzlatilgan. Mobil yangi makondagisini chaqirishi kerak.
         */
        @Test
        @DisplayName("Mobil YANGI endpointni chaqiradi")
        void mobileCallsTheNewEndpoint() throws Exception {
            if (mobileMissing()) {
                return;
            }
            assertThat(read(AUTH_API)).contains(REFRESH_URL);

            assertThat(read(AUTH_API))
                    .as("eski, muzlatilgan endpoint chaqirilmoqda")
                    .doesNotContain("post('/api/v1/auth/refresh'");
        }
    }
}
