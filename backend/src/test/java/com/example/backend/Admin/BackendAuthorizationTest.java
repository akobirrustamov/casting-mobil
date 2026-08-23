package com.example.backend.Admin;

import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Backend avtorizatsiyasi — ikki qavat.
 *
 * <h2>Qavat 1: Spring Security (bazaviy rol)</h2>
 * {@code /api/v1/app/admin/**} — faqat xodimlar. USER tokeni bu makonga
 * routing'dan OLDIN to'xtatiladi.
 *
 * Nega kerak: ichkaridagi ruxsat tekshiruvi YOZILISHI kerak. Kimdir yangi
 * endpoint qo'shib uni yozishni unutsa, ilgari `/api/**` faqat
 * autentifikatsiya talab qilardi — oddiy USER tokeni bilan yetib borish
 * mumkin edi. Endi bu avtomatik yopiq.
 *
 * <h2>Qavat 2: ruxsat va rol ierarxiyasi (endpoint darajasida)</h2>
 * Qaysi WORKER nima qila olishi, kim kimni boshqara olishi.
 *
 * <h2>Frontend hisobga olinmaydi</h2>
 * Menyuda elementni yashirish xavfsizlik EMAS. Bu yerdagi barcha test
 * to'g'ridan-to'g'ri HTTP so'rov yuboradi — panel umuman ishtirok etmaydi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
class BackendAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestStaffFactory staff;

    private String token(String phone, PlatformRole role, Permission... permissions) {
        EnumSet<Permission> set = permissions.length == 0
                ? EnumSet.noneOf(Permission.class)
                : EnumSet.copyOf(java.util.Arrays.asList(permissions));
        return staff.tokenForRole(phone, role, set);
    }

    // ------------------------------------------------------------- qavat 1

    @Nested
    @DisplayName("Spring Security qavati")
    class SecurityLayer {

        /**
         * MAVJUD BO'LMAGAN yo'l orqali tekshiramiz.
         *
         * Agar to'xtatish Spring Security'da bo'lsa: USER **403** oladi
         * (routing'gacha), xodim esa **404** (himoyadan o'tdi, handler yo'q).
         *
         * Agar javob ikkalasida ham 404 bo'lsa — demak rol qoidasi ishlamayapti
         * va himoya faqat metod ichidagi tekshiruvga tayanadi.
         */
        @Test
        @DisplayName("USER admin makoniga UMUMAN kira olmaydi")
        void plainUserIsStoppedBeforeRouting() throws Exception {
            mockMvc.perform(get("/api/v1/app/admin/bunday-endpoint-yoq")
                            .header("Authorization", "Bearer "
                                    + token("+998900001001", PlatformRole.USER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Xodim esa himoyadan o'tadi (404 - handler yo'q)")
        void staffPassesSecurityLayer() throws Exception {
            mockMvc.perform(get("/api/v1/app/admin/bunday-endpoint-yoq")
                            .header("Authorization", "Bearer "
                                    + token("+998900001002", PlatformRole.WORKER)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Parametr turi noto'g'ri bo'lsa 400, 500 emas")
        void typeMismatchIsBadRequest() throws Exception {
            // {userId} UUID kutadi. Ilgari "abc" 500 qaytarardi -
            // klient uchun bu "serverda nosozlik", aslida so'rov noto'g'ri.
            mockMvc.perform(get("/api/v1/app/admin/users/abc")
                            .header("Authorization", "Bearer "
                                    + token("+998900001003", PlatformRole.ADMIN)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Tokensiz - 401")
        void anonymousIsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/app/admin/content"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------------------- qavat 2

    @Nested
    @DisplayName("Huquq oshirishga urinishlar")
    class PrivilegeEscalation {

        @Test
        @DisplayName("WORKER xodimlar moduliga kira olmaydi")
        void workerCannotReachStaffModule() throws Exception {
            String worker = token("+998900001011", PlatformRole.WORKER,
                    Permission.CONTENT_VIEW);

            mockMvc.perform(get("/api/v1/app/admin/staff").param("size", "200")
                            .header("Authorization", "Bearer " + worker))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/app/admin/audit-logs")
                            .header("Authorization", "Bearer " + worker))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("WORKER o'ziga ruxsat qo'sha olmaydi")
        void workerCannotGrantSelfPermissions() throws Exception {
            String worker = token("+998900001012", PlatformRole.WORKER,
                    Permission.CONTENT_VIEW);
            UUID selfId = selfId(worker);

            mockMvc.perform(put("/api/v1/app/admin/staff/" + selfId + "/permissions")
                            .header("Authorization", "Bearer " + worker)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"CONTENT_DELETE\",\"SETTINGS_EDIT\"]"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN o'zidan yuqori rol yarata olmaydi")
        void adminCannotCreateHigherRole() throws Exception {
            String admin = token("+998900001013", PlatformRole.ADMIN);

            // ⚠️ Telefon TO'G'RI formatda bo'lishi shart (+998 + 9 raqam).
            // Aks holda 422 keladi va test rol tekshiruvini emas,
            // validatsiyani sinagan bo'lardi.
            String[][] cases = {
                    {"SUPER_ADMIN", "+998900001021"},
                    {"HYPER_ADMIN", "+998900001022"},
                    {"ADMIN", "+998900001023"}};

            for (String[] c : cases) {
                mockMvc.perform(post("/api/v1/app/admin/staff")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "name", "Urinish", "phone", c[1],
                                        "password", "Parol123", "role", c[0]))))
                        .andExpect(status().isForbidden());
            }
        }

        @Test
        @DisplayName("ADMIN o'z rolini ko'tara olmaydi")
        void adminCannotPromoteSelf() throws Exception {
            String admin = token("+998900001014", PlatformRole.ADMIN);
            UUID selfId = selfId(admin);

            mockMvc.perform(put("/api/v1/app/admin/staff/" + selfId + "/role")
                            .param("role", "SUPER_ADMIN")
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN o'ziga TENG rolli xodimga tegа olmaydi")
        void adminCannotTouchEqualRole() throws Exception {
            String admin = token("+998900001015", PlatformRole.ADMIN);
            UUID peerId = selfId(token("+998900001016", PlatformRole.ADMIN));

            // "Topilmadi" - teng rolli hisob borligini ham oshkor qilmaymiz.
            mockMvc.perform(post("/api/v1/app/admin/staff/" + peerId + "/block")
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Ruxsatsiz WORKER yozish amallarini bajara olmaydi")
        void viewOnlyWorkerCannotWrite() throws Exception {
            String viewer = token("+998900001017", PlatformRole.WORKER,
                    Permission.CONTENT_VIEW);

            mockMvc.perform(post("/api/v1/app/admin/content")
                            .header("Authorization", "Bearer " + viewer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "contentType", "MOVIE", "structureType", "SINGLE",
                                    "accessPolicy", "FREE", "status", "DRAFT",
                                    "translations", Map.of("UZ", Map.of("title", "Ruxsatsiz"))))))
                    .andExpect(status().isForbidden());
        }
    }

    private UUID selfId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/app/admin/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }
}
