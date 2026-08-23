package com.example.backend.Cms;

import com.example.backend.Admin.Dto.TariffSaveRequest;
import com.example.backend.Admin.Dto.TariffTextDto;
import com.example.backend.Cms.Entity.Tariff;
import com.example.backend.Cms.Entity.TariffTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.TariffRepo;
import com.example.backend.Cms.Service.MonetizationService;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §36 — premium tariflar.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TariffModuleTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MonetizationService monetizationService;
    @Autowired private UserAdminService userAdminService;
    @Autowired private TariffRepo tariffRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    // -------------------------------------------------------------- pul

    @Nested
    @DisplayName("Pul qiymati")
    class Money {

        @Test
        @DisplayName("⚠️ Narx BigDecimal — floating point EMAS")
        void priceIsBigDecimal() throws NoSuchFieldException {
            Field price = Tariff.class.getDeclaredField("price");

            // Buyurtmachi talabi. double bilan 0.1 + 0.2 != 0.3 va pul
            // hisobida bu sekin-asta yig'ilib boradigan xatoga aylanadi.
            assertThat(price.getType()).isEqualTo(BigDecimal.class);
        }

        @Test
        @DisplayName("Tiyinlar yo'qolmaydi")
        void fractionalAmountSurvivesRoundTrip() {
            TariffSaveRequest r = request(3, new BigDecimal("49999.99"));

            Tariff saved = monetizationService.saveTariff(null, null, r);

            assertThat(saved.getPrice()).isEqualByComparingTo("49999.99");
        }

        @Test
        @DisplayName("Manfiy narx rad etiladi")
        void negativePriceIsRejected() {
            TariffSaveRequest r = request(1, new BigDecimal("-1"));

            assertThatThrownBy(() -> monetizationService.saveTariff(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("manfiy");
        }
    }

    // ----------------------------------------------------------- maydonlar

    @Nested
    @DisplayName("Maydonlar")
    class Fields {

        @Test
        @DisplayName("⚠️ Tavsif va imkoniyatlar ALOHIDA saqlanadi")
        void descriptionAndFeaturesAreSeparate() {
            TariffSaveRequest r = request(6, new BigDecimal("99000.00"));
            r.getTranslations().get(Locale.UZ).setDescription("Oilaviy tomosha uchun");
            r.getTranslations().get(Locale.UZ).setFeatures("4K\n4 qurilma\nreklamasiz");

            Tariff saved = monetizationService.saveTariff(null, null, r);
            TariffTranslation uz = saved.getTranslations().stream()
                    .filter(t -> t.getLocale() == Locale.UZ).findFirst().orElseThrow();

            // Ilgari umumiy TranslationDto ishlatilardi va uning
            // `description` maydoni `features` ustuniga yozilardi — ya'ni
            // ТЗ dagi ikki tushuncha bitta katakka qo'shib yuborilgan edi.
            assertThat(uz.getDescription()).isEqualTo("Oilaviy tomosha uchun");
            assertThat(uz.getFeatures()).isEqualTo("4K\n4 qurilma\nreklamasiz");
        }

        @Test
        @DisplayName("Bejak ixtiyoriy")
        void badgeIsOptional() {
            Tariff saved = monetizationService.saveTariff(null, null,
                    request(1, new BigDecimal("24000.00")));

            assertThat(saved.getTranslations()).allSatisfy(t ->
                    assertThat(t.getName()).isNotBlank());
        }

        @Test
        @DisplayName("Faol tarif nomi uch tilda majburiy")
        void activeTariffNeedsAllThreeNames() {
            TariffSaveRequest r = request(1, new BigDecimal("24000.00"));
            r.getTranslations().remove(Locale.RU);
            r.getTranslations().remove(Locale.EN);

            // Tarif foydalanuvchiga ko'rinadi va u PUL to'laydi.
            assertThatThrownBy(() -> monetizationService.saveTariff(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RU");
        }

        @Test
        @DisplayName("Nofaol tarif uchun o'zbekcha yetarli")
        void inactiveTariffNeedsOnlyBaseLanguage() {
            TariffSaveRequest r = request(1, new BigDecimal("24000.00"));
            r.setActive(false);
            r.getTranslations().remove(Locale.RU);
            r.getTranslations().remove(Locale.EN);

            assertThat(monetizationService.saveTariff(null, null, r).getActive()).isFalse();
        }
    }

    // -------------------------------------------------------------- seed

    @Nested
    @DisplayName("Boshlang'ich narxlar")
    class Seed {

        @Test
        @DisplayName("ТЗ dagi to'rt tarif migratsiyada bor")
        void seededTariffsMatchTheSpec() {
            Map<Integer, String> expected = Map.of(
                    1, "24000.00", 3, "49999.00", 6, "99000.00", 12, "159900.00");

            for (Map.Entry<Integer, String> e : expected.entrySet()) {
                Tariff t = tariffRepo.findAll().stream()
                        .filter(x -> e.getKey().equals(x.getDurationMonths()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(
                                e.getKey() + " oylik tarif seedda yo'q"));
                assertThat(t.getPrice()).isEqualByComparingTo(e.getValue());
            }
        }

        @Test
        @DisplayName("⚠️ Seeddan keyin YANGI tarif yaratish ishlaydi (V19)")
        void newTariffCanBeCreatedAfterSeed() {
            // V5 tariflarni ANIQ ID bilan qo'shadi (1-4), lekin
            // ketma-ketlikni oldinga surmaydi — u hamon 1 dan boshlaydi.
            // Natijada admin panelida BIRINCHI yangi tarif yaratish
            // `duplicate key` bilan yiqilardi va §36 ning asosiy talabi
            // («admin panel orqali o'zgartirilishi shart») buzilardi.
            Tariff created = monetizationService.saveTariff(null, null,
                    request(2, new BigDecimal("39000.00")));

            assertThat(created.getId()).isNotNull();
            assertThat(tariffRepo.findById(created.getId())).isPresent();
        }

        @Test
        @DisplayName("Valyuta paketi ham yaratiladi — u ham seed qilingan")
        void newCurrencyPackageCanBeCreated() {
            var req = new com.example.backend.Admin.Dto.CurrencyPackageSaveRequest();
            req.setKind(com.example.backend.Cms.Enums.CurrencyKind.STARS);
            req.setAmount(25L);
            req.setPrice(new BigDecimal("5000.00"));
            req.setActive(true);
            req.setSortOrder(9);

            var created = monetizationService.savePackage(null, null, req);

            assertThat(created.getId()).isNotNull();
        }

        @Test
        @DisplayName("Yangi tarif tarjimalari ham saqlanadi")
        void newTariffTranslationsArePersisted() {
            // cms_tariff_translation ham aniq ID bilan seed qilingan —
            // uchinchi ta'sirlangan jadval.
            Tariff created = monetizationService.saveTariff(null, null,
                    request(4, new BigDecimal("69000.00")));

            assertThat(created.getTranslations()).hasSize(3);
        }

        @Test
        @DisplayName("⚠️ Narx KODDA emas — admin o'zgartira oladi")
        void priceIsNotHardcoded() {
            Tariff monthly = tariffRepo.findAll().stream()
                    .filter(t -> Integer.valueOf(1).equals(t.getDurationMonths()))
                    .findFirst().orElseThrow();

            TariffSaveRequest r = request(1, new BigDecimal("29000.00"));
            monetizationService.saveTariff(null, monthly.getId(), r);

            assertThat(tariffRepo.findById(monthly.getId()).orElseThrow().getPrice())
                    .isEqualByComparingTo("29000.00");
        }
    }

    // ------------------------------------------------------------ muddat

    @Nested
    @DisplayName("Tarif muddati")
    class Duration {

        @Test
        @DisplayName("⚠️ Tarif tanlansa MUDDAT O'SHANDAN olinadi")
        void tariffDurationWins() {
            Tariff yearly = tariffRepo.findAll().stream()
                    .filter(t -> Integer.valueOf(12).equals(t.getDurationMonths()))
                    .findFirst().orElseThrow();
            User u = appUser();

            // Admin 12 oylik tarifni tanladi, lekin months=1 yubordi.
            var account = userAdminService.grantPremium(null, u.getId(), 1, yearly.getId());

            // Ilgari tarifning durationMonths maydoni umuman o'qilmasdi —
            // u bezak edi. Foydalanuvchi 1 oy olardi, obuna yozuvida esa
            // 12 oylik tarif turardi va hisobot yolg'on ko'rsatardi.
            long months = ChronoUnit.MONTHS.between(
                    LocalDateTime.now(), account.getPremiumUntil());
            assertThat(months).isGreaterThanOrEqualTo(11);
        }

        @Test
        @DisplayName("Tarifsiz sovg'ada months ishlaydi")
        void monthsWorkWithoutTariff() {
            User u = appUser();

            var account = userAdminService.grantPremium(null, u.getId(), 2, null);

            long months = ChronoUnit.MONTHS.between(
                    LocalDateTime.now(), account.getPremiumUntil());
            assertThat(months).isGreaterThanOrEqualTo(1).isLessThan(3);
        }

        @Test
        @DisplayName("Tarif ham, muddat ham yo'q — aniq xato")
        void neitherTariffNorMonthsIsRejected() {
            User u = appUser();

            assertThatThrownBy(() -> userAdminService.grantPremium(null, u.getId(), null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Muddat");
        }

        @Test
        @DisplayName("Mavjud bo'lmagan tarif rad etiladi")
        void unknownTariffIsRejected() {
            User u = appUser();

            // Ilgari `orElse(null)` edi: noto'g'ri ID jimgina e'tiborsiz
            // qolardi va obuna tarifsiz yozilardi.
            assertThatThrownBy(() -> userAdminService.grantPremium(null, u.getId(), 1, 999_999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private TariffSaveRequest request(int months, BigDecimal price) {
        TariffSaveRequest r = new TariffSaveRequest();
        r.setCode("t" + SEQ.incrementAndGet());
        r.setDurationMonths(months);
        r.setPrice(price);
        r.setCurrency("UZS");
        r.setActive(true);
        Map<Locale, TariffTextDto> tr = new LinkedHashMap<>();
        for (Locale l : List.of(Locale.UZ, Locale.RU, Locale.EN)) {
            tr.put(l, TariffTextDto.builder().name(months + " oy " + l.name()).build());
        }
        r.setTranslations(tr);
        return r;
    }

    private User appUser() {
        Role r = roleRepo.findByName(UserRoles.ROLE_USER);
        if (r == null) {
            int nextId = roleRepo.findAll().stream()
                    .mapToInt(Role::getId).max().orElse(0) + 1;
            r = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99890" + (9300000 + SEQ.incrementAndGet()));
        u.setPassword("x");
        u.setName("Tarif " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(r)));
        return userRepo.save(u);
    }
}
