package com.example.backend.Cms;

import com.example.backend.Cms.Entity.CurrencyPackage;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Repository.CurrencyPackageRepo;
import com.example.backend.Cms.Service.CurrencyPricingService;
import com.example.backend.Cms.Service.MonetizationService;
import com.example.backend.Cms.Service.SettingKeys;
import com.example.backend.Cms.Service.SettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §40 va §41 — yulduz va tanga narxi.
 *
 * <h2>Asosiy masala</h2>
 * ТЗ: «1 STAR = X UZS admin panel orqali boshqarilishi kerak.»
 *
 * Sozlama bor edi va admin uni tahrirlay ham olardi — lekin uni HECH
 * QAYERDA o'qilmasdi. Ya'ni admin qiymatni o'zgartirardi va hech narsa
 * o'zgarmasdi. Bu «ishlaydi» deb hisoblanadigan, aslida esa ishlamaydigan
 * xatolar turkumi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CurrencyPricingTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private CurrencyPricingService pricingService;
    @Autowired private MonetizationService monetizationService;
    @Autowired private SettingsService settingsService;
    @Autowired private CurrencyPackageRepo packageRepo;

    // ---------------------------------------------------------------- seed

    @Nested
    @DisplayName("Boshlang'ich paketlar")
    class Seed {

        @Test
        @DisplayName("ТЗ dagi beshta qiymat seedda bor")
        void seededAmountsMatchTheSpec() {
            List<Long> stars = packageRepo.findAll().stream()
                    .filter(p -> p.getKind() == CurrencyKind.STARS)
                    .map(CurrencyPackage::getAmount)
                    .sorted()
                    .toList();

            assertThat(stars).containsExactly(10L, 50L, 100L, 500L, 1000L);
        }

        @Test
        @DisplayName("Tanga paketlari ham xuddi shunday")
        void coinPackagesMatchToo() {
            List<Long> coins = packageRepo.findAll().stream()
                    .filter(p -> p.getKind() == CurrencyKind.UZCASTING_COIN)
                    .map(CurrencyPackage::getAmount)
                    .sorted()
                    .toList();

            assertThat(coins).containsExactly(10L, 50L, 100L, 500L, 1000L);
        }
    }

    // ----------------------------------------------------------------- kurs

    @Nested
    @DisplayName("Kurs admin panel orqali boshqariladi")
    class Rate {

        @Test
        @DisplayName("⚠️ Kurs belgilanmagan — narx NOMA'LUM, nol emas")
        void withoutRatePriceIsUnknown() {
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);

            // Nol narx «bepul» degani. Aslida narx shunchaki hali
            // belgilanmagan — buyurtmachi kursni aytmagan.
            assertThat(pricingService.effectivePrice(pack)).isNull();
            assertThat(pricingService.isPurchasable(pack)).isFalse();
        }

        @Test
        @DisplayName("⚠️ Kurs o'rnatilsa narx HAQIQATAN o'zgaradi")
        void settingTheRateChangesThePrice() {
            CurrencyPackage pack = pack(CurrencyKind.STARS, 100L, BigDecimal.ZERO);
            assertThat(pricingService.effectivePrice(pack)).isNull();

            settingsService.update(null, SettingKeys.STAR_RATE, "500");

            // Ilgari kurs hech qayerda o'qilmasdi: admin uni o'zgartirardi
            // va hech narsa o'zgarmasdi.
            assertThat(pricingService.effectivePrice(pack))
                    .isEqualByComparingTo("50000");
            assertThat(pricingService.isPurchasable(pack)).isTrue();
        }

        @Test
        @DisplayName("Yulduz va tanga kursi ALOHIDA")
        void starAndCoinRatesAreIndependent() {
            settingsService.update(null, SettingKeys.STAR_RATE, "500");
            settingsService.update(null, SettingKeys.COIN_RATE, "100");

            CurrencyPackage stars = pack(CurrencyKind.STARS, 10L, BigDecimal.ZERO);
            CurrencyPackage coins = pack(CurrencyKind.UZCASTING_COIN, 10L, BigDecimal.ZERO);

            assertThat(pricingService.effectivePrice(stars)).isEqualByComparingTo("5000");
            assertThat(pricingService.effectivePrice(coins)).isEqualByComparingTo("1000");
        }

        @Test
        @DisplayName("Paketning O'Z narxi kursdan ustun — chegirma uchun")
        void ownPriceWinsOverRate() {
            settingsService.update(null, SettingKeys.STAR_RATE, "500");
            // 1000 ta yulduz kurs bo'yicha 500 000 bo'lardi, lekin
            // paketda chegirma bor.
            CurrencyPackage pack = pack(CurrencyKind.STARS, 1000L, new BigDecimal("400000.00"));

            assertThat(pricingService.effectivePrice(pack))
                    .as("Paketlarda chegirma bo'ladi, buni kurs bilan "
                            + "ifodalab bo'lmaydi")
                    .isEqualByComparingTo("400000.00");
        }

        @Test
        @DisplayName("Kurs kasr bo'lishi mumkin")
        void rateCanBeFractional() {
            settingsService.update(null, SettingKeys.STAR_RATE, "1250.50");
            CurrencyPackage pack = pack(CurrencyKind.STARS, 10L, BigDecimal.ZERO);

            // Pul BigDecimal — tiyinlar yo'qolmaydi.
            assertThat(pricingService.effectivePrice(pack))
                    .isEqualByComparingTo("12505.00");
        }
    }

    // ------------------------------------------------------------ ro'yxat

    @Nested
    @DisplayName("Ochiq ro'yxat")
    class PublicList {

        @Test
        @DisplayName("⚠️ Narxsiz paket «bepul» bo'lib ko'rinmaydi")
        void unpricedPackagesAreNotOfferedAsFree() {
            // V5 barcha paketlarni 0.00 narx bilan va active = true qilib
            // qo'shgan. Bayroqsiz ular ro'yxatda «1000 yulduz — 0 so'm»
            // bo'lib chiqardi.
            long purchasable = monetizationService.packages().stream()
                    .filter(p -> !Boolean.FALSE.equals(p.getActive()))
                    .filter(pricingService::isPurchasable)
                    .count();

            assertThat(purchasable)
                    .as("Kurs belgilanmagan — sotib olinadigan paket bo'lmasligi kerak")
                    .isZero();
        }

        @Test
        @DisplayName("Kurs belgilangach paketlar sotuvga chiqadi")
        void packagesBecomePurchasableOnceRateIsSet() {
            settingsService.update(null, SettingKeys.STAR_RATE, "500");

            long purchasable = monetizationService.packages().stream()
                    .filter(p -> p.getKind() == CurrencyKind.STARS)
                    .filter(p -> !Boolean.FALSE.equals(p.getActive()))
                    .filter(pricingService::isPurchasable)
                    .count();

            assertThat(purchasable).isEqualTo(5);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private CurrencyPackage pack(CurrencyKind kind, long amount, BigDecimal price) {
        return packageRepo.save(CurrencyPackage.builder()
                .kind(kind)
                .amount(amount)
                .price(price)
                .active(true)
                .sortOrder(SEQ.incrementAndGet())
                .build());
    }
}
