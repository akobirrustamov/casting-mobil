package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.*;
import com.example.backend.Cms.Enums.SubscriptionSource;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Cms.Repository.*;
import com.example.backend.Entity.User;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.RoleMapper;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Mobil foydalanuvchilarni admin paneldan boshqarish.
 *
 * Bu yerda XODIMLAR emas, oddiy foydalanuvchilar: bloklash, Premium sovg'a
 * qilish va tortib olish, qurilmalarni ko'rish va chiqarib yuborish.
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepo userRepo;
    private final UserAccountRepo accountRepo;
    private final UserDeviceRepo deviceRepo;
    private final UserBalanceRepo balanceRepo;
    private final SubscriptionRepo subscriptionRepo;
    private final TariffRepo tariffRepo;
    private final SettingsService settingsService;
    private final AuditService auditService;

    /**
     * Foydalanuvchilarni qidirish: telefon, email yoki ism bo'yicha (§35, §38).
     *
     * Faqat USER rolidagilar — xodimlar alohida bo'limda.
     */
    @Transactional(readOnly = true)
    public List<User> search(String query, int limit) {
        List<User> all = userRepo.findAll();
        String q = query == null ? "" : query.trim().toLowerCase();

        return all.stream()
                .filter(u -> {
                    PlatformRole role = RoleMapper.highestRole(u);
                    return role == null || role == PlatformRole.USER;
                })
                .filter(u -> q.isEmpty()
                        || contains(u.getPhone(), q)
                        || contains(u.getEmail(), q)
                        || contains(u.getName(), q)
                        || u.getId().toString().equalsIgnoreCase(q))
                .limit(limit)
                .toList();
    }

    /** Hisob yozuvi yo'q bo'lsa yaratiladi — eski foydalanuvchilarda u yo'q. */
    @Transactional
    public UserAccount accountOf(UUID userId) {
        return accountRepo.findByUserId(userId).orElseGet(() -> {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> BusinessException.notFound("User", userId));
            return accountRepo.save(UserAccount.builder().user(user).build());
        });
    }

    @Transactional
    public UserBalance balanceOf(UUID userId) {
        return balanceRepo.findByUserId(userId).orElseGet(() -> {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> BusinessException.notFound("User", userId));
            return balanceRepo.save(UserBalance.builder().user(user).build());
        });
    }

    @Transactional
    public UserAccount setBlocked(User actor, UUID userId, boolean blocked, String reason) {
        UserAccount account = accountOf(userId);
        account.setStatus(blocked ? UserStatus.BLOCKED : UserStatus.ACTIVE);
        account.setBlockedReason(blocked ? reason : null);
        UserAccount saved = accountRepo.save(account);

        auditService.log(actor, blocked ? AuditAction.USER_BLOCKED : AuditAction.USER_UNBLOCKED,
                "User", userId, null, reason == null ? Map.of() : Map.of("reason", reason));
        return saved;
    }

    // --------------------------------------------------------------- premium

    /**
     * Premium sovg'a qilish (§38).
     *
     * Obuna yozuvi {@code ADMIN_GIFT} manbasi bilan yaratiladi va
     * {@code paidAmount} null qoladi — bu daromad EMAS va hisobotda shunday
     * hisoblanishi kerak.
     */
    @Transactional
    public UserAccount grantPremium(User actor, UUID userId, Integer months, Long tariffId) {
        if (months == null || months <= 0) {
            throw BusinessException.validation("Muddat oylarda va noldan katta bo'lishi kerak");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));
        UserAccount account = accountOf(userId);

        // Mavjud obuna ustiga qo'shiladi, boshidan boshlanmaydi
        LocalDateTime from = account.hasActivePremium()
                ? account.getPremiumUntil() : LocalDateTime.now();
        LocalDateTime until = from.plusMonths(months);

        Tariff tariff = tariffId == null ? null : tariffRepo.findById(tariffId).orElse(null);

        subscriptionRepo.save(Subscription.builder()
                .user(user)
                .tariff(tariff)
                .startAt(LocalDateTime.now())
                .endAt(until)
                .source(SubscriptionSource.ADMIN_GIFT)
                .paidAmount(null)
                .grantedBy(actor == null ? null : actor.getId())
                .build());

        account.setPremiumUntil(until);
        UserAccount saved = accountRepo.save(account);

        auditService.log(actor, AuditAction.PREMIUM_GRANTED, "User", userId, null,
                Map.of("months", months, "until", until.toString()));
        return saved;
    }

    /** Premiumni muddatidan oldin tortib olish (§38). */
    @Transactional
    public UserAccount revokePremium(User actor, UUID userId) {
        UserAccount account = accountOf(userId);
        LocalDateTime before = account.getPremiumUntil();

        account.setPremiumUntil(null);
        UserAccount saved = accountRepo.save(account);

        LocalDateTime now = LocalDateTime.now();
        for (Subscription s : subscriptionRepo.findAllByUserIdOrderByEndAtDesc(userId)) {
            if (s.isActiveAt(now)) {
                s.setRevokedAt(now);
                s.setRevokedBy(actor == null ? null : actor.getId());
                subscriptionRepo.save(s);
            }
        }

        auditService.log(actor, AuditAction.PREMIUM_REVOKED, "User", userId,
                Map.of("premiumUntil", String.valueOf(before)), Map.of("premiumUntil", "null"));
        return saved;
    }

    // -------------------------------------------------------------- qurilma

    @Transactional(readOnly = true)
    public List<UserDevice> devices(UUID userId) {
        return deviceRepo.findAllByUserIdOrderByLastActiveAtDesc(userId);
    }

    /**
     * Qurilmani chiqarib yuborish.
     *
     * O'chirilmaydi, {@code active = false} qilinadi: tarix saqlanadi va
     * o'sha qurilma qayta kirsa tanib olinadi.
     */
    @Transactional
    public void revokeDevice(User actor, UUID userId, Long deviceRowId) {
        UserDevice device = deviceRepo.findById(deviceRowId)
                .orElseThrow(() -> BusinessException.notFound("Device", deviceRowId));
        if (!device.getUser().getId().equals(userId)) {
            throw BusinessException.validation("Bu qurilma boshqa foydalanuvchiga tegishli");
        }
        device.setActive(false);
        deviceRepo.save(device);

        auditService.log(actor, AuditAction.DEVICE_REVOKED, "UserDevice", deviceRowId, null,
                Map.of("userId", userId.toString(), "deviceId", device.getDeviceId()));
    }

    /**
     * Yangi qurilmani ro'yxatga olish — limit tekshiriladi.
     *
     * Buyurtmachi: bitta hisobdan 2 tadan ortiq qurilma bo'lmasin. Limit
     * sozlamada, kodda emas.
     *
     * Bu metod mobil ilova uchun; admin panel uni chaqirmaydi, lekin qoida
     * shu yerda turishi kerak — aks holda u klientda takrorlanardi.
     */
    @Transactional
    public UserDevice registerDevice(UUID userId, String deviceId, String name, String platform) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        Optional<UserDevice> known = deviceRepo.findByUserIdAndDeviceId(userId, deviceId);
        if (known.isPresent()) {
            UserDevice d = known.get();
            d.setActive(true);
            d.setLastActiveAt(LocalDateTime.now());
            return deviceRepo.save(d);
        }

        int limit = settingsService.getInt(SettingKeys.DEVICE_LIMIT, 2);
        List<UserDevice> active = deviceRepo.findAllByUserIdAndActiveTrueOrderByLastActiveAtAsc(userId);
        if (active.size() >= limit) {
            throw new BusinessException("DEVICE_LIMIT_REACHED",
                    "Bitta hisobdan " + limit + " tadan ortiq qurilmaga kirish mumkin emas. "
                            + "Sozlamalardan eski qurilmani chiqaring.",
                    HttpStatus.CONFLICT);
        }

        return deviceRepo.save(UserDevice.builder()
                .user(user).deviceId(deviceId).deviceName(name).platform(platform)
                .build());
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
