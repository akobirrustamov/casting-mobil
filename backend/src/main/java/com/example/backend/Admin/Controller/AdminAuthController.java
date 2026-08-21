package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.Dto.AdminLoginRequest;
import com.example.backend.Admin.Dto.AdminLoginResponse;
import com.example.backend.Admin.Dto.AdminUserDto;
import com.example.backend.Admin.Service.AdminUserMapper;
import com.example.backend.Entity.User;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Admin panelga kirish.
 *
 * Mavjud {@code /api/v1/auth/login} dan farqi: bu yerda ROL TEKSHIRILADI.
 * {@code USER} roli admin panelga kira olmaydi - u mobil ilova uchun.
 */
@RestController
@RequestMapping("/api/v1/app/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PermissionService permissionService;
    private final AdminUserMapper mapper;
    private final AuditService auditService;
    private final com.example.backend.Cms.Service.StaffService staffService;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        Optional<User> found = userRepo.findByPhone(request.getPhone().trim());

        // Foydalanuvchi yo'qligi va parol xatoligi bir xil javob beradi -
        // aks holda qaysi raqamlar ro'yxatdan o'tganini aniqlash mumkin bo'lardi.
        if (found.isEmpty() || !passwordEncoder.matches(request.getPassword(), found.get().getPassword())) {
            throw new BusinessException("INVALID_CREDENTIALS",
                    "Telefon yoki parol noto'g'ri", HttpStatus.UNAUTHORIZED);
        }

        User user = found.get();
        PlatformRole role = permissionService.roleOf(user);

        if (role == null || !role.canAccessAdminPanel()) {
            throw BusinessException.accessDenied(
                    "Bu hisob admin panelga kira olmaydi");
        }

        // Faolsizlantirilgan yoki bloklangan xodim kira olmaydi.
        // Sabab javobda aytiladi - xodim nima uchun kira olmayotganini
        // bilishi kerak, aks holda u parolini qayta-qayta terib yurardi.
        String blocked = staffService.blockedReason(user.getId());
        if (blocked != null) {
            throw BusinessException.accessDenied(blocked);
        }

        staffService.recordLogin(user);
        auditService.log(user, "ADMIN_LOGIN", "User", user.getId());

        return ResponseEntity.ok(new AdminLoginResponse(
                jwtService.generateJwtToken(user),
                jwtService.generateJwtRefreshToken(user),
                mapper.toDto(user)));
    }

    /** Sahifa yangilanganda profilni tiklash uchun. */
    @GetMapping("/me")
    public ResponseEntity<AdminUserDto> me() {
        User user = CurrentUser.get();
        PlatformRole role = permissionService.roleOf(user);
        if (role == null || !role.canAccessAdminPanel()) {
            throw BusinessException.accessDenied("Admin panelga ruxsat yo'q");
        }
        return ResponseEntity.ok(mapper.toDto(user));
    }
}
