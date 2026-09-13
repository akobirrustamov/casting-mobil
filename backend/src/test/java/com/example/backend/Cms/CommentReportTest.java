package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.CommentStatus;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.CommentReportRepo;
import com.example.backend.Cms.Repository.CommentRepo;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Izohga shikoyat — {@code POST /api/v1/app/comments/{id}/report} (13.09.2026).
 *
 * <h2>Nima uchun bu endpoint bor</h2>
 * Google Play'ning UGC siyosati: foydalanuvchi yozgan matn ko'rinadigan
 * ilovada begona yozuvga shikoyat qilish yo'li bo'lishi shart. Admin
 * paneldagi moderatsiya navbati ({@code reportedOnly} filtri va shikoyatlar
 * bo'yicha saralash) boshidan bor edi, lekin hisoblagichni oshiradigan
 * hech kim yo'q edi — ya'ni filtr hamisha bo'sh ro'yxat qaytarardi.
 *
 * <h2>Nima jim buziladi</h2>
 * <ul>
 *   <li>Bir odam bitta izohga necha marta xohlasa shuncha shikoyat qiladi
 *       va uni moderatsiya navbatining tepasiga sun'iy chiqarib qo'yadi.</li>
 *   <li>Shikoyat izohni avtomatik yashiradi — kelishib olgan bir nechta
 *       hisob istalgan odamni jimlata oladi.</li>
 *   <li>Qoralama kontentdagi izohga shikoyat qilish orqali o'sha
 *       kontentning borligi oshkor bo'ladi.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentReportTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentFixtures contentFixtures;
    @Autowired private CommentRepo commentRepo;
    @Autowired private CommentReportRepo reportRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private EntityManager entityManager;

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------- yordamchi

    private User person(String name) {
        return userRepo.save(User.builder()
                .phone("+99893" + (1_000_000 + SEQ.incrementAndGet() % 1_000_000))
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
        c.setTranslations(Translations.all("Shikoyat filmi " + SEQ.incrementAndGet()));
        return contentFixtures.create(c);
    }

    private Comment comment(Content content, User author, CommentStatus status) {
        return commentRepo.save(Comment.builder()
                .content(content)
                .author(author)
                .text("Shikoyat qilinadigan matn")
                .status(status)
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

    /** Bitta so'rovga biriktiriladigan kirish — {@code .with(authentication(...))} uchun. */
    private static UsernamePasswordAuthenticationToken token(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private String url(Comment comment) {
        return "/api/v1/app/comments/" + comment.getId() + "/report";
    }

    private static String body(String reason) {
        return "{\"reason\":\"" + reason + "\"}";
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }

    // ------------------------------------------------------------- shikoyat

    @Nested
    @DisplayName("Shikoyat qilish")
    class Reporting {

        @Test
        @DisplayName("Hisoblagich oshadi — moderator navbatida ko'rinadi")
        void counterGrows() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            Comment target = comment(film, person("Muallif"), CommentStatus.VISIBLE);
            signIn(person("Shikoyatchi"));

            mockMvc.perform(post(url(target))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("SPAM")))
                    .andExpect(status().isNoContent());
            flush();

            assertThat(commentRepo.findById(target.getId()).orElseThrow()
                    .getReportsCount()).isEqualTo(1);
        }

        /**
         * ⚠️ Shikoyat — navbatga qo'yish, qaror emas. Aks holda kelishib
         * olgan uch-to'rtta hisob istalgan izohni o'chira olardi.
         */
        @Test
        @DisplayName("Izoh ko'rinishda qoladi — qarorni moderator qabul qiladi")
        void commentStaysVisible() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            Comment target = comment(film, person("Muallif"), CommentStatus.VISIBLE);
            signIn(person("Shikoyatchi"));

            mockMvc.perform(post(url(target))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("INSULT")))
                    .andExpect(status().isNoContent());
            flush();

            assertThat(commentRepo.findById(target.getId()).orElseThrow()
                    .getStatus()).isEqualTo(CommentStatus.VISIBLE);
        }

        @Test
        @DisplayName("Ikkinchi marta — 409, hisoblagich qo'shimcha oshmaydi")
        void secondReportIsRejected() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            Comment target = comment(film, person("Muallif"), CommentStatus.VISIBLE);
            signIn(person("Shikoyatchi"));

            mockMvc.perform(post(url(target))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("SPAM")))
                    .andExpect(status().isNoContent());
            flush();

            mockMvc.perform(post(url(target))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("SPAM")))
                    .andExpect(status().isConflict());
            flush();

            assertThat(commentRepo.findById(target.getId()).orElseThrow()
                    .getReportsCount()).isEqualTo(1);
            assertThat(reportRepo.count()).isEqualTo(1);
        }

        /**
         * ⚠️ Bu yerda kirish {@code .with(authentication(...))} orqali
         * beriladi, {@code SecurityContextHolder} orqali emas.
         *
         * MockMvc'da bitta testda ikki marta kirilsa, IKKINCHI so'rov
         * baribir birinchisining foydalanuvchisini ko'radi: kontekst
         * so'rovlar orasida tiklanadi. Natijada ikkinchi odamning
         * shikoyati «siz allaqachon shikoyat qilgansiz» degan javob olardi
         * va test yolg'on qizarardi — kod esa to'g'ri.
         *
         * Prodda bunday hol yo'q: har bir so'rov o'z tokeni bilan keladi.
         */
        @Test
        @DisplayName("Ikki xil odam — hisoblagich ikki bo'ladi")
        void twoPeopleTwoReports() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            Comment target = comment(film, person("Muallif"), CommentStatus.VISIBLE);

            mockMvc.perform(post(url(target))
                            .with(authentication(token(person("Birinchi"))))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("SPAM")))
                    .andExpect(status().isNoContent());
            flush();

            mockMvc.perform(post(url(target))
                            .with(authentication(token(person("Ikkinchi"))))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("ADULT")))
                    .andExpect(status().isNoContent());
            flush();

            assertThat(commentRepo.findById(target.getId()).orElseThrow()
                    .getReportsCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Sabab yuborilmasa ham qabul qilinadi — OTHER bo'ladi")
        void reasonIsOptional() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            Comment target = comment(film, person("Muallif"), CommentStatus.VISIBLE);
            signIn(person("Shikoyatchi"));

            mockMvc.perform(post(url(target)))
                    .andExpect(status().isNoContent());
            flush();

            assertThat(reportRepo.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Cheklovlar")
    class Limits {

        @Test
        @DisplayName("O'z izohiga shikoyat qilib bo'lmaydi")
        void ownCommentCannotBeReported() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            User author = person("Muallif");
            Comment own = comment(film, author, CommentStatus.VISIBLE);
            signIn(author);

            mockMvc.perform(post(url(own))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("SPAM")))
                    .andExpect(status().isUnprocessableEntity());
            flush();

            assertThat(commentRepo.findById(own.getId()).orElseThrow()
                    .getReportsCount()).isZero();
        }

        @Test
        @DisplayName("O'chirilgan izoh — 404, shikoyat qiladigan narsa yo'q")
        void deletedCommentIsGone() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            Comment gone = comment(film, person("Muallif"), CommentStatus.DELETED);
            signIn(person("Shikoyatchi"));

            mockMvc.perform(post(url(gone))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("SPAM")))
                    .andExpect(status().isNotFound());
        }

        /**
         * Qoralama kontent hamma uchun «yo'q» — izohlar ro'yxatida ham
         * shunday. Shikoyat orqali uning borligini bilib bo'lmasin.
         */
        @Test
        @DisplayName("Qoralama kontentdagi izoh — 404")
        void draftContentIsHidden() throws Exception {
            Content draft = film(PublicationStatus.DRAFT);
            Comment target = comment(draft, person("Muallif"), CommentStatus.VISIBLE);
            signIn(person("Shikoyatchi"));

            mockMvc.perform(post(url(target))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("SPAM")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Tokensiz — 401")
        void anonymousCannotReport() throws Exception {
            Content film = film(PublicationStatus.PUBLISHED);
            Comment target = comment(film, person("Muallif"), CommentStatus.VISIBLE);

            mockMvc.perform(post(url(target))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("SPAM")))
                    .andExpect(status().isUnauthorized());
        }
    }
}
