package com.example.backend.Admin;

import com.example.backend.Cms.Enums.StaffStatus;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Xodimlarni boshqarish — ro'yxat, filtrlar va amallar.
 *
 * <h2>Hard delete YO'Q</h2>
 * Buyurtmachi talabi va texnik sabab: audit jurnalidagi yozuvlar
 * {@code actor_id} saqlaydi. Xodim o'chirilsa, o'tmishdagi amallarni kimga
 * bog'lash noma'lum bo'lardi. Shuning uchun faolsizlantirish.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
class StaffManagementTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestStaffFactory staff;

    private String admin() {
        return staff.tokenForRole("+998900002001", PlatformRole.SUPER_ADMIN,
                EnumSet.noneOf(Permission.class));
    }

    private String createWorker(String token, String name, String phone) throws Exception {
        String body = mockMvc.perform(post("/api/v1/app/admin/staff")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "phone", phone,
                                "password", "Parol123", "role", "WORKER"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private JsonNode findRow(String token, String id) throws Exception {
        String body = mockMvc.perform(get("/api/v1/app/admin/staff").param("size", "200")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode row : objectMapper.readTree(body).get("items")) {
            if (id.equals(row.get("id").asText())) {
                return row;
            }
        }
        throw new AssertionError("Xodim ro'yxatda topilmadi: " + id);
    }

    // -------------------------------------------------------------- ro'yxat

    @Nested
    @DisplayName("Ro'yxat")
    class Listing {

        @Test
        @DisplayName("Barcha talab qilingan maydonlar qaytadi")
        void rowHasEveryRequiredField() throws Exception {
            String token = admin();
            String id = createWorker(token, "Maydon sinovi", "+998900002011");

            JsonNode row = findRow(token, id);

            // ТЗ §12 da sanalgan maydonlar.
            assertThat(row.has("id")).isTrue();
            assertThat(row.has("avatarUrl")).isTrue();
            assertThat(row.get("name").asText()).isEqualTo("Maydon sinovi");
            assertThat(row.get("phone").asText()).isEqualTo("+998900002011");
            assertThat(row.has("email")).isTrue();
            assertThat(row.get("role").asText()).isEqualTo("WORKER");
            assertThat(row.get("status").asText()).isEqualTo("ACTIVE");
            assertThat(row.has("createdBy")).isTrue();
            assertThat(row.get("createdAt").isNull()).isFalse();
            // Hali kirmagan.
            assertThat(row.get("lastLoginAt").isNull()).isTrue();
        }

        @Test
        @DisplayName("createdBy yaratuvchini ko'rsatadi")
        void createdByIsRecorded() throws Exception {
            String token = admin();
            String id = createWorker(token, "Kim yaratdi", "+998900002012");

            JsonNode row = findRow(token, id);
            assertThat(row.get("createdBy").isNull())
                    .as("Kim yaratgani yozilishi kerak")
                    .isFalse();
        }

        @Test
        @DisplayName("Rol bo'yicha filtr")
        void filterByRole() throws Exception {
            String token = admin();
            createWorker(token, "Filtr worker", "+998900002013");

            String body = mockMvc.perform(get("/api/v1/app/admin/staff")
                            .param("role", "WORKER")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            for (JsonNode row : objectMapper.readTree(body).get("items")) {
                assertThat(row.get("role").asText()).isEqualTo("WORKER");
            }
        }

        @Test
        @DisplayName("Qidiruv ism va telefon bo'yicha")
        void searchByNameAndPhone() throws Exception {
            String token = admin();
            createWorker(token, "Alisher Navoiy", "+998900002014");

            for (String needle : new String[]{"alisher", "900002014"}) {
                String body = mockMvc.perform(get("/api/v1/app/admin/staff")
                                .param("q", needle)
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();

                assertThat(objectMapper.readTree(body).get("items").size())
                        .as("'%s' bo'yicha qidiruv natija berishi kerak", needle)
                        .isGreaterThan(0);
            }
        }

        @Test
        @DisplayName("Holat bo'yicha filtr")
        void filterByStatus() throws Exception {
            String token = admin();
            String id = createWorker(token, "Faolsiz bo'ladi", "+998900002015");

            mockMvc.perform(post("/api/v1/app/admin/staff/" + id + "/deactivate")
                            .param("reason", "sinov")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            String body = mockMvc.perform(get("/api/v1/app/admin/staff")
                            .param("status", "INACTIVE")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getContentAsString();

            boolean found = false;
            for (JsonNode row : objectMapper.readTree(body).get("items")) {
                assertThat(row.get("status").asText()).isEqualTo("INACTIVE");
                if (id.equals(row.get("id").asText())) {
                    found = true;
                }
            }
            assertThat(found).isTrue();
        }
    }

    // --------------------------------------------------------------- amallar

    @Nested
    @DisplayName("Amallar")
    class Actions {

        @Test
        @DisplayName("Tahrirlash — ism, telefon, email")
        void editUpdatesFields() throws Exception {
            String token = admin();
            String id = createWorker(token, "Eski ism", "+998900002021");

            mockMvc.perform(put("/api/v1/app/admin/staff/" + id)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "name", "Yangi ism",
                                    "phone", "+998900002022",
                                    "email", "yangi@uzcasting.uz"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Yangi ism"))
                    .andExpect(jsonPath("$.email").value("yangi@uzcasting.uz"));
        }

        @Test
        @DisplayName("Boshqa xodimning telefoniga o'zgartirib bo'lmaydi")
        void phoneStaysUnique() throws Exception {
            String token = admin();
            createWorker(token, "Birinchi", "+998900002023");
            String second = createWorker(token, "Ikkinchi", "+998900002024");

            mockMvc.perform(put("/api/v1/app/admin/staff/" + second)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "name", "Ikkinchi", "phone", "+998900002023"))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Parolni tiklash — javobda parol QAYTMAYDI")
        void passwordResetNeverEchoesPassword() throws Exception {
            String token = admin();
            String id = createWorker(token, "Parol tiklash", "+998900002025");

            String body = mockMvc.perform(put("/api/v1/app/admin/staff/" + id + "/password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"YangiParol9\"}"))
                    .andExpect(status().isNoContent())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain("YangiParol9");
        }

        @Test
        @DisplayName("Zaif parol rad etiladi")
        void weakPasswordRejected() throws Exception {
            String token = admin();
            String id = createWorker(token, "Zaif parol", "+998900002026");

            mockMvc.perform(put("/api/v1/app/admin/staff/" + id + "/password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"1234\"}"))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Faolsizlantirish va qaytarish")
        void deactivateThenActivate() throws Exception {
            String token = admin();
            String id = createWorker(token, "Aylanma", "+998900002027");

            mockMvc.perform(post("/api/v1/app/admin/staff/" + id + "/deactivate")
                            .param("reason", "ishdan bo'shadi")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("INACTIVE"))
                    .andExpect(jsonPath("$.statusReason").value("ishdan bo'shadi"));

            mockMvc.perform(post("/api/v1/app/admin/staff/" + id + "/activate")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Bloklash va blokdan chiqarish")
        void blockThenUnblock() throws Exception {
            String token = admin();
            String id = createWorker(token, "Bloklanadi", "+998900002028");

            mockMvc.perform(post("/api/v1/app/admin/staff/" + id + "/block")
                            .param("reason", "tergov")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("BLOCKED"));

            mockMvc.perform(post("/api/v1/app/admin/staff/" + id + "/unblock")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }
    }

    // ------------------------------------------------------------ himoya

    @Nested
    @DisplayName("Himoya")
    class Guards {

        @Test
        @DisplayName("Faolsizlantirilgan xodimning MAVJUD tokeni ishlamaydi")
        void deactivationAppliesToExistingToken() throws Exception {
            String token = admin();
            String phone = "+998900002031";
            String workerToken = staff.tokenForRole(phone, PlatformRole.WORKER,
                    EnumSet.of(Permission.CONTENT_VIEW));

            mockMvc.perform(get("/api/v1/app/admin/content")
                            .header("Authorization", "Bearer " + workerToken))
                    .andExpect(status().isOk());

            String id = findRowByPhone(token, phone);
            mockMvc.perform(post("/api/v1/app/admin/staff/" + id + "/deactivate")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // Token o'zgarmadi — lekin holat har so'rovda bazadan o'qiladi.
            mockMvc.perform(get("/api/v1/app/admin/content")
                            .header("Authorization", "Bearer " + workerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("O'zini faolsizlantira olmaydi")
        void cannotDeactivateSelf() throws Exception {
            String token = admin();
            String selfBody = mockMvc.perform(get("/api/v1/app/admin/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getContentAsString();
            String selfId = objectMapper.readTree(selfBody).get("id").asText();

            mockMvc.perform(post("/api/v1/app/admin/staff/" + selfId + "/deactivate")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Hard delete endpointi UMUMAN yo'q")
        void hardDeleteDoesNotExist() throws Exception {
            String token = admin();
            String id = createWorker(token, "O'chmaydi", "+998900002032");

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .delete("/api/v1/app/admin/staff/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    private String findRowByPhone(String token, String phone) throws Exception {
        String body = mockMvc.perform(get("/api/v1/app/admin/staff").param("size", "200")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode row : objectMapper.readTree(body).get("items")) {
            if (phone.equals(row.get("phone").asText())) {
                return row.get("id").asText();
            }
        }
        throw new AssertionError("Topilmadi: " + phone);
    }
}
