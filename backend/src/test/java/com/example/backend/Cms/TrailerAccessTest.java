package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.MediaRole;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.Cms.Service.StorageService;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Treyler yopiq kontent ekranida.
 *
 * <h2>Nima uchun bu alohida test</h2>
 * Bu yerda ikkita QARAMA-QARSHI talab bir joyda uchrashadi:
 *
 * <ul>
 *   <li>treyler sotib olmagan odamga KO'RINISHI kerak — aks holda u
 *       umuman ma'nosiz;</li>
 *   <li>filmning o'zi o'sha odamga KO'RINMASLIGI kerak — aks holda
 *       pullik kontent bepul tarqaladi.</li>
 * </ul>
 *
 * Ikkalasi bitta metodda ({@code AccessService.canReadMedia}) hal
 * qilinadi. Shuning uchun har bir test juft: «treyler ochildi» yolg'iz
 * o'zi hech narsani isbotlamaydi — yonida «film ochilmadi» turishi shart.
 *
 * ⚠️ Uchinchi talab: NASHR QILINMAGAN kontentning treyleri ham
 * berilmaydi. Aks holda chiqish sanasidan oldin rolik id ni terib
 * topilardi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TrailerAccessTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentService contentService;
    @Autowired private EpisodeService episodeService;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private StorageService storageService;

    // ------------------------------------------------------------- yordamchi

    /** Omborda haqiqiy fayli bor video yozuvi. */
    private MediaAsset video(String name) {
        int n = SEQ.incrementAndGet();
        String key = "/test/" + name + "-" + n + ".mp4";
        storageService.storeAt(
                new ByteArrayInputStream(("mazmun " + n).getBytes(StandardCharsets.UTF_8)),
                key, "video/mp4");

        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey(key)
                .originalFilename(name + ".mp4")
                .type(MediaType.VIDEO)
                .mimeType("video/mp4")
                .sizeBytes(64L)
                .durationSeconds(name.equals("treyler") ? 90 : 5400)
                .status(MediaStatus.READY)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private ContentSaveRequest.MediaLink link(MediaRole role, MediaAsset file, int order) {
        ContentSaveRequest.MediaLink l = new ContentSaveRequest.MediaLink();
        l.setRole(role);
        l.setMediaId(file.getId());
        l.setSortOrder(order);
        return l;
    }

    /** Pullik film: asosiy video + berilgan reklama roliklari. */
    private Content movie(PublicationStatus status,
                          MediaAsset main,
                          List<ContentSaveRequest.MediaLink> promo) {
        List<ContentSaveRequest.MediaLink> media = new ArrayList<>();
        media.add(link(MediaRole.VIDEO, main, 0));
        media.addAll(promo);

        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.PURCHASE_ONLY);
        c.setStatus(status);
        c.setDurationMinutes(90);
        c.setPremierePrice(new BigDecimal("5000"));
        c.setTranslations(Translations.all("Film " + SEQ.incrementAndGet()));
        c.setMedia(media);
        return contentService.create(null, c);
    }

    /** Ko'p qismli pullik serial: treyler KONTENTGA biriktiriladi. */
    private Content series(List<ContentSaveRequest.MediaLink> promo) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.SERIES);
        c.setStructureType(StructureType.EPISODIC);
        c.setAccessPolicy(AccessPolicy.PURCHASE_ONLY);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setPremierePrice(new BigDecimal("15000"));
        c.setTranslations(Translations.all("Serial " + SEQ.incrementAndGet()));
        c.setMedia(new ArrayList<>(promo));
        return contentService.create(null, c);
    }

    private Episode episode(Content content, int number) {
        EpisodeSaveRequest e = new EpisodeSaveRequest();
        e.setEpisodeNumber(number);
        e.setStatus(PublicationStatus.PUBLISHED);
        e.setPrice(new BigDecimal("3000"));
        e.setSortOrder(number);
        e.setTranslations(Translations.all(number + "-qism"));
        return episodeService.saveEpisode(null, content.getId(), null, e);
    }

    // ------------------------------------------------------------------ test

    @Nested
    @DisplayName("Yopiq kontent javobida")
    class WatchResponse {

        /**
         * ⚠️ Asosiy holat. {@code allowed=false} bo'lgani holda treyler
         * KELADI, {@code sources} esa BO'SH qoladi.
         *
         * Ikkalasi birga tekshiriladi ataylab: treylerni qo'shishning eng
         * xavfli usuli — uni {@code sources} ga qo'shib yuborish. Shunda
         * pleyer «filmni» ochardi va odam 90 soniyalik rolikni butun film
         * deb olardi, kassa esa bo'sh qolardi.
         */
        @Test
        @DisplayName("Treyler bor, film manbasi yo'q")
        void trailerComesWithDenial() throws Exception {
            MediaAsset main = video("film");
            MediaAsset promo = video("treyler");
            Content content = movie(PublicationStatus.PUBLISHED, main,
                    List.of(link(MediaRole.TRAILER, promo, 0)));

            mockMvc.perform(get("/api/v1/app/watch/content/" + content.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(false))
                    .andExpect(jsonPath("$.sources").isEmpty())
                    .andExpect(jsonPath("$.trailer.mediaId").value(promo.getId()))
                    .andExpect(jsonPath("$.trailer.url")
                            .value("/api/v1/app/media/" + promo.getId() + "/raw"))
                    .andExpect(jsonPath("$.trailer.durationSeconds").value(90));
        }

        /**
         * ⚠️ Qism yo'lida ham treyler keladi.
         *
         * Bu nusxa-test emas: {@code /watch/{episodeId}} treylerni QISMDAN
         * emas, uning KONTENTIDAN oladi — ya'ni boshqa bog'lanish bo'yicha,
         * lazy yuklanadigan to'plamdan. Bu yerda xato «treyler yo'q» emas,
         * 500 bo'lib chiqardi, va faqat serialda.
         */
        @Test
        @DisplayName("Serialning qismida ham treyler bor")
        void episodeGetsContentTrailer() throws Exception {
            MediaAsset promo = video("treyler");
            Content serial = series(List.of(link(MediaRole.TRAILER, promo, 0)));
            Episode first = episode(serial, 1);

            mockMvc.perform(get("/api/v1/app/watch/" + first.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(false))
                    .andExpect(jsonPath("$.sources").isEmpty())
                    .andExpect(jsonPath("$.trailer.mediaId").value(promo.getId()));
        }

        /**
         * Treyler yuklanmagan bo'lsa maydon {@code null} — bo'sh obyekt emas.
         * Klient «treyler bormi» degan savolga bitta tekshiruv bilan javob
         * olsin.
         */
        @Test
        @DisplayName("Treylersiz kontentda maydon bo'sh")
        void withoutTrailerFieldIsNull() throws Exception {
            Content content = movie(PublicationStatus.PUBLISHED, video("film"), List.of());

            mockMvc.perform(get("/api/v1/app/watch/content/" + content.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trailer").doesNotExist());
        }

        /**
         * ⚠️ TRAILER TEASER dan ustun.
         *
         * Tizer qisqa e'lon uchun, treyler esa aynan tomosha qildirish
         * uchun. Tartib ro'yxatdagi joyiga qarab tanlansa, admin fayllarni
         * qanday qo'shgani hal qilardi — ya'ni tasodif.
         */
        @Test
        @DisplayName("Ikkalasi bo'lsa — treyler tanlanadi")
        void trailerWinsOverTeaser() throws Exception {
            MediaAsset teaser = video("tizer");
            MediaAsset trailer = video("treyler");

            // ⚠️ Tizer ro'yxatda BIRINCHI va sortOrder'i ham kichik —
            // ya'ni «birinchisini ol» degan sodda mantiq uni tanlardi.
            Content content = movie(PublicationStatus.PUBLISHED, video("film"),
                    List.of(link(MediaRole.TEASER, teaser, 0),
                            link(MediaRole.TRAILER, trailer, 1)));

            mockMvc.perform(get("/api/v1/app/watch/content/" + content.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trailer.mediaId").value(trailer.getId()));
        }

        /** Tizer yolg'iz bo'lsa — u ham yaraydi, hech narsadan ko'ra yaxshi. */
        @Test
        @DisplayName("Yolg'iz tizer ham beriladi")
        void teaserAloneIsUsed() throws Exception {
            MediaAsset teaser = video("tizer");
            Content content = movie(PublicationStatus.PUBLISHED, video("film"),
                    List.of(link(MediaRole.TEASER, teaser, 0)));

            mockMvc.perform(get("/api/v1/app/watch/content/" + content.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trailer.mediaId").value(teaser.getId()));
        }

        /**
         * ⚠️ Nashr qilinmagan kontentda treyler ham yo'q.
         *
         * Bu «qulf» emas, «hali yo'q»: film chiqmagan ekan, uning roligi
         * ham chiqmasligi kerak. Aks holda sana oldidan tarqab ketardi.
         */
        @Test
        @DisplayName("Qoralamada treyler berilmaydi")
        void draftGivesNoTrailer() throws Exception {
            MediaAsset promo = video("treyler");
            Content content = movie(PublicationStatus.DRAFT, video("film"),
                    List.of(link(MediaRole.TRAILER, promo, 0)));

            mockMvc.perform(get("/api/v1/app/watch/content/" + content.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reason").value("NOT_PUBLISHED"))
                    .andExpect(jsonPath("$.trailer").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Faylning o'zi")
    class RawFile {

        /**
         * ⚠️ Butun ishning ma'nosi shu ikki qatorda.
         *
         * Treyler mehmonga ochiladi, film — yo'q. Agar birinchi tekshiruv
         * yiqilsa, rolik hech kimga ko'rinmaydi; ikkinchisi yiqilsa,
         * pullik film bepul tarqaladi. Shuning uchun ular BITTA testda.
         */
        @Test
        @DisplayName("Treyler mehmonga ochiq, film — yopiq")
        void trailerOpenMovieClosed() throws Exception {
            MediaAsset main = video("film");
            MediaAsset promo = video("treyler");
            movie(PublicationStatus.PUBLISHED, main, List.of(link(MediaRole.TRAILER, promo, 0)));

            mockMvc.perform(get("/api/v1/app/media/" + promo.getId() + "/raw"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/app/media/" + main.getId() + "/raw"))
                    .andExpect(status().isNotFound());
        }

        /** Qoralamaning roligi ham berilmaydi — javobda ham, fayl bo'lib ham. */
        @Test
        @DisplayName("Qoralamaning treyleri ham yopiq")
        void draftTrailerStaysClosed() throws Exception {
            MediaAsset promo = video("treyler");
            movie(PublicationStatus.DRAFT, video("film"), List.of(link(MediaRole.TRAILER, promo, 0)));

            mockMvc.perform(get("/api/v1/app/media/" + promo.getId() + "/raw"))
                    .andExpect(status().isNotFound());
        }

        /**
         * ⚠️ Hech qayerga biriktirilmagan video ochilmaydi.
         *
         * Yangi shox qo'shilganda eng oson yo'l qo'yiladigan xato — uni
         * «rol topilmadi» holatiga ham qo'llab yuborish. Unda har qanday
         * yuklangan fayl id bo'yicha ochilardi.
         */
        @Test
        @DisplayName("Biriktirilmagan video baribir yopiq")
        void orphanVideoStaysClosed() throws Exception {
            MediaAsset orphan = video("yolgiz");

            mockMvc.perform(get("/api/v1/app/media/" + orphan.getId() + "/raw"))
                    .andExpect(status().isNotFound());
        }

        /**
         * ⚠️ Filtr AYNAN reklama rollari bo‘yicha, «har qanday rol» emas.
         *
         * Yangi shox {@code findFirstByMediaIdAndRoleIn} bilan yozilgan, va unga
         * barcha rollarni berib yuborish juda oson — kod baribir ishlaydi.
         * Unda pullik filmni GALLERY roli bilan ham biriktirish kifoya
         * edi: fayl ochilib ketardi, chunki ikkinchi shox faqat VIDEO
         * rolini biladi.
         *
         * Holat sun'iy emas: panelda bitta faylni bir nechta rol bilan
         * qo'shish mumkin.
         */
        @Test
        @DisplayName("GALLERY roli treyler o'rnini bosmaydi")
        void galleryRoleIsNotPromo() throws Exception {
            MediaAsset gallery = video("gallereya");
            movie(PublicationStatus.PUBLISHED, video("film"),
                    List.of(link(MediaRole.GALLERY, gallery, 0)));

            mockMvc.perform(get("/api/v1/app/media/" + gallery.getId() + "/raw"))
                    .andExpect(status().isNotFound());
        }
    }
}
