package com.example.backend.Services;

import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Entity.UserPermission;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.UserPermissionRepo;
import com.example.backend.Services.PermissionService.PermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Ruxsat mantiqining testi (ТЗ §78, 6-punkt).
 *
 * Ikki muhim qoida shu yerda qo'riqlanadi:
 *   1. ADMIN va undan yuqori rollar ruxsatlar jadvaliga qaramaydi;
 *   2. hech kim o'zida bo'lmagan ruxsatni boshqaga bera olmaydi.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionServiceTest {

    @Mock
    private UserPermissionRepo userPermissionRepo;

    @InjectMocks
    private PermissionServiceImpl service;

    private User hyperAdmin;
    private User admin;
    private User worker;
    private User plainUser;
    private User noRole;

    @BeforeEach
    void setUp() {
        hyperAdmin = userWith(UserRoles.ROLE_GIPERSUPERADMIN);
        admin = userWith(UserRoles.ROLE_ADMIN);
        worker = userWith(UserRoles.ROLE_WORKER);
        plainUser = userWith(UserRoles.ROLE_USER);
        noRole = User.builder().id(UUID.randomUUID()).roles(List.of()).build();
    }

    private User userWith(UserRoles role) {
        return User.builder()
                .id(UUID.randomUUID())
                .roles(List.of(new Role(1, role)))
                .build();
    }

    @Nested
    @DisplayName("hasPermission")
    class HasPermission {

        @Test
        @DisplayName("ADMIN va yuqorisi jadvalga qaramaydi - doim ruxsat")
        void adminBypassesPermissionTable() {
            assertThat(service.hasPermission(admin, Permission.CONTENT_DELETE)).isTrue();
            assertThat(service.hasPermission(hyperAdmin, Permission.USER_BLOCK)).isTrue();
            // Jadvalga umuman murojaat qilinmasligi kerak
            verify(userPermissionRepo, never()).existsByUserIdAndPermission(any(), any());
        }

        @Test
        @DisplayName("WORKER: faqat unga berilgan ruxsat")
        void workerUsesPermissionTable() {
            when(userPermissionRepo.existsByUserIdAndPermission(worker.getId(), Permission.CONTENT_VIEW))
                    .thenReturn(true);
            when(userPermissionRepo.existsByUserIdAndPermission(worker.getId(), Permission.CONTENT_DELETE))
                    .thenReturn(false);

            assertThat(service.hasPermission(worker, Permission.CONTENT_VIEW)).isTrue();
            assertThat(service.hasPermission(worker, Permission.CONTENT_DELETE)).isFalse();
        }

        @Test
        @DisplayName("USER hech qanday ruxsatga ega emas")
        void userHasNoPermissions() {
            for (Permission p : Permission.values()) {
                assertThat(service.hasPermission(plainUser, p))
                        .as("USER da %s bo'lmasligi kerak", p)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("null va roli tanilmagan foydalanuvchi - ruxsat yo'q")
        void nullAndUnknownAreDenied() {
            assertThat(service.hasPermission(null, Permission.CONTENT_VIEW)).isFalse();
            assertThat(service.hasPermission(admin, null)).isFalse();
            assertThat(service.hasPermission(noRole, Permission.CONTENT_VIEW)).isFalse();
        }
    }

    @Nested
    @DisplayName("replacePermissions")
    class ReplacePermissions {

        /**
         * ⚠️ Bu testlar NATIJAGA qaraydi, implementatsiyaga emas.
         *
         * Ilgari ular {@code deleteAllByUserId} chaqirilganini tekshirardi.
         * Shu sababli ular «hammasini o'chirib qayta yozish» usulini
         * qotirib qo'ygan edi — va aynan o'sha usul mavjud ruxsat qayta
         * berilganda UNIQUE cheklovni buzib 500 qaytarardi.
         *
         * Endi muhimi: oxirida qaysi ruxsatlar qoladi.
         */
        @Test
        @DisplayName("ADMIN worker'ga istalgan ruxsat bera oladi")
        void adminCanGrantAnything() {
            UUID target = UUID.randomUUID();
            when(userPermissionRepo.findAllByUserId(target)).thenReturn(List.of());

            service.replacePermissions(admin, target,
                    Set.of(Permission.CONTENT_CREATE, Permission.COMMENT_MODERATE));

            verify(userPermissionRepo).saveAll(argThat((Iterable<UserPermission> rows) -> {
                int n = 0;
                for (UserPermission ignored : rows) n++;
                return n == 2;
            }));
        }

        @Test
        @DisplayName("Mavjud ruxsat qayta berilsa - u qayta YOZILMAYDI")
        void alreadyGrantedPermissionIsNotReinserted() {
            UUID target = UUID.randomUUID();
            when(userPermissionRepo.findAllByUserId(target)).thenReturn(List.of(
                    UserPermission.builder().userId(target)
                            .permission(Permission.CONTENT_VIEW).build()));

            // CONTENT_VIEW allaqachon bor, CONTENT_EDIT yangi.
            service.replacePermissions(admin, target,
                    Set.of(Permission.CONTENT_VIEW, Permission.CONTENT_EDIT));

            // Faqat YANGISI yoziladi - aks holda UNIQUE(user_id, permission) buziladi.
            verify(userPermissionRepo).saveAll(argThat((Iterable<UserPermission> rows) -> {
                int n = 0;
                Permission only = null;
                for (UserPermission row : rows) {
                    n++;
                    only = row.getPermission();
                }
                return n == 1 && only == Permission.CONTENT_EDIT;
            }));
        }

        @Test
        @DisplayName("O'zida bo'lmagan ruxsatni bera OLMAYDI")
        void cannotGrantWhatYouDoNotHave() {
            when(userPermissionRepo.existsByUserIdAndPermission(eq(worker.getId()), any()))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.replacePermissions(
                    worker, UUID.randomUUID(), Set.of(Permission.CONTENT_DELETE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CONTENT_DELETE");

            // Hech narsa o'chirilmasligi ham kerak - amal butunlay bekor bo'ladi
            verify(userPermissionRepo, never()).deleteAll(any());
            verify(userPermissionRepo, never()).saveAll(any());
        }

        @Test
        @DisplayName("Bo'sh to'plam - eskilari o'chiriladi, yangisi qo'shilmaydi")
        void emptySetClearsPermissions() {
            UUID target = UUID.randomUUID();
            when(userPermissionRepo.findAllByUserId(target)).thenReturn(List.of(
                    UserPermission.builder().userId(target)
                            .permission(Permission.CONTENT_VIEW).build(),
                    UserPermission.builder().userId(target)
                            .permission(Permission.MEDIA_VIEW).build()));

            service.replacePermissions(admin, target, Set.of());

            // Ikkalasi ham olib tashlanadi.
            verify(userPermissionRepo).deleteAll(argThat((Iterable<UserPermission> rows) -> {
                int n = 0;
                for (UserPermission ignored : rows) n++;
                return n == 2;
            }));
            verify(userPermissionRepo, never()).saveAll(any());
        }

        @Test
        @DisplayName("targetUserId null bo'lsa - xato")
        void nullTargetRejected() {
            assertThatThrownBy(() -> service.replacePermissions(admin, null, Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Rol va panel")
    class RolesAndPanel {

        @Test
        @DisplayName("roleOf eng yuqori rolni beradi")
        void roleOfReturnsHighest() {
            User multi = User.builder()
                    .id(UUID.randomUUID())
                    .roles(List.of(new Role(1, UserRoles.ROLE_WORKER),
                                   new Role(2, UserRoles.ROLE_ADMIN)))
                    .build();
            assertThat(service.roleOf(multi)).isEqualTo(PlatformRole.ADMIN);
        }

        @Test
        @DisplayName("Universitetdan qolgan rollar tanilmaydi")
        void legacyRolesAreIgnored() {
            User dean = userWith(UserRoles.ROLE_DEKAN);
            assertThat(service.roleOf(dean)).isNull();
            assertThat(service.canAccessAdminPanel(dean)).isFalse();
        }

        @Test
        @DisplayName("USER admin panelga kira olmaydi, xodimlar kiradi")
        void panelAccess() {
            assertThat(service.canAccessAdminPanel(plainUser)).isFalse();
            assertThat(service.canAccessAdminPanel(worker)).isTrue();
            assertThat(service.canAccessAdminPanel(admin)).isTrue();
        }

        @Test
        @DisplayName("canManageUser: teng rolni boshqarib bo'lmaydi")
        void cannotManageEqual() {
            User otherAdmin = userWith(UserRoles.ROLE_ADMIN);
            assertThat(service.canManageUser(admin, otherAdmin)).isFalse();
            assertThat(service.canManageUser(admin, worker)).isTrue();
            assertThat(service.canManageUser(worker, admin)).isFalse();
        }
    }
}
