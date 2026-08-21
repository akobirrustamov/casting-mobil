package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.StaffProfile;
import com.example.backend.Cms.Enums.StaffStatus;
import com.example.backend.Cms.Repository.StaffProfileRepo;
import com.example.backend.Entity.User;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Xodimlarni boshqarish.
 *
 * <h2>Hard delete YO'Q</h2>
 * Buyurtmachi talabi: iloji boricha faolsizlantirish ishlatilsin. Sabab
 * texnik ham: audit jurnalidagi har bir yozuv {@code actor_id} saqlaydi.
 * Xodim o'chirilsa, o'sha id hech kimga tegishli bo'lmay qoladi va
 * o'tmishdagi amallarni kimga bog'lash noma'lum bo'lardi.
 *
 * Shuning uchun {@link #deactivate} — hisob qoladi, kirish yopiladi.
 *
 * <h2>Holat kirishni to'xtatadi</h2>
 * {@code ACTIVE} bo'lmagan xodim tizimga kira olmaydi va mavjud tokeni ham
 * darhol kuchsizlanadi — tekshiruv har so'rovda bazadan o'qiladi
 * ({@code PermissionInterceptor}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffProfileRepo profileRepo;
    private final UserRepo userRepo;
    private final PermissionService permissionService;
    private final AuditService auditService;

    // ------------------------------------------------------------- profil

    /**
     * Profilni topadi, bo'lmasa yaratadi.
     *
     * Eski xodimlar (AutoRun, seeder yoki bu funksiya qo'shilishidan oldin
     * yaratilganlar) profilsiz bo'lishi mumkin. Ular uchun ACTIVE profil
     * yaratiladi — ya'ni mavjud hisoblar ishlashda davom etadi.
     */
    @Transactional
    public StaffProfile profileOf(User user) {
        return profileRepo.findByUserId(user.getId())
                .orElseGet(() -> profileRepo.save(StaffProfile.builder()
                        .user(user)
                        .status(StaffStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build()));
    }

    /** Ro'yxat uchun: bir so'rovda hamma profil. */
    @Transactional(readOnly = true)
    public Map<UUID, StaffProfile> profilesOf(Collection<UUID> userIds) {
        Map<UUID, StaffProfile> byUser = new HashMap<>();
        if (userIds.isEmpty()) {
            return byUser;
        }
        for (StaffProfile p : profileRepo.findAllByUserIdIn(userIds)) {
            byUser.put(p.getUser().getId(), p);
        }
        return byUser;
    }

    /** Yangi xodim yaratilganda chaqiriladi. */
    @Transactional
    public StaffProfile register(User created, User actor) {
        return profileRepo.save(StaffProfile.builder()
                .user(created)
                .status(StaffStatus.ACTIVE)
                .createdBy(actor == null ? null : actor.getId())
                .createdAt(LocalDateTime.now())
                .build());
    }

    /** Muvaffaqiyatli kirishdan keyin. */
    @Transactional
    public void recordLogin(User user) {
        StaffProfile profile = profileOf(user);
        profile.setLastLoginAt(LocalDateTime.now());
        profileRepo.save(profile);
    }

    /**
     * Xodim ishlay oladimi.
     *
     * Profilsiz xodim ACTIVE hisoblanadi — eski hisoblar sinmasin.
     */
    @Transactional(readOnly = true)
    public boolean canWork(UUID userId) {
        return profileRepo.findByUserId(userId)
                .map(p -> p.getStatus().canWork())
                .orElse(true);
    }

    /** Kirish yopilgan bo'lsa sababi, aks holda {@code null}. */
    @Transactional(readOnly = true)
    public String blockedReason(UUID userId) {
        return profileRepo.findByUserId(userId)
                .filter(p -> !p.getStatus().canWork())
                .map(p -> p.getStatus() == StaffStatus.INACTIVE
                        ? "Hisobingiz faolsizlantirilgan"
                        : "Hisobingiz bloklangan"
                        + (p.getStatusReason() == null ? "" : ": " + p.getStatusReason()))
                .orElse(null);
    }

    // ------------------------------------------------------------- amallar

    @Transactional
    public StaffProfile activate(User actor, User target) {
        return changeStatus(actor, target, StaffStatus.ACTIVE, null,
                AuditAction.STAFF_UPDATED);
    }

    @Transactional
    public StaffProfile deactivate(User actor, User target, String reason) {
        guardSelf(actor, target);
        return changeStatus(actor, target, StaffStatus.INACTIVE, reason,
                AuditAction.STAFF_DEACTIVATED);
    }

    @Transactional
    public StaffProfile block(User actor, User target, String reason) {
        guardSelf(actor, target);
        return changeStatus(actor, target, StaffStatus.BLOCKED, reason,
                AuditAction.STAFF_DEACTIVATED);
    }

    @Transactional
    public StaffProfile unblock(User actor, User target) {
        return changeStatus(actor, target, StaffStatus.ACTIVE, null,
                AuditAction.STAFF_UPDATED);
    }

    private StaffProfile changeStatus(User actor, User target, StaffStatus status,
                                      String reason, String auditAction) {
        StaffProfile profile = profileOf(target);
        StaffStatus before = profile.getStatus();

        profile.setStatus(status);
        profile.setStatusReason(reason);
        profile.setStatusChangedAt(LocalDateTime.now());
        profile.setStatusChangedBy(actor == null ? null : actor.getId());
        StaffProfile saved = profileRepo.save(profile);

        auditService.log(actor, auditAction, "User", target.getId(),
                Map.of("status", before.name()),
                Map.of("status", status.name(), "reason", String.valueOf(reason)));
        return saved;
    }

    /**
     * O'z hisobiga tegib bo'lmaydi.
     *
     * Aks holda xodim o'zini faolsizlantirib, tizimdan chiqib qolardi va
     * uni faqat boshqa admin qaytara olardi. Bu foydali emas, xatosi esa
     * qaytarib bo'lmaydigan darajada bezovta.
     */
    private void guardSelf(User actor, User target) {
        if (actor != null && actor.getId().equals(target.getId())) {
            throw BusinessException.accessDenied("O'z hisobingizni o'zgartira olmaysiz");
        }
    }

    // ----------------------------------------------------------- ro'yxat

    /**
     * Ko'rish doirasi.
     *
     * HYPER_ADMIN barcha xodimlarni ko'radi (audit uchun), qolganlar faqat
     * o'zidan quyi rollarni. Batafsil: {@code roadmap.md}.
     */
    @Transactional(readOnly = true)
    public List<User> visibleStaff(User actor) {
        PlatformRole actorRole = permissionService.roleOf(actor);
        boolean seesEveryone = actorRole == PlatformRole.HYPER_ADMIN;

        return userRepo.findAll().stream()
                .filter(u -> {
                    PlatformRole r = permissionService.roleOf(u);
                    if (r == null || r == PlatformRole.USER) {
                        return false;
                    }
                    return seesEveryone || actorRole.canManage(r);
                })
                .toList();
    }
}
