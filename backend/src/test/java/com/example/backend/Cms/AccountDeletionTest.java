package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Entity.UserDevice;
import com.example.backend.Cms.Entity.UserFavorite;
import com.example.backend.Cms.Entity.WatchProgress;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.CommentStatus;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.FavoriteType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Cms.Enums.WatchTargetType;
import com.example.backend.Cms.Repository.CommentRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Repository.UserDeviceRepo;
import com.example.backend.Cms.Repository.UserFavoriteRepo;
import com.example.backend.Cms.Repository.WatchProgressRepo;
import com.example.backend.Cms.Service.AccountDeletionService;
import com.example.backend.Entity.RefreshToken;
import com.example.backend.Entity.User;
import com.example.backend.Repository.RefreshTokenRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.support.ContentFixtures;
import com.example.backend.support.Translations;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hisobni o'chirish — {@code DELETE /api/v1/app/me} (13.09.2026).
 *
 * <h2>Nima uchun bu test bor</h2>
 * Google Play'ning «Data deletion» siyosati shartsiz: hisob yaratish mumkin
 * bo'lgan ilovada uni o'chirish ham bo'lishi kerak. Lekin tekshiruvchi
 * faqat tugmaning BORLIGINI ko'radi — u bosilgandan keyin bazada nima
 * qolganini hech kim tekshirmaydi. Aynan shu yerda jim buziladi.
 *
 * <h2>Nima jim buziladi</h2>
 * <ul>
 *   <li>Telefon raqami qolib ketadi — «o'chirdim» degan odamning raqami
 *       bazada turaveradi va u bilan qaytadan ro'yxatdan o'tib bo'lmaydi.</li>
 *   <li>Sessiya bekor qilinmaydi — telefondagi ilova o'chirilgan hisob
 *       nomidan ishlashda davom etadi.</li>
 *   <li>To'lov yozuvlari cascade bilan ketadi — buxgalteriya yo'qoladi,
 *       va buni faqat hisobot yig'ilganda payqashadi.</li>
 *   <li>Izohlar muallif ismi bilan qoladi — ism o'chirilishi kerak edi.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountDeletionTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentFixtures contentFixtures;
    @Autowired private UserRepo userRepo;
    @Autowired private UserAccountRepo accountRepo;
    @Autowired private UserDeviceRepo deviceRepo;
    @Autowired private UserFavoriteRepo favoriteRepo;
    @Autowired private WatchProgressRepo watchProgressRepo;
    @Autowired private RefreshTokenRepo refreshTokenRepo;
    @Autowired private CommentRepo commentRepo;
    @Autowired private EntityManager entityManager;

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------- yordamchi

    private String uniquePhone() {
        return "+99890" + (1_000_000 + SEQ.incrementAndGet() % 1_000_000);
    }

    private User person(String name, String phone) {
        return userRepo.save(User.builder()
                .phone(phone)
                .name(name)
                .email("delete" + SEQ.incrementAndGet() + "@example.com")
                .avatarUrl("https://example.com/avatar.png")
                .password("hash")
                .passwordSet(true)
                .roles(List.of())
                .build());
    }

    /**
     * ⚠️ YANGI kontekst, {@code getContext().setAuthentication(...)} emas.
     *
     * MockMvc so'rovidan keyin holder'da kechiktirilgan (deferred) kontekst
     * qoladi, va unga yozilgan yangi foydalanuvchi KEYINGI so'rovga
     * yetib bormaydi — server eskisini ko'radi. Bitta testda ikki kishi
     * qatnashsa (masalan ikkita shikoyat), ikkinchisi birinchisi bo'lib
     * qolardi va sabab umuman ko'rinmasdi.
     */
    private void signIn(User user) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    private Content film() {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setTranslations(Translations.all("O'chirish filmi " + SEQ.incrementAndGet()));
        return contentFixtures.create(c);
    }

    /** Odamning izi bo'lgan hamma narsa: qurilma, sevimli, ko'rish joyi, sessiya. */
    private void fillTraces(User user, Content content) {
        deviceRepo.save(UserDevice.builder()
                .user(user)
                .deviceId("device-" + SEQ.incrementAndGet())
                .deviceName("Ali iPhone")
                .platform("ios")
                .createdAt(LocalDateTime.now())
                .build());

        favoriteRepo.save(UserFavorite.builder()
                .user(user)
                .type(FavoriteType.CONTENT)
                .targetId(content.getId())
                .createdAt(LocalDateTime.now())
                .build());

        watchProgressRepo.save(WatchProgress.builder()
                .user(user)
                .type(WatchTargetType.CONTENT)
                .targetId(content.getId())
                .positionSeconds(120)
                .updatedAt(LocalDateTime.now())
                .build());

        // ⚠️ `RefreshToken.id` — qo'lda beriladi (@Id, @GeneratedValue yo'q):
        // tokenning o'zi shu UUID'dan yasaladi, ya'ni uni baza emas,
        // kod tanlaydi.
        refreshTokenRepo.save(RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }

    // ------------------------------------------------------------- o'chirish

    @Nested
    @DisplayName("O'chirish")
    class Deleting {

        @Test
        @DisplayName("Shaxsiy ma'lumot tozalanadi, qator esa nomsiz qoladi")
        void personalDataIsWiped() throws Exception {
            User user = person("Ali", uniquePhone());
            signIn(user);

            mockMvc.perform(delete("/api/v1/app/me"))
                    .andExpect(status().isNoContent());
            flush();

            User after = userRepo.findById(user.getId()).orElseThrow();
            assertThat(after.getPhone()).isNull();
            assertThat(after.getEmail()).isNull();
            assertThat(after.getGoogleSub()).isNull();
            assertThat(after.getAvatarUrl()).isNull();
            assertThat(after.getName()).isEqualTo(AccountDeletionService.ANONYMOUS_NAME);
        }

        /**
         * ⚠️ Eng muhim tekshiruv. Raqam bo'shamasa, «o'chirish» aslida
         * umrbod blok bo'lardi: odam ilovaga boshqa hech qachon kira
         * olmasdi, chunki raqami band bo'lib qolardi.
         */
        @Test
        @DisplayName("Raqam bo'shaydi — u bilan qaytadan ro'yxatdan o'tish mumkin")
        void phoneBecomesFree() throws Exception {
            String phone = uniquePhone();
            User user = person("Malika", phone);
            signIn(user);

            mockMvc.perform(delete("/api/v1/app/me"))
                    .andExpect(status().isNoContent());
            flush();

            assertThat(userRepo.findByPhone(phone)).isEmpty();

            // Xuddi shu raqam bilan yangi hisob — hech qanday cheklovga urilmaydi.
            User again = person("Malika", phone);
            assertThat(again.getId()).isNotEqualTo(user.getId());
        }

        @Test
        @DisplayName("Sessiyalar bekor qilinadi — telefondagi ilova kirishda qololmaydi")
        void sessionsAreRevoked() throws Exception {
            User user = person("Bekzod", uniquePhone());
            Content film = film();
            fillTraces(user, film);
            flush();
            assertThat(refreshTokenRepo.findAllByUserId(user.getId())).isNotEmpty();

            signIn(userRepo.findById(user.getId()).orElseThrow());
            mockMvc.perform(delete("/api/v1/app/me"))
                    .andExpect(status().isNoContent());
            flush();

            assertThat(refreshTokenRepo.findAllByUserId(user.getId())).isEmpty();
        }

        @Test
        @DisplayName("Qurilmalar, saqlanganlar va ko'rish joyi o'chadi")
        void personalListsAreRemoved() throws Exception {
            User user = person("Dilnoza", uniquePhone());
            Content film = film();
            fillTraces(user, film);
            flush();

            signIn(userRepo.findById(user.getId()).orElseThrow());
            mockMvc.perform(delete("/api/v1/app/me"))
                    .andExpect(status().isNoContent());
            flush();

            UUID id = user.getId();
            assertThat(deviceRepo.findAll().stream()
                    .anyMatch(d -> d.getUser().getId().equals(id))).isFalse();
            assertThat(favoriteRepo.findAll().stream()
                    .anyMatch(f -> f.getUser().getId().equals(id))).isFalse();
            assertThat(watchProgressRepo.findAll().stream()
                    .anyMatch(w -> w.getUser().getId().equals(id))).isFalse();
        }

        @Test
        @DisplayName("Hisob DELETED deb belgilanadi va sanasi qo'yiladi")
        void accountIsMarkedDeleted() throws Exception {
            User user = person("Sardor", uniquePhone());
            accountRepo.save(UserAccount.builder()
                    .user(user)
                    .premiumUntil(LocalDateTime.now().plusDays(30))
                    .build());
            flush();

            signIn(userRepo.findById(user.getId()).orElseThrow());
            mockMvc.perform(delete("/api/v1/app/me"))
                    .andExpect(status().isNoContent());
            flush();

            UserAccount account = accountRepo.findByUserId(user.getId()).orElseThrow();
            assertThat(account.getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(account.getDeletedAt()).isNotNull();
            // Hisob yo'q — Premium ham yo'q.
            assertThat(account.getPremiumUntil()).isNull();
        }

        /**
         * ⚠️ Izoh QOLADI. Uni o'chirish qolganlarning javoblarini ma'nosiz
         * qoldirardi, Google esa buni talab qilmaydi — unga shaxsiy
         * ma'lumot muhim, va ism allaqachon tozalangan.
         */
        @Test
        @DisplayName("Izoh matni qoladi, muallif nomsiz bo'ladi")
        void commentSurvivesWithoutAuthorName() throws Exception {
            User user = person("Nodira", uniquePhone());
            Content film = film();
            Comment comment = commentRepo.save(Comment.builder()
                    .content(film)
                    .author(user)
                    .text("Ajoyib film edi")
                    .status(CommentStatus.VISIBLE)
                    .build());
            flush();

            signIn(userRepo.findById(user.getId()).orElseThrow());
            mockMvc.perform(delete("/api/v1/app/me"))
                    .andExpect(status().isNoContent());
            flush();

            Comment after = commentRepo.findById(comment.getId()).orElseThrow();
            assertThat(after.getText()).isEqualTo("Ajoyib film edi");
            assertThat(after.getStatus()).isEqualTo(CommentStatus.VISIBLE);
            assertThat(after.getAuthor().getName())
                    .isEqualTo(AccountDeletionService.ANONYMOUS_NAME);
        }

        @Test
        @DisplayName("Tokensiz — 401, begonaning hisobi o'chmaydi")
        void anonymousCannotDelete() throws Exception {
            mockMvc.perform(delete("/api/v1/app/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
