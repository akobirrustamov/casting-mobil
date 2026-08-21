package com.example.backend.Cms;

import com.example.backend.Cms.Entity.AdDailyStatistic;
import com.example.backend.Cms.Entity.AnalyticsEvent;
import com.example.backend.Cms.Enums.AnalyticsEventType;
import com.example.backend.Cms.Repository.AdDailyStatisticRepo;
import com.example.backend.Cms.Repository.AnalyticsEventRepo;
import com.example.backend.Cms.Service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §29 — reklama analitikasi.
 *
 * <h2>Nima tekshiriladi</h2>
 * Beshta ko'rsatkich to'g'ri hisoblanadimi: impressions, clicks,
 * unique impressions, unique clicks, CTR.
 *
 * <h2>Topilgan xato</h2>
 * Unikal sanoq har agregatsiya to'plamida QO'SHIB borilardi. Agregatsiya
 * har 5 daqiqada ishlaydi, ya'ni bir kunda reklamani uch marta ko'rgan
 * odam 3 ta «unikal» bo'lib hisoblanardi va ko'rsatkich asta-sekin
 * JAMI ga yaqinlashib ma'nosini yo'qotardi.
 *
 * Endi unikal butun kun bo'yicha qayta hisoblanadi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdAnalyticsTest {

    private static final AtomicLong AD_ID = new AtomicLong(9000);

    @Autowired private AnalyticsService analyticsService;
    @Autowired private AnalyticsEventRepo eventRepo;
    @Autowired private AdDailyStatisticRepo adStatRepo;

    /** Hodisa yozadi. {@code userKey} — kim ko'rgani. */
    private void event(AnalyticsEventType type, Long adId, String userKey, LocalDate day) {
        eventRepo.save(AnalyticsEvent.builder()
                .type(type)
                .targetId(adId)
                .deviceKey(userKey)
                .eventDate(day)
                .processed(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private AdDailyStatistic statOf(Long adId, LocalDate day) {
        return adStatRepo.findByAdvertisementIdAndStatDate(adId, day).orElseThrow();
    }

    // ------------------------------------------------------ ko'rsatkichlar

    @Nested
    @DisplayName("Beshta ko'rsatkich")
    class Metrics {

        @Test
        @DisplayName("Jami va unikal alohida hisoblanadi")
        void totalsAndUniquesAreSeparate() {
            Long ad = AD_ID.incrementAndGet();
            LocalDate day = LocalDate.now();

            // Ali reklamani ikki marta ko'rdi, Vali bir marta.
            event(AnalyticsEventType.AD_IMPRESSION, ad, "ali", day);
            event(AnalyticsEventType.AD_IMPRESSION, ad, "ali", day);
            event(AnalyticsEventType.AD_IMPRESSION, ad, "vali", day);
            // Faqat Ali bosdi.
            event(AnalyticsEventType.AD_CLICK, ad, "ali", day);

            analyticsService.aggregate();
            AdDailyStatistic stat = statOf(ad, day);

            assertThat(stat.getImpressions()).isEqualTo(3);
            assertThat(stat.getUniqueImpressions())
                    .as("Ali ikki marta ko'rdi, lekin u BITTA odam")
                    .isEqualTo(2);
            assertThat(stat.getClicks()).isEqualTo(1);
            assertThat(stat.getUniqueClicks()).isEqualTo(1);
        }

        @Test
        @DisplayName("CTR = bosishlar / ko'rsatishlar")
        void ctrIsComputed() {
            Long ad = AD_ID.incrementAndGet();
            LocalDate day = LocalDate.now();

            for (int i = 0; i < 10; i++) {
                event(AnalyticsEventType.AD_IMPRESSION, ad, "user" + i, day);
            }
            event(AnalyticsEventType.AD_CLICK, ad, "user0", day);
            event(AnalyticsEventType.AD_CLICK, ad, "user1", day);

            analyticsService.aggregate();

            // 2 / 10 = 20%
            assertThat(statOf(ad, day).ctr()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("Ko'rsatishsiz CTR — nolga bo'linish yo'q")
        void ctrWithoutImpressionsIsZero() {
            assertThat(AdDailyStatistic.builder().build().ctr()).isEqualTo(0.0);
        }
    }

    // -------------------------------------------------- takroriy agregatsiya

    @Nested
    @DisplayName("Takroriy agregatsiya")
    class RepeatedAggregation {

        @Test
        @DisplayName("Bir odam ikki to'plamda ko'rsa ham UNIKAL bittaligicha qoladi")
        void uniquesDoNotAccumulateAcrossBatches() {
            Long ad = AD_ID.incrementAndGet();
            LocalDate day = LocalDate.now();

            // 1-to'plam: Ali ko'rdi.
            event(AnalyticsEventType.AD_IMPRESSION, ad, "ali", day);
            analyticsService.aggregate();
            assertThat(statOf(ad, day).getUniqueImpressions()).isEqualTo(1);

            // 5 daqiqadan keyin Ali yana ko'rdi - YANGI to'plam.
            event(AnalyticsEventType.AD_IMPRESSION, ad, "ali", day);
            analyticsService.aggregate();

            AdDailyStatistic stat = statOf(ad, day);

            // ⚠️ Aynan shu yerda xato bor edi: unikal ham qo'shilib 2 bo'lardi.
            // Agregatsiya har 5 daqiqada ishlaydi, ya'ni bir soat ko'rgan
            // foydalanuvchi 12 ta "unikal" bo'lib chiqardi.
            assertThat(stat.getUniqueImpressions())
                    .as("Ali bitta odam - nechа to'plamda uchrashidan qat'i nazar")
                    .isEqualTo(1);
            assertThat(stat.getImpressions())
                    .as("JAMI esa to'g'ri qo'shiladi")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("Yangi odam qo'shilsa unikal o'sadi")
        void newUserIncreasesUniques() {
            Long ad = AD_ID.incrementAndGet();
            LocalDate day = LocalDate.now();

            event(AnalyticsEventType.AD_IMPRESSION, ad, "ali", day);
            analyticsService.aggregate();

            event(AnalyticsEventType.AD_IMPRESSION, ad, "vali", day);
            analyticsService.aggregate();

            assertThat(statOf(ad, day).getUniqueImpressions()).isEqualTo(2);
        }

        @Test
        @DisplayName("Har kun alohida hisoblanadi")
        void daysAreCountedSeparately() {
            Long ad = AD_ID.incrementAndGet();
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            event(AnalyticsEventType.AD_IMPRESSION, ad, "ali", yesterday);
            event(AnalyticsEventType.AD_IMPRESSION, ad, "ali", today);
            analyticsService.aggregate();

            // Bir odam ikki kun ko'rdi - har kunda 1 ta unikal (D25).
            assertThat(statOf(ad, yesterday).getUniqueImpressions()).isEqualTo(1);
            assertThat(statOf(ad, today).getUniqueImpressions()).isEqualTo(1);
        }
    }

    // ------------------------------------------------------------ manba

    @Nested
    @DisplayName("Dashboard xom hodisalarni skanerlamaydi")
    class NoRawScan {

        @Test
        @DisplayName("Statistika kunlik jamlanmadan o'qiladi")
        void statisticsComeFromDailyAggregate() {
            Long ad = AD_ID.incrementAndGet();
            LocalDate day = LocalDate.now();

            for (int i = 0; i < 50; i++) {
                event(AnalyticsEventType.AD_IMPRESSION, ad, "user" + i, day);
            }
            analyticsService.aggregate();

            // Jamlanma qatori 50 ta hodisadan BITTA qator yasaydi.
            // Dashboard shu qatorni o'qiydi, 50 ta hodisani emas (ТЗ §29).
            assertThat(adStatRepo.findAllByAdvertisementIdAndStatDateBetweenOrderByStatDateAsc(
                    ad, day, day)).hasSize(1);
            assertThat(statOf(ad, day).getImpressions()).isEqualTo(50);
        }

        @Test
        @DisplayName("Qayta ishlangan hodisa ikkinchi marta hisoblanmaydi")
        void processedEventsAreNotCountedTwice() {
            Long ad = AD_ID.incrementAndGet();
            LocalDate day = LocalDate.now();

            event(AnalyticsEventType.AD_IMPRESSION, ad, "ali", day);
            analyticsService.aggregate();
            analyticsService.aggregate();   // hodisasiz ikkinchi urinish

            assertThat(statOf(ad, day).getImpressions()).isEqualTo(1);
        }
    }
}
