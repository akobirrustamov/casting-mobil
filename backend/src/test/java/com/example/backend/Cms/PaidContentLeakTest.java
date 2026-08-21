package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.support.Translations;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pullik video ochiq sizib chiqmasligini qo'riqlaydi.
 *
 * <h2>Nega bu test kerak</h2>
 * {@link AccessService#canWatch} qanchalik to'g'ri bo'lmasin, agar video
 * faylning o'zi ochiq URL orqali olinsa - butun monetizatsiya ma'nosiz.
 * Bu yerda AYNAN shu ikki qavat birga tekshiriladi:
 *   1) tomosha qarori video havolasini bermaydi;
 *   2) fayl endpointi ham mustaqil ravishda rad etadi.
 *
 * Kimdir kelajakda {@code /media/*!/raw} ni yana shartsiz ochiq qilsa yoki
 * rad javobiga {@code sources} qo'shib yuborsa - test darhol yiqiladi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaidContentLeakTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContentService contentService;

    @Autowired
    private EpisodeService episodeService;

    @Autowired
    private MediaAssetRepo mediaAssetRepo;

    @Autowired
    private AccessService accessService;

    // ------------------------------------------------------------- yordamchi

    private MediaAsset video(String name) {
        MediaAsset a = new MediaAsset();
        a.setStorageKey("test/" + name + ".mp4");
        a.setOriginalFilename(name + ".mp4");
        a.setType(MediaType.VIDEO);
        a.setMimeType("video/mp4");
        a.setSizeBytes(1024L);
        a.setStatus(MediaStatus.READY);
        a.setCreatedAt(LocalDateTime.now());
        return mediaAssetRepo.save(a);
    }

    private MediaAsset poster(String name) {
        MediaAsset a = new MediaAsset();
        a.setStorageKey("test/" + name + ".jpg");
        a.setOriginalFilename(name + ".jpg");
        a.setType(MediaType.IMAGE);
        a.setMimeType("image/jpeg");
        a.setSizeBytes(512L);
        a.setStatus(MediaStatus.READY);
        a.setCreatedAt(LocalDateTime.now());
        return mediaAssetRepo.save(a);
    }

    /** Nashr qilingan kontent + bitta qism + unga bog'langan video. */
    private Episode publishedEpisode(AccessPolicy policy, String title, MediaAsset media) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.SERIES);
        c.setStructureType(StructureType.EPISODIC);
        c.setAccessPolicy(policy);
        c.setStatus(PublicationStatus.PUBLISHED);
        if (policy != AccessPolicy.FREE) {
            c.setPremierePrice(new BigDecimal("50000"));
        }
        c.setTranslations(Translations.all(title));
        Content content = contentService.create(null, c);

        EpisodeSaveRequest e = new EpisodeSaveRequest();
        e.setEpisodeNumber(1);
        e.setStatus(PublicationStatus.PUBLISHED);
        e.setTranslations(Translations.all(title + " 1-qism"));
        if (policy != AccessPolicy.FREE) {
            e.setPrice(new BigDecimal("10000"));
        }

        EpisodeSaveRequest.VideoLink link = new EpisodeSaveRequest.VideoLink();
        link.setMediaId(media.getId());
        link.setPartNumber(1);
        e.setVideos(List.of(link));

        return episodeService.saveEpisode(null, content.getId(), null, e);
    }

    // ------------------------------------------------------ tomosha qarori

    @Nested
    @DisplayName("Tomosha qarori")
    class WatchDecision {

        @Test
        @DisplayName("Pullik qism: anonimga havola BERILMAYDI")
        void paidEpisodeGivesNoSourcesToAnonymous() throws Exception {
            MediaAsset media = video("maxfiy-qism");
            Episode ep = publishedEpisode(AccessPolicy.PREMIUM_OR_PURCHASE, "Pullik serial", media);

            mockMvc.perform(get("/api/v1/app/watch/" + ep.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(false))
                    .andExpect(jsonPath("$.reason").value("NOT_AUTHENTICATED"))
                    .andExpect(jsonPath("$.requiredAction").value("SIGN_IN"))
                    // Eng muhimi: ro'yxat bo'sh.
                    .andExpect(jsonPath("$.sources").isEmpty())
                    // Narx ko'rsatiladi - klient tugma chizishi kerak.
                    .andExpect(jsonPath("$.episodePrice").value(10000));
        }

        @Test
        @DisplayName("Bepul qism: havola beriladi")
        void freeEpisodeGivesSources() throws Exception {
            MediaAsset media = video("bepul-qism");
            Episode ep = publishedEpisode(AccessPolicy.FREE, "Bepul serial", media);

            mockMvc.perform(get("/api/v1/app/watch/" + ep.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(true))
                    .andExpect(jsonPath("$.reason").value("FREE"))
                    .andExpect(jsonPath("$.sources[0].mediaId").value(media.getId()))
                    .andExpect(jsonPath("$.sources[0].url")
                            .value("/api/v1/app/media/" + media.getId() + "/raw"));
        }

        @Test
        @DisplayName("Javobda hech qachon storageKey chiqmaydi")
        void responseNeverExposesStorageKey() throws Exception {
            MediaAsset media = video("ichki-yol");
            Episode ep = publishedEpisode(AccessPolicy.FREE, "Yo'l sinovi", media);

            String body = mockMvc.perform(get("/api/v1/app/watch/" + ep.getId()))
                    .andReturn().getResponse().getContentAsString();

            // Ichki saqlash yo'li klientga kerak emas va uni oshkor qilish
            // storage tuzilishini ochib beradi.
            assertThat(body).doesNotContain("storageKey");
            assertThat(body).doesNotContain(media.getStorageKey());
        }
    }

    // ------------------------------------------------------------ fayl qavati

    @Nested
    @DisplayName("Fayl endpointi mustaqil tekshiradi")
    class RawEndpoint {

        @Test
        @DisplayName("Pullik video: anonim uchun 404")
        void paidVideoIsNotDownloadable() throws Exception {
            MediaAsset media = video("pullik-fayl");
            publishedEpisode(AccessPolicy.PREMIUM_OR_PURCHASE, "Pullik film", media);

            // 404, 403 emas: faylning umuman bor-yo'qligini ham oshkor qilmaymiz.
            mockMvc.perform(get("/api/v1/app/media/" + media.getId() + "/raw"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Hech qaysi qismga bog'lanmagan video ham ochiq emas")
        void orphanVideoIsNotPublic() {
            MediaAsset orphan = video("biriktirilmagan");
            assertThat(accessService.canReadMedia(null, orphan)).isFalse();
        }

        @Test
        @DisplayName("Rasm ochiq qoladi - afishalar buzilmasin")
        void imagesStayPublic() {
            assertThat(accessService.canReadMedia(null, poster("afisha"))).isTrue();
        }

        @Test
        @DisplayName("Bepul qism videosi ochiq")
        void freeVideoIsReadable() {
            MediaAsset media = video("bepul-fayl");
            publishedEpisode(AccessPolicy.FREE, "Bepul film", media);
            assertThat(accessService.canReadMedia(null, media)).isTrue();
        }
    }
}
