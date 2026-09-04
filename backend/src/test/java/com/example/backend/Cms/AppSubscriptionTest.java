package com.example.backend.Cms;

import com.example.backend.Cms.Entity.Subscription;
import com.example.backend.Cms.Entity.Tariff;
import com.example.backend.Cms.Entity.TariffTranslation;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.SubscriptionSource;
import com.example.backend.Cms.Repository.SubscriptionRepo;
import com.example.backend.Cms.Repository.TariffRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
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
 * Tariflar va «Mening obunam» — ilova tomoni.
 *
 * <h2>⚠️ Qaysi bo'shliq to'ldirilyapti</h2>
 * Obuna moduli backendda ham, admin panelda ham to'liq edi: entity'lar,
 * narxlar, hisobotlar, sahifalar. Mobil ilovada esa faqat ikkita maydon
 * bor edi — {@code /app/me} javobidagi {@code premium{active, until}}.
 *
 * Ya'ni odam o'z telefonida obuna sotib ololmasdi, narxlarni ko'ra
 * olmasdi va nima uchun to'laganini ham bilolmasdi.
 *
 * <h2>Nima uchun til alohida tekshiriladi</h2>
 * Tilni noto'g'ri tanlash xatoga o'xshamaydi: ekran ochiladi, ro'yxat
 * chiqadi, hech narsa yiqilmaydi — shunchaki ruscha tanlagan odam
 * o'zbekcha nomlarni ko'radi. Bunday nosozlik testsiz oylab yashaydi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppSubscriptionTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final String TARIFFS = "/api/v1/app/tariffs";
    private static final String MY_SUBSCRIPTION = "/api/v1/app/me/subscription";

    @Autowired private MockMvc mockMvc;
    @Autowired private TariffRepo tariffRepo;
    @Autowired private SubscriptionRepo subscriptionRepo;
    @Autowired private UserAccountRepo accountRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private JwtService jwtService;

    // ------------------------------------------------------------- tariflar

    @Nested
    @DisplayName("Tariflar")
    class Tariffs {

        /**
         * ⚠️ Admin tarifni O'CHIRMAYDI — nofaol qiladi, chunki unga
         * bog'langan obunalar tarixi qoladi. Nofaolini ko'rsatish esa
         * narxi bekor qilingan tarifni sotishga urinish bo'lardi.
         */
        @Test
        @DisplayName("Nofaol tarif ro'yxatga tushmaydi")
        void inactiveIsHidden() throws Exception {
            String hidden = tariff(1, "24000.00", false).getCode();
            String shown = tariff(3, "49999.00", true).getCode();

            String json = body(get(TARIFFS));

            org.assertj.core.api.Assertions.assertThat(json).contains(shown);
            org.assertj.core.api.Assertions.assertThat(json).doesNotContain(hidden);
        }

        @Test
        @DisplayName("So'rovdagi til qo'llanadi")
        void languageFromQuery() throws Exception {
            Tariff t = tariff(1, "24000.00", true);

            mockMvc.perform(get(TARIFFS).param("locale", "RU"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locale").value("RU"))
                    .andExpect(jsonPath("$.tariffs[?(@.code=='" + t.getCode() + "')].name")
                            .value("1 oy RU"));
        }

        /**
         * ⚠️ Nashr paytida uchala til majburiy, lekin eski yozuvlarda
         * kamchilik bo'lishi mumkin. Bo'sh nom qaytarish odamga bo'sh
         * katak ko'rsatardi — o'zbekchasi hech bo'lmasa nimadir aytadi.
         */
        @Test
        @DisplayName("Tarjima yo'q bo'lsa o'zbekchasiga qaytiladi")
        void fallsBackToUzbek() throws Exception {
            Tariff t = tariffRepo.save(Tariff.builder()
                    .code("only-uz-" + SEQ.incrementAndGet())
                    .durationMonths(1).price(new BigDecimal("24000.00"))
                    .currency("UZS").active(true).highlighted(false).sortOrder(900)
                    .build());
            t.addTranslation(TariffTranslation.builder()
                    .locale(Locale.UZ).name("Faqat o'zbekcha").build());
            tariffRepo.save(t);

            mockMvc.perform(get(TARIFFS).param("locale", "EN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffs[?(@.code=='" + t.getCode() + "')].name")
                            .value("Faqat o'zbekcha"));
        }

        /**
         * ⚠️ {@code locale} parametrining sukut qiymati bo'lmasligi kerak:
         * {@code defaultValue = "UZ"} qo'yilsa, parametr yuborilmagan
         * holat «o'zbekcha so'raldi» dan farq qilmasdi va profildagi
         * til hech qachon o'qilmasdi.
         */
        @Test
        @DisplayName("Parametr bo'lmasa profildagi til olinadi")
        void languageFromProfile() throws Exception {
            User u = user();
            accountRepo.save(UserAccount.builder()
                    .user(u).language(Locale.RU).createdAt(LocalDateTime.now()).build());
            Tariff t = tariff(1, "24000.00", true);

            mockMvc.perform(get(TARIFFS).header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locale").value("RU"))
                    .andExpect(jsonPath("$.tariffs[?(@.code=='" + t.getCode() + "')].name")
                            .value("1 oy RU"));
        }

        /**
         * ⚠️ Narx — hisobga kirishdan OLDIN ko'riladigan narsa. Yopiq
         * bo'lsa, mehmon nimaga pul to'lashini bilmasdan ro'yxatdan
         * o'tishi kerak bo'lardi.
         */
        @Test
        @DisplayName("Mehmon narxlarni ko'ra oladi")
        void guestCanRead() throws Exception {
            tariff(1, "24000.00", true);

            mockMvc.perform(get(TARIFFS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locale").value("UZ"));
        }

        /**
         * Ustunda bu bitta matn. Ajratishni klientga qoldirish uchta
         * klientda uchta ajratish qoidasi degani edi — va ular albatta
         * farq qilardi.
         */
        @Test
        @DisplayName("Imkoniyatlar tayyor ro'yxat bo'lib keladi")
        void featuresAreSplit() throws Exception {
            Tariff t = tariffRepo.save(Tariff.builder()
                    .code("feat-" + SEQ.incrementAndGet())
                    .durationMonths(1).price(new BigDecimal("24000.00"))
                    .currency("UZS").active(true).highlighted(false).sortOrder(901)
                    .build());
            // Oxiridagi bo'sh qator — admin tasodifan enter bosgan hol.
            t.addTranslation(TariffTranslation.builder()
                    .locale(Locale.UZ).name("Test")
                    .features("4K sifat\nReklamasiz\n\n").build());
            tariffRepo.save(t);

            mockMvc.perform(get(TARIFFS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffs[?(@.code=='" + t.getCode() + "')].features[0]")
                            .value("4K sifat"))
                    // Bo'sh qator tashlanadi — aks holda ro'yxatda
                    // osilgan bo'sh element paydo bo'lardi.
                    .andExpect(jsonPath("$.tariffs[?(@.code=='" + t.getCode() + "')].features.length()")
                            .value(2));
        }

        /** «Oyiga atigi X so'm» yozuvi uchun — hisobni server qiladi. */
        @Test
        @DisplayName("Oylik narx hisoblanadi")
        void monthlyPriceIsComputed() throws Exception {
            Tariff t = tariff(3, "49500.00", true);

            mockMvc.perform(get(TARIFFS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffs[?(@.code=='" + t.getCode() + "')].monthlyPrice")
                            .value(16500));
        }
    }

    // ---------------------------------------------------------- mening obunam

    @Nested
    @DisplayName("Mening obunam")
    class Mine {

        @Test
        @DisplayName("Token yo'q bo'lsa 401")
        void anonymousRejected() throws Exception {
            mockMvc.perform(get(MY_SUBSCRIPTION))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Faol obuna active=true beradi")
        void activeSubscription() throws Exception {
            User u = user();
            LocalDateTime until = LocalDateTime.now().plusMonths(1);
            accountRepo.save(UserAccount.builder()
                    .user(u).premiumUntil(until).createdAt(LocalDateTime.now()).build());

            mockMvc.perform(get(MY_SUBSCRIPTION).header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.until").isNotEmpty());
        }

        /**
         * ⚠️ «Tugagan» va «hech qachon bo'lmagan» — odam uchun ikki
         * boshqa narsa: birinchisida u pul to'lagan va uzaytirishi
         * mumkin. Sana shuning uchun saqlanadi.
         */
        @Test
        @DisplayName("Muddati o'tganda sana saqlanadi")
        void expiredKeepsTheDate() throws Exception {
            User u = user();
            accountRepo.save(UserAccount.builder()
                    .user(u).premiumUntil(LocalDateTime.now().minusDays(3))
                    .createdAt(LocalDateTime.now()).build());

            mockMvc.perform(get(MY_SUBSCRIPTION).header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false))
                    .andExpect(jsonPath("$.until").isNotEmpty());
        }

        @Test
        @DisplayName("Obuna umuman bo'lmaganda sana null")
        void neverSubscribed() throws Exception {
            mockMvc.perform(get(MY_SUBSCRIPTION).header("Authorization", token(user())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false))
                    .andExpect(jsonPath("$.until").doesNotExist())
                    .andExpect(jsonPath("$.history.length()").value(0));
        }

        /**
         * ⚠️ Sovg'ada summa {@code null}, 0 emas: nol «bepul sotib
         * olindi» degan ma'no berardi va hisobotda daromad qatoriga
         * tushardi.
         */
        @Test
        @DisplayName("Sovg'a obunasida summa yo'q")
        void giftHasNoAmount() throws Exception {
            User u = user();
            subscription(u, tariff(1, "24000.00", true), SubscriptionSource.ADMIN_GIFT, null);

            mockMvc.perform(get(MY_SUBSCRIPTION).header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.history.length()").value(1))
                    .andExpect(jsonPath("$.history[0].source").value("ADMIN_GIFT"))
                    .andExpect(jsonPath("$.history[0].paidAmount").doesNotExist());
        }

        @Test
        @DisplayName("Xarid summasi va tarif nomi qaytadi")
        void purchaseShowsAmountAndName() throws Exception {
            User u = user();
            subscription(u, tariff(1, "24000.00", true),
                    SubscriptionSource.PURCHASE, new BigDecimal("24000.00"));

            mockMvc.perform(get(MY_SUBSCRIPTION)
                            .param("locale", "RU")
                            .header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.history[0].paidAmount").value(24000.00))
                    .andExpect(jsonPath("$.history[0].tariffName").value("1 oy RU"))
                    .andExpect(jsonPath("$.history[0].currency").value("UZS"));
        }

        /** Id parametrda emas — tokenda. Boshqaning tarixi ko'rinmaydi. */
        @Test
        @DisplayName("Tarix faqat o'ziniki")
        void historyIsPerUser() throws Exception {
            User owner = user();
            subscription(owner, tariff(1, "24000.00", true),
                    SubscriptionSource.PURCHASE, new BigDecimal("24000.00"));

            mockMvc.perform(get(MY_SUBSCRIPTION).header("Authorization", token(user())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.history.length()").value(0));
        }

        /**
         * Muddatidan oldin tortib olingan obuna tarixda QOLADI (§42,
         * §58) — lekin faol emas. Yozuvni yashirish nizoli holatda
         * «men to'laganman» degan odamga javob bermasdi.
         */
        @Test
        @DisplayName("Tortib olingan obuna tarixda qoladi, lekin faol emas")
        void revokedStaysInHistory() throws Exception {
            User u = user();
            Subscription s = subscription(u, tariff(1, "24000.00", true),
                    SubscriptionSource.PURCHASE, new BigDecimal("24000.00"));
            s.setRevokedAt(LocalDateTime.now());
            subscriptionRepo.save(s);

            mockMvc.perform(get(MY_SUBSCRIPTION).header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.history.length()").value(1))
                    .andExpect(jsonPath("$.history[0].active").value(false))
                    .andExpect(jsonPath("$.history[0].revokedAt").isNotEmpty());
        }
    }

    // ---------------------------------------------------------- yordamchi

    private String body(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b)
            throws Exception {
        return mockMvc.perform(b)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private Tariff tariff(int months, String price, boolean active) {
        Tariff t = tariffRepo.save(Tariff.builder()
                .code("t" + SEQ.incrementAndGet())
                .durationMonths(months)
                .price(new BigDecimal(price))
                .currency("UZS")
                .active(active)
                .highlighted(false)
                .sortOrder(500 + SEQ.get())
                .build());

        for (Locale l : List.of(Locale.UZ, Locale.RU, Locale.EN)) {
            t.addTranslation(TariffTranslation.builder()
                    .locale(l).name(months + " oy " + l.name()).build());
        }
        return tariffRepo.save(t);
    }

    private Subscription subscription(User u, Tariff t, SubscriptionSource source,
                                      BigDecimal paid) {
        return subscriptionRepo.save(Subscription.builder()
                .user(u).tariff(t)
                .startAt(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusMonths(1))
                .source(source)
                .paidAmount(paid)
                .createdAt(LocalDateTime.now())
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
        u.setPhone("+99890" + (9100000 + n));
        u.setPassword("xesh-" + n);
        u.setName("Obunachi " + n);
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    private String token(User u) {
        return "Bearer " + jwtService.generateJwtToken(u);
    }
}
