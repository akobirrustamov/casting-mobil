package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.RequirePermission;
import com.example.backend.Cms.Service.SessionAdminService;
import com.example.backend.Enums.PlatformRole;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Majburiy chiqarish — faqat SUPER_ADMIN va undan yuqori.
 *
 * <h2>Nega super admin, oddiy admin emas</h2>
 * Ommaviy chiqarish — barcha foydalanuvchilarni bir bosishda kirish
 * oynasiga qaytaradi. Bu hodisa (token sizib chiqdi, hisob buzildi)
 * paytidagi chora, kundalik amal emas; xato bosilsa, butun auditoriya
 * qayta kirishga majbur bo'ladi.
 *
 * @see SessionAdminService
 */
@RestController
@RequestMapping("/api/v1/app/admin/sessions")
@RequiredArgsConstructor
public class SessionAdminController {

    private final SessionAdminService sessionAdminService;

    /** Ommaviy chiqarish. Javob: {@code {users, sessions}}. */
    @PostMapping("/logout-all")
    @RequirePermission(role = PlatformRole.SUPER_ADMIN)
    public ResponseEntity<Map<String, Integer>> logoutAll(@RequestBody LogoutAllRequest request) {
        return ResponseEntity.ok(sessionAdminService.logoutAll(CurrentUser.get(), request.getScope()));
    }

    /** Bitta foydalanuvchini barcha qurilmalaridan chiqarish. Javob: {@code {sessions}}. */
    @PostMapping("/users/{userId}/logout")
    @RequirePermission(role = PlatformRole.SUPER_ADMIN)
    public ResponseEntity<Map<String, Integer>> logoutUser(@PathVariable UUID userId) {
        int sessions = sessionAdminService.logoutUser(CurrentUser.get(), userId);
        return ResponseEntity.ok(Map.of("sessions", sessions));
    }

    @Data
    public static class LogoutAllRequest {
        private SessionAdminService.Scope scope;
    }
}
