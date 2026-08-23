package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.TaxonomySaveRequest;
import com.example.backend.Admin.TestStaffFactory;
import com.example.backend.Cms.Entity.Category;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Service.AnalyticsService;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.TaxonomyService;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.support.Translations;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ТЗ §47 — hisobot filtrlari.
 *
 * <h2>Davr</h2>
 * today · yesterday · last7 · last30 · maxsus davr.
 *
 * <h2>Obyekt</h2>
 * kontent · kategoriya · ijodkor · tarif · reklama.
 *
 * <h2>Xato kodi</h2>
 * Loyiha konvensiyasi: validatsiya xatosi — <b>422</b>, 400 emas.
 * 400 «so'rovni umuman o'qib bo'lmadi» degani, bu yerda esa so'rov
 * tushunarli, lekin qiymat noto'g'ri.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
@Transactional
class ReportFiltersTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestStaffFactory staff;
    @Autowired private ContentService contentService;
    @Autowired private TaxonomyService taxonomyService;
    @Autowired private AnalyticsService analyticsService;

    private String token;

    private String token() {
        if (token == null) {
            token = staff.tokenForRole("+998900005001", PlatformRole.ADMIN,
                    EnumSet.of(Permission.REPORT_VIEW));
        }
        return token;
    }

    private JsonNode report(String query) throws Exception {
        String body = mockMvc.perform(get("/api/v1/app/admin/reports/overview" + query)
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    // --------------------------------------------------------------- davr

    @Nested
    @DisplayName("Davr filtri")
    class Period {

        @Test
        @DisplayName("Beshta davr ham qo'llab-quvvatlanadi")
        void allFivePeriodsWork() throws Exception {
            for (String p : new String[]{"today", "yesterday", "last7", "last30"}) {
                JsonNode r = report("?period=" + p);
                assertThat(r.get("from").asText()).isNotBlank();
                assertThat(r.get("to").asText()).isNotBlank();
            }
            // Maxsus davr
            JsonNode custom = report("?from=2026-01-01&to=2026-01-31");
            assertThat(custom.get("from").asText()).isEqualTo("2026-01-01");
            assertThat(custom.get("to").asText()).isEqualTo("2026-01-31");
        }

        @Test
        @DisplayName("⚠️ NOMA'LUM davr jimgina «last30» ga aylanmaydi")
        void unknownPeriodIsRejected() throws Exception {
            // Ilgari `default` tarmog'i ham «last30» ni, ham noma'lum
            // qiymatni qamrardi: panel period=last90 yuborsa, hisobot
            // 30 kunlik ma'lumot qaytarardi va admin 90 kunlik hisobotni
            // ko'rdim deb o'ylardi.
            mockMvc.perform(get("/api/v1/app/admin/reports/overview?period=last90")
                            .header("Authorization", "Bearer " + token()))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("⚠️ Yarim to'ldirilgan maxsus davr ham xato beradi")
        void halfFilledCustomRangeIsRejected() throws Exception {
            // Ilgari faqat `from` berilsa u jimgina e'tiborsiz qolardi.
            mockMvc.perform(get("/api/v1/app/admin/reports/overview?from=2026-01-01")
                            .header("Authorization", "Bearer " + token()))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Teskari davr rad etiladi")
        void reversedRangeIsRejected() throws Exception {
            mockMvc.perform(get("/api/v1/app/admin/reports/overview"
                            + "?from=2026-03-01&to=2026-01-01")
                            .header("Authorization", "Bearer " + token()))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // -------------------------------------------------------------- obyekt

    @Nested
    @DisplayName("Obyekt filtri")
    class EntityFilter {

        @Test
        @DisplayName("Kontent filtri ro'yxatni ham, JAMLANMANI ham toraytiradi")
        void contentFilterNarrowsTotalsToo() throws Exception {
            Content a = content(null);
            Content b = content(null);
            views(a.getId(), 30);
            views(b.getId(), 70);
            analyticsService.aggregate();

            JsonNode all = report("?period=today");
            JsonNode filtered = report("?period=today&contentId=" + a.getId());

            // ⚠️ ASOSIY TEKSHIRUV: filtr qo'llanganda umumiy son ham
            // torayishi shart. Aks holda ro'yxat torayib, jamlanma butun
            // platformaniki bo'lib qolardi — hisobot o'z-o'ziga zid.
            assertThat(all.get("totalViews").asLong()).isGreaterThanOrEqualTo(100);
            assertThat(filtered.get("totalViews").asLong()).isEqualTo(30);
            assertThat(filtered.get("topContent")).hasSize(1);
        }

        @Test
        @DisplayName("Kategoriya filtri o'sha kategoriyadagi kontentni oladi")
        void categoryFilterWorks() throws Exception {
            Category drama = category("Drama");
            Content inDrama = content(drama.getId());
            Content outside = content(null);
            views(inDrama.getId(), 40);
            views(outside.getId(), 60);
            analyticsService.aggregate();

            JsonNode r = report("?period=today&categoryId=" + drama.getId());

            assertThat(r.get("totalViews").asLong()).isEqualTo(40);
        }

        @Test
        @DisplayName("Filtrlar BIRGA ishlaydi — kesishma, birlashma emas")
        void filtersIntersect() throws Exception {
            Category drama = category("Drama");
            Content inDrama = content(drama.getId());
            Content outside = content(null);
            views(inDrama.getId(), 40);
            views(outside.getId(), 60);
            analyticsService.aggregate();

            // «Shu kategoriya VA shu kontent» — kontent kategoriyaga
            // kirmaydi, ya'ni natija bo'sh. Birlashma bo'lganda filtr
            // qo'shgan sari natija KENGAYARDI, bu kutilganiga teskari.
            JsonNode r = report("?period=today&categoryId=" + drama.getId()
                    + "&contentId=" + outside.getId());

            assertThat(r.get("totalViews").asLong()).isZero();
            assertThat(r.get("topContent")).isEmpty();
        }

        @Test
        @DisplayName("Mos kontent yo'q — bo'sh grafik, soxta qator emas")
        void emptyFilterResultGivesEmptySeries() throws Exception {
            JsonNode r = report("?period=today&contentId=999999");

            assertThat(r.get("totalViews").asLong()).isZero();
            assertThat(r.get("series")).isEmpty();
        }

        @Test
        @DisplayName("Qo'llangan filtrlar javobda qaytariladi")
        void appliedFiltersAreEchoed() throws Exception {
            JsonNode r = report("?period=today&contentId=42&tariffId=7");

            // Usiz admin «bu son butun platformanikimi yoki
            // filtrlanganmi» degan savolga javob topa olmasdi —
            // ayniqsa saqlangan yoki ulashilgan havolada.
            assertThat(r.get("appliedFilters").get("contentId").asLong()).isEqualTo(42);
            assertThat(r.get("appliedFilters").get("tariffId").asLong()).isEqualTo(7);
            assertThat(r.get("appliedFilters").get("creatorId").isNull()).isTrue();
        }

        @Test
        @DisplayName("Tarif filtri obuna daromadini toraytiradi")
        void tariffFilterNarrowsRevenue() throws Exception {
            JsonNode all = report("?period=today");
            JsonNode filtered = report("?period=today&tariffId=999999");

            assertThat(all.has("subscriptionRevenue")).isTrue();
            // Mavjud bo'lmagan tarif — nol, xato emas.
            assertThat(filtered.get("subscriptionRevenue").decimalValue())
                    .isEqualByComparingTo("0");
        }
    }

    // ------------------------------------------------------------ yordamchi

    private void views(Long contentId, int count) {
        for (int i = 0; i < count; i++) {
            analyticsService.record(AnalyticsEventType.CONTENT_VIEW,
                    contentId, null, null, "qurilma-" + i);
        }
    }

    private Category category(String name) {
        TaxonomySaveRequest req = new TaxonomySaveRequest();
        req.setSortOrder(0);
        req.setActive(true);
        req.setTranslations(Translations.all(name + " " + SEQ.incrementAndGet()));
        return taxonomyService.saveCategory(null, null, req);
    }

    private Content content(Long categoryId) {
        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(ContentType.MOVIE);
        req.setStructureType(StructureType.SINGLE);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(PublicationStatus.PUBLISHED);
        req.setVisibility(ContentVisibility.PUBLIC);
        req.setCategoryId(categoryId);
        req.setTranslations(Translations.all("Hisobot filmi " + SEQ.incrementAndGet()));
        return contentService.create(null, req);
    }
}
