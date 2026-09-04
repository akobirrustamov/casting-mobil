package com.example.backend.Cms;

import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Entity.NotificationTranslation;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.NotificationAudience;
import com.example.backend.Cms.Enums.NotificationStatus;
import com.example.backend.Cms.Repository.NotificationRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bildirishnomalar — ilova ichida o'qish.
 *
 * <h2>⚠️ Qaysi bo'shliq yopilyapti</h2>
 * Modul backendda to'liq edi — jadval, tarjimalar, rejalashtirish,
 * admin sahifasi. Ilovada esa bo'sh ekran turardi: admin yozgan xabarni
 * hech kim ko'rmasdi.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * Ikki narsa jimgina buzilishi mumkin: qoralama xabarning chiqib
 * ketishi va auditoriya filtri. Ikkalasi ham ekranda «ishlayotgandek»
 * ko'rinadi — ro'yxat chiqadi, xato yo'q; shunchaki noto'g'ri odam
 * noto'g'ri xabarni ko'radi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppNotificationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String URL = "/api/v1/app/notifications";

    @Autowired private MockMvc mockMvc;
    @Autowired private NotificationRepo notificationRepo;
    @Autowired private UserAccountRepo accountRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private JwtService jwtService;

    @Nested
    @DisplayName("Nima ko'rinadi")
    class Visibility {

        @Test
        @DisplayName("Yuborilgan xabar ro'yxatda")
        void sentIsListed() throws Exception {
            String title = notification(NotificationStatus.SENT, NotificationAudience.ALL);

            mockMvc.perform(get(URL).header("Authorization", token(user())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(title)));
        }

        /**
         * ⚠️ Qoralama hali xabar EMAS — u admin yozib tugatmagan matn.
         * Uning chiqib ketishi eng jimgina xato bo'lardi: ro'yxat
         * ishlaydi, shunchaki ichida tayyor bo'lmagan matn turadi.
         */
        @Test
        @DisplayName("Qoralama ko'rinmaydi")
        void draftIsHidden() throws Exception {
            String title = notification(NotificationStatus.DRAFT, NotificationAudience.ALL);

            mockMvc.perform(get(URL).header("Authorization", token(user())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(
                            org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(title))));
        }

        @Test
        @DisplayName("Rejalashtirilgani ham ko'rinmaydi")
        void scheduledIsHidden() throws Exception {
            String title = notification(NotificationStatus.SCHEDULED, NotificationAudience.ALL);

            mockMvc.perform(get(URL).header("Authorization", token(user())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(
                            org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(title))));
        }
    }

    @Nested
    @DisplayName("Auditoriya")
    class Audience {

        @Test
        @DisplayName("PREMIUM_ONLY faqat obunachiga")
        void premiumOnly() throws Exception {
            String title = notification(NotificationStatus.SENT, NotificationAudience.PREMIUM_ONLY);

            User plain = user();
            mockMvc.perform(get(URL).header("Authorization", token(plain)))
                    .andExpect(content().string(
                            org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(title))));

            User premium = premiumUser();
            mockMvc.perform(get(URL).header("Authorization", token(premium)))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(title)));
        }

        /** «Obuna oling» taklifi obunachiga ko'rsatilmaydi. */
        @Test
        @DisplayName("NON_PREMIUM obunachiga ko'rinmaydi")
        void nonPremium() throws Exception {
            String title = notification(NotificationStatus.SENT, NotificationAudience.NON_PREMIUM);

            mockMvc.perform(get(URL).header("Authorization", token(premiumUser())))
                    .andExpect(content().string(
                            org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(title))));

            mockMvc.perform(get(URL).header("Authorization", token(user())))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(title)));
        }
    }

    @Nested
    @DisplayName("Til va kirish")
    class LanguageAndAccess {

        @Test
        @DisplayName("So'rovdagi til qo'llanadi")
        void languageFromQuery() throws Exception {
            String base = notification(NotificationStatus.SENT, NotificationAudience.ALL);

            mockMvc.perform(get(URL).param("locale", "RU")
                            .header("Authorization", token(user())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(base + " RU")));
        }

        /** Auditoriya Premium holatiga bog'liq — mehmon uchun javob yo'q. */
        @Test
        @DisplayName("Token yo'q bo'lsa 401")
        void anonymousRejected() throws Exception {
            mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Xabar yo'q bo'lsa bo'sh ro'yxat, xato emas")
        void emptyIsNotAnError() throws Exception {
            mockMvc.perform(get(URL).header("Authorization", token(user())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    // ---------------------------------------------------------- yordamchi

    /** @return sarlavhaning o'zbekcha matni — javobdan izlash uchun */
    private String notification(NotificationStatus status, NotificationAudience audience) {
        String base = "Xabar-" + SEQ.incrementAndGet() + "-" + System.nanoTime() % 100000;

        Notification n = Notification.builder()
                .status(status)
                .audience(audience)
                .sentAt(status == NotificationStatus.SENT ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now())
                .build();

        for (Locale l : List.of(Locale.UZ, Locale.RU, Locale.EN)) {
            NotificationTranslation t = NotificationTranslation.builder()
                    .notification(n)
                    .locale(l)
                    .title(l == Locale.UZ ? base : base + " " + l.name())
                    .body("Matn")
                    .build();
            n.getTranslations().add(t);
        }
        notificationRepo.save(n);
        return base;
    }

    private User user() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        int n = SEQ.incrementAndGet();
        u.setPhone("+99890" + (8500000 + n));
        u.setPassword("xesh-" + n);
        u.setName("O'quvchi " + n);
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    private User premiumUser() {
        User u = user();
        accountRepo.save(UserAccount.builder()
                .user(u)
                .premiumUntil(LocalDateTime.now().plusMonths(1))
                .createdAt(LocalDateTime.now())
                .build());
        return u;
    }

    private String token(User u) {
        return "Bearer " + jwtService.generateJwtToken(u);
    }
}
