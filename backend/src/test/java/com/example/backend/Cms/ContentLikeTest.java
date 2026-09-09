package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.AnalyticsEventType;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.FavoriteType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Service.AnalyticsService;
import com.example.backend.Cms.Service.ContentLikeService;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.FavoriteService;
import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.support.Translations;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * «Yoqdi» va ko'rishlar soni.
 *
 * <h2>Nima uchun bu yerda ko'p test «takroriy so'rov» haqida</h2>
 * Buyurtmachining talabi bir gap edi: «like bosish va prasmotrlani
 * mobilga qilish kerak». Lekin butun murakkablik shu yerda yashiringan:
 * telefon tarmog'ida so'rov yo'qoladi va klient uni QAYTA yuboradi.
 *
 * Agar server «bor bo'lsa o'chir, yo'q bo'lsa qo'y» qilsa, qayta
 * yuborilgan so'rov odam bosgan «yoqdi» ni jimgina yechib qo'yadi.
 * Shuning uchun PUT va DELETE alohida va idempotent — va aynan shu
 * xususiyat bu yerda tekshiriladi.
 *
 * <h2>Ikkinchi mavzu — «yoqdi» sevimlilar EMAS</h2>
 * Ular alohida jadvalda. Bitta qilib qo'yilsa, ro'yxatdan olib tashlagan
 * odam kontentdan «yoqdi» ni ham yulib olardi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContentLikeTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentLikeService likeService;
    @Autowired private ContentService contentService;
    @Autowired private ContentRepo contentRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private FavoriteService favoriteService;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private EntityManager entityManager;

    // ------------------------------------------------------------- yordamchi

    private User person(String name) {
        return userRepo.save(User.builder()
                .phone("+99890" + (1_000_000 + SEQ.incrementAndGet() % 1_000_000))
                .name(name)
                .roles(List.of())
                .build());
    }

    private Content movie() {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setDurationMinutes(90);
        c.setPremierePrice(new BigDecimal("5000"));
        c.setTranslations(Translations.all("Film " + SEQ.incrementAndGet()));
        return contentService.create(null, c);
    }

    /** Bazadagi haqiqiy qiymat — kontekstdagi eski nusxa emas. */
    private long likeCount(Content content) {
        entityManager.flush();
        entityManager.clear();
        return contentRepo.findById(content.getId()).orElseThrow().getLikeCount();
    }

    private void signIn(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Nested
    @DisplayName("Takroriy so'rov holatni buzmaydi")
    class Idempotent {

        @Test
        @DisplayName("Ikki marta PUT — bitta «yoqdi»")
        void repeatedLikeCountsOnce() {
            User viewer = person("Tomoshabin");
            Content film = movie();

            likeService.like(viewer, film.getId());
            ContentLikeService.LikeState second = likeService.like(viewer, film.getId());

            assertThat(second.liked()).isTrue();
            assertThat(second.likeCount()).isEqualTo(1);
            assertThat(likeCount(film)).isEqualTo(1);
        }

        @Test
        @DisplayName("Ikki marta DELETE — sanoq manfiy bo'lmaydi")
        void repeatedUnlikeStaysAtZero() {
            User viewer = person("Tomoshabin");
            Content film = movie();

            likeService.like(viewer, film.getId());
            likeService.unlike(viewer, film.getId());
            ContentLikeService.LikeState second = likeService.unlike(viewer, film.getId());

            assertThat(second.liked()).isFalse();
            assertThat(second.likeCount()).isZero();
            assertThat(likeCount(film)).isZero();
        }

        @Test
        @DisplayName("Bosib, keyin yechib — boshlang'ich holat")
        void likeThenUnlikeReturnsToStart() {
            User viewer = person("Tomoshabin");
            Content film = movie();

            assertThat(likeService.like(viewer, film.getId()).likeCount()).isEqualTo(1);
            assertThat(likeService.unlike(viewer, film.getId()).likeCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Sanoq odamlar bo'yicha")
    class PerPerson {

        @Test
        @DisplayName("Ikki odam — ikkita «yoqdi», biri yechsa bittasi qoladi")
        void twoPeopleCountTwice() {
            User first = person("Birinchi");
            User second = person("Ikkinchi");
            Content film = movie();

            likeService.like(first, film.getId());
            likeService.like(second, film.getId());
            assertThat(likeCount(film)).isEqualTo(2);

            likeService.unlike(first, film.getId());

            assertThat(likeCount(film)).isEqualTo(1);
            // ⚠️ Muhim: birinchisi yechgani ikkinchisining «yoqdi» siga
            // tegmasligi kerak. Umumiy sanoqni tekshirishning o'zi buni
            // isbotlamaydi.
            assertThat(likeService.isLiked(second.getId(), film.getId())).isTrue();
            assertThat(likeService.isLiked(first.getId(), film.getId())).isFalse();
        }

        @Test
        @DisplayName("Bitta odamning «yoqdi» si boshqa kontentga o'tmaydi")
        void likeDoesNotLeakToOtherContent() {
            User viewer = person("Tomoshabin");
            Content liked = movie();
            Content other = movie();

            likeService.like(viewer, liked.getId());

            assertThat(likeCount(liked)).isEqualTo(1);
            assertThat(likeCount(other)).isZero();
        }
    }

    @Nested
    @DisplayName("«Yoqdi» — «Saqlanganlar» emas")
    class NotFavorites {

        /**
         * ⚠️ Bu ikkalasi bir xil ko'ringani uchun aynan shu test kerak:
         * ikkalasi ham «yurakcha», ikkalasi ham odam va kontent orasidagi
         * bog'lanish. Farqi — biri ommaviy sanoq, ikkinchisi shaxsiy
         * ro'yxat, va ular BIR-BIRINI o'chirmasligi kerak.
         */
        @Test
        @DisplayName("«Yoqdi» saqlanganlar ro'yxatiga tushmaydi")
        void likeDoesNotAppearInFavorites() {
            User viewer = person("Tomoshabin");
            Content film = movie();

            likeService.like(viewer, film.getId());

            assertThat(favoriteService.list(viewer, FavoriteType.CONTENT))
                    .doesNotContain(film.getId());
        }

        @Test
        @DisplayName("Saqlanganlarga qo'shish «yoqdi» sanog'ini oshirmaydi")
        void favoriteDoesNotCountAsLike() {
            User viewer = person("Tomoshabin");
            Content film = movie();

            favoriteService.add(viewer, FavoriteType.CONTENT, List.of(film.getId()));

            assertThat(likeCount(film)).isZero();
            assertThat(likeService.isLiked(viewer.getId(), film.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("Ekranga chiqishi")
    class OnScreen {

        @Test
        @DisplayName("Mehmon sanoqni ko'radi, lekin «bosgan» emas")
        void guestSeesCountButNotLiked() throws Exception {
            User viewer = person("Tomoshabin");
            Content film = movie();
            likeService.like(viewer, film.getId());
            entityManager.flush();

            SecurityContextHolder.clearContext();

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.likeCount").value(1))
                    .andExpect(jsonPath("$.liked").value(false));
        }

        @Test
        @DisplayName("Bosgan odam uchun liked = true")
        void likerSeesLikedTrue() throws Exception {
            User viewer = person("Tomoshabin");
            Content film = movie();
            likeService.like(viewer, film.getId());
            entityManager.flush();

            signIn(viewer);
            try {
                mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.liked").value(true))
                        .andExpect(jsonPath("$.likeCount").value(1));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        @Test
        @DisplayName("Ko'rishlar soni javobda bor")
        void viewCountIsReturned() throws Exception {
            Content film = movie();
            entityManager.flush();

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.viewCount").value(0));
        }
    }

    @Nested
    @DisplayName("Ko'rishlar hodisalardan yig'iladi")
    class Views {

        /**
         * ⚠️ 07.09.2026 gacha bu ustun hech qachon oshmasdi: hodisalar
         * faqat kunlik jadvalga tushardi. Ya'ni ilova doim nol
         * ko'rsatgan bo'lardi, va buni «hali hech kim ko'rmagan» deb
         * o'qish mumkin edi.
         */
        @Test
        @DisplayName("Uchta CONTENT_VIEW — kontentda uchta ko'rish")
        void eventsRaiseContentViewCount() {
            Content film = movie();

            for (int i = 0; i < 3; i++) {
                analyticsService.record(AnalyticsEventType.CONTENT_VIEW,
                        film.getId(), null, null, "qurilma-" + i);
            }
            analyticsService.aggregate();

            entityManager.clear();
            assertThat(contentRepo.findById(film.getId()).orElseThrow().getViewCount())
                    .isEqualTo(3);
        }

        @Test
        @DisplayName("Ijro hodisasi ko'rishlarni oshirmaydi")
        void playIsNotAView() {
            Content film = movie();

            analyticsService.record(AnalyticsEventType.CONTENT_PLAY,
                    film.getId(), null, null, "qurilma");
            analyticsService.aggregate();

            entityManager.clear();
            assertThat(contentRepo.findById(film.getId()).orElseThrow().getViewCount())
                    .isZero();
        }
    }

    @Nested
    @DisplayName("Chegaralar")
    class Guards {

        @Test
        @DisplayName("O'chirilgan kontentga «yoqdi» qo'yib bo'lmaydi")
        void deletedContentCannotBeLiked() {
            User viewer = person("Tomoshabin");
            Content film = movie();
            contentService.archive(null, film.getId());
            entityManager.flush();

            assertThatThrownBy(() -> likeService.like(viewer, film.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Mavjud bo'lmagan kontent — xato, 500 emas")
        void missingContentIsNotFound() {
            User viewer = person("Tomoshabin");

            assertThatThrownBy(() -> likeService.like(viewer, 9_999_999L))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
