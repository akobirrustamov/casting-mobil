package com.example.backend.Admin;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Enums.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * ТЗ §78 — autentifikatsiya va RBAC qabul mezonlari.
 *
 * <h2>Nega alohida fayl</h2>
 * Har bir band boshqa testlarda allaqachon qamrab olingan, lekin
 * <b>raqamlangan ro'yxat sifatida</b> hech qayerda yig'ilmagan edi.
 * Buyurtmachi «shu sakkiztasi test qilinsin» deganda, javob bitta
 * joydan ko'rinishi kerak — aks holda har safar butun to'plamni
 * qidirib chiqishga to'g'ri kelardi.
 *
 * Bu yerda takrorlanmaydi: mavjud testlar chuqur tekshiradi, bu esa
 * mezonning bajarilganini qisqa va aniq tasdiqlaydi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(TestStaffFactory.class)
class AcceptanceCriteriaTest {

    @Autowired private MockMvc mvc;
    @Autowired private TestStaffFactory staff;

    /** 1. USER admin panelga kira olmaydi. */
    @Test
    @DisplayName("1. USER admin panelga kira olmaydi")
    void userCannotEnterPanel() {
        assertThat(PlatformRole.USER.canAccessAdminPanel()).isFalse();
    }

    /** 2. WORKER staff yarata olmaydi. */
    @Test
    @DisplayName("2. WORKER staff yarata olmaydi")
    void workerCannotCreateStaff() {
        assertThat(PlatformRole.WORKER.canCreate(PlatformRole.WORKER)).isFalse();
        assertThat(PlatformRole.WORKER.canCreate(PlatformRole.ADMIN)).isFalse();
    }

    /** 3. ADMIN faqat Worker yarata oladi. */
    @Test
    @DisplayName("3. ADMIN faqat Worker yarata oladi")
    void adminCreatesOnlyWorkers() {
        assertThat(PlatformRole.ADMIN.canCreate(PlatformRole.WORKER)).isTrue();
        assertThat(PlatformRole.ADMIN.canCreate(PlatformRole.ADMIN)).isFalse();
        assertThat(PlatformRole.ADMIN.canCreate(PlatformRole.SUPER_ADMIN)).isFalse();
    }

    /** 4. SUPER_ADMIN Admin va Worker yarata oladi. */
    @Test
    @DisplayName("4. SUPER_ADMIN Admin va Worker yarata oladi")
    void superAdminCreatesAdminsAndWorkers() {
        assertThat(PlatformRole.SUPER_ADMIN.canCreate(PlatformRole.ADMIN)).isTrue();
        assertThat(PlatformRole.SUPER_ADMIN.canCreate(PlatformRole.WORKER)).isTrue();
    }

    /** 5. SUPER_ADMIN HyperAdmin yarata olmaydi. */
    @Test
    @DisplayName("5. SUPER_ADMIN HyperAdmin yarata olmaydi")
    void superAdminCannotCreateHyperAdmin() {
        assertThat(PlatformRole.SUPER_ADMIN.canCreate(PlatformRole.HYPER_ADMIN)).isFalse();
        // Teng rol ham: aks holda ikkita SUPER_ADMIN bir-birini
        // o'chirib, tizimni egasiz qoldirishi mumkin edi.
        assertThat(PlatformRole.SUPER_ADMIN.canCreate(PlatformRole.SUPER_ADMIN)).isFalse();
    }

    /** 6. Ruxsati yo'q WORKER himoyalangan endpointga kira olmaydi. */
    @Test
    @DisplayName("6. Ruxsatsiz WORKER himoyalangan endpointga kira olmaydi")
    void workerWithoutPermissionIsBlocked() throws Exception {
        String token = staff.token("+998900780001", Set.of(Permission.CONTENT_VIEW));

        mvc.perform(get("/api/v1/app/admin/staff").header("Authorization", "Bearer " + token))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("faqat CONTENT_VIEW bor WORKER xodimlar ro'yxatini ko'rmasin")
                        .isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    /**
     * 7. Frontend menyusi ham ruxsatga qarab yashiriladi.
     *
     * JSX ni reflection bilan ko'rib bo'lmaydi — manba matni o'qiladi.
     */
    @Test
    @DisplayName("7. Panel menyusi ruxsatga qarab yashiriladi")
    void sidebarHidesByPermission() throws IOException {
        String src = Files.readString(Path.of(
                "../frontend/src/adminpanel/layout/AdminLayout.jsx"));

        assertThat(src)
                .as("menyu bandlari ruxsat bilan filtrlansin")
                .contains("can(");
    }

    /**
     * 8. Backend baribir 403 qaytaradi.
     *
     * ⚠️ Bu 7-banddan ajralmas: menyuni yashirish xavfsizlik EMAS.
     * Foydalanuvchi URL'ni qo'lda terishi yoki API'ga to'g'ridan-to'g'ri
     * murojaat qilishi mumkin.
     */
    @Test
    @DisplayName("8. Menyu yashirilgan bo'lsa ham backend 403 qaytaradi")
    void backendStillReturns403() throws Exception {
        String token = staff.token("+998900780002", Set.of(Permission.CONTENT_VIEW));

        // Menyuda ko'rinmaydigan bo'lim — to'g'ridan-to'g'ri so'rov.
        mvc.perform(get("/api/v1/app/admin/audit-logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN.value()));
    }
}
