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
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.exceptions.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import java.util.UUID;
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
    private final com.example.backend.Services.AuthService.RefreshTokenService refreshTokenService;
    private final com.example.backend.Security.LoginAttemptService loginAttempts;

    /**
     * ⚠️ Refresh token javob tanasida EMAS, {@code httpOnly} cookie'da
     * qaytariladi (§61).
     *
     * ТЗ «localStorage'ga tashlashdan oldin xavfsizlikni hisobga ol»
     * deydi. Sabab aniq: {@code localStorage} ni sahifadagi HAR QANDAY
     * JavaScript o'qiy oladi. Bitta XSS — masalan buzilgan npm paketi —
     * refresh tokenni o'g'irlab, uzoq muddatli kirish huquqini beradi.
     * {@code httpOnly} cookie'ni esa JavaScript umuman ko'rmaydi.
     *
     * Access token qisqa muddatli va faqat xotirada saqlanadi.
     */
    private static final String REFRESH_COOKIE = "uz_refresh";

    @Value("${app.auth.cookie-secure:true}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request,
                                                    HttpServletRequest httpRequest,
                                                    HttpServletResponse httpResponse) {
        String login = request.getPhone().trim();

        // Hisob bo'yicha himoya: IP limiti bitta hisobga qaratilgan,
        // ko'p manbali hujumni to'xtata olmaydi (§61).
        long locked = loginAttempts.lockedMinutesLeft(login);
        if (locked > 0) {
            throw new BusinessException("ACCOUNT_LOCKED",
                    "Ko'p marta xato urinildi. " + locked + " daqiqadan keyin qayta urinib ko'ring.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        Optional<User> found = userRepo.findByPhone(login);

        // Foydalanuvchi yo'qligi va parol xatoligi bir xil javob beradi -
        // aks holda qaysi raqamlar ro'yxatdan o'tganini aniqlash mumkin bo'lardi.
        if (found.isEmpty() || !passwordEncoder.matches(request.getPassword(), found.get().getPassword())) {
            loginAttempts.recordFailure(login);
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

        loginAttempts.recordSuccess(login);
        staffService.recordLogin(user);
        auditService.log(user, AuditAction.ADMIN_LOGIN, "User", user.getId());

        setRefreshCookie(httpResponse, refreshTokenService.issue(user, httpRequest));
        return ResponseEntity.ok(new AdminLoginResponse(
                jwtService.generateJwtToken(user),
                null,
                mapper.toDto(user)));
    }

    /**
     * Access tokenni yangilaydi (§61).
     *
     * Refresh token cookie'dan olinadi — ilgari eski moduldagi
     * {@code /auth/refresh} uni URL QUERY PARAMETRIDA qabul qilardi va
     * u server loglariga, proxy loglariga hamda brauzer tarixiga tushib
     * qolardi.
     *
     * Har chaqiruvda token AYLANTIRILADI: eskisi bekor qilinadi.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AdminLoginResponse> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        UUID userId = refreshTokenService.rotate(refreshToken, httpRequest);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN",
                        "Foydalanuvchi topilmadi", HttpStatus.UNAUTHORIZED));

        // ⚠️ Yangilashda ham huquq qayta tekshiriladi. Aks holda
        // bo'shatilgan xodim token muddati tugaguncha ishlayverardi -
        // va rotatsiya tufayli MUDDATSIZ uzaytirardi.
        PlatformRole role = permissionService.roleOf(user);
        String blocked = staffService.blockedReason(user.getId());
        if (role == null || !role.canAccessAdminPanel() || blocked != null) {
            refreshTokenService.revokeAll(user.getId());
            clearRefreshCookie(httpResponse);
            throw BusinessException.accessDenied(
                    blocked != null ? blocked : "Admin panelga ruxsat yo'q");
        }

        String rotated = refreshTokenService.issue(user, httpRequest);
        refreshTokenService.linkReplacement(refreshToken, rotated);
        setRefreshCookie(httpResponse, rotated);

        return ResponseEntity.ok(new AdminLoginResponse(
                jwtService.generateJwtToken(user), null, mapper.toDto(user)));
    }

    /** Chiqish — token serverda ham kuchini yo'qotadi. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        refreshTokenService.revoke(refreshToken);
        clearRefreshCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", buildCookie(token, refreshCookieMaxAge()));
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie("", 0));
    }

    private String buildCookie(String value, long maxAgeSeconds) {
        // SameSite=Strict: cookie faqat bizning saytimizdan yuborilgan
        // so'rovlarga qo'shiladi, ya'ni CSRF uchun yo'l qolmaydi.
        StringBuilder sb = new StringBuilder()
                .append(REFRESH_COOKIE).append('=').append(value)
                .append("; Path=/api/v1/app/admin/auth")
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; HttpOnly")
                .append("; SameSite=Strict");
        if (cookieSecure) {
            sb.append("; Secure");
        }
        return sb.toString();
    }

    private long refreshCookieMaxAge() {
        return refreshTokenMs / 1000;
    }

    @Value("${app.jwt.refresh-token-ms:86400000}")
    private long refreshTokenMs;

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
