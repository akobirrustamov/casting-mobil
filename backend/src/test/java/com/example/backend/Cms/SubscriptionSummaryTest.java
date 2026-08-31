package com.example.backend.Cms;

import com.example.backend.Cms.Entity.Subscription;
import com.example.backend.Cms.Entity.Tariff;
import com.example.backend.Cms.Enums.SubscriptionSource;
import com.example.backend.Cms.Repository.SubscriptionRepo;
import com.example.backend.Cms.Repository.TariffRepo;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Obunalar jamlanmasi — panel grafiklari uchun.
 *
 * <h2>⚠️ Qo'riqlanadigan asosiy qoida: SON ≠ DAROMAD</h2>
 * Sovg'a obunalarda ({@code ADMIN_GIFT}) to'lov yo'q. Ular obunachi
 * sifatida sanaladi, lekin daromadga kirmaydi.
 *
 * Ikkalasi bitta songa qo'shilsa panel «10 ta obuna sotildi» deb
 * ko'rsatardi, holbuki ularning yarmi bepul berilgan — va bu
 * xulosaga qarab narx siyosati o'zgartirilishi mumkin edi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SubscriptionSummaryTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String URL = "/api/v1/app/admin/subscriptions/summary";

    @Autowired private MockMvc mockMvc;
    @Autowired private SubscriptionRepo subscriptionRepo;
    @Autowired private TariffRepo tariffRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------- yordamchi

    private User staff() {
        Role role = roleRepo.findByName(UserRoles.ROLE_ADMIN);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_ADMIN));
        }
        User u = new User();
        u.setPhone("+99890" + (9400000 + SEQ.incrementAndGet()));
        u.setPassword(passwordEncoder.encode("Parol123!"));
        u.setName("Xodim " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    private String token() {
        return "Bearer " + jwtService.generateJwtToken(staff());
    }

    private Tariff tariff(String code) {
        Tariff t = new Tariff();
        t.setCode(code + SEQ.incrementAndGet());
        t.setDurationMonths(1);
        t.setPrice(new BigDecimal("50000"));
        return tariffRepo.save(t);
    }

    /** @param paid {@code null} — sovg'a obuna, to'lov yo'q */
    private Subscription subscription(Tariff tariff, SubscriptionSource source,
                                      BigDecimal paid, LocalDateTime endAt) {
        return subscriptionRepo.save(Subscription.builder()
                .user(staff())
                .tariff(tariff)
                .source(source)
                .paidAmount(paid)
                .startAt(LocalDateTime.now().minusDays(1))
                .endAt(endAt)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ---------------------------------------------------------------- testlar

    @Nested
    @DisplayName("⚠️ Son va daromad aralashmaydi")
    class CountVersusRevenue {

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV.
         *
         * Ikkita obuna: biri sotilgan, biri sovg'a. Obunachi ikkita,
         * daromad esa faqat bittasidan.
         */
        @Test
        @DisplayName("Sovg'a obuna SANALADI, lekin daromadga kirmaydi")
        void giftCountsButEarnsNothing() throws Exception {
            Tariff t = tariff("oy");
            subscription(t, SubscriptionSource.PURCHASE, new BigDecimal("50000"),
                    LocalDateTime.now().plusDays(20));
            subscription(t, SubscriptionSource.ADMIN_GIFT, null,
                    LocalDateTime.now().plusDays(20));

            mockMvc.perform(get(URL).header("Authorization", token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.byTariff[?(@.tariffId == " + t.getId() + ")].subscribers")
                            .value(2))
                    // BigDecimal JSON'da `50000.0` bo'lib keladi.
                    .andExpect(jsonPath("$.byTariff[?(@.tariffId == " + t.getId() + ")].revenue")
                            .value(50000.0));
        }

        /**
         * ⚠️ Manba kesimida DAROMAD umuman yo'q va bo'lmasligi kerak:
         * «sovg'aning daromadi» degan tushuncha mavjud emas.
         */
        @Test
        @DisplayName("Manba kesimi faqat SON beradi")
        void sourceBreakdownHasNoMoney() throws Exception {
            Tariff t = tariff("oy");
            subscription(t, SubscriptionSource.ADMIN_GIFT, null,
                    LocalDateTime.now().plusDays(20));

            mockMvc.perform(get(URL).header("Authorization", token()))
                    .andExpect(jsonPath("$.bySource[0].total").exists())
                    .andExpect(jsonPath("$.bySource[0].revenue").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Holat bo'yicha sanoq")
    class Counting {

        @Test
        @DisplayName("Faol va muddati o'tgan alohida sanaladi")
        void activeAndExpiredAreSeparate() throws Exception {
            Tariff t = tariff("oy");
            subscription(t, SubscriptionSource.PURCHASE, new BigDecimal("1"),
                    LocalDateTime.now().plusDays(10));
            subscription(t, SubscriptionSource.PURCHASE, new BigDecimal("1"),
                    LocalDateTime.now().minusDays(1));

            mockMvc.perform(get(URL).header("Authorization", token()))
                    .andExpect(jsonPath("$.active").value(1))
                    .andExpect(jsonPath("$.expired").value(1));
        }

        /**
         * ⚠️ Bekor qilingan obuna HECH QAYERGA kirmaydi.
         *
         * Uning muddati hali tugamagan bo'lishi mumkin — shuning
         * uchun faqat sanaga qarash yetarli emas va ikkala shart ham
         * tekshiriladi.
         */
        @Test
        @DisplayName("Bekor qilingan obuna sanalmaydi")
        void revokedIsExcluded() throws Exception {
            Tariff t = tariff("oy");
            Subscription s = subscription(t, SubscriptionSource.PURCHASE,
                    new BigDecimal("50000"), LocalDateTime.now().plusDays(30));
            s.setRevokedAt(LocalDateTime.now());
            subscriptionRepo.save(s);

            mockMvc.perform(get(URL).header("Authorization", token()))
                    .andExpect(jsonPath("$.active").value(0))
                    .andExpect(jsonPath("$.expired").value(0))
                    .andExpect(jsonPath("$.byTariff[?(@.tariffId == " + t.getId() + ")]")
                            .isEmpty());
        }
    }

    @Nested
    @DisplayName("Kunlik yangi obunalar")
    class NewByDay {

        /**
         * ⚠️ Bu SON, daromad emas — shuning uchun sovg'a ham sanaladi.
         *
         * `revenueByDay` boshqa savolga javob beradi. Ikkala savol ham
         * kerak va ular bitta grafikka qo'shilmaydi.
         */
        @Test
        @DisplayName("Sovg'a ham sanaladi — bu SON grafigi")
        void giftIsCountedHere() throws Exception {
            Tariff t = tariff("oy");
            subscription(t, SubscriptionSource.ADMIN_GIFT, null,
                    LocalDateTime.now().plusDays(20));

            mockMvc.perform(get(URL).header("Authorization", token()))
                    .andExpect(jsonPath("$.newByDay").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("Ruxsat")
    class Access {

        @Test
        @DisplayName("Tokensiz — yopiq")
        void anonymousIsRejected() throws Exception {
            mockMvc.perform(get(URL)).andExpect(status().is4xxClientError());
        }
    }
}
