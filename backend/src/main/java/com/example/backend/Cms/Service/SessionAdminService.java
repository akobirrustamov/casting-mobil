package com.example.backend.Cms.Service;

import com.example.backend.Entity.User;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.RefreshTokenRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.RoleMapper;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Majburiy chiqarish — bitta foydalanuvchi yoki ommaviy (super admin).
 *
 * <h2>Ikki qadam, ikkalasi ham shart</h2>
 * <ol>
 *   <li>{@code users.sessions_valid_after = now} — qo'lda turgan ACCESS
 *       tokenlar darhol yaroqsiz ({@code MyFilter}). Busiz odam yana
 *       15 daqiqa ishlayverardi.</li>
 *   <li>Refresh tokenlar bekor qilinadi — ilova yangi access token ololmaydi
 *       va kirish oynasiga qaytadi. Busiz 1-qadam hech narsa bermasdi:
 *       ilova 401 olib, darhol tokenni yangilab qo'yardi.</li>
 * </ol>
 *
 * <h2>⚠️ Kimga tegmaydi</h2>
 * <ul>
 *   <li>amalni bajarayotgan adminning o'ziga — aks holda tugmani bosgan
 *       odam birinchi bo'lib paneldan chiqib ketardi;</li>
 *   <li>o'zi bilan teng yoki yuqori roldagi xodimlarga — super admin
 *       boshqa super admin va hyper adminni chiqara olmaydi. Xodim
 *       boshqaruvidagi qoida bilan bir xil ({@link PlatformRole#canManage}).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SessionAdminService {

    /** Ommaviy chiqarish kimlarga. */
    public enum Scope {
        /** Mobil ilova foydalanuvchilari (xodim roli yo'qlar). */
        APP_USERS,
        /** Admin panel xodimlari — faqat o'zidan past rollar. */
        STAFF,
        /** Ikkalasi. */
        ALL
    }

    /** Admin panelga kira oladigan rollar — «ilova foydalanuvchisi emas» degani. */
    private static final List<PlatformRole> STAFF_ROLES = Arrays.stream(PlatformRole.values())
            .filter(PlatformRole::canAccessAdminPanel)
            .toList();

    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final AuditService auditService;

    /**
     * Bitta foydalanuvchini barcha qurilmalaridan chiqaradi.
     *
     * @return bekor qilingan refresh tokenlar soni (panelda «N ta sessiya»)
     */
    @Transactional
    public int logoutUser(User actor, UUID userId) {
        if (actor.getId().equals(userId)) {
            throw BusinessException.validation(
                    "O'zingizni bu yerdan chiqara olmaysiz — panelning «Chiqish» tugmasidan foydalaning");
        }
        User target = userRepo.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        PlatformRole targetRole = RoleMapper.highestRole(target);
        if (targetRole != null && targetRole.canAccessAdminPanel() && !outranks(actor, targetRole)) {
            throw BusinessException.accessDenied(
                    "O'zingiz bilan teng yoki yuqori roldagi xodimni chiqara olmaysiz");
        }

        LocalDateTime now = cutoffNow();
        userRepo.markLoggedOut(userId, now);
        int revoked = refreshTokenRepo.revokeAllForUser(userId, now);

        auditService.log(actor, AuditAction.SESSIONS_REVOKED, "User", userId, null,
                Map.of("refreshTokensRevoked", revoked));
        return revoked;
    }

    /**
     * Ommaviy chiqarish.
     *
     * @return {@code users} — chiqarilgan foydalanuvchilar,
     *         {@code sessions} — bekor qilingan refresh tokenlar
     */
    @Transactional
    public Map<String, Integer> logoutAll(User actor, Scope scope) {
        if (scope == null) {
            throw BusinessException.validation("Kimlarni chiqarish kerakligi ko'rsatilmagan");
        }
        PlatformRole actorRole = RoleMapper.highestRole(actor);
        LocalDateTime now = cutoffNow();

        int users = 0;
        if (scope == Scope.APP_USERS || scope == Scope.ALL) {
            users += userRepo.markAppUsersLoggedOut(now, actor.getId(), names(STAFF_ROLES));
        }
        if (scope == Scope.STAFF || scope == Scope.ALL) {
            List<PlatformRole> lower = STAFF_ROLES.stream().filter(r -> outranks(actorRole, r)).toList();
            List<PlatformRole> protectedRoles = STAFF_ROLES.stream().filter(r -> !outranks(actorRole, r)).toList();
            if (!lower.isEmpty()) {
                users += userRepo.markStaffLoggedOut(now, actor.getId(), names(lower), names(protectedRoles));
            }
        }
        int sessions = refreshTokenRepo.revokeAllMarkedAt(now);

        auditService.log(actor, AuditAction.SESSIONS_REVOKED_BULK, "User", null, null,
                Map.of("scope", scope.name(), "users", users, "refreshTokensRevoked", sessions));
        return Map.of("users", users, "sessions", sessions);
    }

    private static boolean outranks(User actor, PlatformRole target) {
        return outranks(RoleMapper.highestRole(actor), target);
    }

    private static boolean outranks(PlatformRole actor, PlatformRole target) {
        return actor != null && actor.getLevel() > target.getLevel();
    }

    private static List<String> names(List<PlatformRole> roles) {
        return roles.stream().map(r -> RoleMapper.toUserRole(r).name()).toList();
    }

    /**
     * KEYINGI butun soniya.
     *
     * ⚠️ JWT {@code iat} soniyagacha kesilgan. Chegara «hozir» (kesilgan)
     * bo'lsa, AYNI soniyada, lekin chiqarishdan oldin berilgan token
     * {@code iat == chegara} bo'lib omon qolardi — yana 15 daqiqa. Keyingi
     * soniya uni ham qamraydi; narxi — chiqarishdan keyingi <1 soniyada
     * berilgan token ham rad etiladi, bunga esa odam qayta kirib ulgurmaydi.
     *
     * {@code revokeAllMarkedAt} shu qiymat bo'yicha tenglik bilan qidiradi.
     */
    private static LocalDateTime cutoffNow() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
    }
}
