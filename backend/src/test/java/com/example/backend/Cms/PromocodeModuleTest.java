package com.example.backend.Cms;

import com.example.backend.Cms.Entity.Promocode;
import com.example.backend.Cms.Entity.Subscription;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.PromocodeGrantType;
import com.example.backend.Cms.Enums.SubscriptionSource;
import com.example.backend.Cms.Repository.PromocodeRepo;
import com.example.backend.Cms.Repository.SubscriptionRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.PromocodeService;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Promokodlar — bepul kunlar. Nima berishini ADMIN tanlaydi.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * Promokod pul beradi. Har bir bo'shliq — bepul tarqatilgan obuna:
 * ikki marta ishlatish, muddati o'tganini qabul qilish, limitdan oshib
 * ketish. Bularning hech biri ekranda ko'rinmaydi — kod «ishlaydi»,
 * shunchaki ko'proq odamga.
 *
 * <h2>Poyga holati alohida faylda</h2>
 * {@code PromocodeRaceTest} — u tranzaksiyasiz ishlaydi, chunki ikki
 * parallel so'rov bitta test tranzaksiyasi ichida bo'lishi mumkin emas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PromocodeModuleTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String REDEEM = "/api/v1/app/promocodes/redeem";
    private static final String MY = "/api/v1/app/promocodes/my";

    @Autowired private MockMvc mockMvc;
    @Autowired private PromocodeService promocodeService;
    @Autowired private PromocodeRepo promocodeRepo;
    @Autowired private SubscriptionRepo subscriptionRepo;
    @Autowired private UserAccountRepo accountRepo;
    @Autowired private UserAdminService userAdminService;
    @Autowired private AccessService accessService;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private JwtService jwtService;

    // ------------------------------------------------------------ ishlatish

    @Nested
    @DisplayName("Ishlatish")
    class Redeem {

        @Test
        @DisplayName("Kod Premium beradi va tarixga PROMO manbasi bilan yoziladi")
        void grantsPremium() {
            User u = user();
            Promocode promo = promo(30, null);

            PromocodeService.Redemption r = promocodeService.redeem(u, promo.getCode());

            LocalDateTime expected = LocalDateTime.now().plusDays(30);
            assertThat(r.premiumUntil()).isCloseTo(expected, org.assertj.core.api.Assertions.within(5, ChronoUnit.SECONDS));

            UserAccount account = accountRepo.findByUserId(u.getId()).orElseThrow();
            assertThat(account.hasActivePremium()).isTrue();

            List<Subscription> history = subscriptionRepo.findAllByUserIdOrderByEndAtDesc(u.getId());
            assertThat(history).hasSize(1);
            assertThat(history.get(0).getSource()).isEqualTo(SubscriptionSource.PROMO);
            // ⚠️ Daromad emas: hisobot `paidAmount is not null` bo'yicha filtrlaydi.
            assertThat(history.get(0).getPaidAmount()).isNull();
        }

        /**
         * ⚠️ Buyurtmachi qarori: mavjud muddat USTIGA. Boshidan boshlash
         * odamning to'lagan kunlarini «yeb» qo'yardi: 20 kuni bor odam
         * 30 kunlik kod kiritsa, 50 emas, 30 qolardi.
         */
        @Test
        @DisplayName("Faol obuna ustiga qo'shiladi, boshidan boshlanmaydi")
        void stacksOnActivePremium() {
            User u = user();
            LocalDateTime existing = LocalDateTime.now().plusDays(20).truncatedTo(ChronoUnit.SECONDS);
            accountRepo.save(UserAccount.builder()
                    .user(u).premiumUntil(existing).createdAt(LocalDateTime.now()).build());

            PromocodeService.Redemption r = promocodeService.redeem(u, promo(30, null).getCode());

            assertThat(r.premiumUntil()).isEqualTo(existing.plusDays(30));
        }

        /**
         * Admin sovg'asi bilan promokod bitta arifmetikadan o'tadi
         * ({@code PremiumGrantService}). Bu test ikkalasini ketma-ket
         * chaqirib, ikkinchisi birinchisi ustiga qo'shilganini tekshiradi.
         */
        @Test
        @DisplayName("Admin sovg'asi va promokod bitta qoidada")
        void adminGiftAndPromoShareOneRule() {
            User u = user();
            userAdminService.grantPremium(null, u.getId(), 1, null);
            LocalDateTime afterGift = accountRepo.findByUserId(u.getId()).orElseThrow().getPremiumUntil();

            PromocodeService.Redemption r = promocodeService.redeem(u, promo(10, null).getCode());

            assertThat(r.premiumUntil()).isEqualTo(afterGift.plusDays(10));
            assertThat(subscriptionRepo.findAllByUserIdOrderByEndAtDesc(u.getId()))
                    .extracting(Subscription::getSource)
                    .containsExactlyInAnyOrder(SubscriptionSource.ADMIN_GIFT, SubscriptionSource.PROMO);
        }

        /** Telefon klaviaturasi: kichik harf, bo'shliq — hammasi bitta kod. */
        @Test
        @DisplayName("Kichik harf va bo'shliqlar qabul qilinadi")
        void normalizesInput() {
            User u = user();
            Promocode promo = promo(7, null);

            promocodeService.redeem(u, "  " + promo.getCode().toLowerCase() + " ");

            assertThat(promocodeService.mine(u.getId())).hasSize(1);
        }
    }

    // ------------------------------------------------------------- casting

    /**
     * Buyurtmachi (04.09.2026): «casting bo'limiga bepul kirish kunlari».
     *
     * ⚠️ Bu Premiumdan ALOHIDA huquq. Ilgari
     * {@code canAccessCasting()} shunchaki «faol Premium» degan ma'noni
     * anglatardi, ya'ni casting kirishini Premiumsiz berishning yo'li
     * yo'q edi.
     */
    @Nested
    @DisplayName("Casting turi")
    class Casting {

        @Test
        @DisplayName("Casting kodi casting ochadi, Premium ochmaydi")
        void castingCodeOpensOnlyCasting() {
            User u = user();

            promocodeService.redeem(u, promo(7, null, PromocodeGrantType.CASTING_DAYS).getCode());

            assertThat(accessService.canAccessCasting(u)).isTrue();
            // ⚠️ Eng muhim tekshiruv: film va seriallar ochilmaydi.
            assertThat(accessService.premiumStatus(u).active()).isFalse();
        }

        /**
         * ⚠️ Casting huquqi obuna EMAS. `cms_subscription` ga yozilsa,
         * «faol obunachilar» soni casting kodlari hisobiga shishib
         * ketardi va daromad hisoboti bilan mos kelmasdi.
         */
        @Test
        @DisplayName("Casting kodi obuna yozuvi yaratmaydi")
        void castingWritesNoSubscription() {
            User u = user();

            promocodeService.redeem(u, promo(7, null, PromocodeGrantType.CASTING_DAYS).getCode());

            assertThat(subscriptionRepo.findAllByUserIdOrderByEndAtDesc(u.getId())).isEmpty();
        }

        @Test
        @DisplayName("Casting muddati ham ustiga qo'shiladi")
        void castingStacks() {
            User u = user();

            var first = promocodeService.redeem(
                    u, promo(5, null, PromocodeGrantType.CASTING_DAYS).getCode());
            var second = promocodeService.redeem(
                    u, promo(7, null, PromocodeGrantType.CASTING_DAYS).getCode());

            assertThat(second.premiumUntil()).isEqualTo(first.premiumUntil().plusDays(7));
        }

        /** Premium casting'ni ham ochadi — teskarisi yo'q. */
        @Test
        @DisplayName("Premium kodi casting'ni ham ochadi")
        void premiumCoversCasting() {
            User u = user();

            promocodeService.redeem(u, promo(30, null).getCode());

            assertThat(accessService.canAccessCasting(u)).isTrue();
        }

        @Test
        @DisplayName("Sukut bo'yicha tur — Premium")
        void defaultTypeIsPremium() {
            Promocode p = promocodeService.create(null,
                    new PromocodeService.Draft(null, null, 30, null, null, null, true, null));

            assertThat(p.getGrantType()).isEqualTo(PromocodeGrantType.PREMIUM_DAYS);
        }

        /**
         * ⚠️ Kod tarqatilgan bo'lishi mumkin. Turni o'zgartirish kodni
         * ishlaydigan holda qoldiradi, lekin u va'da qilingandan boshqa
         * narsa bera boshlaydi — bu kodni buzishdan ham yomonroq.
         */
        @Test
        @DisplayName("Tahrirlashda tur o'zgarmaydi")
        void typeIsImmutable() {
            Promocode p = promo(7, null, PromocodeGrantType.CASTING_DAYS);

            promocodeService.update(null, p.getId(),
                    new PromocodeService.Draft(null, PromocodeGrantType.PREMIUM_DAYS,
                            7, null, null, null, null, null));

            assertThat(promocodeRepo.findById(p.getId()).orElseThrow().getGrantType())
                    .isEqualTo(PromocodeGrantType.CASTING_DAYS);
        }
    }

    // --------------------------------------------------------------- rad etish

    @Nested
    @DisplayName("Rad etish sabablari aniq")
    class Rejections {

        @Test
        @DisplayName("Topilmadi")
        void notFound() {
            assertCode(() -> promocodeService.redeem(user(), "YOQ-KOD-123"), "PROMO_NOT_FOUND");
        }

        @Test
        @DisplayName("Ikkinchi marta — allaqachon ishlatilgan")
        void secondUseRejected() {
            User u = user();
            Promocode promo = promo(30, null);
            promocodeService.redeem(u, promo.getCode());

            assertCode(() -> promocodeService.redeem(u, promo.getCode()), "PROMO_ALREADY_USED");
            // Ikkinchi urinish hech narsa bermagan.
            assertThat(subscriptionRepo.findAllByUserIdOrderByEndAtDesc(u.getId())).hasSize(1);
        }

        @Test
        @DisplayName("Muddati o'tgan")
        void expired() {
            Promocode promo = promo(30, null);
            promo.setValidUntil(LocalDateTime.now().minusMinutes(1));
            promocodeRepo.save(promo);

            assertCode(() -> promocodeService.redeem(user(), promo.getCode()), "PROMO_EXPIRED");
        }

        @Test
        @DisplayName("Hali boshlanmagan")
        void notYetValid() {
            Promocode promo = promo(30, null);
            promo.setValidFrom(LocalDateTime.now().plusDays(1));
            promocodeRepo.save(promo);

            assertCode(() -> promocodeService.redeem(user(), promo.getCode()), "PROMO_EXPIRED");
        }

        @Test
        @DisplayName("To'xtatilgan")
        void disabled() {
            Promocode promo = promo(30, null);
            promo.setActive(false);
            promocodeRepo.save(promo);

            assertCode(() -> promocodeService.redeem(user(), promo.getCode()), "PROMO_INACTIVE");
        }

        @Test
        @DisplayName("Limit tugagan")
        void exhausted() {
            Promocode promo = promo(30, 2);
            promocodeService.redeem(user(), promo.getCode());
            promocodeService.redeem(user(), promo.getCode());

            assertCode(() -> promocodeService.redeem(user(), promo.getCode()), "PROMO_EXHAUSTED");
        }

        @Test
        @DisplayName("Bo'sh yoki g'alati kod — validatsiya")
        void malformed() {
            assertCode(() -> promocodeService.redeem(user(), "   "), "VALIDATION_ERROR");
            assertCode(() -> promocodeService.redeem(user(), "ab"), "VALIDATION_ERROR");
            assertCode(() -> promocodeService.redeem(user(), "kod!@#"), "VALIDATION_ERROR");
        }
    }

    // ------------------------------------------------------------------ admin

    @Nested
    @DisplayName("Admin")
    class Admin {

        @Test
        @DisplayName("Kod bo'sh bo'lsa generatsiya qilinadi")
        void generatesCode() {
            Promocode p = promocodeService.create(null,
                    new PromocodeService.Draft(null, null, 30, 100, null, null, true, "Instagram"));

            assertThat(p.getCode()).hasSize(8).matches("[A-Z0-9]+");
            // Chalkash belgilar generatsiyada yo'q.
            assertThat(p.getCode()).doesNotContain("0", "O", "1", "I");
        }

        @Test
        @DisplayName("Berilgan kod katta harfda saqlanadi")
        void storesUppercase() {
            Promocode p = promocodeService.create(null,
                    new PromocodeService.Draft("yangi-2026", null, 30, null, null, null, true, null));

            assertThat(p.getCode()).isEqualTo("YANGI-2026");
        }

        @Test
        @DisplayName("Takror kod rad etiladi")
        void duplicateRejected() {
            promocodeService.create(null,
                    new PromocodeService.Draft("TAKROR", null, 30, null, null, null, true, null));

            assertCode(() -> promocodeService.create(null,
                    new PromocodeService.Draft("takror", null, 30, null, null, null, true, null)),
                    "PROMO_CODE_TAKEN");
        }

        /**
         * ⚠️ Kod tarqatilgan bo'lishi mumkin. O'zgartirish odamlar qo'lidagi
         * kodni yaroqsiz qilardi.
         */
        @Test
        @DisplayName("Tahrirlashda kodning o'zi o'zgarmaydi")
        void codeIsImmutable() {
            Promocode p = promo(30, null);
            String original = p.getCode();

            promocodeService.update(null, p.getId(),
                    new PromocodeService.Draft("BOSHQA", null, 60, null, null, null, null, null));

            Promocode reloaded = promocodeRepo.findById(p.getId()).orElseThrow();
            assertThat(reloaded.getCode()).isEqualTo(original);
            assertThat(reloaded.getGrantDays()).isEqualTo(60);
        }

        @Test
        @DisplayName("Validatsiya: kunlar, limit, sanalar")
        void validation() {
            assertCode(() -> promocodeService.create(null,
                    new PromocodeService.Draft(null, null, 0, null, null, null, true, null)),
                    "VALIDATION_ERROR");
            assertCode(() -> promocodeService.create(null,
                    new PromocodeService.Draft(null, null, 30, 0, null, null, true, null)),
                    "VALIDATION_ERROR");
            LocalDateTime now = LocalDateTime.now();
            assertCode(() -> promocodeService.create(null,
                    new PromocodeService.Draft(null, null, 30, null, now, now.minusDays(1), true, null)),
                    "VALIDATION_ERROR");
        }
    }

    // --------------------------------------------------------------- endpoint

    @Nested
    @DisplayName("Endpoint")
    class Endpoints {

        @Test
        @DisplayName("Token yo'q bo'lsa 401")
        void anonymousRejected() throws Exception {
            mockMvc.perform(post(REDEEM).contentType("application/json")
                            .content("{\"code\":\"X\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Muvaffaqiyatda kunlar va muddat qaytadi")
        void redeemReturnsResult() throws Exception {
            User u = user();
            Promocode promo = promo(30, null);

            mockMvc.perform(post(REDEEM)
                            .header("Authorization", token(u))
                            .contentType("application/json")
                            .content("{\"code\":\"" + promo.getCode() + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.days").value(30))
                    .andExpect(jsonPath("$.until").isNotEmpty());

            mockMvc.perform(get(MY).header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].code").value(promo.getCode()));
        }

        /** Ilova xato matnini KOD bo'yicha tanlaydi — kod javobda bo'lishi shart. */
        @Test
        @DisplayName("Xato kodi javobda aniq")
        void errorCodeIsExposed() throws Exception {
            mockMvc.perform(post(REDEEM)
                            .header("Authorization", token(user()))
                            .contentType("application/json")
                            .content("{\"code\":\"YOQ-KOD\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PROMO_NOT_FOUND"));
        }
    }

    // ---------------------------------------------------------- yordamchi

    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(code);
    }

    private Promocode promo(int days, Integer max) {
        return promo(days, max, PromocodeGrantType.PREMIUM_DAYS);
    }

    private Promocode promo(int days, Integer max, PromocodeGrantType type) {
        return promocodeRepo.save(Promocode.builder()
                .code("T" + SEQ.incrementAndGet() + "-PROMO")
                .grantType(type)
                .grantDays(days)
                .maxRedemptions(max)
                .active(true)
                .build());
    }

    private User user() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        int n = SEQ.incrementAndGet();
        u.setPhone("+99890" + (9000000 + n));
        u.setPassword("xesh-" + n);
        u.setName("Promo " + n);
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    private String token(User u) {
        return "Bearer " + jwtService.generateJwtToken(u);
    }
}
