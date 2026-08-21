package com.example.backend.Admin;

import com.example.backend.Cms.Entity.AdDailyStatistic;
import com.example.backend.Cms.Repository.AdDailyStatisticRepo;
import com.example.backend.Enums.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.EnumSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ТЗ §29 — «Har bir reklama uchun Admin ko'ra olishi kerak».
 *
 * Umumiy hisobotda faqat TOP-10 banner chiqadi. 30 ta banneri bor admin
 * 25-chisining natijasini ko'ra olmasdi — shuning uchun alohida endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
class AdStatisticsEndpointTest {

    private static final long AD_ID = 77_001L;

    @Autowired private MockMvc mockMvc;
    @Autowired private TestStaffFactory staff;
    @Autowired private AdDailyStatisticRepo adStatRepo;

    private String tokenWithView;
    private String tokenWithoutView;

    @BeforeEach
    void seed() {
        tokenWithView = staff.token("+998900000301",
                EnumSet.of(Permission.ADVERTISEMENT_VIEW));
        tokenWithoutView = staff.token("+998900000302",
                EnumSet.of(Permission.CONTENT_VIEW));

        adStatRepo.findAllByAdvertisementIdAndStatDateBetweenOrderByStatDateAsc(
                AD_ID, LocalDate.now().minusDays(400), LocalDate.now()).forEach(adStatRepo::delete);

        LocalDate today = LocalDate.now();
        adStatRepo.save(AdDailyStatistic.builder()
                .advertisementId(AD_ID).statDate(today.minusDays(1))
                .impressions(100L).clicks(5L)
                .uniqueImpressions(80L).uniqueClicks(4L).build());
        adStatRepo.save(AdDailyStatistic.builder()
                .advertisementId(AD_ID).statDate(today)
                .impressions(300L).clicks(15L)
                .uniqueImpressions(200L).uniqueClicks(12L).build());
    }

    @Test
    @DisplayName("Beshta ko'rsatkich ham qaytadi")
    void returnsAllFiveMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/app/admin/advertisements/" + AD_ID + "/statistics")
                        .header("Authorization", "Bearer " + tokenWithView))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impressions").value(400))
                .andExpect(jsonPath("$.clicks").value(20))
                .andExpect(jsonPath("$.uniqueImpressions").value(280))
                .andExpect(jsonPath("$.uniqueClicks").value(16))
                // 20 / 400 = 5%
                .andExpect(jsonPath("$.ctr").value(5.0))
                .andExpect(jsonPath("$.daily.length()").value(2));
    }

    @Test
    @DisplayName("Kunlik kesim grafik uchun sana bo'yicha tartiblangan")
    void dailyRowsAreOrdered() throws Exception {
        mockMvc.perform(get("/api/v1/app/admin/advertisements/" + AD_ID + "/statistics")
                        .header("Authorization", "Bearer " + tokenWithView))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daily[0].impressions").value(100))
                .andExpect(jsonPath("$.daily[1].impressions").value(300))
                .andExpect(jsonPath("$.daily[1].ctr").value(5.0));
    }

    @Test
    @DisplayName("Ma'lumot yo'q reklama — nol, soxta son emas")
    void unknownAdReturnsEmptyState() throws Exception {
        mockMvc.perform(get("/api/v1/app/admin/advertisements/999999/statistics")
                        .header("Authorization", "Bearer " + tokenWithView))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impressions").value(0))
                .andExpect(jsonPath("$.ctr").value(0.0))
                .andExpect(jsonPath("$.daily.length()").value(0));
    }

    @Test
    @DisplayName("Davr chegaralanadi — days parametri")
    void windowIsHonoured() throws Exception {
        // Faqat bugun: kechagi 100 ta ko'rsatish tushib qolishi kerak.
        mockMvc.perform(get("/api/v1/app/admin/advertisements/" + AD_ID + "/statistics")
                        .param("days", "1")
                        .header("Authorization", "Bearer " + tokenWithView))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impressions").value(300))
                .andExpect(jsonPath("$.daily.length()").value(1));
    }

    @Test
    @DisplayName("Ruxsatsiz xodim statistikani ko'ra olmaydi")
    void requiresAdvertisementViewPermission() throws Exception {
        mockMvc.perform(get("/api/v1/app/admin/advertisements/" + AD_ID + "/statistics")
                        .header("Authorization", "Bearer " + tokenWithoutView))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tokensiz — 401")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/app/admin/advertisements/" + AD_ID + "/statistics"))
                .andExpect(status().isUnauthorized());
    }
}
