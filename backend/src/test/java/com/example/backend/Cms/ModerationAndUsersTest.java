package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Repository.CommentRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.ModerationService;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.support.CapturingStatementInspector;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §34 (izoh moderatsiyasi) va §35 (foydalanuvchilarni boshqarish).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ModerationAndUsersTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private ModerationService moderationService;
    @Autowired private UserAdminService userAdminService;
    @Autowired private CommentRepo commentRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private UserAccountRepo accountRepo;
    @Autowired private ContentService contentService;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

    // -------------------------------------------------------------- §34

    @Nested
    @DisplayName("Izoh moderatsiyasi (ТЗ §34)")
    class Moderation {

        @Test
        @DisplayName("⚠️ Filtrlar BIRGA ishlaydi — bir-birini inkor qilmaydi")
        void filtersCombine() {
            Content filmA = content();
            Content filmB = content();
            User author = appUser();

            comment(filmA, author, CommentStatus.HIDDEN, "yashirilgan A");
            comment(filmA, author, CommentStatus.VISIBLE, "ko'rinadigan A");
            comment(filmB, author, CommentStatus.HIDDEN, "yashirilgan B");

            Page<Comment> result = moderationService.comments(
                    CommentStatus.HIDDEN, filmA.getId(), null, null, null,
                    null, false, PageRequest.of(0, 20));

            // Ilgari if/else zanjiri edi: kino filtri status filtrini
            // jimgina yutib yuborardi va ro'yxatda ko'rinadigan izoh ham
            // chiqardi. Ekranda hech qanday xato ko'rinmasdi.
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getText()).isEqualTo("yashirilgan A");
        }

        @Test
        @DisplayName("Foydalanuvchi bo'yicha filtr — ТЗ da bor edi, kodda yo'q edi")
        void filterByUser() {
            Content film = content();
            User ali = appUser();
            User vali = appUser();

            comment(film, ali, CommentStatus.VISIBLE, "Ali yozdi");
            comment(film, vali, CommentStatus.VISIBLE, "Vali yozdi");

            Page<Comment> result = moderationService.comments(
                    null, null, ali.getId(), null, null, null, false,
                    PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getText()).isEqualTo("Ali yozdi");
        }

        @Test
        @DisplayName("Sana bo'yicha filtr — ТЗ da bor edi, kodda yo'q edi")
        void filterByDate() {
            Content film = content();
            User author = appUser();

            Comment old = comment(film, author, CommentStatus.VISIBLE, "eski");
            old.setCreatedAt(LocalDateTime.now().minusDays(10));
            commentRepo.save(old);
            comment(film, author, CommentStatus.VISIBLE, "yangi");

            Page<Comment> result = moderationService.comments(
                    null, film.getId(), null, LocalDateTime.now().minusDays(1), null,
                    null, false, PageRequest.of(0, 20));

            assertThat(result.getContent()).extracting(Comment::getText)
                    .containsExactly("yangi");
        }

        @Test
        @DisplayName("Matn qidiruvi status bilan birga ishlaydi")
        void searchCombinesWithStatus() {
            Content film = content();
            User author = appUser();

            comment(film, author, CommentStatus.HIDDEN, "haqoratli so'z bor");
            comment(film, author, CommentStatus.VISIBLE, "haqoratli so'z bor");

            Page<Comment> result = moderationService.comments(
                    CommentStatus.HIDDEN, null, null, null, null,
                    "haqoratli", false, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(CommentStatus.HIDDEN);
        }

        @Test
        @DisplayName("Shikoyat qilinganlar filtri boshqa filtr bilan birga")
        void reportedOnlyCombines() {
            Content film = content();
            User author = appUser();

            Comment reported = comment(film, author, CommentStatus.VISIBLE, "shikoyatli");
            reported.setReportsCount(3);
            commentRepo.save(reported);
            comment(film, author, CommentStatus.VISIBLE, "tinch");

            Page<Comment> result = moderationService.comments(
                    CommentStatus.VISIBLE, film.getId(), null, null, null,
                    null, true, PageRequest.of(0, 20));

            assertThat(result.getContent()).extracting(Comment::getText)
                    .containsExactly("shikoyatli");
        }

        @Test
        @DisplayName("Bitta harfli qidiruv e'tiborsiz qoldiriladi")
        void oneLetterSearchIsIgnored() {
            Content film = content();
            comment(film, appUser(), CommentStatus.VISIBLE, "matn");

            // Bitta harf butun bazani skanerlashiga arzimaydi.
            Page<Comment> result = moderationService.comments(
                    null, film.getId(), null, null, null, "z", false,
                    PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("ТЗ §34 ro'yxat maydonlari to'liq")
        void listFieldsAreComplete() {
            Content film = content();
            User author = appUser();
            Comment c = comment(film, author, CommentStatus.VISIBLE, "izoh matni");
            c.setReportsCount(2);
            commentRepo.save(c);

            var dto = com.example.backend.Admin.Dto.CommentDto.from(
                    commentRepo.findById(c.getId()).orElseThrow());

            // user · content · episode (ixtiyoriy) · comment · createdAt ·
            // status · reportsCount
            assertThat(dto.getAuthorId()).isEqualTo(author.getId());
            assertThat(dto.getAuthorName()).isNotBlank();
            assertThat(dto.getContentId()).isEqualTo(film.getId());
            assertThat(dto.getEpisodeId()).isNull();       // ixtiyoriy
            assertThat(dto.getText()).isEqualTo("izoh matni");
            assertThat(dto.getCreatedAt()).isNotNull();
            assertThat(dto.getStatus()).isEqualTo(CommentStatus.VISIBLE);
            assertThat(dto.getReportsCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("⚠️ Telefon raqami standart holatda BERILMAYDI")
        void phoneIsNotExposedByDefault() {
            Comment c = comment(content(), appUser(), CommentStatus.VISIBLE, "izoh");

            var dto = com.example.backend.Admin.Dto.CommentDto.from(
                    commentRepo.findById(c.getId()).orElseThrow());

            // Izohni moderatsiya qilish uchun muallifning ismi va ID'si
            // yetarli. Ilgari telefon har doim qaytarilardi — ya'ni faqat
            // COMMENT_VIEW ruxsati berilgan xodim butun foydalanuvchi
            // bazasining telefonlarini ko'rardi.
            assertThat(dto.getAuthorPhone()).isNull();
            assertThat(dto.getAuthorId()).isNotNull();
        }

        @Test
        @DisplayName("USER_VIEW ruxsati bo'lsa telefon ko'rinadi")
        void phoneIsShownWithUserView() {
            User author = appUser();
            Comment c = comment(content(), author, CommentStatus.VISIBLE, "izoh");

            var dto = com.example.backend.Admin.Dto.CommentDto.from(
                    commentRepo.findById(c.getId()).orElseThrow(), true);

            assertThat(dto.getAuthorPhone()).isEqualTo(author.getPhone());
        }

        @Test
        @DisplayName("Muallifni bloklash izoh ekranidan mumkin")
        void authorCanBeBlockedFromModeration() {
            User author = appUser();
            Comment c = comment(content(), author, CommentStatus.VISIBLE, "qoidabuzar izoh");

            // ТЗ §34: «block user where authorized». Moderator izohdan
            // muallif ID'sini oladi va uni bloklaydi — ruxsati bo'lsa.
            var dto = com.example.backend.Admin.Dto.CommentDto.from(
                    commentRepo.findById(c.getId()).orElseThrow());
            var blocked = userAdminService.setBlocked(null, dto.getAuthorId(),
                    true, "Qoidabuzar izoh");

            assertThat(blocked.getStatus()).isEqualTo(UserStatus.BLOCKED);
        }

        @Test
        @DisplayName("Ro'yxat N+1 yubormaydi")
        void listDoesNotIssueNPlusOneQueries() {
            Content film = content();
            for (int i = 0; i < 10; i++) {
                comment(film, appUser(), CommentStatus.VISIBLE, "izoh " + i);
            }

            // ⚠️ Kontekst tozalanmasa obyektlar allaqachon yuklangan
            // bo'ladi va so'rov umuman yuborilmaydi — test bo'sh o'tardi.
            em.flush();
            em.clear();

            CapturingStatementInspector.clear();
            var page = moderationService.comments(null, film.getId(), null, null, null,
                    null, false, PageRequest.of(0, 10));
            page.getContent().forEach(com.example.backend.Admin.Dto.CommentDto::from);

            int userSelects = CapturingStatementInspector.selectsFrom("users").size();

            assertThat(page.getContent()).hasSize(10);
            // author, content va episode - dangasa @ManyToOne. join fetch
            // bo'lmasa 10 qatorlik sahifa 30 tagacha qo'shimcha so'rov
            // yuborardi.
            assertThat(userSelects)
                    .as("Mualliflar uchun yuborilgan so'rovlar: " + userSelects)
                    .isLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Yashirish va tiklash — hard delete yo'q")
        void hideAndRestore() {
            Comment c = comment(content(), appUser(), CommentStatus.VISIBLE, "izoh");

            moderationService.changeStatus(null, c.getId(), CommentStatus.HIDDEN);
            assertThat(commentRepo.findById(c.getId()).orElseThrow().getStatus())
                    .isEqualTo(CommentStatus.HIDDEN);

            moderationService.changeStatus(null, c.getId(), CommentStatus.VISIBLE);
            assertThat(commentRepo.findById(c.getId()).orElseThrow().getStatus())
                    .isEqualTo(CommentStatus.VISIBLE);

            // DELETED ham holat — satr baribir qoladi, moderator qarori
            // saqlanadi.
            moderationService.changeStatus(null, c.getId(), CommentStatus.DELETED);
            assertThat(commentRepo.findById(c.getId())).isPresent();
        }
    }

    // -------------------------------------------------------------- §35

    @Nested
    @DisplayName("Foydalanuvchilarni boshqarish (ТЗ §35)")
    class Users {

        @Test
        @DisplayName("Xodimlar ilova foydalanuvchilari ro'yxatiga tushmaydi")
        void staffAreExcluded() {
            User appUser = appUser();
            User staff = staffUser();

            List<Object> ids = userAdminService
                    .searchPage(null, PageRequest.of(0, 200))
                    .getContent().stream()
                    .map(r -> (Object) r.user().getId()).toList();

            // Xodimlar §12 dagi alohida ekranda boshqariladi. Aralashsa,
            // admin o'zini bloklab qo'yishi mumkin edi.
            assertThat(ids).contains(appUser.getId());
            assertThat(ids).doesNotContain(staff.getId());
        }

        @Test
        @DisplayName("Sahifalash bazada — ro'yxat chegaralanadi")
        void listIsPaged() {
            for (int i = 0; i < 5; i++) {
                appUser();
            }

            Page<UserAdminService.AppUserRow> first =
                    userAdminService.searchPage(null, PageRequest.of(0, 2));

            assertThat(first.getContent()).hasSize(2);
            assertThat(first.getTotalElements()).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Telefon bo'yicha qidiruv")
        void searchByPhone() {
            User u = appUser();

            Page<UserAdminService.AppUserRow> result =
                    userAdminService.searchPage(u.getPhone(), PageRequest.of(0, 20));

            assertThat(result.getContent()).extracting(r -> r.user().getId())
                    .contains(u.getId());
        }

        @Test
        @DisplayName("ID bo'yicha qidiruv — ТЗ §38 premium sovg'a qilish uchun")
        void searchById() {
            User u = appUser();

            // §38: foydalanuvchini telefon, email YOKI ID orqali topish.
            // UUID'ni `like` bilan qidirib bo'lmaydi.
            var result = userAdminService.searchPage(u.getId().toString(),
                    PageRequest.of(0, 20));

            assertThat(result.getContent()).extracting(r -> r.user().getId())
                    .containsExactly(u.getId());
        }

        @Test
        @DisplayName("UUID bo'lmagan matn oddiy qidiruv sifatida ishlaydi")
        void nonUuidTextIsPlainSearch() {
            User u = appUser();

            assertThat(userAdminService.searchPage("Sinov", PageRequest.of(0, 200))
                    .getContent()).extracting(r -> r.user().getId())
                    .contains(u.getId());
        }

        @Test
        @DisplayName("Premium uzaytiriladi — boshidan boshlanmaydi")
        void premiumIsExtendedNotReset() {
            User u = appUser();

            var first = userAdminService.grantPremium(null, u.getId(), 1, null);
            LocalDateTime afterFirst = first.getPremiumUntil();

            var second = userAdminService.grantPremium(null, u.getId(), 1, null);

            // Aks holda ikkinchi sovg'a birinchisini yeb qo'yardi va
            // foydalanuvchi to'lagan muddatini yo'qotardi.
            assertThat(second.getPremiumUntil()).isAfter(afterFirst);
        }

        @Test
        @DisplayName("ТЗ §35 ro'yxat maydonlari to'liq")
        void listFieldsAreComplete() {
            User u = appUser();
            userAdminService.grantPremium(null, u.getId(), 1, null);

            var row = userAdminService.searchPage(u.getPhone(), PageRequest.of(0, 20))
                    .getContent().stream()
                    .filter(r -> r.user().getId().equals(u.getId()))
                    .findFirst().orElseThrow();
            var dto = com.example.backend.Admin.Dto.AppUserDto.from(
                    row.user(), row.account(), row.balance(), row.activeDevices());

            // id · avatar · name · phone · email · status ·
            // premium status · premium expiresAt · createdAt · lastActiveAt
            assertThat(dto.getId()).isEqualTo(u.getId());
            assertThat(dto.getName()).isNotBlank();
            assertThat(dto.getPhone()).isNotBlank();
            assertThat(dto.getStatus()).isNotNull();
            assertThat(dto.getPremiumActive()).isTrue();
            assertThat(dto.getPremiumUntil()).isAfter(LocalDateTime.now());
            assertThat(dto.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("⚠️ createdAt — RO'YXATDAN O'TGAN sana, admin tekkan vaqt emas")
        void createdAtIsRegistrationNotAdminTouch() {
            User u = appUser();
            LocalDateTime registered = u.getCreatedAt();

            assertThat(registered).isNotNull();

            // Admin keyinroq bloklaydi — shunda UserAccount satri yaratiladi.
            userAdminService.setBlocked(null, u.getId(), true, "sinov");

            var row = userAdminService.searchPage(u.getPhone(), PageRequest.of(0, 20))
                    .getContent().stream()
                    .filter(r -> r.user().getId().equals(u.getId()))
                    .findFirst().orElseThrow();
            var dto = com.example.backend.Admin.Dto.AppUserDto.from(
                    row.user(), row.account(), row.balance(), row.activeDevices());

            // Ilgari bu qiymat UserAccount dan olinardi. Hisob satri esa
            // DANGASA yaratiladi — ya'ni 2020-yilda ro'yxatdan o'tib
            // 2026-yilda bloklangan odam ro'yxatda «2026» bo'lib chiqardi.
            // Bo'sh katakdan ham yomon: admin raqamga ishonadi.
            assertThat(dto.getCreatedAt()).isEqualTo(registered);
            assertThat(dto.getCreatedAt())
                    .isNotEqualTo(row.account().getCreatedAt());
        }

        @Test
        @DisplayName("Bloklash va blokdan chiqarish")
        void blockAndUnblock() {
            User u = appUser();

            UserAccount blocked = userAdminService.setBlocked(null, u.getId(),
                    true, "Qoidabuzarlik");
            assertThat(blocked.getStatus()).isEqualTo(UserStatus.BLOCKED);
            assertThat(blocked.getBlockedReason()).isEqualTo("Qoidabuzarlik");

            UserAccount unblocked = userAdminService.setBlocked(null, u.getId(), false, null);
            assertThat(unblocked.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(unblocked.getBlockedReason()).isNull();
        }

        @Test
        @DisplayName("Premium berish va qaytarib olish")
        void grantAndRevokePremium() {
            User u = appUser();

            UserAccount granted = userAdminService.grantPremium(null, u.getId(), 1, null);
            assertThat(granted.hasActivePremium()).isTrue();
            assertThat(granted.getPremiumUntil()).isAfter(LocalDateTime.now());

            UserAccount revoked = userAdminService.revokePremium(null, u.getId());
            assertThat(revoked.hasActivePremium()).isFalse();
        }

        @Test
        @DisplayName("Ro'yxatda createdAt bor — ТЗ §35 talab qiladi")
        void listExposesCreatedAt() {
            User u = appUser();
            accountRepo.save(UserAccount.builder()
                    .user(u)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build());

            var row = userAdminService.searchPage(u.getPhone(), PageRequest.of(0, 20))
                    .getContent().stream()
                    .filter(r -> r.user().getId().equals(u.getId()))
                    .findFirst().orElseThrow();

            assertThat(com.example.backend.Admin.Dto.AppUserDto.from(
                    row.user(), row.account(), row.balance(), row.activeDevices())
                    .getCreatedAt()).isNotNull();
        }
    }

    // ------------------------------------------------------------ yordamchi

    private Comment comment(Content content, User author, CommentStatus status, String text) {
        return commentRepo.save(Comment.builder()
                .content(content)
                .author(author)
                .text(text)
                .status(status)
                .reportsCount(0)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private User appUser() {
        return newUser(UserRoles.ROLE_USER);
    }

    private User staffUser() {
        return newUser(UserRoles.ROLE_ADMIN);
    }

    private User newUser(UserRoles role) {
        Role r = roleRepo.findByName(role);
        if (r == null) {
            int nextId = roleRepo.findAll().stream()
                    .mapToInt(Role::getId).max().orElse(0) + 1;
            r = roleRepo.save(new Role(nextId, role));
        }
        User u = new User();
        u.setPhone("+99890" + (7000000 + SEQ.incrementAndGet()));
        u.setPassword("x");
        u.setName("Sinov " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(r)));
        return userRepo.save(u);
    }

    private Content content() {
        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(ContentType.MOVIE);
        req.setStructureType(StructureType.SINGLE);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(PublicationStatus.DRAFT);
        req.setTranslations(Translations.all("Izoh filmi " + SEQ.incrementAndGet()));
        return contentService.create(null, req);
    }
}
