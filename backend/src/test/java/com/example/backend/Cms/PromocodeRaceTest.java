package com.example.backend.Cms;

import com.example.backend.Cms.Entity.Promocode;
import com.example.backend.Cms.Repository.PromocodeRedemptionRepo;
import com.example.backend.Cms.Repository.PromocodeRepo;
import com.example.backend.Cms.Repository.SubscriptionRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Service.PromocodeService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Promokod — poyga holati.
 *
 * <h2>⚠️ Nima uchun bu test TRANZAKSIYASIZ</h2>
 * Ikki parallel so'rov bitta test tranzaksiyasi ichida bo'lishi mumkin
 * emas: ular alohida ulanishlarda, alohida tranzaksiyalarda yurishi va
 * bir-birining qulfini KO'RISHI kerak. {@code @Transactional} qo'yilsa,
 * ikkalasi ham bitta ulanishda ketardi va poyga umuman bo'lmasdi — test
 * har doim o'tardi va hech narsani qo'riqlamasdi.
 *
 * <h2>⚠️ O'zidan keyin tozalaydi</h2>
 * Tranzaksiya yo'q — ya'ni yozilgan qatorlar bazada QOLADI. Birinchi
 * variantda ular qolgan edi va {@code SubscriptionSummaryTest} yiqildi:
 * u faol obunalarni butun baza bo'yicha sanaydi, va bu yerdan qolgan
 * bitta PROMO obunasi hisobni buzdi. {@code TestDatabaseReset} o'chirib
 * qo'yilgan, shuning uchun tozalash shu yerda.
 *
 * <h2>Nima tekshiriladi</h2>
 * Bitta o'rinli kod, bir nechta odam bir vaqtda. Aynan BITTASI o'tishi
 * kerak. {@code lockByCode} bo'lmasa, hammasi «0 dan 1» ni ko'rib
 * o'tardi va limit buzilardi — ya'ni bepul tarqatilgan obuna.
 */
@SpringBootTest
@ActiveProfiles("test")
class PromocodeRaceTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private PromocodeService promocodeService;
    @Autowired private PromocodeRepo promocodeRepo;
    @Autowired private PromocodeRedemptionRepo redemptionRepo;
    @Autowired private SubscriptionRepo subscriptionRepo;
    @Autowired private UserAccountRepo accountRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    private final List<User> createdUsers = new ArrayList<>();
    private Promocode createdPromo;

    @AfterEach
    void cleanUp() {
        // Bog'liqlik tartibida: ishlatilganlar → obunalar → hisoblar →
        // foydalanuvchilar → kod. Audit yozuvi FK siz (actor_id oddiy
        // ustun), unga tegilmaydi.
        if (createdPromo != null) {
            redemptionRepo.deleteAll(redemptionRepo
                    .findAllByPromocodeIdOrderByRedeemedAtDesc(createdPromo.getId()));
        }
        for (User u : createdUsers) {
            subscriptionRepo.deleteAll(subscriptionRepo.findAllByUserIdOrderByEndAtDesc(u.getId()));
            accountRepo.findByUserId(u.getId()).ifPresent(accountRepo::delete);
            userRepo.delete(u);
        }
        if (createdPromo != null) {
            promocodeRepo.delete(createdPromo);
        }
        createdUsers.clear();
        createdPromo = null;
    }

    @Test
    @DisplayName("Oxirgi o'rin uchun poyga — faqat bittasi o'tadi")
    void onlyOneWinsTheLastSlot() throws Exception {
        int contenders = 6;
        createdPromo = promocodeRepo.save(Promocode.builder()
                .code("RACE-" + SEQ.incrementAndGet() + "-" + System.nanoTime() % 100000)
                .grantDays(30)
                .maxRedemptions(1)
                .active(true)
                .build());

        for (int i = 0; i < contenders; i++) {
            createdUsers.add(user());
        }

        // Hamma bir vaqtda boshlashi uchun: darvoza ochilguncha kutadi.
        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        List<Future<String>> results = new ArrayList<>();

        for (User u : createdUsers) {
            Callable<String> attempt = () -> {
                gate.await(5, TimeUnit.SECONDS);
                try {
                    promocodeService.redeem(u, createdPromo.getCode());
                    return "OK";
                } catch (BusinessException e) {
                    return e.getCode();
                }
            };
            results.add(pool.submit(attempt));
        }

        gate.countDown();
        List<String> outcomes = new ArrayList<>();
        for (Future<String> f : results) {
            outcomes.add(f.get(20, TimeUnit.SECONDS));
        }
        pool.shutdownNow();

        long winners = outcomes.stream().filter("OK"::equals).count();
        assertThat(winners).as("g'oliblar: " + outcomes).isEqualTo(1);
        assertThat(outcomes).filteredOn(o -> !o.equals("OK"))
                .allMatch("PROMO_EXHAUSTED"::equals);

        // Baza ham bitta yozuvni ko'rsatadi — xotiradagi natija emas.
        assertThat(redemptionRepo.countByPromocodeId(createdPromo.getId())).isEqualTo(1);
    }

    private User user() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        int n = SEQ.incrementAndGet();
        u.setPhone("+99890" + (8900000 + n) + (System.nanoTime() % 10));
        u.setPassword("xesh-" + n);
        u.setName("Poyga " + n);
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }
}
