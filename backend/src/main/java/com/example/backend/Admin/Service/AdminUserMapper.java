package com.example.backend.Admin.Service;

import com.example.backend.Admin.Dto.AdminUserDto;
import com.example.backend.Entity.User;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Services.PermissionService.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminUserMapper {

    private final PermissionService permissionService;

    public AdminUserDto toDto(User user) {
        PlatformRole role = permissionService.roleOf(user);
        return AdminUserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(role)
                // Faqat WORKER'da ma'noli: yuqori rollarda ruxsatlar jadvali ishlatilmaydi.
                .permissions(role == PlatformRole.WORKER
                        ? permissionService.permissionsOf(user.getId())
                        : Set.of())
                .creatableRoles(role == null ? Set.of() : role.creatableRoles())
                .build();
    }

    /** WORKER uchun amaldagi ruxsatlar; yuqori rollar uchun HAMMASI. */
    public Set<com.example.backend.Enums.Permission> effectivePermissions(User user) {
        PlatformRole role = permissionService.roleOf(user);
        if (role == null || role == PlatformRole.USER) {
            return Set.of();
        }
        if (role.isAtLeast(PlatformRole.ADMIN)) {
            return EnumSet.allOf(com.example.backend.Enums.Permission.class);
        }
        return permissionService.permissionsOf(user.getId());
    }
}
