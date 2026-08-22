package com.example.backend.Cms;

import com.example.backend.Admin.Dto.DonationReportDto;
import com.example.backend.Cms.Entity.DonationTransaction;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import com.example.backend.Cms.Payment.PaymentNotConfiguredException;
import com.example.backend.Cms.Payment.PaymentProvider;
import com.example.backend.Cms.Repository.DonationRepo;
import com.example.backend.Cms.Service.MonetizationService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §42 (donat hisoboti) va §44 (to'lov).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DonationAndPaymentTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MonetizationService monetizationService;
    @Autowired private DonationRepo donationRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private PaymentProvider paymentProvider;

    // -------------------------------------------------------------- §42

    @Nested
    @DisplayName("Donat hisoboti (ТЗ §42)")
    class Report {

        @Test
        @DisplayName("STARS va COIN ALOHIDA hisoblanadi")
        void currenciesAreNotSummedTogether() {
            User sender = user();
            donate(sender, DonationTargetType.CREATOR, 1L, CurrencyKind.STARS, 100);
            donate(sender, DonationTargetType.CREATOR, 1L, CurrencyKind.UZCASTING_COIN, 5);

            DonationReportDto report = monetizationService.donationReport(20, 30);

            // Ularni qo'shish 10 so'm va 10 dollarni qo'shishday bo'lardi:
            // kurs admin panelida alohida belgilanadi (§40, §41).
            assertThat(report.getByKind()).hasSize(2);
            assertThat(report.getByKind())
                    .extracting(DonationReportDto.KindTotal::getKind)
                    .containsExactlyInAnyOrder(CurrencyKind.STARS, CurrencyKind.UZCASTING_COIN);
            assertThat(report.getByKind()).filteredOn(k -> k.getKind() == CurrencyKind.STARS)
                    .first().extracting(DonationReportDto.KindTotal::getTotal).isEqualTo(100L);
        }

        @Test
        @DisplayName("Top ijodkorlar va top kontent alohida ro'yxat")
        void creatorsAndContentAreSeparateLists() {
            User sender = user();
            donate(sender, DonationTargetType.CREATOR, 11L, CurrencyKind.STARS, 500);
            donate(sender, DonationTargetType.CONTENT, 22L, CurrencyKind.STARS, 300);

            DonationReportDto report = monetizationService.donationReport(20, 30);

            assertThat(report.getTopCreators())
                    .extracting(DonationReportDto.TargetRow::getTargetId).contains(11L);
            assertThat(report.getTopCreators())
                    .extracting(DonationReportDto.TargetRow::getTargetId).doesNotContain(22L);
            assertThat(report.getTopContent())
                    .extracting(DonationReportDto.TargetRow::getTargetId).contains(22L);
        }

        @Test
        @DisplayName("Kunlik summalar grafik uchun keladi")
        void dailyTotalsArePresent() {
            User sender = user();
            donate(sender, DonationTargetType.CREATOR, 33L, CurrencyKind.STARS, 50);

            DonationReportDto report = monetizationService.donationReport(20, 30);

            assertThat(report.getDaily()).isNotEmpty();
            assertThat(report.getDaily()).allSatisfy(d ->
                    assertThat(d.getDate()).isNotNull());
        }

        @Test
        @DisplayName("Davr chegarasidan tashqaridagi donat kunlik kesimga kirmaydi")
        void oldDonationIsOutsideWindow() {
            User sender = user();
            DonationTransaction old = donate(sender, DonationTargetType.CREATOR, 44L,
                    CurrencyKind.STARS, 70);
            old.setCreatedAt(LocalDateTime.now().minusDays(100));
            donationRepo.save(old);

            DonationReportDto report = monetizationService.donationReport(20, 7);

            assertThat(report.getDaily())
                    .as("100 kun oldingi donat 7 kunlik oynaga kirmasligi kerak")
                    .allSatisfy(d -> assertThat(d.getDate())
                            .isAfterOrEqualTo(java.time.LocalDate.now().minusDays(6)));
        }

        @Test
        @DisplayName("Tranzaksiyalar ro'yxati eng yangisidan boshlanadi")
        void transactionsAreNewestFirst() {
            User sender = user();
            donate(sender, DonationTargetType.CREATOR, 55L, CurrencyKind.STARS, 10);
            donate(sender, DonationTargetType.CREATOR, 55L, CurrencyKind.STARS, 20);

            var page = monetizationService.donationTransactions(PageRequest.of(0, 10));

            assertThat(page.getContent()).isNotEmpty();
            assertThat(page.getContent().get(0).getCreatedAt())
                    .isAfterOrEqualTo(page.getContent().get(page.getContent().size() - 1)
                            .getCreatedAt());
        }

        @Test
        @DisplayName("Donatsiz hisobot bo'sh — soxta raqam yo'q")
        void emptyReportHasNoInventedNumbers() {
            DonationReportDto report = monetizationService.donationReport(20, 30);

            // Ma'lumot yo'q bo'lsa bo'sh holat, o'ylab topilgan son emas.
            assertThat(report.getTotalTransactions()).isNotNull();
            assertThat(report.getTopCreators()).isNotNull();
            assertThat(report.getTopContent()).isNotNull();
        }
    }

    // -------------------------------------------------------------- §44

    @Nested
    @DisplayName("To'lov (ТЗ §44)")
    class Payment {

        @Test
        @DisplayName("Provayder sozlanmagan deb halol aytadi")
        void providerReportsItselfAsNotConfigured() {
            assertThat(paymentProvider.isConfigured()).isFalse();
        }

        @Test
        @DisplayName("⚠️ Soxta muvaffaqiyat QAYTARMAYDI")
        void neverReturnsFakeSuccess() {
            // Soxta «to'landi» eng xavfli variant: foydalanuvchi premium
            // olardi, pul esa hech qayerdan kelmasdi va buni faqat oy
            // oxirida hisob-kitobda payqashardi.
            assertThatThrownBy(() -> paymentProvider.init(
                    "order-1", new BigDecimal("24000.00"), "UZS"))
                    .isInstanceOf(PaymentNotConfiguredException.class)
                    .hasMessageContaining("sozlanmagan");
        }

        @Test
        @DisplayName("Xato 503 — dastur xatosi emas, sozlama yetishmayapti")
        void unavailableNotServerError() {
            // ⚠️ try/catch ishlatilmaydi: istisno TASHLANMASA blok jimgina
            // tugab, test o'tib ketardi va hech narsani tekshirmasdi.
            assertThatThrownBy(() -> paymentProvider.init("order-2", BigDecimal.TEN, "UZS"))
                    .isInstanceOf(PaymentNotConfiguredException.class)
                    .extracting(e -> ((PaymentNotConfiguredException) e).getStatus())
                    .isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private DonationTransaction donate(User sender, DonationTargetType type,
                                       Long targetId, CurrencyKind kind, long amount) {
        return donationRepo.save(DonationTransaction.builder()
                .sender(sender)
                .targetType(type)
                .targetId(targetId)
                .kind(kind)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private User user() {
        Role r = roleRepo.findByName(UserRoles.ROLE_USER);
        if (r == null) {
            int nextId = roleRepo.findAll().stream()
                    .mapToInt(Role::getId).max().orElse(0) + 1;
            r = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99890" + (8000000 + SEQ.incrementAndGet()));
        u.setPassword("x");
        u.setName("Donat " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(r)));
        return userRepo.save(u);
    }
}
