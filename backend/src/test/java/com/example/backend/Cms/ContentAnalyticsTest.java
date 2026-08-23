package com.example.backend.Cms;

import com.example.backend.Admin.TestStaffFactory;
import com.example.backend.Cms.Enums.AnalyticsEventType;
import com.example.backend.Cms.Repository.ContentDailyStatisticRepo;
import com.example.backend.Cms.Service.AnalyticsService;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ТЗ §46 — kontent analitikasi.
 *
 * <h2>Voronka</h2>
 * <pre>
 *   CONTENT_VIEW  →  CONTENT_PLAY  →  CONTENT_COMPLETE
 *   (sahifa ochildi)  (o'ynatildi)     (oxirigacha ko'rildi)
 * </pre>
 *
 * Uchala bosqich alohida ma'noga ega: {@code view → play} pastligi
 * afisha qiziqtirmayotganini, {@code play → complete} pastligi esa
 * kontentning O'ZI ushlab turolmayotganini bildiradi. Ularni bitta
 * «ko'rishlar» soniga qo'shish bu farqni yo'q qilardi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
@Transactional
class ContentAnalyticsTest {

    private static final AtomicLong CONTENT_ID = new AtomicLong(55_000);

    @Autowired private MockMvc mockMvc;
    @Autowired private TestStaffFactory staff;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private ContentDailyStatisticRepo statRepo;

    private String reportToken;

    private String token() {
        if (reportToken == null) {
            reportToken = staff.tokenForRole("+998900004001", PlatformRole.ADMIN,
                    EnumSet.of(Permission.REPORT_VIEW));
        }
        return reportToken;
    }

    private void event(AnalyticsEventType type, Long contentId, String device) {
        analyticsService.record(type, contentId, null, null, device);
    }

    // ------------------------------------------------------------ ingestion

    @Nested
    @DisplayName("Hodisa qabul qilish")
    class Ingestion {

