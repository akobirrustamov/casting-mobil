package com.example.backend.Cms;

import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Entity.UserBalance;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import com.example.backend.Cms.Repository.CreatorRepo;
import com.example.backend.Cms.Repository.DonationRepo;
import com.example.backend.Cms.Repository.UserBalanceRepo;
import com.example.backend.Cms.Service.DonationService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §39 — bir vaqtda yuborilgan donatlar.
 *
 * <h2>Nima uchun alohida test</h2>
 * Bu sinf {@code @Transactional} EMAS: bir tranzaksiya ichida ikkita
 * parallel oqim bir-birini ko'rmaydi va poyga umuman yuzaga kelmaydi.
 * Ya'ni oddiy testda bu xato hech qachon ko'rinmasdi.
 *
 * <h2>Qanday xato qidiryapmiz</h2>
 * Ikki donat bir vaqtda yuborilsa, ikkalasi ham ESKI balansni o'qib,
 * ikkalasi ham yechishi mumkin. Natijada 100 ta yulduzi bor odam 200 ta
 * yulduzlik donat qilardi — pul yo'qdan bor bo'lardi.
 *
 * {@code UserBalance.@Version} buni to'xtatishi kerak.
 *
 * <h2>⚠️ Ortidan tozalash SHART</h2>
 * Tranzaksiyasiz test ma'lumoti bazada QOLADI va H2 butun to'plam uchun
 * bitta. Tozalanmasa, bu yerda yaratilgan donat boshqa testlarning
 * hisobot natijasiga qo'shilib, ularni tushunarsiz tarzda yiqitardi —
 * va sabab bu faylda ekanligi umuman ko'rinmasdi.
 */
@SpringBootTest
@ActiveProfiles("test")
class DonationConcurrencyTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    /** Balans faqat 100, har biri 60 so'raydi — ikkalasi sig'maydi. */
    private static final long BALANCE = 100L;
    private static final long AMOUNT = 60L;

    @Autowired private DonationService donationService;
    @Autowired private UserBalanceRepo balanceRepo;
    @Autowired private DonationRepo donationRepo;
    @Autowired private CreatorRepo creatorRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    /** Shu testda yaratilgan yozuvlar — tozalash uchun. */
    private final List<Long> createdDonations = new ArrayList<>();
    private User createdUser;
    private Creator createdCreator;

    @org.junit.jupiter.api.AfterEach
    void cleanUp() {
        createdDonations.forEach(donationRepo::deleteById);
        if (createdUser != null) {
            balanceRepo.findByUserId(createdUser.getId()).ifPresent(balanceRepo::delete);
            userRepo.deleteById(createdUser.getId());
        }
        if (createdCreator != null) {
            creatorRepo.deleteById(createdCreator.getId());
        }
    }

    @Test
    @DisplayName("⚠️ Bir vaqtda ikki donat balansni IKKI MARTA yechmaydi")
    void concurrentDonationsCannotOverspend() throws Exception {
        User sender = userWithBalance();
        Creator creator = creator();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger succeeded = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    // Ikkalasi ham AYNI paytda boshlansin.
                    start.await();
                    donationService.donate(sender, DonationTargetType.CREATOR,
                            creator.getId(), CurrencyKind.STARS, AMOUNT);
                    succeeded.incrementAndGet();
                } catch (Exception expected) {
                    // Bittasi yiqilishi KERAK — balans ikkalasiga yetmaydi.
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        UserBalance balance = balanceRepo.findByUserId(sender.getId()).orElseThrow();
        var mine = donationRepo.findAll().stream()
                .filter(d -> d.getSender() != null
                        && d.getSender().getId().equals(sender.getId()))
                .toList();
        mine.forEach(d -> createdDonations.add(d.getId()));
        long donated = mine.stream().mapToLong(d -> d.getAmount()).sum();

        // ⚠️ ASOSIY TEKSHIRUV: faqat BITTASI o'tishi kerak.
        assertThat(succeeded.get())
                .as("100 ta yulduz bilan 60+60 = 120 ta donat qilib bo'lmaydi")
                .isEqualTo(1);
        assertThat(donated).isEqualTo(AMOUNT);
        assertThat(balance.getStarsBalance()).isEqualTo(BALANCE - AMOUNT);
        assertThat(balance.getStarsBalance())
                .as("Balans hech qachon manfiy bo'lmasin")
                .isNotNegative();
    }

    // ------------------------------------------------------------ yordamchi

    private User userWithBalance() {
        Role r = roleRepo.findByName(UserRoles.ROLE_USER);
        if (r == null) {
            int nextId = roleRepo.findAll().stream()
                    .mapToInt(Role::getId).max().orElse(0) + 1;
            r = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99890" + (9900000 + SEQ.incrementAndGet()));
        u.setPassword("x");
        u.setName("Poyga " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(r)));
        u = userRepo.save(u);
        createdUser = u;

        balanceRepo.save(UserBalance.builder()
                .user(u)
                .starsBalance(BALANCE)
                .coinBalance(0L)
                .moneyBalance(BigDecimal.ZERO)
                .build());
        return u;
    }

    private Creator creator() {
        createdCreator = creatorRepo.save(Creator.builder()
                .slug("poyga-ijodkor-" + SEQ.incrementAndGet())
                .active(true)
                .featured(false)
                .sortOrder(0)
                .starsReceived(0L)
                .build());
        return createdCreator;
    }
}
