package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
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
import com.example.backend.support.ContentFixtures;
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
import java.time.LocalDate;
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
    @Autowired private ContentFixtures contentFixtures;
    @Autowired private ContentRepo contentRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private FavoriteService favoriteService;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private com.example.backend.Cms.Service.EpisodeService episodeService;
    @Autowired private com.example.backend.Cms.Repository.EpisodeRepo episodeRepo;
    @Autowired private com.example.backend.Cms.Repository.ContentLikeRepo likeRepo;
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
        return contentFixtures.create(c);
    }

    private Content series() {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MINI_SERIES);
        c.setStructureType(StructureType.EPISODIC);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setTranslations(Translations.all("Serial " + SEQ.incrementAndGet()));
        return contentFixtures.create(c);
    }

    private Episode episode(Content content, int number) {
        com.example.backend.Admin.Dto.EpisodeSaveRequest e =
                new com.example.backend.Admin.Dto.EpisodeSaveRequest();
        e.setEpisodeNumber(number);
        e.setStatus(PublicationStatus.PUBLISHED);
        e.setSortOrder(number);
        e.setTranslations(Translations.all(number + "-qism"));
        return episodeService.saveEpisode(null, content.getId(), null, e);
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

    /**
     * Qism ko'rishlari.
     *
     * ⚠️ {@code cms_episode.view_count} — kontentnikidan KEYIN
     * topilgan o'sha kasallik: ustun bor edi, admin panel uni
     * ko'rsatardi, lekin hodisalarni jamlash {@code episodeId} ni
     * tashlab yuborardi. Panel yolg'on nol ko'rsatardi va buzuq ekani
     * ko'rinmasdi — nol «hali hech kim ko'rmagan» degan haqiqatga
     * o'xshab turardi.
     */
    @Nested
    @DisplayName("Qism ko'rishlari")
    class EpisodeViews {

        @Test
        @DisplayName("Qism bilan kelgan hodisa QISM sanog'ini oshiradi")
        void eventsRaiseEpisodeViewCount() {
            Content series = series();
            Episode first = episode(series, 1);

            for (int i = 0; i < 4; i++) {
                analyticsService.record(AnalyticsEventType.CONTENT_VIEW,
                        series.getId(), first.getId(), null, "qurilma-" + i);
            }
            analyticsService.aggregate();

            entityManager.clear();
            assertThat(episodeRepo.findById(first.getId()).orElseThrow().getViewCount())
                    .isEqualTo(4);
        }

        /**
         * Bitta hodisa ikki xil savolga javob beradi: «bu serialni necha
         * kishi ochdi» va «uning qaysi qismi ochildi». Ikkalasi ham
         * sanaladi, lekin ular BIR-BIRIGA QO'SHILMAYDI.
         */
        @Test
        @DisplayName("O'sha hodisa kontent sanog'ini ham oshiradi")
        void sameEventAlsoCountsForContent() {
            Content series = series();
            Episode first = episode(series, 1);

            analyticsService.record(AnalyticsEventType.CONTENT_VIEW,
                    series.getId(), first.getId(), null, "qurilma");
            analyticsService.aggregate();

            entityManager.clear();
            assertThat(contentRepo.findById(series.getId()).orElseThrow().getViewCount())
                    .isEqualTo(1);
            assertThat(episodeRepo.findById(first.getId()).orElseThrow().getViewCount())
                    .isEqualTo(1);
        }

        /**
         * ⚠️ Ro'yxatdagi har qator o'z sonini ko'rsatishi kerak.
         * Ikkinchi qism birinchisining sanog'ini olib qo'ysa, ro'yxat
         * to'g'ri ishlayotgandek KO'RINARDI — raqamlar joyida, faqat
         * hammasi bir xil.
         */
        @Test
        @DisplayName("Har qism o'z sanog'ini oladi")
        void episodesAreCountedSeparately() {
            Content series = series();
            Episode first = episode(series, 1);
            Episode second = episode(series, 2);

            analyticsService.record(AnalyticsEventType.CONTENT_VIEW,
                    series.getId(), first.getId(), null, "a");
            analyticsService.record(AnalyticsEventType.CONTENT_VIEW,
                    series.getId(), first.getId(), null, "b");
            analyticsService.record(AnalyticsEventType.CONTENT_VIEW,
                    series.getId(), second.getId(), null, "a");
            analyticsService.aggregate();

            entityManager.clear();
            assertThat(episodeRepo.findById(first.getId()).orElseThrow().getViewCount())
                    .isEqualTo(2);
            assertThat(episodeRepo.findById(second.getId()).orElseThrow().getViewCount())
                    .isEqualTo(1);
        }

        /**
         * Kontent kartochkasi qismsiz ochiladi — hodisada
         * {@code episodeId} yo'q. Bunday hodisa hech qaysi qismga
         * yozilmasligi kerak.
         */
        @Test
        @DisplayName("Qismsiz hodisa hech qaysi qismga tushmaydi")
        void eventWithoutEpisodeTouchesNoEpisode() {
            Content series = series();
            Episode first = episode(series, 1);

            analyticsService.record(AnalyticsEventType.CONTENT_VIEW,
                    series.getId(), null, null, "qurilma");
            analyticsService.aggregate();

            entityManager.clear();
            assertThat(episodeRepo.findById(first.getId()).orElseThrow().getViewCount())
                    .isZero();
        }

        @Test
        @DisplayName("Ijro hodisasi qism ko'rishlarini oshirmaydi")
        void playIsNotAnEpisodeView() {
            Content series = series();
            Episode first = episode(series, 1);

            analyticsService.record(AnalyticsEventType.CONTENT_PLAY,
                    series.getId(), first.getId(), null, "qurilma");
            analyticsService.aggregate();

            entityManager.clear();
            assertThat(episodeRepo.findById(first.getId()).orElseThrow().getViewCount())
                    .isZero();
        }
    }

    /**
     * Hisobot so'rovlari (admin panel statistikasi).
     *
     * <h2>Nima bu yerda tekshiriladi</h2>
     * Uchta narsa, va uchalasi ham skrinshotda ko'rinmaydi:
     *
     * 1. {@code cast(... as date)} H2 da haqiqatan ishlaydimi. So'rov
     *    xato bo'lsa panel butun sahifani yo'qotardi, kod esa
     *    kompilyatsiyadan o'tardi.
     * 2. Har kontent O'Z sonini oladimi. Guruhlash buzilsa, jadvaldagi
     *    barcha qatorlar bir xil son ko'rsatardi — to'ldirilgan
     *    jadval ishlayotgandek KO'RINARDI.
     * 3. Yechilgan «yoqdi» o'tmishdan ham yo'qolishi. Bu hujjatlashtirilgan
     *    xatti-harakat, xato emas — lekin uni bilmasdan «grafik
     *    o'zgarib ketdi» deb xato qidirish oson.
     */
    @Nested
    @DisplayName("Hisobot so'rovlari")
    class Statistics {

        private final LocalDate today = LocalDate.now();

        @Test
        @DisplayName("Kunlik qator qo'yilgan «yoqdi» ni beradi")
        void dailySeriesCountsLikes() {
            Content film = movie();
            likeService.like(person("Statistika 001"), film.getId());
            likeService.like(person("Statistika 002"), film.getId());
            entityManager.flush();

            var rows = likeRepo.dailyForContent(film.getId(), today.minusDays(6), today);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).getDay()).isEqualTo(today);
            assertThat(rows.get(0).getTotal()).isEqualTo(2);
        }

        /**
         * ⚠️ Guruhlash buzilsa jadval TO'LDIRILGAN bo'lib ko'rinadi:
         * raqamlar joyida, faqat hammasi bir xil.
         */
        @Test
        @DisplayName("Har kontent o'z sonini oladi")
        void likesAreGroupedPerContent() {
            Content first = movie();
            Content second = movie();
            likeService.like(person("Statistika 003"), first.getId());
            likeService.like(person("Statistika 004"), first.getId());
            likeService.like(person("Statistika 005"), second.getId());
            entityManager.flush();

            var byContent = likeRepo.likesByContentBetween(
                    List.of(first.getId(), second.getId()), today.minusDays(6), today);

            assertThat(byContent).hasSize(2);
            assertThat(byContent.stream()
                    .filter(r -> r.getContentId().equals(first.getId()))
                    .findFirst().orElseThrow().getTotal()).isEqualTo(2);
            assertThat(byContent.stream()
                    .filter(r -> r.getContentId().equals(second.getId()))
                    .findFirst().orElseThrow().getTotal()).isEqualTo(1);
        }

        @Test
        @DisplayName("Davrdan tashqaridagi kun sanalmaydi")
        void outsideThePeriodIsNotCounted() {
            Content film = movie();
            likeService.like(person("Statistika 006"), film.getId());
            entityManager.flush();

            // Kechagacha bo'lgan davr — bugungi «yoqdi» unga kirmaydi.
            assertThat(likeRepo.dailyForContent(
                    film.getId(), today.minusDays(6), today.minusDays(1))).isEmpty();
        }

        /**
         * ⚠️ Bu XATO EMAS, jadval tuzilishining oqibati: yechilgan
         * «yoqdi» yozuvi o'chadi, ya'ni u qo'yilgan kundan ham
         * yo'qoladi. Ko'rish bilan solishtirib bo'lmaydi — ko'rish
         * sodir bo'lgan voqea, «yoqdi» esa hozirgi holat.
         *
         * Test shu xatti-harakatni QOTIRADI: kimdir uni «tuzatib»,
         * hisobotni jimgina boshqa narsaga aylantirmasin.
         */
        @Test
        @DisplayName("Yechilgan «yoqdi» o'tmish sonidan ham yo'qoladi")
        void unlikeAlsoDisappearsFromHistory() {
            Content film = movie();
            User person = person("Statistika 007");
            likeService.like(person, film.getId());
            entityManager.flush();
            assertThat(likeRepo.countBetween(today.minusDays(6), today)).isPositive();

            long before = likeRepo.countBetween(today.minusDays(6), today);
            likeService.unlike(person, film.getId());
            entityManager.flush();

            assertThat(likeRepo.countBetween(today.minusDays(6), today)).isEqualTo(before - 1);
        }

        @Test
        @DisplayName("Filtr qo'llansa faqat tanlangan kontent sanaladi")
        void filterNarrowsTheCount() {
            Content counted = movie();
            Content ignored = movie();
            likeService.like(person("Statistika 008"), counted.getId());
            likeService.like(person("Statistika 009"), ignored.getId());
            entityManager.flush();

            assertThat(likeRepo.countBetweenForContents(
                    List.of(counted.getId()), today.minusDays(6), today)).isEqualTo(1);
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
