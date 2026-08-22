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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Ilova foydalanuvchilari ro'yxati (ТЗ §35).
     *
     * Qidiruv telefon, email yoki ism bo'yicha. Faqat USER rolidagilar —
     * xodimlar alohida bo'limda boshqariladi (§12).
     *
     * <h2>Nima o'zgardi</h2>
     * Ilgari {@code findAll()} chaqirilib, xodimlar Java'da ajratilardi va
     * chegara faqat shundan keyin qo'llanardi — ya'ni panelni ochish BUTUN
     * jadvalni xotiraga tortardi. Endi filtr ham, sahifalash ham bazada.
     *
     * <h2>N+1</h2>
     * Har bir foydalanuvchi uchun hisob, balans va qurilmalar alohida
     * so'ralardi: 50 kishilik sahifa 150 ta qo'shimcha so'rov degani edi.
     * Endi uchalasi ham bitta {@code in (...)} so'rovi bilan olinadi va
     * {@link AppUserRow} ichida beriladi.
     */
    @Transactional(readOnly = true)
    public Page<AppUserRow> searchPage(String query, Pageable pageable) {
        String q = query == null || query.isBlank() ? null : query.trim();

        // ТЗ §38: foydalanuvchini ID orqali ham topish kerak. UUID'ni
        // `like` bilan qidirib bo'lmaydi, shuning uchun matn to'g'ri UUID
        // bo'lsa alohida parametrga tushadi.
        UUID exactId = null;
        if (q != null) {
            try {
                exactId = UUID.fromString(q);
            } catch (IllegalArgumentException ignored) {
                // Oddiy qidiruv matni — ID emas. Bu normal holat.
            }
        }

        Page<User> page = userRepo.findAppUsers(q, exactId, pageable);

        List<UUID> ids = page.getContent().stream().map(User::getId).toList();
        if (ids.isEmpty()) {
            return page.map(u -> new AppUserRow(u, null, null, 0));
        }

        Map<UUID, UserAccount> accounts = new HashMap<>();
        accountRepo.findAllByUserIdIn(ids)
                .forEach(a -> accounts.put(a.getUser().getId(), a));

        Map<UUID, UserBalance> balances = new HashMap<>();
        balanceRepo.findAllByUserIdIn(ids)
                .forEach(b -> balances.put(b.getUser().getId(), b));

        Map<UUID, Integer> deviceCounts = new HashMap<>();
        deviceRepo.findAllByUserIdInAndActiveTrue(ids)
                .forEach(d -> deviceCounts.merge(d.getUser().getId(), 1, Integer::sum));

        return page.map(u -> new AppUserRow(u,
                accounts.get(u.getId()),
                balances.get(u.getId()),
                deviceCounts.getOrDefault(u.getId(), 0)));
    }

    /** Ro'yxatning bitta qatori — foydalanuvchi va u bilan bog'liq hammasi. */
    public record AppUserRow(User user, UserAccount account,
                             UserBalance balance, int activeDevices) {
    }

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
        // ⚠️ Tarif tanlangan bo'lsa, MUDDAT O'SHANDAN olinadi.
        //
        // Ilgari muddat faqat `months` parametridan kelardi va tarifning
        // `durationMonths` maydoni umuman o'qilmasdi — u bezak edi.
        // Natijada admin «12 oy — 159 900» tarifini tanlab `months=1`
        // yuborsa, foydalanuvchi 1 oy olardi, obuna yozuvida esa 12 oylik
        // tarif turardi. Hisobotda bu tarif «12 oylik» bo'lib ko'rinardi.
        //
        // Endi tarif o'z muddatini belgilaydi; `months` esa faqat tarifsiz
        // erkin sovg'a uchun.
        Tariff tariff = tariffId == null ? null : tariffRepo.findById(tariffId)
                .orElseThrow(() -> BusinessException.notFound("Tariff", tariffId));

        Integer effectiveMonths = months;
        if (tariff != null && tariff.getDurationMonths() != null
                && tariff.getDurationMonths() > 0) {
            effectiveMonths = tariff.getDurationMonths();
        }

        if (effectiveMonths == null || effectiveMonths <= 0) {
            throw BusinessException.validation(
                    "Muddat oylarda va noldan katta bo'lishi kerak "
                            + "(yoki muddati belgilangan tarif tanlansin)");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));
        UserAccount account = accountOf(userId);

        // Mavjud obuna ustiga qo'shiladi, boshidan boshlanmaydi
        LocalDateTime from = account.hasActivePremium()
                ? account.getPremiumUntil() : LocalDateTime.now();
        LocalDateTime until = from.plusMonths(effectiveMonths);

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
                Map.of("months", effectiveMonths,
                        "tariff", tariff == null ? "—" : tariff.getCode(),
                        "until", until.toString()));
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
