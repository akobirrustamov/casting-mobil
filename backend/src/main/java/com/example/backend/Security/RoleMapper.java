package com.example.backend.Security;

import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Enums.UserRoles;

import java.util.List;

/**
 * DB'dagi {@link UserRoles} ni biznes ierarxiyasidagi {@link PlatformRole} ga o'giradi.
 *
 * Nega kerak: DB enum'ida boshqa loyihadan qolgan qiymatlar bor va nomlar tarixiy
 * (ROLE_GIPERSUPERADMIN). Ierarxiya logikasi shu nomlarga bog'lanib qolmasligi kerak.
 */
public final class RoleMapper {

    private RoleMapper() {
    }

    /**
     * Bitta DB rolini platforma roliga o'giradi.
     *
     * @return mos PlatformRole, yoki {@code null} — agar rol UZCASTING'da ishlatilmasa
     *         (universitetdan qolgan qiymatlar).
     */
    public static PlatformRole toPlatformRole(UserRoles role) {
        if (role == null) {
            return null;
        }
        switch (role) {
            case ROLE_GIPERSUPERADMIN:
                return PlatformRole.HYPER_ADMIN;
            case ROLE_SUPERADMIN:
                return PlatformRole.SUPER_ADMIN;
            case ROLE_ADMIN:
                return PlatformRole.ADMIN;
            case ROLE_WORKER:
                return PlatformRole.WORKER;
            case ROLE_USER:
                return PlatformRole.USER;
            default:
                // ROLE_REKTOR, ROLE_STUDENT, ROLE_TEACHER, ROLE_DEKAN — platformada roli yo'q
                return null;
        }
    }

    /** Platforma rolidan DB roliga teskari o'girish (staff yaratishda kerak). */
    public static UserRoles toUserRole(PlatformRole role) {
        if (role == null) {
            return null;
        }
        switch (role) {
            case HYPER_ADMIN:
                return UserRoles.ROLE_GIPERSUPERADMIN;
            case SUPER_ADMIN:
                return UserRoles.ROLE_SUPERADMIN;
            case ADMIN:
                return UserRoles.ROLE_ADMIN;
            case WORKER:
                return UserRoles.ROLE_WORKER;
            case USER:
            default:
                return UserRoles.ROLE_USER;
        }
    }

    /**
     * Foydalanuvchining eng yuqori platforma roli.
     *
     * Bir userda bir nechta rol bo'lishi mumkin (users_roles many-to-many),
     * shuning uchun ierarxiyada eng yuqorisi tanlanadi.
     *
     * @return eng yuqori rol, yoki {@code null} — rollari umuman tanilmasa.
     */
    public static PlatformRole highestRole(User user) {
        if (user == null) {
            return null;
        }
        List<Role> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        PlatformRole highest = null;
        for (Role role : roles) {
            PlatformRole mapped = toPlatformRole(role.getName());
            if (mapped == null) {
                continue;
            }
            if (highest == null || mapped.getLevel() > highest.getLevel()) {
                highest = mapped;
            }
        }
        return highest;
    }
}
