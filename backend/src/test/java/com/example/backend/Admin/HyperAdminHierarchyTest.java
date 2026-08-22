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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HYPER_ADMIN ierarxiyasi — HTTP darajasida.
 *
 * <h2>Asosiy qaror</h2>
 * <b>HYPER_ADMIN boshqa HYPER_ADMIN yarata OLMAYDI.</b>
 *
 * Sabab {@link PlatformRole#creatableRoles} da batafsil yozilgan, qisqasi:
 * {@code canManage} qat'iy taqqoslash ishlatadi, ya'ni ikkita HYPER_ADMIN
 * bir-birini o'chira olmaydi. Agar teng rol yaratish mumkin bo'lsa, bitta
 * o'g'irlangan hisob hech kim olib tashlay olmaydigan cheksiz HYPER_ADMIN
 * yaratardi — bir martalik buzilish doimiy nazoratga aylanardi.
 *
 * Yagona HYPER_ADMIN yo'qolsa, tiklash serverda (environment + qayta ishga
 * tushirish), veb-interfeys orqali emas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
class HyperAdminHierarchyTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestStaffFactory staff;

    private String hyperAdmin() {
        return staff.tokenForRole("+998900000501", PlatformRole.HYPER_ADMIN,
                EnumSet.noneOf(Permission.class));
    }

    private String createStaffBody(String name, String phone, String role) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name, "phone", phone,
                "password", "Parol123", "role", role));
    }

    // ------------------------------------------------------------- qaror

    @Nested
    @DisplayName("Teng rol yaratish taqiqlangan")
    class NoPeerCreation {

        @Test
        @DisplayName("HYPER_ADMIN boshqa HYPER_ADMIN yarata olmaydi")
        void hyperAdminCannotCreatePeer() throws Exception {
            mockMvc.perform(post("/api/v1/app/admin/staff")
                            .header("Authorization", "Bearer " + hyperAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createStaffBody("Ikkinchi hyper",
                                    "+998900000502", "HYPER_ADMIN")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Enum darajasida ham teng rol yaratib bo'lmaydi")
        void ruleHoldsForEveryRole() {
            // Butun ierarxiyada bir xil qoida: faqat QAT'IY quyi rol.
            for (PlatformRole role : PlatformRole.values()) {
                assertThat(role.canCreate(role))
                        .as("%s o'ziga teng rol yarata olmasligi kerak", role)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("Ikkita teng rol bir-birini boshqara olmaydi")
        void equalRolesCannotManageEachOther() {
            // Aynan shu sabab teng rol yaratishga yo'l qo'yilmaydi:
            // yaratilgan hisobni keyin HECH KIM olib tashlay olmasdi.
            for (PlatformRole role : PlatformRole.values()) {
                assertThat(role.canManage(role)).isFalse();
            }
        }
    }

    // ------------------------------------------------- quyi rollar bilan

    @Nested
    @DisplayName("HYPER_ADMIN huquqlari")
    class Rights {

        @Test
        @DisplayName("SuperAdmin, Admin va Worker yarata oladi")
        void createsLowerRoles() throws Exception {
            String token = hyperAdmin();
            String[][] cases = {
                    {"SUPER_ADMIN", "+998900000511"},
                    {"ADMIN", "+998900000512"},
                    {"WORKER", "+998900000513"}};

            for (String[] c : cases) {
                mockMvc.perform(post("/api/v1/app/admin/staff")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createStaffBody("Yangi " + c[0], c[1], c[0])))
                        .andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("Xodim rolini va ruxsatlarini o'zgartira oladi")
        void managesRolesAndPermissions() throws Exception {
            String token = hyperAdmin();

            String created = mockMvc.perform(post("/api/v1/app/admin/staff")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createStaffBody("Boshqariladigan",
                                    "+998900000521", "WORKER")))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String id = objectMapper.readTree(created).get("id").asText();

            // Ruxsatlarni almashtirish
            mockMvc.perform(put("/api/v1/app/admin/staff/" + id + "/permissions")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"CONTENT_VIEW\",\"MEDIA_VIEW\"]"))
                    .andExpect(status().isOk());

            // Rolni ko'tarish. Bu yo'l ilgari 500 qaytargan edi:
            // o'zgarmas List.of(...) Hibernate to'plamiga berilardi.
            mockMvc.perform(put("/api/v1/app/admin/staff/" + id + "/role")
                            .param("role", "ADMIN")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("BARCHA staff hisoblarini ko'radi — o'zini va teng rollarni ham")
        void seesEveryStaffAccount() throws Exception {
            String token = hyperAdmin();

            String body = mockMvc.perform(get("/api/v1/app/admin/staff").param("size", "200")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            var rows = objectMapper.readTree(body).get("items");
            long hyperCount = 0;
            boolean selfManageable = true;
            for (var row : rows) {
                if ("HYPER_ADMIN".equals(row.get("role").asText())) {
                    hyperCount++;
                    // Teng rol ko'rinadi, LEKIN boshqarib bo'lmaydi.
                    if (row.get("manageable").asBoolean()) {
                        selfManageable = false;
                    }
                }
            }

            // Ilgari HYPER_ADMIN boshqa HYPER_ADMIN hisobini UMUMAN ko'rmasdi -
            // AutoRun yaratgan master hisob amalda backdoor edi.
            assertThat(hyperCount)
                    .as("HYPER_ADMIN kamida o'zini ko'rishi kerak")
                    .isGreaterThanOrEqualTo(1);

            assertThat(selfManageable)
                    .as("Teng rol ko'rinadi, lekin manageable=false bo'lishi kerak")
                    .isTrue();
        }

        @Test
        @DisplayName("Barcha modullarga kiradi")
        void reachesEveryModule() throws Exception {
            String token = hyperAdmin();
            String[] modules = {
                    "/api/v1/app/admin/content", "/api/v1/app/admin/users",
                    "/api/v1/app/admin/staff", "/api/v1/app/admin/tariffs",
                    "/api/v1/app/admin/settings", "/api/v1/app/admin/audit-logs",
                    "/api/v1/app/admin/reports/overview", "/api/v1/app/admin/media",
                    "/api/v1/app/admin/notifications", "/api/v1/app/admin/comments"};

            for (String module : modules) {
                mockMvc.perform(get(module).header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk());
            }
        }
    }

    // ------------------------------------------------ o'zini himoya qilish

    @Nested
    @DisplayName("O'ziga tegib bo'lmaydi")
    class SelfProtection {

        @Test
        @DisplayName("O'z rolini o'zgartira olmaydi")
        void cannotChangeOwnRole() throws Exception {
            String token = hyperAdmin();
            UUID selfId = UUID.fromString(objectMapper
                    .readTree(mockMvc.perform(get("/api/v1/app/admin/auth/me")
                                    .header("Authorization", "Bearer " + token))
                            .andReturn().getResponse().getContentAsString())
                    .get("id").asText());

            mockMvc.perform(put("/api/v1/app/admin/staff/" + selfId + "/role")
                            .param("role", "WORKER")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("O'zini bloklay olmaydi")
        void cannotBlockSelf() throws Exception {
            String token = hyperAdmin();
            UUID selfId = UUID.fromString(objectMapper
                    .readTree(mockMvc.perform(get("/api/v1/app/admin/auth/me")
                                    .header("Authorization", "Bearer " + token))
                            .andReturn().getResponse().getContentAsString())
                    .get("id").asText());

            mockMvc.perform(post("/api/v1/app/admin/staff/" + selfId + "/block")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }
}
