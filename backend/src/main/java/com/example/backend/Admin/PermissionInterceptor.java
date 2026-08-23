package com.example.backend.Admin;

import com.example.backend.Cms.Service.StaffService;
import com.example.backend.Entity.User;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * {@link RequirePermission} ni so'rov TANASI o'qilishidan OLDIN tekshiradi.
 *
 * <h2>Nega aspekt yetarli emas edi</h2>
 * Spring {@code @Valid @RequestBody} ni controller metodi CHAQIRILISHIDAN
 * oldin tekshiradi. Ya'ni aspekt (u metod chaqiruviga ulanadi) validatsiyadan
 * KEYIN ishlaydi.
 *
 * Natijada ruxsatsiz xodim noto'g'ri tana yuborsa <b>422 VALIDATION_ERROR</b>
 * olardi, <b>403</b> emas. Hech narsa yarata olmasdi — bu xavfsizlik teshigi
 * emas edi — lekin javobdan qaysi maydonlar majburiy ekanini bilib olardi.
 *
 * Interceptor esa {@code preHandle} da, argumentlar hal qilinishidan ham
 * oldin ishlaydi. Shu sababli ruxsat birinchi tekshiriladi.
 *
 * <h2>Aspekt nega qoldirildi</h2>
 * {@link PermissionAspect} controller'dan tashqarida — masalan servis
 * metodlarida — ishlatilganda kerak. Ikkalasi bir xil annotatsiyani
 * tekshiradi, ya'ni takroriy tekshiruv zararsiz.
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;
    private final StaffService staffService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        // Bloklangan xodim HAR QANDAY admin so'roviga kira olmaydi -
        // annotatsiya bor-yo'qligidan qat'i nazar. Shu sababli bloklash
        // DARHOL kuchga kiradi: mavjud token ham ishlamay qoladi.
        rejectIfBlocked(request);

        RequirePermission annotation = method.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            // Annotatsiyasiz endpointlar tekshiruvni metod ichida bajaradi.
            // AdminEndpointGuardTest ularning qo'riqlanganini kafolatlaydi.
            return true;
        }

        var user = CurrentUser.get();

        if (annotation.role() != PlatformRole.USER) {
            PlatformRole role = permissionService.roleOf(user);
            if (role == null || !role.isAtLeast(annotation.role())) {
                throw BusinessException.accessDenied(
                        "Kerakli rol: " + annotation.role() + " yoki undan yuqori");
            }
        }

        for (Permission permission : annotation.value()) {
            if (!permissionService.hasPermission(user, permission)) {
                throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
            }
        }
        return true;
    }

    /**
     * Bloklangan hisobni to'xtatadi.
     *
     * Tekshiruv har so'rovda bazadan o'qiladi. Agar holat tokenga yozilganda
     * edi, bloklangan xodim tokeni muddati tugaguncha ishlab turardi.
     */
    private void rejectIfBlocked(HttpServletRequest request) {
        // ⚠️ FAQAT admin makoni. Sabablari ikkita:
        //
        // 1) Eski casting endpointlari o'z xatti-harakatini saqlashi kerak —
        //    ular bu tekshiruvni bilmaydi va bilmasligi ham kerak.
        // 2) Bloklangan ILOVA foydalanuvchisi uchun AccessService allaqachon
        //    ma'noli javob beradi ({@code USER_BLOCKED} + nima qilish kerak).
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/v1/app/admin/")) {
            return;
        }

        User user = CurrentUser.getOrNull();
        if (user == null) {
            return;
        }

        // Holat har so'rovda bazadan o'qiladi. Agar u tokenga yozilganda
        // edi, faolsizlantirilgan xodim tokeni muddati tugaguncha ishlardi.
        String blocked = staffService.blockedReason(user.getId());
        if (blocked != null) {
            throw BusinessException.accessDenied(blocked);
        }
    }
}