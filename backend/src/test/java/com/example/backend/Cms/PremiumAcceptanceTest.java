package com.example.backend.Cms;

import com.example.backend.Cms.Entity.Tariff;
import com.example.backend.Cms.Repository.TariffRepo;
import com.example.backend.Cms.Service.MonetizationService;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Entity.AuditLog;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.AuditLogRepo;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Services.AuditService.AuditAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §83 — premium qabul mezonlari.
 *
 * Beshta amal va ularning auditga tushishi. Har biri boshqa testlarda
 * chuqurroq tekshirilgan; bu yerda <b>ro'yxat sifatida</b> yig'iladi —
 * §78 va §79 dagi kabi.
 *
 * <h2>Nega audit alohida muhim</h2>
 * Premium — pul o'rnini bosadigan huquq. «Kim kimga premium berdi»
 * degan savolga javob bo'lmasa, uni tekshirib ham, bahslashib ham
 * bo'lmaydi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PremiumAcceptanceTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MonetizationService monetizationService;
    @Autowired private UserAdminService userAdminService;
    @Autowired private TariffRepo tariffRepo;
    @Autowired private AuditLogRepo auditLogRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    /** 1. Admin tariflarni ko'radi. */
    @Test
    @DisplayName("1. Tariflar ro'yxati ko'rinadi")
    void tariffsAreVisible() {
        assertThat(monetizationService.tariffs())
                .as("V5 da seed qilingan tariflar bo'lishi kerak")
                .isNotEmpty();
    }

    /** 2. Admin narxni o'zgartiradi — va bu auditga tushadi. */
    @Test
    @DisplayName("2. Narx o'zgaradi va auditga tushadi")
    void priceChangeIsAudited() {
        User admin = admin();
        Tariff tariff = monetizationService.tariffs().get(0);
        BigDecimal newPrice = tariff.getPrice().add(new BigDecimal("1000.00"));

        monetizationService.saveTariff(admin, tariff.getId(),
                priceRequest(tariff, newPrice));

        assertThat(tariffRepo.findById(tariff.getId()).orElseThrow().getPrice())
                .isEqualByComparingTo(newPrice);
        assertThat(auditFor(admin, AuditAction.TARIFF_CHANGED))
                .as("narx o'zgarishi izsiz qolmasin - bu pul masalasi")
                .isNotEmpty();
    }

    /** 3. Admin foydalanuvchini telefon orqali topadi. */
    @Test
    @DisplayName("3. Foydalanuvchi telefon bo'yicha topiladi")
    void userIsFoundByPhone() {
        User target = appUser();

        var found = userAdminService.searchPage(target.getPhone(), PageRequest.of(0, 10));

        assertThat(found.getContent())
                .as("premium berish uchun admin avval odamni topishi kerak")
                .isNotEmpty();
    }

    /** 4 va 5. Premium beriladi va bekor qilinadi — ikkalasi ham auditda. */
    @Test
    @DisplayName("4-5. Premium beriladi, bekor qilinadi va ikkalasi auditga tushadi")
    void grantAndRevokeAreAudited() {
        User admin = admin();
        User target = appUser();
        Tariff tariff = monetizationService.tariffs().get(0);

        userAdminService.grantPremium(admin, target.getId(), null, tariff.getId());
        assertThat(auditFor(admin, AuditAction.PREMIUM_GRANTED)).isNotEmpty();

        userAdminService.revokePremium(admin, target.getId());
        assertThat(auditFor(admin, AuditAction.PREMIUM_REVOKED))
                .as("bekor qilish ham xuddi berish kabi izlanadigan bo'lsin")
                .isNotEmpty();
    }

    // ------------------------------------------------------------ yordamchi

    private List<AuditLog> auditFor(User actor, String action) {
        return auditLogRepo.search(action, actor.getId(), null, null, null, null,
                PageRequest.of(0, 20)).getContent();
    }

    private com.example.backend.Admin.Dto.TariffSaveRequest priceRequest(
            Tariff tariff, BigDecimal price) {
        var r = new com.example.backend.Admin.Dto.TariffSaveRequest();
        r.setPrice(price);
        r.setDurationMonths(tariff.getDurationMonths());
        r.setActive(true);
        r.setSortOrder(tariff.getSortOrder());
        var tr = new java.util.LinkedHashMap<com.example.backend.Cms.Enums.Locale,
                com.example.backend.Admin.Dto.TariffTextDto>();
        for (var loc : com.example.backend.Cms.Enums.Locale.values()) {
            tr.put(loc, com.example.backend.Admin.Dto.TariffTextDto.builder()
                    .name("Tarif " + loc).build());
        }
        r.setTranslations(tr);
        return r;
    }

    private User admin() {
        return user(UserRoles.ROLE_ADMIN, "Admin");
    }

    private User appUser() {
        return user(UserRoles.ROLE_USER, "Foydalanuvchi");
    }

    private User user(UserRoles roleName, String label) {
        Role role = roleRepo.findByName(roleName);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, roleName));
        }
        User u = new User();
        int n = SEQ.incrementAndGet();
        u.setPhone("+99891" + (1000000 + n));
        u.setPassword("xesh");
        u.setName(label + " " + n);
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }
}
