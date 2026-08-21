package com.example.backend.Admin;

import com.example.backend.Entity.User;
import com.example.backend.exceptions.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext'dan joriy foydalanuvchini olishning yagona joyi.
 *
 * Controller'larda @AuthenticationPrincipal yozib yurmaslik va tekshiruv
 * mantiqini takrorlamaslik uchun.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** Joriy foydalanuvchi yoki null. */
    public static User getOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }

    /** Joriy foydalanuvchi. Bo'lmasa 401. */
    public static User get() {
        User user = getOrNull();
        if (user == null) {
            throw new BusinessException("UNAUTHORIZED", "Avtorizatsiya talab qilinadi",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return user;
    }
}