        @Test
        @DisplayName("Uchala hodisa turi ham qabul qilinadi")
        void allThreeEventTypesAreAccepted() throws Exception {
            long id = CONTENT_ID.incrementAndGet();
            String body = """
                    {"deviceKey":"qurilma-1","events":[
                      {"type":"CONTENT_VIEW","targetId":%d},
                      {"type":"CONTENT_PLAY","targetId":%d},
                      {"type":"CONTENT_COMPLETE","targetId":%d}
                    ]}""".formatted(id, id, id);

            mockMvc.perform(post("/api/v1/app/analytics/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("Anonim ham yubora oladi — mobil klient tokensiz bo'lishi mumkin")
        void anonymousCanSendEvents() throws Exception {
            long id = CONTENT_ID.incrementAndGet();
            String body = """
                    {"deviceKey":"anonim-1","events":[
                      {"type":"CONTENT_VIEW","targetId":%d}]}""".formatted(id);

            mockMvc.perform(post("/api/v1/app/analytics/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isAccepted());
        }
    }

    // ---------------------------------------------------------- jamlanma

    @Nested
    @DisplayName("Jamlanma")
    class Aggregation {

        @Test
        @DisplayName("Uchala bosqich ALOHIDA sanaladi")
        void funnelStagesAreCountedSeparately() {
            long id = CONTENT_ID.incrementAndGet();

            // 10 kishi ochdi, 4 tasi o'ynatdi, 1 tasi tugatdi.
            for (int i = 0; i < 10; i++) {
                event(AnalyticsEventType.CONTENT_VIEW, id, "user" + i);
            }
            for (int i = 0; i < 4; i++) {
                event(AnalyticsEventType.CONTENT_PLAY, id, "user" + i);
            }
            event(AnalyticsEventType.CONTENT_COMPLETE, id, "user0");

            analyticsService.aggregate();
            var stat = statRepo.findByContentIdAndStatDate(
                    id, java.time.LocalDate.now()).orElseThrow();

            assertThat(stat.getViews()).isEqualTo(10);
            assertThat(stat.getPlays()).isEqualTo(4);
            assertThat(stat.getCompletes()).isEqualTo(1);
        }

        @Test
        @DisplayName("Tugatish foizi O'YNATISHDAN hisoblanadi, ochishdan emas")
        void completionRateIsBasedOnPlays() {
            long id = CONTENT_ID.incrementAndGet();

            for (int i = 0; i < 100; i++) {
                event(AnalyticsEventType.CONTENT_VIEW, id, "user" + i);
            }
            for (int i = 0; i < 10; i++) {
                event(AnalyticsEventType.CONTENT_PLAY, id, "user" + i);
            }
            for (int i = 0; i < 5; i++) {
                event(AnalyticsEventType.CONTENT_COMPLETE, id, "user" + i);
            }

            analyticsService.aggregate();
            var stat = statRepo.findByContentIdAndStatDate(
                    id, java.time.LocalDate.now()).orElseThrow();

            // 5/10 = 50%, 5/100 = 5% EMAS. Ochib ham o'ynatmaganlar
            // kontentni tugatmagan deb hisoblanmasligi kerak — ular uni
            // umuman boshlamagan.
            assertThat(stat.completionRate()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("O'ynatishsiz tugatish foizi nol — nolga bo'linish yo'q")
        void completionRateWithoutPlaysIsZero() {
            long id = CONTENT_ID.incrementAndGet();
            event(AnalyticsEventType.CONTENT_VIEW, id, "user0");

            analyticsService.aggregate();
            var stat = statRepo.findByContentIdAndStatDate(
                    id, java.time.LocalDate.now()).orElseThrow();

            assertThat(stat.completionRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Unikal tomoshabin QAYTA hisoblanadi, qo'shilmaydi")
        void uniqueViewersAreRecomputed() {
            long id = CONTENT_ID.incrementAndGet();

            event(AnalyticsEventType.CONTENT_VIEW, id, "ali");
            analyticsService.aggregate();

            // 5 daqiqadan keyin Ali yana ochdi — YANGI to'plam.
            event(AnalyticsEventType.CONTENT_VIEW, id, "ali");
            analyticsService.aggregate();

            var stat = statRepo.findByContentIdAndStatDate(
                    id, java.time.LocalDate.now()).orElseThrow();

            // Bir soat tomosha qilgan odam 12 ta «unikal tomoshabin»
            // bo'lib chiqmasligi kerak.
            assertThat(stat.getUniqueViewers()).isEqualTo(1);
            assertThat(stat.getViews()).isEqualTo(2);
        }
    }

    // ------------------------------------------------------------ hisobot

    @Nested
    @DisplayName("Har bir kontent uchun hisobot")
    class PerContentReport {

        @Test
        @DisplayName("Voronkaning uchala bosqichi ham qaytadi")
        void reportReturnsTheWholeFunnel() throws Exception {
            long id = CONTENT_ID.incrementAndGet();
            for (int i = 0; i < 20; i++) {
                event(AnalyticsEventType.CONTENT_VIEW, id, "u" + i);
            }
            for (int i = 0; i < 10; i++) {
                event(AnalyticsEventType.CONTENT_PLAY, id, "u" + i);
            }
            for (int i = 0; i < 3; i++) {
                event(AnalyticsEventType.CONTENT_COMPLETE, id, "u" + i);
            }
            analyticsService.aggregate();

            mockMvc.perform(get("/api/v1/app/admin/reports/content/" + id + "/statistics")
                            .header("Authorization", "Bearer " + token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.views").value(20))
                    .andExpect(jsonPath("$.plays").value(10))
                    .andExpect(jsonPath("$.completes").value(3))
                    // 10/20 = 50%
                    .andExpect(jsonPath("$.playRate").value(50.0))
                    // 3/10 = 30%
                    .andExpect(jsonPath("$.completionRate").value(30.0));
        }

        @Test
        @DisplayName("Ma'lumot yo'q kontent — nol, soxta son emas")
        void unknownContentReturnsEmptyState() throws Exception {
            mockMvc.perform(get("/api/v1/app/admin/reports/content/999999/statistics")
                            .header("Authorization", "Bearer " + token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.views").value(0))
                    .andExpect(jsonPath("$.completionRate").value(0.0))
                    .andExpect(jsonPath("$.daily.length()").value(0));
        }

        @Test
        @DisplayName("Ruxsatsiz xodim ko'ra olmaydi")
        void requiresReportPermission() throws Exception {
            String noReport = staff.tokenForRole("+998900004002", PlatformRole.WORKER,
                    EnumSet.of(Permission.CONTENT_VIEW));

            mockMvc.perform(get("/api/v1/app/admin/reports/content/1/statistics")
                            .header("Authorization", "Bearer " + noReport))
                    .andExpect(status().isForbidden());
        }
    }
}
