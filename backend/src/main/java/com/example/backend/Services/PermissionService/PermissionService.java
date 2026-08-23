package com.example.backend.Services.PermissionService;

import com.example.backend.Entity.User;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;

import java.util.Set;
import java.util.UUID;

/**
 * Ruxsatlarni tekshirish va boshqarishning yagona nuqtasi.
 *
 * Authorization logikasi bu yerdan tashqariga sochilmasligi kerak (§103):
 * controller'lar ham, servis'lar ham shu interfeys orqali so'raydi.
 */
public interface PermissionService {

    /** Foydalanuvchining eng yuqori platforma roli. Rol tanilmasa — {@code null}. */
    PlatformRole roleOf(User user);

    /** Admin panelga umuman kira oladimi (USER — yo'q). */
    boolean canAccessAdminPanel(User user);

    /**
     * Ruxsat bormi.
     *
     * ADMIN va undan yuqori rollar uchun doim {@code true} — ularga fine-grained
     * ruxsat qo'llanmaydi. WORKER uchun user_permission jadvalidan tekshiriladi.
     * USER uchun doim {@code false}.
     */
    boolean hasPermission(User user, Permission permission);

    /** Foydalanuvchining aniq ruxsatlari (WORKER uchun ma'noli). */
    Set<Permission> permissionsOf(UUID userId);

    /**
     * Worker'ga ruxsatlar to'plamini o'rnatadi (eskilarini almashtiradi).
     *
     * @param actor kim beryapti — o'zida yo'q ruxsatni bera olmaydi (§10)
     * @throws IllegalArgumentException actor bermoqchi bo'lgan ruxsat o'zida bo'lmasa
     */
    void replacePermissions(User actor, UUID targetUserId, Set<Permission> permissions);

    /** {@code actor} {@code target} rolidagi hisobni yarata oladimi. */
    boolean canCreateRole(User actor, PlatformRole target);

    /** {@code actor} {@code target} foydalanuvchisini boshqara oladimi. */
    boolean canManageUser(User actor, User target);
}
