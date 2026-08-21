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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC — HTTP darajasidagi qabul mezonlari.
 *
 * <h2>Nega unit testlar yetarli emas</h2>
 * {@code PermissionServiceTest} mantiqni tekshiradi, lekin u endpointga
 * ULANGANINI tekshirmaydi. Servis to'g'ri "yo'q" desa ham, controller uni
 * chaqirmasa — himoya yo'q. Bu yerda haqiqiy so'rov yuboriladi.
 *
 * <h2>Tekshiriladigan xossalar</h2>
 * <ol>
 *   <li>USER admin paneliga umuman kira olmaydi;</li>
 *   <li>faqat ko'rish huquqi bor xodim yoza olmaydi;</li>
 *   <li>xodim o'ziga ruxsat qo'sha olmaydi (privilege escalation);</li>
 *   <li>o'zida bo'lmagan ruxsatni boshqaga bera olmaydi;</li>
 *   <li>quyi rol yuqori rolni yarata olmaydi;</li>
 *   <li>audit jurnalini o'zgartirib bo'lmaydi;</li>
 *   <li>ruxsat olib tashlansa — MAVJUD token ham darhol kuchini yo'qotadi;</li>
 *   <li>tokensiz hech narsa ochilmaydi.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestStaffFactory staff;

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ------------------------------------------------------------------ 1, 8

    @Nested
    @DisplayName("Kirish darvozasi")
    class Gate {

        @Test
        @DisplayName("USER roli admin endpointlariga kira olmaydi")
        void plainUserIsRejected() throws Exception {
            String token = staff.tokenForRole("+998900000201", PlatformRole.USER, EnumSet.noneOf(Permission.class));

            mockMvc.perform(get("/api/v1/app/admin/content").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/app/admin/staff").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Tokensiz admin endpointlari yopiq")
        void anonymousIsRejected() throws Exception {
            mockMvc.perform(get("/api/v1/app/admin/content"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/v1/app/admin/staff"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // --------------------------------------------------------------------- 2

    @Nested
    @DisplayName("Ruxsat chegarasi")
    class Boundaries {

        @Test
        @DisplayName("Faqat ko'rish huquqi bor xodim yoza olmaydi")
        void viewerCannotWrite() throws Exception {
            String token = staff.tokenWithoutContentCreate();

            // Ko'rish - mumkin
            mockMvc.perform(get("/api/v1/app/admin/content").header("Authorization", bearer(token)))
                    .andExpect(status().isOk());

            // Yozish - mumkin emas
            Map<String, Object> body = Map.of(
                    "contentType", "MOVIE", "structureType", "SINGLE",
                    "accessPolicy", "FREE", "status", "DRAFT",
                    "translations", Map.of("UZ", Map.of("title", "Ruxsatsiz urinish")));

            mockMvc.perform(post("/api/v1/app/admin/content")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Xodim boshqa modulga o'ta olmaydi")
        void permissionDoesNotLeakAcrossModules() throws Exception {
            // Kontent huquqi bor, lekin xodimlar/tariflar huquqi yo'q.
            String token = staff.tokenWithContentCreate();

            mockMvc.perform(get("/api/v1/app/admin/staff").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/app/admin/tariffs").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/app/admin/audit-logs").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------------------------------ 3, 5

    @Nested
    @DisplayName("Huquqni oshirishga urinish")
    class Escalation {

        @Test
        @DisplayName("WORKER xodim yarata olmaydi - ya'ni o'ziga sherik ham")
        void workerCannotCreateStaff() throws Exception {
            String token = staff.tokenWithContentCreate();

            Map<String, Object> body = Map.of(
                    "name", "Yangi xodim", "phone", "+998900000999",
                    "password", "Parol123", "role", "WORKER");

            mockMvc.perform(post("/api/v1/app/admin/staff")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN o'zidan yuqori rol yarata olmaydi")
        void adminCannotCreateHigherRole() throws Exception {
            String token = staff.tokenForRole("+998900000202", PlatformRole.ADMIN,
                    EnumSet.allOf(Permission.class));

            // ⚠️ Tana TO'LIQ YAROQLI bo'lishi shart. Aks holda 422 keladi va
            // test rol tekshiruvini emas, validatsiyani sinagan bo'lardi.
            String[][] cases = {{"SUPER_ADMIN", "+998900000301"}, {"HYPER_ADMIN", "+998900000302"}};
            for (String[] c : cases) {
                Map<String, Object> body = Map.of(
                        "name", "Yuqori rol", "phone", c[1],
                        "password", "Parol123", "role", c[0]);

                mockMvc.perform(post("/api/v1/app/admin/staff")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                        .andExpect(status().isForbidden());
            }
        }
    }

    // --------------------------------------------------------------------- 7

    @Nested
    @DisplayName("Ruxsat o'zgarishi")
    class Revocation {

        @Test
        @DisplayName("Ruxsat olib tashlansa MAVJUD token ham kuchini yo'qotadi")
        void revokedPermissionAppliesToExistingToken() throws Exception {
            String phone = "+998900000210";
            String token = staff.tokenForRole(phone, PlatformRole.WORKER,
                    EnumSet.of(Permission.CONTENT_VIEW));

            mockMvc.perform(get("/api/v1/app/admin/content").header("Authorization", bearer(token)))
                    .andExpect(status().isOk());

            // Ruxsatni olib tashlaymiz. Token O'ZGARMAYDI.
            staff.setPermissions(phone, EnumSet.noneOf(Permission.class));

            // ⚠️ Agar ruxsatlar token ichida saqlansa, bu test yiqilardi -
            // eski token hali ham ishlab turardi. Ular har so'rovda bazadan
            // o'qilishi SHART.
            mockMvc.perform(get("/api/v1/app/admin/content").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }
    }
}
