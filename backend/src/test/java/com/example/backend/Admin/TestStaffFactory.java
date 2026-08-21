package com.example.backend.Admin;

import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Entity.UserPermission;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserPermissionRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Testlar uchun turli ruxsatga ega xodimlar va ular uchun token.
 *
 * Har bir test o'zi foydalanuvchi yaratib o'tirmasin — ruxsat farqi
 * kerak bo'ladigan joylar ko'p.
 */
@TestComponent
@RequiredArgsConstructor
public class TestStaffFactory {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final UserPermissionRepo userPermissionRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserAccountRepo userAccountRepo;

    public String tokenWithContentCreate() {
        return token("+998900000101", EnumSet.of(
                Permission.CONTENT_VIEW, Permission.CONTENT_CREATE, Permission.CONTENT_EDIT));
    }

    public String tokenWithoutContentCreate() {
        return token("+998900000102", EnumSet.of(Permission.CONTENT_VIEW));
    }

    /** Aniq rol bilan xodim (yoki oddiy foydalanuvchi) yaratadi. */
    @Transactional
    public String tokenForRole(String phone, PlatformRole platformRole,
                               Set<Permission> permissions) {
        UserRoles legacyRole = switch (platformRole) {
            case HYPER_ADMIN -> UserRoles.ROLE_GIPERSUPERADMIN;
            case SUPER_ADMIN -> UserRoles.ROLE_SUPERADMIN;
            case ADMIN -> UserRoles.ROLE_ADMIN;
            case WORKER -> UserRoles.ROLE_WORKER;
            case USER -> UserRoles.ROLE_USER;
        };
        User user = ensureUser(phone, legacyRole);
        replacePermissions(user, permissions);
        return jwtService.generateJwtToken(user);
    }

    /** Ruxsatlarni almashtiradi - token qayta berilmaydi. */
    @Transactional
    public void setPermissions(String phone, Set<Permission> permissions) {
        User user = userRepo.findByPhone(phone).orElseThrow();
        replacePermissions(user, permissions);
    }

    private void replacePermissions(User user, Set<Permission> permissions) {
        userPermissionRepo.deleteAll(userPermissionRepo.findAllByUserId(user.getId()));
        permissions.forEach(p -> userPermissionRepo.save(
                UserPermission.builder().userId(user.getId()).permission(p).build()));
    }

    private User ensureUser(String phone, UserRoles legacyRole) {
        return userRepo.findByPhone(phone).orElseGet(() -> {
            Role role = roleRepo.findByName(legacyRole);
            if (role == null) {
                int nextId = roleRepo.findAll().stream()
                        .mapToInt(Role::getId).max().orElse(0) + 1;
                role = roleRepo.save(new Role(nextId, legacyRole));
            }
            return userRepo.save(User.builder()
                    .phone(phone)
                    .name("Test " + phone)
                    .password(passwordEncoder.encode("12345678"))
                    .roles(List.of(role))
                    .build());
        });
    }

    @Transactional
    public String token(String phone, Set<Permission> permissions) {
        User user = userRepo.findByPhone(phone).orElseGet(() -> {
            // findByName null qaytaradi (Optional emas) - mavjud kod shunday.
            Role role = roleRepo.findByName(UserRoles.ROLE_WORKER);
            if (role == null) {
                int nextId = roleRepo.findAll().stream()
                        .mapToInt(Role::getId).max().orElse(0) + 1;
                role = roleRepo.save(new Role(nextId, UserRoles.ROLE_WORKER));
            }
            return userRepo.save(User.builder()
                    .phone(phone)
                    .name("Test xodim " + phone)
                    .password(passwordEncoder.encode("12345678"))
                    .roles(List.of(role))
                    .build());
        });

        if (userPermissionRepo.findAllByUserId(user.getId()).isEmpty()) {
            permissions.forEach(p -> userPermissionRepo.save(
                    UserPermission.builder().userId(user.getId()).permission(p).build()));
        }

        return jwtService.generateJwtToken(user);
    }
}
