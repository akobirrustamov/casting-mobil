package com.example.backend.Cms;

import com.example.backend.Admin.TestStaffFactory;
import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Entity.UserBalance;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import com.example.backend.Cms.Repository.CreatorRepo;
import com.example.backend.Cms.Repository.UserBalanceRepo;
import com.example.backend.Cms.Service.DonationService;
import com.example.backend.Entity.User;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.UserRepo;
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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ТЗ §43 — donat balansi.
 *
 * ТЗ: «Hozir mobil UI yozilmaydi. Lekin backend/data model buning uchun
 * tayyor bo'lsin.»
 *
 * <h2>«Tayyor» nimani anglatadi</h2>
 * Faqat entity mavjudligi emas. Profil ekrani ikkita savolga javob
 * berishi kerak: «qancha bor» va «qayerga ketdi». Ikkinchisisiz birinchi
 * son ishonchsiz bo'lib qoladi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
@Transactional
class DonationBalanceTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestStaffFactory staff;
    @Autowired private DonationService donationService;
    @Autowired private UserBalanceRepo balanceRepo;
    @Autowired private CreatorRepo creatorRepo;
    @Autowired private UserRepo userRepo;

    // ------------------------------------------------------------ model

    @Nested
    @DisplayName("Ma'lumot modeli tayyor")
    class Model {

        @Test
        @DisplayName("Ikkala balans ham alohida maydonda")
        void bothBalancesExist() throws NoSuchFieldException {
            assertThat(UserBalance.class.getDeclaredField("starsBalance").getType())
                    .isEqualTo(Long.class);
            assertThat(UserBalance.class.getDeclaredField("coinBalance").getType())
                    .isEqualTo(Long.class);
        }

        @Test
        @DisplayName("⚠️ Yulduz va tanga BUTUN son — bo'linmaydi")
        void virtualCurrenciesAreWholeNumbers() throws NoSuchFieldException {
            // Yarim yulduz degan tushuncha yo'q. BigDecimal ishlatilsa
            // «0.5 yulduz» yozib qo'yish mumkin bo'lardi.
            assertThat(UserBalance.class.getDeclaredField("starsBalance").getType())
                    .isNotEqualTo(BigDecimal.class);
        }

        @Test
        @DisplayName("Pul balansi esa BigDecimal")
        void moneyBalanceIsBigDecimal() throws NoSuchFieldException {
            Field money = UserBalance.class.getDeclaredField("moneyBalance");

            // Pul bo'linadi va floating point unga yaramaydi.
            assertThat(money.getType()).isEqualTo(BigDecimal.class);
        }

        @Test
        @DisplayName("Balans bir vaqtda o'zgarishdan himoyalangan")
        void balanceHasOptimisticLock() throws NoSuchFieldException {
            assertThat(UserBalance.class.getDeclaredField("version")
                    .isAnnotationPresent(jakarta.persistence.Version.class))
                    .as("@Version bo'lmasa ikki oqim balansni ikki marta yechardi")
                    .isTrue();
        }
    }

    // ------------------------------------------------------------ endpoint

    @Nested
    @DisplayName("Balans endpointi")
    class BalanceEndpoint {

        @Test
        @DisplayName("Hisobi yo'q foydalanuvchida nol — bu HAQIQIY nol")
        void newUserSeesZero() throws Exception {
            String token = appToken();

            mockMvc.perform(get("/api/v1/app/donations/balance")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.moneyBalance").value(0))
                    .andExpect(jsonPath("$.starsBalance").value(0))
                    .andExpect(jsonPath("$.coinBalance").value(0));
        }

        /**
         * Maketda («Screen 4») profildagi birinchi son aynan so'mdagi
         * balans. Maydon {@code UserBalance} da BOR edi, lekin DTO uni
         * bermasdi — ilova o'sha joyga chiziqcha qo'yardi. Endi uchala
         * son ham serverdan keladi.
         */
        @Test
        @DisplayName("So'mdagi balans ham qaytadi, faqat yulduz va tanga emas")
        void moneyBalanceIsReturned() throws Exception {
            String phone = "+998900003" + (100 + SEQ.incrementAndGet());
            String token = staff.tokenForRole(phone, PlatformRole.USER,
                    EnumSet.noneOf(Permission.class));
            User u = userRepo.findByPhone(phone).orElseThrow();

            balanceRepo.save(UserBalance.builder()
                    .user(u)
                    .moneyBalance(new BigDecimal("56000.00"))
                    .starsBalance(456L)
                    .coinBalance(56L)
                    .build());

            mockMvc.perform(get("/api/v1/app/donations/balance")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.moneyBalance").value(56000.00))
                    .andExpect(jsonPath("$.starsBalance").value(456))
                    .andExpect(jsonPath("$.coinBalance").value(56));
        }

        @Test
        @DisplayName("Tokensiz ko'rib bo'lmaydi")
        void requiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/v1/app/donations/balance"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("⚠️ ID parametri YO'Q — boshqaning balansini o'qib bo'lmaydi")
        void balanceHasNoIdParameter() throws Exception {
            // Kimning balansi ko'rsatilishi TOKENDAN olinadi. ID parametri
            // bo'lsa, uni almashtirib boshqa odamning balansini o'qish
            // mumkin bo'lardi.
            String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "src/main/java/com/example/backend/Cms/Controller/DonationController.java"));

            assertThat(source)
                    .contains("@GetMapping(\"/balance\")")
                    .doesNotContain("balance(@PathVariable")
                    .doesNotContain("balance(@RequestParam");
        }
    }

    // -------------------------------------------------------------- tarix

    @Nested
    @DisplayName("Tarix — «qayerga ketdi»")
    class History {

        @Test
        @DisplayName("O'z donatlari ko'rinadi")
        void ownDonationsAreListed() throws Exception {
            User me = userWith(100L);
            String token = tokenFor(me);
            Creator creator = creator();

            donationService.donate(me, DonationTargetType.CREATOR,
                    creator.getId(), CurrencyKind.STARS, 30L);

            mockMvc.perform(get("/api/v1/app/donations/my")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].amount").value(30));
        }

        @Test
        @DisplayName("⚠️ Boshqaning donatlari KO'RINMAYDI")
        void otherUsersDonationsAreHidden() throws Exception {
            User me = userWith(100L);
            User other = userWith(100L);
            Creator creator = creator();

            donationService.donate(other, DonationTargetType.CREATOR,
                    creator.getId(), CurrencyKind.STARS, 70L);

            mockMvc.perform(get("/api/v1/app/donations/my")
                            .header("Authorization", "Bearer " + tokenFor(me)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(0));
        }

        @Test
        @DisplayName("Balans va tarix bir-biriga mos")
        void balanceMatchesHistory() {
            User me = userWith(100L);
            Creator creator = creator();

            donationService.donate(me, DonationTargetType.CREATOR,
                    creator.getId(), CurrencyKind.STARS, 30L);
            donationService.donate(me, DonationTargetType.CREATOR,
                    creator.getId(), CurrencyKind.STARS, 20L);

            // 100 - 30 - 20 = 50. Tarix aynan shu 50 ni tushuntiradi.
            assertThat(balanceRepo.findByUserId(me.getId()).orElseThrow()
                    .getStarsBalance()).isEqualTo(50L);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private String appToken() {
        return staff.tokenForRole("+998900001" + (100 + SEQ.incrementAndGet()),
                PlatformRole.USER, EnumSet.noneOf(Permission.class));
    }

    private String tokenFor(User u) {
        return staff.tokenForRole(u.getPhone(), PlatformRole.USER,
                EnumSet.noneOf(Permission.class));
    }

    private User userWith(long stars) {
        String phone = "+998900002" + (100 + SEQ.incrementAndGet());
        staff.tokenForRole(phone, PlatformRole.USER, EnumSet.noneOf(Permission.class));
        User u = userRepo.findByPhone(phone).orElseThrow();

        balanceRepo.save(UserBalance.builder()
                .user(u)
                .starsBalance(stars)
                .coinBalance(0L)
                .moneyBalance(BigDecimal.ZERO)
                .build());
        return u;
    }

    private Creator creator() {
        return creatorRepo.save(Creator.builder()
                .slug("balans-ijodkor-" + SEQ.incrementAndGet())
                .active(true)
                .featured(false)
                .sortOrder(0)
                .starsReceived(0L)
                .build());
    }
}
