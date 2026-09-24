package com.example.backend.Admin;

import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.Services.AuthService.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Majburiy chiqarish (SUPER_ADMIN).
 *
 * <h2>Nimani qo'riqlaydi</h2>
 * «Chiqarildi» degani IKKALASI: qo'ldagi access token darhol ishlamaydi
 * va refresh token yangi token bermaydi. Bittasi qolsa, admin panelda
 * «chiqarildi» deb turadi, odam esa ishlayveradi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestStaffFactory.class)
class SessionAdminTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String ME = "/api/v1/app/me";
    private static final String REFRESH = "/api/v1/app/auth/refresh";

    @Autowired private MockMvc mockMvc;
    @Autowired private TestStaffFactory staff;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private RefreshTokenService refreshTokenService;

    private User appUser() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99891" + (7100000 + SEQ.incrementAndGet()));
        u.setPassword(passwordEncoder.encode("Parol123!"));
        u.setName("Tomoshabin " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    private String superAdmin() {
        return staff.tokenForRole("+998900007001", PlatformRole.SUPER_ADMIN, Set.of());
    }

    private void expectMe(String token, int status) throws Exception {
        mockMvc.perform(get(ME).header("Authorization", "Bearer " + token))
                .andExpect(status().is(status));
    }

    private void expectRefreshRejected(String refreshToken) throws Exception {
        mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bitta foydalanuvchi: access ham, refresh ham darhol yaroqsiz")
    void logoutUserKillsBothTokens() throws Exception {
        User u = appUser();
        String access = jwtService.generateJwtToken(u);
        String refresh = refreshTokenService.issue(u, null);
        expectMe(access, 200);

        mockMvc.perform(post("/api/v1/app/admin/sessions/users/" + u.getId() + "/logout")
                        .header("Authorization", "Bearer " + superAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions").value(1));

        expectMe(access, 401);
        expectRefreshRejected(refresh);
    }

    @Test
    @DisplayName("Ommaviy (ilova): foydalanuvchilar chiqadi, admin o'zi qoladi")
    void logoutAllAppUsersSparesActor() throws Exception {
        User u = appUser();
        String access = jwtService.generateJwtToken(u);
        String admin = superAdmin();

        mockMvc.perform(post("/api/v1/app/admin/sessions/logout-all")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scope\":\"APP_USERS\"}"))
                .andExpect(status().isOk());

        expectMe(access, 401);
        mockMvc.perform(get("/api/v1/app/admin/users").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ommaviy (xodimlar): pastdagi xodim chiqadi, teng roldagi qoladi")
    void logoutStaffOnlyLowerRoles() throws Exception {
        String worker = staff.tokenForRole("+998900007002", PlatformRole.WORKER,
                Set.of(com.example.backend.Enums.Permission.USER_VIEW));
        String otherSuper = staff.tokenForRole("+998900007003", PlatformRole.SUPER_ADMIN, Set.of());

        mockMvc.perform(post("/api/v1/app/admin/sessions/logout-all")
                        .header("Authorization", "Bearer " + superAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scope\":\"STAFF\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/app/admin/users").header("Authorization", "Bearer " + worker))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/app/admin/users").header("Authorization", "Bearer " + otherSuper))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN ommaviy chiqarishni bajara olmaydi")
    void adminIsForbidden() throws Exception {
        String admin = staff.tokenForRole("+998900007004", PlatformRole.ADMIN, Set.of());
        mockMvc.perform(post("/api/v1/app/admin/sessions/logout-all")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scope\":\"ALL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chiqarilgandan keyin qayta kirish ishlaydi")
    void reloginAfterLogoutWorks() throws Exception {
        User u = appUser();
        mockMvc.perform(post("/api/v1/app/admin/sessions/users/" + u.getId() + "/logout")
                        .header("Authorization", "Bearer " + superAdmin()))
                .andExpect(status().isOk());

        // Chegara — keyingi butun soniya (SessionAdminService.cutoffNow).
        Thread.sleep(2100);
        expectMe(jwtService.generateJwtToken(u), 200);
    }
}
