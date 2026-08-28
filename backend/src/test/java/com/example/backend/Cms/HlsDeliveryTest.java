package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.Content;
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
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HLS manzillari {@code /watch} javobida.
 *
 * <h2>⚠️ Bu test MOBIL SHARTNOMASINI qo'riqlaydi</h2>
 * Mobil ilova {@code url} maydonini NISBIY deb hisoblaydi va uning
 * oldiga o'z {@code BASE_URL} ini qo'yadi. Mutlaq CDN manzili o'sha
 * maydonga yozilsa {@code https://uzcasting.sitehttps://cdn…} chiqardi
 * — video jimgina ochilmasdi, hech qanday xato ko'rsatmasdan.
 *
 * Shuning uchun {@code url} tegilmaydi, CDN manzili esa YANGI
 * {@code hlsUrl} maydoniga yoziladi. Ikkalasi bitta javobda yonma-yon
 * keladi va eski ilova buzilmaydi (§33).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "app.video.cdn.base-url=https://video.uzcasting.site")
class HlsDeliveryTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentService contentService;
    @Autowired private MediaAssetRepo mediaAssetRepo;

    /**
     * @param hlsMasterKey {@code null} = transcoding qilinmagan
     */
    private MediaAsset video(String hlsMasterKey) {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/video-" + SEQ.incrementAndGet() + ".mp4")
                .originalFilename("kino.mp4")
                .type(MediaType.VIDEO)
                .mimeType("video/mp4")
                .sizeBytes(1024L)
                .durationSeconds(5400)
                .status(MediaStatus.READY)
                .hlsMasterKey(hlsMasterKey)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Content freeMovie(MediaAsset file) {
        ContentSaveRequest.MediaLink link = new ContentSaveRequest.MediaLink();
        link.setRole(MediaRole.VIDEO);
        link.setMediaId(file.getId());
        link.setSortOrder(0);

        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setDurationMinutes(90);
        c.setTranslations(Translations.all("Film " + SEQ.incrementAndGet()));
        c.setMedia(List.of(link));
        return contentService.create(null, c);
    }

    @Nested
    @DisplayName("Transcoding TUGAGAN video")
    class Transcoded {

        @Test
        @DisplayName("`hlsUrl` to'liq CDN manzili bo'lib keladi")
        void hlsUrlIsAbsolute() throws Exception {
            MediaAsset file = video("/videos/42/hls/master.m3u8");
            Content film = freeMovie(file);

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sources[0].hlsUrl")
                            .value("https://video.uzcasting.site/videos/42/hls/master.m3u8"));
        }

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV.
         *
         * `url` NISBIY qolishi shart — mobil uning oldiga `BASE_URL`
         * qo'yadi. Bu yerda mutlaq manzil paydo bo'lsa, eski ilovalar
         * uchun video ochilmay qolardi.
         */
        @Test
        @DisplayName("Eski `url` maydoni NISBIY qoladi — mobil buzilmaydi")
        void relativeUrlIsUnchanged() throws Exception {
            MediaAsset file = video("/videos/42/hls/master.m3u8");
            Content film = freeMovie(file);

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    // Aynan nisbiy yo'l, CDN manzili EMAS.
                    .andExpect(jsonPath("$.sources[0].url")
                            .value("/api/v1/app/media/" + file.getId() + "/raw"));
        }
    }

    @Nested
    @DisplayName("Transcoding TUGAMAGAN video")
    class NotTranscoded {

        /**
         * ⚠️ `null` — bu «HLS hali yo'q» degani va klient eski yo'lga
         * qaytadi.
         *
         * O'ylab topilgan manzil qaytarilsa pleyer mavjud bo'lmagan
         * faylni so'rardi va nosozlik «video buzuq» bo'lib ko'rinardi.
         */
        @Test
        @DisplayName("`hlsUrl` null bo'ladi, `url` esa ISHLAYDI")
        void hlsUrlIsNullButPlaybackStillWorks() throws Exception {
            MediaAsset file = video(null);
            Content film = freeMovie(file);

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(true))
                    .andExpect(jsonPath("$.sources[0].hlsUrl").doesNotExist())
                    // Eski yo'l ishlashda davom etadi — bu §33 talabi.
                    .andExpect(jsonPath("$.sources[0].url")
                            .value("/api/v1/app/media/" + file.getId() + "/raw"));
        }
    }

    @Nested
    @DisplayName("Ruxsat berilmagan holat")
    class Denied {

        /**
         * ⚠️ Rad javobida manbalar UMUMAN bo'lmaydi — na `url`, na
         * `hlsUrl`.
         *
         * CDN manzili chiqib ketsa, u imzosiz ochiq havola bo'lardi va
         * ro'yxatdan o'tmagan odam pullik videoni ko'ra olardi
         * (§4.10 — bu masala hali ochiq).
         */
        @Test
        @DisplayName("Manbalar ro'yxati BO'SH — CDN manzili oshkor bo'lmaydi")
        void noSourcesWhenDenied() throws Exception {
            MediaAsset file = video("/videos/99/hls/master.m3u8");

            ContentSaveRequest.MediaLink link = new ContentSaveRequest.MediaLink();
            link.setRole(MediaRole.VIDEO);
            link.setMediaId(file.getId());
            link.setSortOrder(0);

            ContentSaveRequest c = new ContentSaveRequest();
            c.setContentType(ContentType.MOVIE);
            c.setStructureType(StructureType.SINGLE);
            c.setAccessPolicy(AccessPolicy.PREMIUM_ONLY);
            c.setStatus(PublicationStatus.PUBLISHED);
            c.setDurationMinutes(90);
            // Pullik kontent narxsiz bo'lolmaydi — `ContentService` buni
            // tekshiradi va bu to'g'ri qoida.
            c.setPremierePrice(new java.math.BigDecimal("25000"));
            c.setTranslations(Translations.all("Pullik " + SEQ.incrementAndGet()));
            c.setMedia(List.of(link));
            Content film = contentService.create(null, c);

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowed").value(false))
                    .andExpect(jsonPath("$.sources").isEmpty());
        }
    }
}
