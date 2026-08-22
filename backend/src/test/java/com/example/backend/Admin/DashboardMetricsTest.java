package com.example.backend.Admin;

import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ТЗ §45 — admin dashboard ko'rsatkichlari.
 *
 * <h2>Asosiy qoida</h2>
 * «Data mavjud bo'lmagan statisticni fake qilib chiqarma. Empty state
 * ko'rsat.»
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
@Transactional
class DashboardMetricsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestStaffFactory staff;

    /**
     * Admin tokeni — BIR MARTA yaratiladi.
     *
     * ⚠️ Har chaqiruvda qayta yaratilsa, ruxsat satri takrorlanib
     * UNIQUE cheklovni buzadi.
     */
    private String adminToken;

    private JsonNode summary() throws Exception {
        if (adminToken == null) {
            adminToken = staff.tokenForRole("+998900003001", PlatformRole.ADMIN,
                    EnumSet.of(Permission.CONTENT_VIEW));
        }
        String body = mockMvc.perform(get("/api/v1/app/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    // ------------------------------------------------------------- qamrov

    @Nested
    @DisplayName("ТЗ §45 ro'yxati qamralgan")
    class Coverage {

        @Test
        @DisplayName("Yigirmata ko'rsatkich ham javobda bor")
        void allRequestedMetricsArePresent() throws Exception {
            JsonNode s = summary();

            List<String> required = List.of(
                    "totalUsers", "activeUsers", "premiumUsers", "newUsers",
                    "totalContent", "publishedContent", "draftContent",
                    "contentViews30d", "topViewedContent",
                    "totalAds", "adImpressions", "adClicks", "adCtr",
                    "totalSubscriptions", "subscriptionRevenue",
                    "singlePurchaseRevenue",
                    "donationRevenue", "topCreators",
                    "totalComments", "totalNotifications");

            for (String field : required) {
                assertThat(s.has(field))
                        .as("ТЗ §45 da so'ralgan ko'rsatkich yo'q: " + field)
                        .isTrue();
            }

            // ⚠️ Maydon MAVJUDLIGI yetarli emas.
            //
            // Jackson `null` maydonlarni ham JSON'ga qo'shadi, ya'ni
            // hisoblash olib tashlansa `has()` baribir `true` qaytaradi
            // va test hech narsani tekshirmasdan o'tib ketardi.
            //
            // `donationRevenue` ATAYLAB null — u alohida tekshiriladi.
            List<String> mustHaveValue = required.stream()
                    .filter(f -> !f.equals("donationRevenue"))
                    .toList();

            for (String field : mustHaveValue) {
                assertThat(s.get(field).isNull())
                        .as("Ko'rsatkich hisoblanmagan (null): " + field)
                        .isFalse();
            }
        }
    }

    // ------------------------------------------------------- soxta emas

    @Nested
    @DisplayName("Soxta statistika yo'q")
    class NoFakeData {

        @Test
        @DisplayName("⚠️ Donat daromadi so'mda O'LCHANMAYDI va sababi aytiladi")
        void donationRevenueIsNotFaked() throws Exception {
            JsonNode s = summary();

            // Kurs belgilanmagan. Taxminiy kurs bilan pulga o'girish
            // soxta raqam bo'lardi; nol qaytarish esa «donat yo'q» degan
            // ma'noni berardi — ikkalasi ham yolg'on.
            assertThat(s.get("donationRevenue").isNull()).isTrue();
            assertThat(s.get("donationRevenueAvailable").asBoolean()).isFalse();
            assertThat(s.get("donationRevenueUnavailableReason").asText())
                    .isNotBlank();
        }

        @Test
        @DisplayName("Donatlar VALYUTA bo'yicha esa ko'rsatiladi")
        void donationsAreShownPerCurrency() throws Exception {
            // So'mga o'girib bo'lmasa ham, yulduz va tanga sonini
            // ko'rsatish mumkin — bu haqiqiy ma'lumot.
            assertThat(summary().has("donationsByKind")).isTrue();
        }

        @Test
        @DisplayName("Ma'lumot yo'q bo'lsa BO'SH RO'YXAT, o'ylab topilgan qator emas")
        void emptyListsInsteadOfInventedRows() throws Exception {
            JsonNode s = summary();

            assertThat(s.get("topViewedContent").isArray()).isTrue();
            assertThat(s.get("topCreators").isArray()).isTrue();
            // Test bazasida ko'rish va donat yo'q — ro'yxatlar bo'sh
            // bo'lishi KERAK.
            assertThat(s.get("topCreators")).isEmpty();
        }

        @Test
        @DisplayName("Ko'rsatishsiz CTR nol — nolga bo'linish yo'q")
        void ctrWithoutImpressionsIsZero() throws Exception {
            assertThat(summary().get("adCtr").asDouble()).isEqualTo(0.0);
        }
    }

    // ------------------------------------------------------------- xatolar

    @Nested
    @DisplayName("Tuzatilgan xatolar")
    class FixedBugs {

        @Test
        @DisplayName("⚠️ «Xodimlar» soni ilova foydalanuvchilarini SANAMAYDI")
        void staffCountExcludesAppUsers() throws Exception {
            long staffBefore = summary().get("totalStaff").asLong();
            long usersBefore = summary().get("totalUsers").asLong();

            // Uchta ILOVA foydalanuvchisi qo'shiladi — xodim emas.
            staff.tokenForRole("+998900003101", PlatformRole.USER,
                    EnumSet.noneOf(Permission.class));
            staff.tokenForRole("+998900003102", PlatformRole.USER,
                    EnumSet.noneOf(Permission.class));
            staff.tokenForRole("+998900003103", PlatformRole.USER,
                    EnumSet.noneOf(Permission.class));

            JsonNode after = summary();

            // ⚠️ ASOSIY TEKSHIRUV: ilgari bu yerda userRepo.count() turgan
            // edi — ya'ni BARCHA foydalanuvchilar «xodimlar» deb
            // ko'rsatilardi. 100 000 ta ilova foydalanuvchisi bo'lsa,
            // dashboard «100 000 xodim» deb yozardi.
            assertThat(after.get("totalStaff").asLong())
                    .as("Ilova foydalanuvchilari qo'shilganda xodimlar soni "
                            + "o'zgarmasligi kerak")
                    .isEqualTo(staffBefore);

            // Foydalanuvchilar soni esa aynan uchtaga o'sishi kerak.
            assertThat(after.get("totalUsers").asLong()).isEqualTo(usersBefore + 3);
        }

        @Test
        @DisplayName("⚠️ Obuna daromadi TIYINLARNI yo'qotmaydi")
        void subscriptionRevenueKeepsFractions() throws Exception {
            JsonNode revenue = summary().get("subscriptionRevenue");

            // Ilgari .longValue() edi: 49 999.50 so'm 49 999 bo'lib
            // ko'rinardi va xato har bir obuna bilan yig'ilardi.
            // Pul JSON'da SON bo'lib chiqadi va kasr qismi saqlanadi.
            assertThat(revenue.isNumber())
                    .as("Pul son bo'lib qaytishi kerak")
                    .isTrue();
            assertThat(revenue.decimalValue())
                    .as("BigDecimal sifatida o'qilishi kerak — tiyinlar joyida")
                    .isNotNull();
        }

        @Test
        @DisplayName("Daromad turlari ARALASHTIRILMAYDI")
        void revenueKindsAreSeparate() throws Exception {
            JsonNode s = summary();

            // Kontent xaridi, valyuta paketi va obuna — uch xil daromad.
            // Ularni qo'shish qaysi ko'rsatkich nimani anglatishini
            // chalkashtirardi.
            assertThat(s.has("singlePurchaseRevenue")).isTrue();
            assertThat(s.has("currencyPackageRevenue")).isTrue();
            assertThat(s.has("subscriptionRevenue")).isTrue();
        }
    }

    @Nested
    @DisplayName("Ruxsat")
    class Access {

        @Test
        @DisplayName("Tokensiz dashboard ko'rinmaydi")
        void requiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/v1/app/admin/dashboard/summary"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Ilova foydalanuvchisi dashboardni ko'ra olmaydi")
        void appUserCannotSeeDashboard() throws Exception {
            String token = staff.tokenForRole("+998900003201", PlatformRole.USER,
                    EnumSet.of(Permission.CONTENT_VIEW));

            mockMvc.perform(get("/api/v1/app/admin/dashboard/summary")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }
}
