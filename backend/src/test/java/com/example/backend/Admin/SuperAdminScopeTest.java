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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SUPER_ADMIN doirasi.
 *
 * <h2>Nima bo'lishi kerak</h2>
 * Admin va Worker yaratadi, ularning hisoblarini boshqaradi, va barcha
 * kontent/monetizatsiya modullariga kiradi.
 *
 * <h2>Nima BO'LMASLIGI kerak</h2>
 * HyperAdmin yarata olmaydi — va o'ziga teng SuperAdmin ham. Ikkinchisi
 * ro'yxatda ko'rsatilmagan, lekin bir xil sababdan taqiqlanadi: teng rolni
 * keyin boshqarib bo'lmaydi ({@link PlatformRole#canManage} qat'iy).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
class SuperAdminScopeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestStaffFactory staff;

    private String superAdmin() {
        return staff.tokenForRole("+998900000901", PlatformRole.SUPER_ADMIN,
                EnumSet.noneOf(Permission.class));
    }

    private String staffBody(String name, String phone, String role) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name, "phone", phone, "password", "Parol123", "role", role));
    }

    @Nested
    @DisplayName("Yaratish doirasi")
    class Creation {

        @Test
        @DisplayName("Admin va Worker yaratadi")
        void createsAdminAndWorker() throws Exception {
            String token = superAdmin();
            String[][] cases = {{"ADMIN", "+998900000911"}, {"WORKER", "+998900000912"}};

            for (String[] c : cases) {
                mockMvc.perform(post("/api/v1/app/admin/staff")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(staffBody("SA " + c[0], c[1], c[0])))
                        .andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("HyperAdmin yarata OLMAYDI")
        void cannotCreateHyperAdmin() throws Exception {
            mockMvc.perform(post("/api/v1/app/admin/staff")
                            .header("Authorization", "Bearer " + superAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(staffBody("Yuqori", "+998900000913", "HYPER_ADMIN")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("O'ziga teng SuperAdmin ham yarata olmaydi")
        void cannotCreatePeer() throws Exception {
            mockMvc.perform(post("/api/v1/app/admin/staff")
                            .header("Authorization", "Bearer " + superAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(staffBody("Teng", "+998900000914", "SUPER_ADMIN")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Modullar")
    class Modules {

        @Test
        @DisplayName("Barcha kerakli modullarga kiradi")
        void reachesRequiredModules() throws Exception {
            String token = superAdmin();
            String[] modules = {
                    "/api/v1/app/admin/content", "/api/v1/app/admin/categories",
                    "/api/v1/app/admin/genres", "/api/v1/app/admin/creators",
                    "/api/v1/app/admin/advertisements", "/api/v1/app/admin/premieres",
                    "/api/v1/app/admin/notifications", "/api/v1/app/admin/comments",
                    "/api/v1/app/admin/users", "/api/v1/app/admin/tariffs",
                    "/api/v1/app/admin/donations/top",
                    "/api/v1/app/admin/reports/overview"};

            for (String module : modules) {
                mockMvc.perform(get(module).header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk());
            }
        }
    }

    @Nested
    @DisplayName("Admin/Worker hisoblarini boshqarish")
    class StaffManagement {

        private String createWorker(String token, String phone) throws Exception {
            String body = mockMvc.perform(post("/api/v1/app/admin/staff")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(staffBody("Boshqariladigan", phone, "WORKER")))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return objectMapper.readTree(body).get("id").asText();
        }

        @Test
        @DisplayName("Mavjud ruxsatni QAYTA berish 500 bermaydi")
        void reassigningExistingPermissionWorks() throws Exception {
            String token = superAdmin();
            String id = createWorker(token, "+998900000921");

            // Birinchi marta
            mockMvc.perform(put("/api/v1/app/admin/staff/" + id + "/permissions")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"CONTENT_VIEW\",\"MEDIA_VIEW\"]"))
                    .andExpect(status().isOk());

            // ⚠️ Aynan shu yer 500 qaytarardi: ruxsatlar oldin butunlay
            // o'chirilib, keyin qaytadan yozilardi va Hibernate INSERT'ni
            // DELETE'dan oldin yuborib UNIQUE cheklovni buzardi.
            mockMvc.perform(put("/api/v1/app/admin/staff/" + id + "/permissions")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"CONTENT_VIEW\",\"CONTENT_EDIT\"]"))
                    .andExpect(status().isOk());

            String after = mockMvc.perform(get("/api/v1/app/admin/staff").param("size", "200")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getContentAsString();

            for (var row : objectMapper.readTree(after).get("items")) {
                if (id.equals(row.get("id").asText())) {
                    var perms = objectMapper.convertValue(row.get("permissions"),
                            String[].class);
                    assertThat(perms).containsExactlyInAnyOrder("CONTENT_VIEW", "CONTENT_EDIT");
                }
            }
        }

        @Test
        @DisplayName("Bloklaydi va blokdan chiqaradi")
        void blocksAndUnblocks() throws Exception {
            String token = superAdmin();
            String id = createWorker(token, "+998900000922");

            mockMvc.perform(post("/api/v1/app/admin/staff/" + id + "/block")
                            .param("reason", "sinov")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/app/admin/staff/" + id + "/unblock")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HYPER_ADMIN hisobi ro'yxatda KO'RINMAYDI")
        void hyperAdminIsNotVisible() throws Exception {
            staff.tokenForRole("+998900000931", PlatformRole.HYPER_ADMIN,
                    EnumSet.noneOf(Permission.class));

            String body = mockMvc.perform(get("/api/v1/app/admin/staff").param("size", "200")
                            .header("Authorization", "Bearer " + superAdmin()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            for (var row : objectMapper.readTree(body).get("items")) {
                assertThat(row.get("role").asText())
                        .as("SUPER_ADMIN faqat o'zidan quyi rollarni ko'rishi kerak")
                        .isNotEqualTo("HYPER_ADMIN");
            }
        }
    }
}
