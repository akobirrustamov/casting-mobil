package com.example.backend.Admin;

import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
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
 * ТЗ §35: «User uchun web admin login mavjud bo'lmasin.»
 *
 * <h2>Nima uchun alohida test</h2>
 * Bu talab BITTA tekshiruvga tayanmaydi. Ilova foydalanuvchisi admin
 * panelga uch xil yo'l bilan urinishi mumkin:
 *
 * <ol>
 *   <li>admin login endpointidan parol bilan;</li>
 *   <li>oddiy ilova tokeni bilan admin endpointiga;</li>
 *   <li>oddiy ilova tokeni bilan admin «men kimman» endpointiga.</li>
 * </ol>
 *
 * Uchalasi ham yopiq bo'lishi kerak — biri ochiq qolsa qolgan ikkitasi
 * ma'nosiz. Frontend menyusini yashirish esa umuman xavfsizlik emas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
class UserCannotEnterPanelTest {

    private static final String PHONE = "+998900000601";
    private static final String PASSWORD = "12345678";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestStaffFactory staff;

    @Test
    @DisplayName("1. Admin login: USER paroli to'g'ri bo'lsa ham kirolmaydi")
    void userCannotLogIntoAdminPanel() throws Exception {
        staff.tokenForRole(PHONE, PlatformRole.USER, EnumSet.noneOf(Permission.class));

        mockMvc.perform(post("/api/v1/app/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("phone", PHONE, "password", PASSWORD))))
                // Parol TO'G'RI — ya'ni rad etish aynan rol sababli.
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("2. Ilova tokeni admin endpointiga o'tmaydi")
    void appTokenIsRejectedByAdminEndpoints() throws Exception {
        String userToken = staff.tokenForRole("+998900000602",
                PlatformRole.USER, EnumSet.noneOf(Permission.class));

        mockMvc.perform(get("/api/v1/app/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Ilova tokeni bilan «men kimman» ham yopiq")
    void appTokenCannotReadAdminIdentity() throws Exception {
        String userToken = staff.tokenForRole("+998900000603",
                PlatformRole.USER, EnumSet.noneOf(Permission.class));

        mockMvc.perform(get("/api/v1/app/admin/auth/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Ruxsat berilgan bo'lsa ham USER kira olmaydi")
    void permissionsDoNotOpenThePanelForUser() throws Exception {
        // ⚠️ Ruxsat berish rolni ko'tarmaydi. Aks holda oddiy
        // foydalanuvchiga bitta ruxsat berib qo'yish uni panelga
        // kiritib yuborardi.
        String userToken = staff.tokenForRole("+998900000604",
                PlatformRole.USER, EnumSet.of(Permission.USER_VIEW));

        mockMvc.perform(get("/api/v1/app/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Xodim esa kira oladi — test teskari tomondan ham ishlaydi")
    void staffCanEnter() throws Exception {
        String adminToken = staff.tokenForRole("+998900000605",
                PlatformRole.ADMIN, EnumSet.of(Permission.USER_VIEW));

        // Ijobiy nazorat: yuqoridagi 403'lar «hamma narsa yopiq»
        // bo'lgani uchun emas, aynan rol sababli.
        mockMvc.perform(get("/api/v1/app/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
