package com.example.backend.Cms;

import com.example.backend.Cms.Entity.Tariff;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Repository.TariffRepo;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Entity.AuditLog;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.AuditLogRepo;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §38 — premium sovg'a qilish.
 *
 * <h2>Nima uchun audit alohida tekshiriladi</h2>
 * ТЗ: «Har bir action Audit Logga tushsin.» Bu talabning tuzog'i shundaki,
 * audit yozuvi YO'Q bo'lsa ham asosiy amal ishlayveradi: premium beriladi,
 * foydalanuvchi ko'radi, hech kim shikoyat qilmaydi. Xato faqat oylar
 * o'tib, «bu odamga premiumni kim bergan?» degan savol chiqqanda
 * bilinadi — va o'shanda javob topib bo'lmaydi.
 *
 * Shuning uchun audit yozuvi asosiy amal bilan bir qatorda tekshiriladi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PremiumGiftTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private UserAdminService userAdminService;
    @Autowired private AuditLogRepo auditLogRepo;
    @Autowired private TariffRepo tariffRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    // ------------------------------------------------------------- qidiruv

    @Nested
    @DisplayName("Foydalanuvchini topish (ТЗ §38)")
    class Lookup {

        @Test
        @DisplayName("Telefon raqami bo'yicha")
        void byPhone() {
            User u = appUser();

            assertThat(found(u.getPhone())).contains(u.getId());
        }

        @Test
        @DisplayName("Email bo'yicha")
        void byEmail() {
            User u = appUser();
            u.setEmail("sovga" + SEQ.incrementAndGet() + "@uzcasting.uz");
            userRepo.save(u);

            assertThat(found(u.getEmail())).contains(u.getId());
        }

        @Test
        @DisplayName("ID bo'yicha")
        void byId() {
            User u = appUser();

            // UUID'ni `like` bilan qidirib bo'lmaydi — alohida shart.
            assertThat(found(u.getId().toString())).containsExactly(u.getId());
        }

        @Test
        @DisplayName("Emaili yo'q foydalanuvchi qidiruvni buzmaydi")
        void nullEmailDoesNotBreakSearch() {
            User u = appUser();
            assertThat(u.getEmail()).isNull();

            // `null like '%...%'` — null, ya'ni mos kelmaydi. To'g'ri.
            assertThat(found("hech-kimda-yoq@example.com")).doesNotContain(u.getId());
            assertThat(found(u.getPhone())).contains(u.getId());
        }

        private List<UUID> found(String query) {
            return userAdminService.searchPage(query, PageRequest.of(0, 50))
                    .getContent().stream().map(r -> r.user().getId()).toList();
        }
    }

    // -------------------------------------------------------------- amallar

    @Nested
    @DisplayName("Amallar")
    class Actions {

        @Test
        @DisplayName("Premium sovg'a qilish va muddat belgilash")
        void grantWithExplicitDuration() {
            User u = appUser();

            UserAccount account = userAdminService.grantPremium(null, u.getId(), 3, null);

            assertThat(account.hasActivePremium()).isTrue();
            assertThat(ChronoUnit.MONTHS.between(LocalDateTime.now(),
                    account.getPremiumUntil())).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Tarif orqali sovg'a — muddat tarifdan")
        void grantViaTariff() {
            Tariff halfYear = tariffRepo.findAll().stream()
                    .filter(t -> Integer.valueOf(6).equals(t.getDurationMonths()))
                    .findFirst().orElseThrow();
            User u = appUser();

            UserAccount account = userAdminService.grantPremium(
                    null, u.getId(), null, halfYear.getId());

            assertThat(ChronoUnit.MONTHS.between(LocalDateTime.now(),
                    account.getPremiumUntil())).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Uzaytirish — mavjud muddat ustiga qo'shiladi")
        void extendAddsToExistingPeriod() {
            User u = appUser();

            LocalDateTime afterFirst =
                    userAdminService.grantPremium(null, u.getId(), 1, null).getPremiumUntil();
            LocalDateTime afterSecond =
                    userAdminService.grantPremium(null, u.getId(), 1, null).getPremiumUntil();

            // Aks holda ikkinchi sovg'a birinchisini yeb qo'yardi va
            // foydalanuvchi to'lagan muddatini yo'qotardi.
            assertThat(afterSecond).isAfter(afterFirst);
            assertThat(ChronoUnit.MONTHS.between(afterFirst, afterSecond))
                    .isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Bekor qilish — premium ham, obuna ham yopiladi")
        void revokeClosesSubscriptionToo() {
            User u = appUser();
            userAdminService.grantPremium(null, u.getId(), 6, null);

            UserAccount revoked = userAdminService.revokePremium(null, u.getId());

            assertThat(revoked.hasActivePremium()).isFalse();
            assertThat(revoked.getPremiumUntil()).isNull();
        }

        @Test
        @DisplayName("Premiumsiz foydalanuvchini bekor qilish xato bermaydi")
        void revokingWithoutPremiumIsSafe() {
            User u = appUser();

            // Admin ikki marta bosishi mumkin — bu xato holat emas.
            assertThat(userAdminService.revokePremium(null, u.getId())
                    .hasActivePremium()).isFalse();
        }

        @Test
        @DisplayName("Muddatsiz va tarifsiz sovg'a rad etiladi")
        void grantWithoutDurationIsRejected() {
            User u = appUser();

            assertThatThrownBy(() ->
                    userAdminService.grantPremium(null, u.getId(), null, null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ---------------------------------------------------------------- audit

    @Nested
    @DisplayName("Audit jurnali (ТЗ §38)")
    class Audit {

        @Test
        @DisplayName("⚠️ Sovg'a qilish audit jurnaliga tushadi")
        void grantIsAudited() {
            User u = appUser();
            userAdminService.grantPremium(null, u.getId(), 3, null);

            assertThat(auditFor(u.getId()))
                    .extracting(AuditLog::getAction)
                    .contains(AuditAction.PREMIUM_GRANTED);
        }

        @Test
        @DisplayName("⚠️ Bekor qilish ham audit jurnaliga tushadi")
        void revokeIsAudited() {
            User u = appUser();
            userAdminService.grantPremium(null, u.getId(), 3, null);
            userAdminService.revokePremium(null, u.getId());

            assertThat(auditFor(u.getId()))
                    .extracting(AuditLog::getAction)
                    .contains(AuditAction.PREMIUM_REVOKED);
        }

        @Test
        @DisplayName("Yozuvda MUDDAT va TARIF ko'rinadi")
        void auditRecordsWhatWasGranted() {
            Tariff yearly = tariffRepo.findAll().stream()
                    .filter(t -> Integer.valueOf(12).equals(t.getDurationMonths()))
                    .findFirst().orElseThrow();
            User u = appUser();

            userAdminService.grantPremium(null, u.getId(), null, yearly.getId());

            AuditLog log = auditFor(u.getId()).stream()
                    .filter(a -> AuditAction.PREMIUM_GRANTED.equals(a.getAction()))
                    .findFirst().orElseThrow();

            // «Kim, kimga, qancha muddatga» — uchalasi ham kerak. Faqat
            // «premium berildi» degan yozuv savolga javob bermaydi.
            assertThat(log.getAfterState())
                    .contains("12")
                    .contains(yearly.getCode());
        }

        @Test
        @DisplayName("Bekor qilishda OLDINGI muddat saqlanadi")
        void revokeRecordsPreviousExpiry() {
            User u = appUser();
            UserAccount granted = userAdminService.grantPremium(null, u.getId(), 3, null);
            String expected = String.valueOf(granted.getPremiumUntil());

            userAdminService.revokePremium(null, u.getId());

            AuditLog log = auditFor(u.getId()).stream()
                    .filter(a -> AuditAction.PREMIUM_REVOKED.equals(a.getAction()))
                    .findFirst().orElseThrow();

            // Nima yo'qotilganini bilmasa, qarorni qaytarib bo'lmaydi.
            assertThat(log.getBeforeState()).contains(expected);
        }

        @Test
        @DisplayName("⚠️ Auditda parol yoki token YO'Q")
        void auditHasNoSecrets() {
            User u = appUser();
            userAdminService.grantPremium(null, u.getId(), 1, null);

            for (AuditLog log : auditFor(u.getId())) {
                String all = String.valueOf(log.getBeforeState())
                        + log.getAfterState() + log.getAction();
                assertThat(all.toLowerCase())
                        .as("Audit jurnaliga maxfiy ma'lumot yozilmasin")
                        .doesNotContain("password")
                        .doesNotContain("parol")
                        .doesNotContain("token")
                        .doesNotContain(String.valueOf(u.getPassword()));
            }
        }

        private List<AuditLog> auditFor(UUID userId) {
            return auditLogRepo.search(null, null, "User", userId.toString(),
                    null, null, PageRequest.of(0, 50)).getContent();
        }
    }

    // ------------------------------------------------------------ yordamchi

    private User appUser() {
        Role r = roleRepo.findByName(UserRoles.ROLE_USER);
        if (r == null) {
            int nextId = roleRepo.findAll().stream()
                    .mapToInt(Role::getId).max().orElse(0) + 1;
            r = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99890" + (9500000 + SEQ.incrementAndGet()));
        u.setPassword("maxfiy-parol-" + SEQ.get());
        u.setName("Sovg'a " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(r)));
        return userRepo.save(u);
    }
}
