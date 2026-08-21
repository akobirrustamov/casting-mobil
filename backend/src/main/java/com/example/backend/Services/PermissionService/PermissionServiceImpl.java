package com.example.backend.Services.PermissionService;

import com.example.backend.Entity.User;
import com.example.backend.Entity.UserPermission;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.UserPermissionRepo;
import com.example.backend.Security.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final UserPermissionRepo userPermissionRepo;

    @Override
    public PlatformRole roleOf(User user) {
        return RoleMapper.highestRole(user);
    }

    @Override
    public boolean canAccessAdminPanel(User user) {
        PlatformRole role = roleOf(user);
        return role != null && role.canAccessAdminPanel();
    }

    @Override
    public boolean hasPermission(User user, Permission permission) {
        if (user == null || permission == null) {
            return false;
        }
        PlatformRole role = roleOf(user);
        if (role == null || role == PlatformRole.USER) {
            return false;
        }
        // ADMIN, SUPER_ADMIN, HYPER_ADMIN — rol darajasida to'liq huquq.
        if (role.isAtLeast(PlatformRole.ADMIN)) {
            return true;
        }
        // WORKER — faqat unga aniq berilgan ruxsatlar.
        return userPermissionRepo.existsByUserIdAndPermission(user.getId(), permission);
    }

    @Override
    public Set<Permission> permissionsOf(UUID userId) {
        if (userId == null) {
            return EnumSet.noneOf(Permission.class);
        }
        List<UserPermission> rows = userPermissionRepo.findAllByUserId(userId);
        if (rows.isEmpty()) {
            return EnumSet.noneOf(Permission.class);
        }
        return rows.stream()
                .map(UserPermission::getPermission)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Permission.class)));
    }

    @Override
    @Transactional
    public void replacePermissions(User actor, UUID targetUserId, Set<Permission> permissions) {
        if (targetUserId == null) {
            throw new IllegalArgumentException("targetUserId bo'sh");
        }
        // ⚠️ EnumSet.copyOf(collection) BO'SH to'plamda IllegalArgumentException
        // tashlaydi. Ruxsatsiz worker yaratish esa mutlaqo normal holat, shuning
        // uchun to'plam qo'lda yig'iladi.
        Set<Permission> requested = EnumSet.noneOf(Permission.class);
        if (permissions != null) {
            requested.addAll(permissions);
        }

        // §10: o'zida bo'lmagan ruxsatni boshqaga bera olmaydi.
        for (Permission p : requested) {
            if (!hasPermission(actor, p)) {
                throw new IllegalArgumentException(
                        "Sizda " + p + " ruxsati yo'q, shuning uchun uni bera olmaysiz");
            }
        }

        // ⚠️ FARQ hisoblanadi, "hammasini o'chirib qaytadan yozish" EMAS.
        //
        // Ilgari shunday edi: deleteAllByUserId(...) keyin saveAll(...).
        // Hibernate DELETE ni flush paytigacha kechiktiradi va INSERT'ni
        // undan OLDIN yuboradi. Natijada mavjud ruxsat qayta berilsa
        // UNIQUE(user_id, permission) buzilardi va 500 qaytardi.
        //
        // Bu juda oddiy holat: admin bitta ruxsatni qo'shib, qolganini
        // o'sha holicha qoldiradi.
        //
        // Farq bilan ishlash bu muammoni butunlay yo'q qiladi va ortiqcha
        // yozuv-o'chiruvni ham qilmaydi.
        List<UserPermission> existing = userPermissionRepo.findAllByUserId(targetUserId);
        Set<Permission> current = EnumSet.noneOf(Permission.class);
        existing.forEach(row -> current.add(row.getPermission()));

        List<UserPermission> obsolete = existing.stream()
                .filter(row -> !requested.contains(row.getPermission()))
                .collect(Collectors.toList());
        if (!obsolete.isEmpty()) {
            userPermissionRepo.deleteAll(obsolete);
        }

        UUID actorId = actor == null ? null : actor.getId();
        List<UserPermission> added = requested.stream()
                .filter(p -> !current.contains(p))
                .map(p -> UserPermission.builder()
                        .userId(targetUserId)
                        .permission(p)
                        .grantedBy(actorId)
                        .build())
                .collect(Collectors.toList());
        if (!added.isEmpty()) {
            userPermissionRepo.saveAll(added);
        }
    }

    @Override
    public boolean canCreateRole(User actor, PlatformRole target) {
        PlatformRole actorRole = roleOf(actor);
        return actorRole != null && actorRole.canCreate(target);
    }

    @Override
    public boolean canManageUser(User actor, User target) {
        PlatformRole actorRole = roleOf(actor);
        PlatformRole targetRole = roleOf(target);
        if (actorRole == null || targetRole == null) {
            return false;
        }
        // O'zini o'zi boshqarish bu tekshiruvdan o'tmaydi — u alohida oqim (profil).
        return actorRole.canManage(targetRole);
    }
}
