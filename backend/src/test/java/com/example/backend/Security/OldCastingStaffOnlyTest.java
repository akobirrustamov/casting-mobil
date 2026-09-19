package com.example.backend.Security;

import com.example.backend.Admin.TestStaffFactory;
import com.example.backend.Entity.Attachment;
import com.example.backend.Entity.CastingUser;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.AttachmentRepo;
import com.example.backend.Repository.CastingUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Eski casting admin amallari — FAQAT xodim.
 *
 * <h2>Nima uchun</h2>
 * Bu endpointlar ilgari faqat «token bormi» ni tekshirardi. Mobil ilova
 * foydalanuvchisi ({@code ROLE_USER}) ham token olgach, u o'z anketasini
 * o'zi qabul qilishi, narx qo'yishi yoki barcha nomzodlarning telefon
 * raqamlarini o'qishi mumkin bo'lib qoldi.
 *
 * Tokensiz holat {@code SecurityRulesTest} da; bu yerda — TOKEN BOR, lekin
 * rol noto'g'ri.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
class OldCastingStaffOnlyTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TestStaffFactory staff;
    @Autowired private CastingUserRepo castingUserRepo;
    @Autowired private AttachmentRepo attachmentRepo;

    private String userToken;
    private String adminToken;
    private String workerToken;

    @BeforeEach
    void tokens() {
        userToken = "Bearer " + staff.tokenForRole("+998900040401", PlatformRole.USER,
                EnumSet.noneOf(Permission.class));
        adminToken = "Bearer " + staff.tokenForRole("+998900040402", PlatformRole.ADMIN,
                EnumSet.noneOf(Permission.class));
        workerToken = "Bearer " + staff.tokenForRole("+998900040403", PlatformRole.WORKER,
                EnumSet.noneOf(Permission.class));
    }

    private CastingUser application() {
        CastingUser c = new CastingUser();
        c.setTelegramId("770040001");
        c.setCastingType("model");
        c.setName("Himoyalangan anketa");
        c.setStatus(0);
        c.setSecondChan(0);
        c.setIsWebShow(false);
        c.setCreatedAt(LocalDateTime.now());
        return castingUserRepo.save(c);
    }

    private void assertDenied(RequestBuilder request, String what) throws Exception {
        MvcResult r = mockMvc.perform(request).andReturn();
        assertThat(r.getResponse().getStatus())
                .as("%s ROLE_USER tokeni bilan 403 bo'lishi kerak", what)
                .isEqualTo(403);
        assertThat(r.getResponse().getContentAsString())
                .as("%s ni xavfsizlik qatlami to'sishi kerak", what)
                .contains("\"code\":\"ACCESS_DENIED\"");
    }

    private void assertAllowed(RequestBuilder request, String what) throws Exception {
        int status = mockMvc.perform(request).andReturn().getResponse().getStatus();
        assertThat(status)
                .as("%s xodim uchun ochiq bo'lishi kerak, lekin %d qaytdi", what, status)
                .isNotIn(401, 403);
    }

    @Test
    @DisplayName("⚠️ ROLE_USER o'z anketasini qabul qila olmaydi — 403, holat o'zgarmaydi")
    void userCannotApprove() throws Exception {
        CastingUser c = application();

        assertDenied(put("/api/v1/casting-user/status/" + c.getId() + "/1/100")
                .header("Authorization", userToken), "PUT /status");
        // Topshiriqdagi aniq yo'l — mavjud bo'lmagan id bilan ham 403 (404 emas).
        assertDenied(put("/api/v1/casting-user/status/1/1/100")
                .header("Authorization", userToken), "PUT /status/1/1/100");

        assertThat(castingUserRepo.findById(c.getId()).orElseThrow().getStatus()).isZero();
    }

    @Test
    @DisplayName("ROLE_USER boshqa admin amallariga ham kira olmaydi")
    void userDeniedOnEveryStaffEndpoint() throws Exception {
        CastingUser c = application();

        assertDenied(get("/api/v1/casting-user").header("Authorization", userToken),
                "GET /casting-user (to'liq ro'yxat)");
        assertDenied(get("/api/v1/casting-user/payed/" + c.getId())
                .header("Authorization", userToken), "GET /payed");
        assertDenied(put("/api/v1/casting-user/price/" + c.getId() + "/100")
                .header("Authorization", userToken), "PUT /price");
        assertDenied(put("/api/v1/casting-user/web-show/" + c.getId())
                .header("Authorization", userToken), "PUT /web-show");
        assertDenied(delete("/api/v1/casting-user/" + c.getId())
                .header("Authorization", userToken), "DELETE /casting-user");
        assertDenied(put("/api/v1/file/" + UUID.randomUUID())
                .header("Authorization", userToken), "PUT /file");
        // USER tokeni bilan admin parolini o'zgartirib hisobni egallab bo'lmasin.
        assertDenied(put("/api/v1/auth/password/" + UUID.randomUUID())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"password\":\"hacked\"}")
                .header("Authorization", userToken), "PUT /auth/password");

        CastingUser after = castingUserRepo.findById(c.getId()).orElseThrow();
        assertThat(after.getSecondChan()).isZero();
        assertThat(after.getIsWebShow()).isFalse();
        assertThat(after.getPrice()).isNull();
    }

    @Test
    @DisplayName("Eski admin sayti (ROLE_ADMIN) va xodim (ROLE_WORKER) ishlashda davom etadi")
    void staffStillAllowed() throws Exception {
        CastingUser c = application();
        UUID photoId = UUID.randomUUID();
        attachmentRepo.save(new Attachment(photoId, "/casting", photoId + "_a.jpg", false));

        for (String token : new String[]{adminToken, workerToken}) {
            assertAllowed(get("/api/v1/casting-user").header("Authorization", token),
                    "GET /casting-user");
            assertAllowed(put("/api/v1/casting-user/price/" + c.getId() + "/100")
                    .header("Authorization", token), "PUT /price");
            assertAllowed(put("/api/v1/casting-user/web-show/" + c.getId())
                    .header("Authorization", token), "PUT /web-show");
            assertAllowed(get("/api/v1/casting-user/payed/" + c.getId())
                    .header("Authorization", token), "GET /payed");
            assertAllowed(put("/api/v1/file/" + photoId)
                    .header("Authorization", token), "PUT /file");
        }

        assertAllowed(put("/api/v1/casting-user/status/" + c.getId() + "/1/100")
                .header("Authorization", adminToken), "PUT /status");
        assertThat(castingUserRepo.findById(c.getId()).orElseThrow().getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ochiq casting yo'llari ROLE_USER tokeni bilan ham ochiq qoladi")
    void publicCastingRoutesStayOpen() throws Exception {
        CastingUser c = application();

        assertAllowed(get("/api/v1/casting-user/web").header("Authorization", userToken),
                "GET /casting-user/web");
        assertAllowed(get("/api/v1/casting-user/appeal/" + c.getId())
                .header("Authorization", userToken), "GET /appeal");
        assertAllowed(get("/api/v1/casting-user/my/770040001")
                .header("Authorization", userToken), "GET /my");
    }
}
