package com.example.backend.Cms;

import com.example.backend.Admin.Dto.SubscriptionDto;
import com.example.backend.Cms.Entity.Subscription;
import com.example.backend.Cms.Enums.SubscriptionSource;
import com.example.backend.Cms.Repository.SubscriptionRepo;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.Permission;
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

/**
 * ТЗ §71, §107 — obunalar ro'yxati.
 *
 * <h2>Nega bu modul kerak bo'ldi</h2>
 * Dashboard obuna daromadini ko'rsatardi, lekin admin <b>qaysi</b>
 * obunalar bu raqamni bergani ko'ra olmasdi: na endpoint, na sahifa
 * bor edi. Moliyaviy ko'rsatkichni tekshirib bo'lmasa, u shunchaki
 * ishonish kerak bo'lgan raqamga aylanadi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubscriptionListTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private SubscriptionRepo subscriptionRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    // -------------------------------------------------------------- filtrlar

    @Nested
    @DisplayName("Filtrlar birga ishlaydi")
    class Filters {

        @Test
        @DisplayName("Manba va qidiruv birga qo'llanadi")
        void sourceAndQueryCombine() {
            User ali = user("Ali");
            User vali = user("Vali");
            sub(ali, SubscriptionSource.PURCHASE, new BigDecimal("24000.00"), true);
            sub(ali, SubscriptionSource.ADMIN_GIFT, null, true);
            sub(vali, SubscriptionSource.PURCHASE, new BigDecimal("24000.00"), true);

            // ⚠️ §34 va §59 da filtrlar bir-birini bekor qilardi.
            // Bu yerda shu xato takrorlanmaganini tekshiramiz.
            var found = search(SubscriptionSource.PURCHASE, null, ali.getName());

            assertThat(found).hasSize(1);
            assertThat(found.get(0).getSource()).isEqualTo(SubscriptionSource.PURCHASE);
        }

        @Test
        @DisplayName("Faol va tugagan obunalar ajratiladi")
        void activeFilterSeparates() {
            User u = user("Faol");
            sub(u, SubscriptionSource.PURCHASE, new BigDecimal("10000.00"), true);
            sub(u, SubscriptionSource.PURCHASE, new BigDecimal("10000.00"), false);

            assertThat(search(null, true, u.getName())).hasSize(1);
            assertThat(search(null, false, u.getName())).hasSize(1);
            assertThat(search(null, null, u.getName())).hasSize(2);
        }

        @Test
        @DisplayName("Bekor qilingan obuna faol hisoblanmaydi")
        void revokedIsNotActive() {
            User u = user("Bekor");
            Subscription s = sub(u, SubscriptionSource.PURCHASE,
                    new BigDecimal("10000.00"), true);
            s.setRevokedAt(LocalDateTime.now());
            subscriptionRepo.save(s);

            // Muddati hali tugamagan, lekin bekor qilingan — bu ikkalasi
            // boshqa narsa va ro'yxatda ham farqlanishi kerak.
            assertThat(search(null, true, u.getName())).isEmpty();
            assertThat(search(null, false, u.getName())).hasSize(1);
        }

        @Test
        @DisplayName("Telefon bo'yicha ham topiladi")
        void searchByPhone() {
            User u = user("Telefonli");
            sub(u, SubscriptionSource.PURCHASE, new BigDecimal("10000.00"), true);

            assertThat(search(null, null, u.getPhone())).hasSize(1);
        }
    }

    // ----------------------------------------------------------------- DTO

    @Nested
    @DisplayName("Qator ma'lumoti")
    class Row {

        @Test
        @DisplayName("Sovg'ada to'lov `null` — nol emas")
        void giftHasNullAmount() {
            User u = user("Sovg'ali");
            Subscription s = sub(u, SubscriptionSource.ADMIN_GIFT, null, true);

            SubscriptionDto dto = SubscriptionDto.from(s);

            // ⚠️ Nol «bepul sotildi» degani, null esa «sotilmagan».
            // Ikkalasini bir xil ko'rsatish hisobotni chalkashtirardi (§45).
            assertThat(dto.getPaidAmount()).isNull();
            assertThat(dto.getSource()).isEqualTo(SubscriptionSource.ADMIN_GIFT);
        }

        @Test
        @DisplayName("Foydalanuvchi maydonlari qaytadi, parol qaytmaydi")
        void carriesUserWithoutSecrets() {
            User u = user("Maxfiy");
            SubscriptionDto dto = SubscriptionDto.from(
                    sub(u, SubscriptionSource.PURCHASE, new BigDecimal("1.00"), true));

            assertThat(dto.getUserName()).isEqualTo(u.getName());
            assertThat(dto.getUserPhone()).isEqualTo(u.getPhone());
            // Entity qaytarilsa parol xeshi ham birga ketardi (§65).
            assertThat(dto.toString()).doesNotContain(u.getPassword());
        }

        @Test
        @DisplayName("Muddati tugagan obuna faol emas")
        void expiredIsNotActive() {
            User u = user("Eskirgan");
            assertThat(SubscriptionDto.from(
                    sub(u, SubscriptionSource.PURCHASE, new BigDecimal("1.00"), false))
                    .isActive()).isFalse();
        }
    }

    // ------------------------------------------------------------- ruxsat

    @Nested
    @DisplayName("Ruxsat")
    class Access {

        @Test
        @DisplayName("Obunalar uchun alohida ruxsat bor")
        void hasDedicatedPermission() {
            // ⚠️ TARIFF_VIEW dan alohida: tarif narxi ommaviy ma'lumot,
            // bu ro'yxat esa KIM qancha to'laganini ochadi.
            assertThat(Permission.valueOf("SUBSCRIPTION_VIEW")).isNotNull();
            assertThat(Permission.SUBSCRIPTION_VIEW)
                    .isNotEqualTo(Permission.TARIFF_VIEW);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private List<SubscriptionDto> search(SubscriptionSource source, Boolean active, String q) {
        return subscriptionRepo.search(source, null, active, null, null, q,
                        LocalDateTime.now(), PageRequest.of(0, 50))
                .getContent().stream().map(SubscriptionDto::from).toList();
    }

    private Subscription sub(User u, SubscriptionSource source,
                             BigDecimal paid, boolean stillRunning) {
        LocalDateTime now = LocalDateTime.now();
        return subscriptionRepo.save(Subscription.builder()
                .user(u)
                .startAt(stillRunning ? now.minusDays(1) : now.minusMonths(3))
                .endAt(stillRunning ? now.plusMonths(1) : now.minusMonths(2))
                .source(source)
                .paidAmount(paid)
                .build());
    }

    private User user(String name) {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        int n = SEQ.incrementAndGet();
        u.setPhone("+99890" + (9900000 + n));
        u.setPassword("xesh-" + n);
        u.setName(name + " " + n);
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    // ------------------------------------------------- saralash (ТЗ §95)

    @Nested
    @DisplayName("Saralash")
    class Sorting {

        /**
         * ⚠️ Ilgari so'rov ichida {@code order by s.startAt desc}
         * turardi. Spring uni {@code Pageable} tartibi bilan
         * BIRLASHTIRARDI — avval qat'iy ustun, keyin so'ralgani. Ya'ni
         * klient so'ragan saralash jimgina bosib ketilardi.
         */
        @Test
        @DisplayName("To'langan summa bo'yicha saralanadi")
        void sortsByPaidAmount() {
            User u = user("Saralash");
            LocalDateTime now = LocalDateTime.now();

            // ⚠️ Sanalar summa bilan QARAMA-QARSHI qo'yilgan: yangi
            // obuna qimmatroq, eskisi arzonroq. Agar so'rov ichida
            // qat'iy `order by startAt desc` qolsa, u birinchi
            // ishlaydi va qimmatini oldinga chiqaradi — ya'ni test
            // farqni ko'radi. Sanalar bir xil bo'lganda esa test
            // hech nimani isbotlamasdi.
            subscriptionRepo.save(Subscription.builder()
                    .user(u).startAt(now.minusDays(1)).endAt(now.plusMonths(1))
                    .source(SubscriptionSource.PURCHASE)
                    .paidAmount(new BigDecimal("50000.00")).build());
            subscriptionRepo.save(Subscription.builder()
                    .user(u).startAt(now.minusMonths(6)).endAt(now.plusMonths(1))
                    .source(SubscriptionSource.PURCHASE)
                    .paidAmount(new BigDecimal("10000.00")).build());

            var asc = subscriptionRepo.search(null, null, null, null, null, u.getName(),
                    LocalDateTime.now(),
                    PageRequest.of(0, 10, org.springframework.data.domain.Sort
                            .by(org.springframework.data.domain.Sort.Direction.ASC, "paidAmount")))
                    .getContent();

            assertThat(asc).hasSize(2);
            assertThat(asc.get(0).getPaidAmount())
                    .as("o'sish tartibida arzoni birinchi")
                    .isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("Boshlanish sanasi bo'yicha saralanadi")
        void sortsByStartDate() {
            User u = user("Sana");
            sub(u, SubscriptionSource.PURCHASE, new BigDecimal("1.00"), false);
            sub(u, SubscriptionSource.PURCHASE, new BigDecimal("1.00"), true);

            var desc = subscriptionRepo.search(null, null, null, null, null, u.getName(),
                    LocalDateTime.now(),
                    PageRequest.of(0, 10, org.springframework.data.domain.Sort
                            .by(org.springframework.data.domain.Sort.Direction.DESC, "startAt")))
                    .getContent();

            assertThat(desc.get(0).getStartAt())
                    .as("kamayish tartibida yangisi birinchi")
                    .isAfter(desc.get(1).getStartAt());
        }
    }
}
