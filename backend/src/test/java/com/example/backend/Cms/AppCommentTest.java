package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.CommentStatus;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.CommentRepo;
import com.example.backend.Cms.Service.AppCommentService;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Entity.User;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ilovadagi izohlar — {@code /api/v1/app/content/{id}/comments} (10.09.2026).
 *
 * <h2>Nima jim buziladi</h2>
 * <ul>
 *   <li>Moderator yashirgan izoh ro'yxatga qaytib chiqadi — moderatsiya
 *       ma'nosini yo'qotadi, xato esa chiqmaydi.</li>
 *   <li>Birov boshqaning izohini o'chira oladi.</li>
 *   <li>Qoralama kontentga izoh yozish orqali uning borligi oshkor
 *       bo'ladi.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppCommentTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentFixtures contentFixtures;
    @Autowired private CommentRepo commentRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private UserAdminService userAdminService;
    @Autowired private EntityManager entityManager;

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------- yordamchi

    private User person(String name) {
        return userRepo.save(User.builder()
                .phone("+99891" + (1_000_000 + SEQ.incrementAndGet() % 1_000_000))
                .name(name)
                .roles(List.of())
                .build());
    }

    private Content film(PublicationStatus status) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(status);
        c.setTranslations(Translations.all("Izohli film " + SEQ.incrementAndGet()));
        return contentFixtures.create(c);
    }

    private Comment comment(Content content, User author, String text, CommentStatus status) {
        return commentRepo.save(Comment.builder()
                .content(content)
                .author(author)
                .text(text)
                .status(status)
                .build());
    }

    private void signIn(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private String url(Content content) {
        return "/api/v1/app/content/" + content.getId() + "/comments";
    }

    private static String body(String text) {
        return "{\"text\":" + (text == null ? "null" : "\"" + text + "\"") + "}";
    }

    // ------------------------------------------------------------- ro'yxat

    @Nested
    @DisplayName("Ro'yxat")
    class Listing {

        @Test
        @DisplayName("Mehmon ko'rinadiganlarini ko'radi — yashirilgan va o'chirilgan yo'q")
        void guestSeesOnlyVisible() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            User author = person("Malika");
            comment(film, author, "Zo'r film", CommentStatus.VISIBLE);
            comment(film, author, "Haqoratli gap", CommentStatus.HIDDEN);
            comment(film, author, "Eski fikr", CommentStatus.DELETED);

            mockMvc.perform(get(url(film)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalItems").value(1))
                    .andExpect(jsonPath("$.items[0].text").value("Zo'r film"))
                    .andExpect(jsonPath("$.items[0].authorName").value("Malika"))
                    .andExpect(jsonPath("$.items[0].mine").value(false));
        }

        /**
         * {@code CommentStatus.HIDDEN} qoidasi: muallif o'z izohini ko'radi.
         * Aks holda odam izohi «yo'qolganini» ko'rib, qayta-qayta yozardi.
         */
        @Test
        @DisplayName("Muallif moderator yashirgan O'Z izohini belgi bilan ko'radi, begonanikini yo'q")
        void authorSeesOwnHidden() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            User author = person("Aziz");
            User other = person("Boshqa");
            comment(film, author, "Mening yashirilgan izohim", CommentStatus.HIDDEN);
            comment(film, other, "Begona yashirilgan", CommentStatus.HIDDEN);

            signIn(author);
            mockMvc.perform(get(url(film)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalItems").value(1))
                    .andExpect(jsonPath("$.items[0].text").value("Mening yashirilgan izohim"))
                    .andExpect(jsonPath("$.items[0].hidden").value(true))
                    .andExpect(jsonPath("$.items[0].mine").value(true));
        }

        @Test
        @DisplayName("Qoralama kontent izohlari — 404, «bor, lekin yopiq» emas")
        void draftContentIsNotFound() throws Exception {
            Content draft = film(PublicationStatus.DRAFT);

            mockMvc.perform(get(url(draft))).andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------- yozish

    @Nested
    @DisplayName("Yozish")
    class Posting {

        @Test
        @DisplayName("Mehmon yoza olmaydi — 401")
        void guestCannotPost() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);

            mockMvc.perform(post(url(film))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Salom")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Kirgan odam yozadi — bo'shliqlar kesiladi, ro'yxatda birinchi")
        void signedInUserPosts() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            User author = person("Dilnoza");
            comment(film, person("Oldingi"), "Oldingi izoh", CommentStatus.VISIBLE);

            signIn(author);
            mockMvc.perform(post(url(film))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("  Juda yoqdi!  ")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").value("Juda yoqdi!"))
                    .andExpect(jsonPath("$.mine").value(true));

            mockMvc.perform(get(url(film)))
                    .andExpect(jsonPath("$.totalItems").value(2))
                    .andExpect(jsonPath("$.items[0].text").value("Juda yoqdi!"));
        }

        @Test
        @DisplayName("Bo'sh va juda uzun izoh rad etiladi")
        void rejectsEmptyAndTooLong() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            signIn(person("Sinovchi"));

            mockMvc.perform(post(url(film))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("   ")))
                    .andExpect(status().is4xxClientError());

            mockMvc.perform(post(url(film))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("a".repeat(AppCommentService.MAX_LENGTH + 1))))
                    .andExpect(status().is4xxClientError());

            assertThat(commentRepo.countByContentIdAndStatus(film.getId(), CommentStatus.VISIBLE))
                    .isZero();
        }

        @Test
        @DisplayName("Bloklangan hisob yoza olmaydi")
        void blockedUserCannotPost() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            User blocked = person("Bloklangan");
            // Admin paneldagi o'sha yo'l — hisob holatini qo'lda yasamaymiz.
            userAdminService.setBlocked(null, blocked.getId(), true, "sinov");

            signIn(blocked);
            mockMvc.perform(post(url(film))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Blokni aylanib o'tish")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Qoralama kontentga yozib bo'lmaydi")
        void cannotPostToDraft() throws Exception {
            Content draft = film(PublicationStatus.DRAFT);
            signIn(person("Sinovchi"));

            mockMvc.perform(post(url(draft))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Salom")))
                    .andExpect(status().isNotFound());
        }
    }

    // ------------------------------------------------------------ o'chirish

    @Nested
    @DisplayName("O'chirish")
    class Deleting {

        @Test
        @DisplayName("O'z izohi — DELETED bo'ladi (hard delete emas) va ro'yxatdan chiqadi")
        void ownCommentIsSoftDeleted() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            User author = person("Muallif");
            Comment mine = comment(film, author, "O'chiraman", CommentStatus.VISIBLE);

            signIn(author);
            mockMvc.perform(delete("/api/v1/app/comments/" + mine.getId()))
                    .andExpect(status().isNoContent());
            // Takror — xato emas: javob yo'qolib, ilova qayta yuborishi mumkin.
            mockMvc.perform(delete("/api/v1/app/comments/" + mine.getId()))
                    .andExpect(status().isNoContent());

            entityManager.flush();
            entityManager.clear();
            assertThat(commentRepo.findById(mine.getId()).orElseThrow().getStatus())
                    .as("yozuv qoladi — shikoyat tarixi va moderator qarori uchun")
                    .isEqualTo(CommentStatus.DELETED);

            mockMvc.perform(get(url(film))).andExpect(jsonPath("$.totalItems").value(0));
        }

        @Test
        @DisplayName("Begona izohni o'chirib bo'lmaydi — 403")
        void cannotDeleteSomeoneElses() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            Comment theirs = comment(film, person("Egasi"), "Meniki", CommentStatus.VISIBLE);

            signIn(person("Begona"));
            mockMvc.perform(delete("/api/v1/app/comments/" + theirs.getId()))
                    .andExpect(status().isForbidden());

            assertThat(commentRepo.findById(theirs.getId()).orElseThrow().getStatus())
                    .isEqualTo(CommentStatus.VISIBLE);
        }
    }
}
