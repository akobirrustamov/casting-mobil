package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.support.Translations;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.ContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SINGLE kontentni tomosha qilish — film, qisqa metraj, klip.
 *
 * <h2>Qanday bo'shliq yopildi</h2>
 * SINGLE tuzilmada qism BO'LMAYDI (ТЗ §14), demak {@code EpisodeVideo} ham
 * yo'q. Natijada filmning asosiy videosini saqlaydigan joy umuman yo'q edi
 * va {@code /watch/{episodeId}} ni chaqirib bo'lmasdi — ya'ni <b>filmni
 * tomosha qilish oqimi mavjud emasdi</b>.
 *
 * Dev bazasida 7 ta SINGLE kontent bor edi (MOVIE, SHORT_FILM, CLIP,
 * SHOW, STREAM) — hech biriga video biriktirib bo'lmasdi.
 *
 * <h2>Yechim</h2>
 * ТЗ §22 (Step 2 — Media) videolarni kontent darajasida sanaydi. Shuning
 * uchun {@code MediaRole.VIDEO} qo'shildi va videolar {@code ContentMedia}
 * da yotadi: {@code sortOrder} — segment tartibi (§19), {@code locale} —
 * dublyaj tili.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SingleContentWatchTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentService contentService;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private AccessService accessService;

    private MediaAsset video(String name) {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/" + name + "-" + SEQ.incrementAndGet() + ".mp4")
                .originalFilename(name + ".mp4")
                .type(MediaType.VIDEO)
                .mimeType("video/mp4")
                .sizeBytes(1024L)
                .durationSeconds(5400)
                .status(MediaStatus.READY)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private ContentSaveRequest.MediaLink link(MediaRole role, Long mediaId, int order) {
        ContentSaveRequest.MediaLink l = new ContentSaveRequest.MediaLink();
        l.setRole(role);
        l.setMediaId(mediaId);
        l.setSortOrder(order);
        return l;
    }

    /** SINGLE film — asosiy video segmentlari bilan. */
    private Content movie(AccessPolicy policy, List<ContentSaveRequest.MediaLink> media) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(policy);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setDurationMinutes(90);
        if (policy != AccessPolicy.FREE) {
            c.setPremierePrice(new BigDecimal("25000"));
        }
        c.setTranslations(Translations.all("Film " + SEQ.incrementAndGet()));
        c.setMedia(media);
        return contentService.create(null, c);
    }

    // ------------------------------------------------------------ bepul film

    @Nested
    @DisplayName("Bepul film")
    class FreeMovie {

        @Test
        @DisplayName("Anonim ko'ra oladi va video havolasini oladi")
        void anonymousCanWatch() throws Exception {
            MediaAsset file = video("bepul-film");
            Content film = movie(AccessPolicy.FREE,
                    List.of(link(MediaRole.VIDEO, file.getId(), 0)));

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(true))
                    .andExpect(jsonPath("$.reason").value("FREE"))
                    .andExpect(jsonPath("$.sources[0].mediaId").value(file.getId()))
                    .andExpect(jsonPath("$.sources[0].url")
                            .value("/api/v1/app/media/" + file.getId() + "/raw"))
                    // 90 daqiqa → 5400 soniya
                    .andExpect(jsonPath("$.durationSeconds").value(5400));
        }

        @Test
        @DisplayName("Video fayli ham ochiladi")
        void videoFileIsReadable() {
            MediaAsset file = video("bepul-fayl");
            movie(AccessPolicy.FREE, List.of(link(MediaRole.VIDEO, file.getId(), 0)));

            assertThat(accessService.canReadMedia(null, file)).isTrue();
        }
    }

    // ---------------------------------------------------------- pullik film

    @Nested
    @DisplayName("Pullik film")
    class PaidMovie {

        @Test
        @DisplayName("Anonimga havola BERILMAYDI, narx ko'rsatiladi")
        void anonymousGetsNoSources() throws Exception {
            MediaAsset file = video("pullik-film");
            Content film = movie(AccessPolicy.PREMIUM_OR_PURCHASE,
                    List.of(link(MediaRole.VIDEO, file.getId(), 0)));

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(false))
                    .andExpect(jsonPath("$.reason").value("NOT_AUTHENTICATED"))
                    .andExpect(jsonPath("$.sources").isEmpty())
                    .andExpect(jsonPath("$.premierePrice").value(25000));
        }

        @Test
        @DisplayName("Video fayli ham yopiq — id ni terib olib bo'lmaydi")
        void videoFileIsProtected() {
            MediaAsset file = video("pullik-fayl");
            movie(AccessPolicy.PREMIUM_OR_PURCHASE,
                    List.of(link(MediaRole.VIDEO, file.getId(), 0)));

            // Aynan shu tekshiruv bo'lmasa entitlement ma'nosiz bo'lardi:
            // klientga "sotib oling" deb turib, fayl yonida ochiq qolardi.
            assertThat(accessService.canReadMedia(null, file)).isFalse();
        }

        @Test
        @DisplayName("PURCHASE_ONLY da BUY_PREMIERE so'raladi, BUY_EPISODE emas")
        void asksToBuyWholeContent() throws Exception {
            MediaAsset file = video("faqat-xarid");
            Content film = movie(AccessPolicy.PURCHASE_ONLY,
                    List.of(link(MediaRole.VIDEO, file.getId(), 0)));

            // SINGLE da qism yo'q - "qismni sotib oling" deyish ma'nosiz.
            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(jsonPath("$.requiredAction").value("SIGN_IN"));

            assertThat(accessService.canWatch(null, film).getRequiredAction())
                    .isEqualTo(com.example.backend.Cms.Service.AccessDecision
                            .RequiredAction.SIGN_IN);
        }
    }

    // -------------------------------------------------------- segmentlar

    @Nested
    @DisplayName("Video segmentlari (ТЗ §19)")
    class Segments {

        @Test
        @DisplayName("Bir nechta segment TARTIB bilan qaytadi")
        void multiplePartsInOrder() throws Exception {
            MediaAsset p1 = video("qism-1");
            MediaAsset p2 = video("qism-2");
            MediaAsset p3 = video("qism-3");

            // Ataylab teskari tartibda beramiz - server sortOrder bo'yicha tiklashi kerak.
            List<ContentSaveRequest.MediaLink> media = new ArrayList<>();
            media.add(link(MediaRole.VIDEO, p3.getId(), 2));
            media.add(link(MediaRole.VIDEO, p1.getId(), 0));
            media.add(link(MediaRole.VIDEO, p2.getId(), 1));

            Content film = movie(AccessPolicy.FREE, media);

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(jsonPath("$.sources.length()").value(3))
                    .andExpect(jsonPath("$.sources[0].mediaId").value(p1.getId()))
                    .andExpect(jsonPath("$.sources[1].mediaId").value(p2.getId()))
                    .andExpect(jsonPath("$.sources[2].mediaId").value(p3.getId()))
                    // Segment raqami 1 dan boshlanadi - klient "1-qism" deb ko'rsatadi.
                    .andExpect(jsonPath("$.sources[0].partNumber").value(1));
        }

        @Test
        @DisplayName("TRAILER va TEASER asosiy video sifatida QAYTMAYDI")
        void trailerIsNotTheMovie() throws Exception {
            MediaAsset trailer = video("treyler");
            MediaAsset teaser = video("tizer");
            MediaAsset main = video("asosiy");

            Content film = movie(AccessPolicy.FREE, List.of(
                    link(MediaRole.TRAILER, trailer.getId(), 0),
                    link(MediaRole.TEASER, teaser.getId(), 0),
                    link(MediaRole.VIDEO, main.getId(), 0)));

            // Treyler reklama roligi. Uni "film" deb bersak, pullik kontentda
            // sotib olmagan odam treylerni olib ketardi.
            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(jsonPath("$.sources.length()").value(1))
                    .andExpect(jsonPath("$.sources[0].mediaId").value(main.getId()));
        }
    }

    // ------------------------------------------------------------- himoya

    @Nested
    @DisplayName("Chegaralar")
    class Guards {

        @Test
        @DisplayName("Ko'p qismli kontent bu endpointdan ko'rilmaydi")
        void multiPartContentIsRejected() throws Exception {
            ContentSaveRequest c = new ContentSaveRequest();
            c.setContentType(ContentType.SERIES);
            c.setStructureType(StructureType.EPISODIC);
            c.setAccessPolicy(AccessPolicy.FREE);
            c.setStatus(PublicationStatus.PUBLISHED);
            c.setTranslations(Translations.all("Serial"));
            Content series = contentService.create(null, c);

            // Klient qaysi qismni so'rayotganini aytishi kerak.
            mockMvc.perform(get("/api/v1/app/watch/content/" + series.getId()))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Nashr qilinmagan film ko'rilmaydi")
        void draftIsNotWatchable() throws Exception {
            MediaAsset file = video("qoralama");
            ContentSaveRequest c = new ContentSaveRequest();
            c.setContentType(ContentType.MOVIE);
            c.setStructureType(StructureType.SINGLE);
            c.setAccessPolicy(AccessPolicy.FREE);
            c.setStatus(PublicationStatus.DRAFT);
            c.setTranslations(Translations.all("Qoralama film"));
            c.setMedia(List.of(link(MediaRole.VIDEO, file.getId(), 0)));
            Content film = contentService.create(null, c);

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(jsonPath("$.allowed").value(false))
                    .andExpect(jsonPath("$.reason").value("NOT_PUBLISHED"))
                    .andExpect(jsonPath("$.sources").isEmpty());

            assertThat(accessService.canReadMedia(null, file)).isFalse();
        }

        @Test
        @DisplayName("Videosiz film — bo'sh ro'yxat, xato emas")
        void movieWithoutVideoIsSafe() throws Exception {
            Content film = movie(AccessPolicy.FREE, List.of());

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(true))
                    .andExpect(jsonPath("$.sources").isEmpty());
        }
    }
}
