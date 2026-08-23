package com.example.backend.Cms;

import com.example.backend.Admin.Dto.AppUserDto;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.UserAccountRepo;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Foydalanuvchi tili panelda (mobil 3 tilli talabi).
 *
 * <h2>Nega bu kerak</h2>
 * Kontent, bildirishnoma va reklama uch tilda saqlanadi, lekin
 * FOYDALANUVCHINING tili hech qayerda ko'rinmasdi. Admin «nega bu odam
 * ruscha xabar oldi?» degan savolga javob topa olmasdi va
 * bildirishnoma yozayotganda RU matnini necha kishi o'qishini bilmasdi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserLanguageTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private UserAccountRepo accountRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    @Nested
    @DisplayName("Ro'yxatda ko'rinadi")
    class InList {

        @Test
        @DisplayName("Tanlangan til DTO'da qaytadi")
        void languageIsExposed() {
            User u = user();
            UserAccount acc = account(u, Locale.RU);

            assertThat(AppUserDto.from(u, acc, null, 0).getLanguage())
                    .isEqualTo(Locale.RU);
        }

        /**
         * ⚠️ Hisobi yo'q foydalanuvchida til {@code null} — «hali
         * tanlamagan», UZ emas.
         *
         * Uni UZ deb ko'rsatish taxminni fakt sifatida ko'rsatish
         * bo'lardi: bu odam hali ilovani ochmagan va hech narsa
         * tanlamagan (§45 — mavjud bo'lmagan ma'lumotni to'qima).
         */
        @Test
        @DisplayName("Hisobi yo'q foydalanuvchida til `null`")
        void unknownLanguageIsNull() {
            assertThat(AppUserDto.from(user(), null, null, 0).getLanguage())
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Auditoriya tillar bo'yicha")
    class Audience {

        @Test
        @DisplayName("Har bir til bo'yicha son hisoblanadi")
        void countsPerLanguage() {
            account(user(), Locale.RU);
            account(user(), Locale.RU);
            account(user(), Locale.EN);

            Map<Locale, Long> byLang = counts();

            assertThat(byLang.get(Locale.RU)).isGreaterThanOrEqualTo(2);
            assertThat(byLang.get(Locale.EN)).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Sukut qiymat UZ — mobil ilova birinchi ochilishda yuboradi")
        void defaultIsUzbek() {
            User u = user();
            UserAccount acc = accountRepo.save(UserAccount.builder()
                    .user(u)
                    .createdAt(LocalDateTime.now())
                    .build());

            // Default «bilmayman» degani emas: davlat tili, va mobil
            // ilova birinchi ochilishda haqiqiy qiymatni yuboradi.
            assertThat(acc.getLanguage()).isEqualTo(Locale.UZ);
        }

        private Map<Locale, Long> counts() {
            Map<Locale, Long> out = new java.util.HashMap<>();
            accountRepo.countByLanguage()
                    .forEach(r -> out.put(r.getLanguage(), r.getTotal()));
            return out;
        }
    }

    // ------------------------------------------------------------ yordamchi

    private UserAccount account(User u, Locale language) {
        return accountRepo.save(UserAccount.builder()
                .user(u)
                .language(language)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private User user() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        int n = SEQ.incrementAndGet();
        u.setPhone("+99893" + (1000000 + n));
        u.setPassword("xesh");
        u.setName("Tilli " + n);
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }
}
