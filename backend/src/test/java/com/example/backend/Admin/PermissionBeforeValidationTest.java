package com.example.backend.Admin;

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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ruxsat so'rov tanasi tekshirilishidan OLDIN ko'rilishini qo'riqlaydi.
 *
 * <h2>Nima buzilgan edi (B16)</h2>
 * Spring {@code @Valid @RequestBody} ni controller metodi chaqirilishidan
 * oldin tekshiradi. Tekshiruv metod ichida bo'lgani uchun ruxsatsiz xodim
 * noto'g'ri tana yuborsa <b>422</b> olardi, <b>403</b> emas — ya'ni
 * validatsiya qoidalarini bilib olardi.
 *
 * Xavfsizlik teshigi emas edi (hech narsa yarata olmasdi), lekin keraksiz
 * ma'lumot oshkorligi. Endi {@code PermissionInterceptor} {@code preHandle}
 * da tekshiradi.
 *
 * <h2>Nega bu test muhim</h2>
 * Kimdir interceptorni ro'yxatdan chiqarib yuborsa yoki annotatsiyani
 * olib tashlasa, javob jimgina 422 ga qaytadi va buni hech kim sezmasdi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
class PermissionBeforeValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestStaffFactory staff;

    /** Ruxsatsiz xodim + ataylab BO'SH (yaroqsiz) tana. */
    @Test
    @DisplayName("Ruxsatsiz xodim yaroqsiz tana yuborsa ham 403 oladi, 422 emas")
    void permissionIsCheckedBeforeValidation() throws Exception {
        String token = staff.tokenWithoutContentCreate();

        mockMvc.perform(post("/api/v1/app/admin/content")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Ruxsatli xodim yaroqsiz tana yuborsa validatsiya xatosi oladi")
    void validationStillRunsForPermittedStaff() throws Exception {
        String token = staff.tokenWithContentCreate();

        mockMvc.perform(post("/api/v1/app/admin/content")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Ruxsatli xodim to'g'ri tana bilan kontent yaratadi")
    void permittedStaffCanCreate() throws Exception {
        String token = staff.tokenWithContentCreate();

        Map<String, Object> body = Map.of(
                "contentType", "MOVIE",
                "structureType", "SINGLE",
                "accessPolicy", "FREE",
                "status", "DRAFT",
                "translations", Map.of("UZ", Map.of("title", "Interceptor sinovi")));

        mockMvc.perform(post("/api/v1/app/admin/content")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }
}
