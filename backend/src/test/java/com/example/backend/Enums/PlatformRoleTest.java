package com.example.backend.Enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rol ierarxiyasining regressiya testi.
 *
 * Bu ТЗ §78 dagi qabul mezonlarining 1–5 punktini qamrab oladi. Privilege
 * escalation shu yerda ushlanadi: agar kimdir {@code creatableRoles()} ni
 * kengaytirib qo'ysa, test darhol yiqiladi.
 */
class PlatformRoleTest {

    @Nested
    @DisplayName("Kim kimni yarata oladi")
    class Creation {

        @Test
        @DisplayName("HYPER_ADMIN: SuperAdmin, Admin, Worker yaratadi")
        void hyperAdminCreatesLowerRoles() {
            assertThat(PlatformRole.HYPER_ADMIN.canCreate(PlatformRole.SUPER_ADMIN)).isTrue();
            assertThat(PlatformRole.HYPER_ADMIN.canCreate(PlatformRole.ADMIN)).isTrue();
            assertThat(PlatformRole.HYPER_ADMIN.canCreate(PlatformRole.WORKER)).isTrue();
        }

        @Test
        @DisplayName("HYPER_ADMIN o'ziga teng rol yarata OLMAYDI")
        void hyperAdminCannotCloneItself() {
            // Ataylab: yagona hyper-admin hisobi environment orqali beriladi.
            // Aks holda bitta buzilgan hisob cheksiz ko'payib ketardi.
            assertThat(PlatformRole.HYPER_ADMIN.canCreate(PlatformRole.HYPER_ADMIN)).isFalse();
        }

        @Test
        @DisplayName("SUPER_ADMIN: Admin va Worker yaratadi, HyperAdmin YO'Q")
        void superAdminCannotCreateHyperAdmin() {
            assertThat(PlatformRole.SUPER_ADMIN.canCreate(PlatformRole.ADMIN)).isTrue();
            assertThat(PlatformRole.SUPER_ADMIN.canCreate(PlatformRole.WORKER)).isTrue();
            assertThat(PlatformRole.SUPER_ADMIN.canCreate(PlatformRole.HYPER_ADMIN)).isFalse();
            assertThat(PlatformRole.SUPER_ADMIN.canCreate(PlatformRole.SUPER_ADMIN)).isFalse();
        }

        @Test
        @DisplayName("ADMIN: faqat Worker yaratadi")
        void adminCreatesOnlyWorker() {
            assertThat(PlatformRole.ADMIN.canCreate(PlatformRole.WORKER)).isTrue();
            assertThat(PlatformRole.ADMIN.canCreate(PlatformRole.ADMIN)).isFalse();
            assertThat(PlatformRole.ADMIN.canCreate(PlatformRole.SUPER_ADMIN)).isFalse();
            assertThat(PlatformRole.ADMIN.canCreate(PlatformRole.HYPER_ADMIN)).isFalse();
        }

        @Test
        @DisplayName("WORKER hech kimni yarata olmaydi")
        void workerCreatesNobody() {
            assertThat(PlatformRole.WORKER.creatableRoles()).isEmpty();
            for (PlatformRole role : PlatformRole.values()) {
                assertThat(PlatformRole.WORKER.canCreate(role)).isFalse();
            }
        }

        @Test
        @DisplayName("USER hech kimni yarata olmaydi")
        void userCreatesNobody() {
            assertThat(PlatformRole.USER.creatableRoles()).isEmpty();
        }

        @Test
        @DisplayName("null xavfsiz - istisno tashlamaydi")
        void nullIsSafe() {
            assertThat(PlatformRole.HYPER_ADMIN.canCreate(null)).isFalse();
            assertThat(PlatformRole.HYPER_ADMIN.canManage(null)).isFalse();
            assertThat(PlatformRole.HYPER_ADMIN.isAtLeast(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Kim kimni boshqara oladi")
    class Management {

        @Test
        @DisplayName("O'ziga teng rolni boshqarib bo'lmaydi")
        void cannotManageEqualRole() {
            assertThat(PlatformRole.ADMIN.canManage(PlatformRole.ADMIN)).isFalse();
            assertThat(PlatformRole.SUPER_ADMIN.canManage(PlatformRole.SUPER_ADMIN)).isFalse();
            assertThat(PlatformRole.HYPER_ADMIN.canManage(PlatformRole.HYPER_ADMIN)).isFalse();
        }

        @Test
        @DisplayName("Yuqori rolni boshqarib bo'lmaydi")
        void cannotManageHigherRole() {
            assertThat(PlatformRole.ADMIN.canManage(PlatformRole.SUPER_ADMIN)).isFalse();
            assertThat(PlatformRole.WORKER.canManage(PlatformRole.ADMIN)).isFalse();
        }

        @Test
        @DisplayName("Quyi rolni boshqarish mumkin")
        void canManageLowerRole() {
            assertThat(PlatformRole.HYPER_ADMIN.canManage(PlatformRole.SUPER_ADMIN)).isTrue();
            assertThat(PlatformRole.SUPER_ADMIN.canManage(PlatformRole.WORKER)).isTrue();
            assertThat(PlatformRole.ADMIN.canManage(PlatformRole.WORKER)).isTrue();
        }
    }

    @Nested
    @DisplayName("Admin panelga kirish")
    class PanelAccess {

        @Test
        @DisplayName("USER admin panelga kira OLMAYDI")
        void userCannotAccessPanel() {
            assertThat(PlatformRole.USER.canAccessAdminPanel()).isFalse();
        }

        @Test
        @DisplayName("Qolgan barcha rollar kira oladi")
        void staffCanAccessPanel() {
            assertThat(PlatformRole.HYPER_ADMIN.canAccessAdminPanel()).isTrue();
            assertThat(PlatformRole.SUPER_ADMIN.canAccessAdminPanel()).isTrue();
            assertThat(PlatformRole.ADMIN.canAccessAdminPanel()).isTrue();
            assertThat(PlatformRole.WORKER.canAccessAdminPanel()).isTrue();
        }
    }

    @Nested
    @DisplayName("Ierarxiya darajalari")
    class Levels {

        @Test
        @DisplayName("Tartib: HYPER > SUPER > ADMIN > WORKER > USER")
        void levelsAreStrictlyDescending() {
            assertThat(PlatformRole.HYPER_ADMIN.getLevel())
                    .isGreaterThan(PlatformRole.SUPER_ADMIN.getLevel());
            assertThat(PlatformRole.SUPER_ADMIN.getLevel())
                    .isGreaterThan(PlatformRole.ADMIN.getLevel());
            assertThat(PlatformRole.ADMIN.getLevel())
                    .isGreaterThan(PlatformRole.WORKER.getLevel());
            assertThat(PlatformRole.WORKER.getLevel())
                    .isGreaterThan(PlatformRole.USER.getLevel());
        }

        @Test
        @DisplayName("isAtLeast o'ziga nisbatan true")
        void isAtLeastIsInclusive() {
            assertThat(PlatformRole.ADMIN.isAtLeast(PlatformRole.ADMIN)).isTrue();
            assertThat(PlatformRole.ADMIN.isAtLeast(PlatformRole.WORKER)).isTrue();
            assertThat(PlatformRole.WORKER.isAtLeast(PlatformRole.ADMIN)).isFalse();
        }
    }
}
