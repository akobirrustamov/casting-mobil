package com.example.backend.Admin;

import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * {@link RequirePermission} annotatsiyasini amalga oshiradi.
 *
 * Metod chaqirilishidan OLDIN ishlaydi: ruxsat yo'q bo'lsa metod tanasi
 * umuman bajarilmaydi.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    @Before("@annotation(annotation)")
    public void check(RequirePermission annotation) {
        var user = CurrentUser.get();

        // Rol talabi (masalan audit jurnali faqat ADMIN dan yuqorisiga)
        if (annotation.role() != PlatformRole.USER) {
            PlatformRole role = permissionService.roleOf(user);
            if (role == null || !role.isAtLeast(annotation.role())) {
                throw BusinessException.accessDenied(
                        "Kerakli rol: " + annotation.role() + " yoki undan yuqori");
            }
        }

        // Ruxsat talabi — barchasi bo'lishi kerak
        for (Permission permission : annotation.value()) {
            if (!permissionService.hasPermission(user, permission)) {
                throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
            }
        }
    }
}
