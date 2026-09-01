package com.example.backend.Cms;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Admin.TestStaffFactory;
import com.example.backend.Enums.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Panelda videoni OLDINDAN KO'RISH.
 *
 * <h2>⚠️ Qanday kamchilikni yopadi</h2>
 * Xodim video yuklardi, lekin uni HECH QACHON ko'ra olmasdi —
 * panelda pleyer umuman yo'q edi. Videoning buzuq emasligini
 * tekshirishning yagona yo'li kontentni NASHR QILIB, ilovadan ochish
 * edi.
 *
 * <h2>⚠️ Nega chipta URL'da</h2>
 * Brauzerning {@code <video src>} elementi {@code Authorization}
 * sarlavhasini YUBORMAYDI. Ya'ni token bo'lsa ham element uni
 * uzatmaydi va xodim o'z videosini ko'ra olmasdi.
 *
 * Chipta esa manzilning o'zida keladi. Bu {@code HlsController} da
 * allaqachon shu sabab bilan qo'llanilgan yechim.
 *
 * <h2>⚠️ Bu yerda XAVFSIZLIK CHEGARASI bor</h2>
 * Chipta URL'da bo'lgani uchun u nusxalanishi mumkin: brauzer
 * tarixida, loglarda, «havolani nusxalash» orqali. Shuning uchun
 * chipta HUQUQ BERMAYDI — u faqat KIMLIGINI aytadi, huquqni esa har
 * so'rovda {@code AccessService} qayta hisoblaydi.
 *
 * Quyidagi testlar aynan shu chegarani qo'riqlaydi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// ⚠️ `TestStaffFactory` — `@TestComponent`, ya'ni oddiy skanerlashga
// tushmaydi va aniq import qilinishi kerak.
@org.springframework.context.annotation.Import(TestStaffFactory.class)
class AdminVideoPreviewTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private TestStaffFactory staff;
    @Autowired private com.example.backend.Cms.Service.StorageService storageService;

    private Long videoId;
    private String token;

    private static final java.util.concurrent.atomic.AtomicInteger SEQ =
            new java.util.concurrent.atomic.AtomicInteger();

    @BeforeEach
    void seed() {
        token = staff.token("+99890001" + (600 + SEQ.incrementAndGet()),
                java.util.EnumSet.of(Permission.MEDIA_VIEW, Permission.CONTENT_VIEW));

        // ⚠️ HAQIQIY fayl ham kerak. Faqat baza yozuvi yaratilsa
        // `storageService.load()` yiqiladi va endpoint 404 beradi —
        // ya'ni test ruxsatni emas, yo'q faylni tekshirgan bo'lardi.
        String key = storageService.store(
                new java.io.ByteArrayInputStream(new byte[4096]),
                "preview-test.mp4", "content");

        MediaAsset video = mediaAssetRepo.save(MediaAsset.builder()
                .storageKey(key)
                .originalFilename("preview-test.mp4")
                .type(MediaType.VIDEO)
                .mimeType("video/mp4")
                .sizeBytes(1024L)
                .status(MediaStatus.READY)
                .build());
        videoId = video.getId();
    }

    @Nested
    @DisplayName("Xodim o'z videosini ko'ra oladi")
    class StaffCanPreview {

        @Test
        @DisplayName("Preview havola chipta bilan qaytadi")
        void previewReturnsTicketedUrl() throws Exception {
            String body = mockMvc.perform(get(
                            "/api/v1/app/admin/media/" + videoId + "/preview")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body)
                    .as("havola aynan shu media'ga ko'rsatsin")
                    .contains("/api/v1/app/media/" + videoId + "/raw");

            assertThat(body)
                    .as("chipta URL'da bo'lsin — <video> sarlavha yubormaydi")
                    .contains("?t=");
        }

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV — chipta HAQIQATAN ishlashi kerak.
         *
         * Havola qaytarilib, keyin 404 bersa tuzatishning ma'nosi
         * yo'q edi.
         */
        @Test
        @DisplayName("Chipta bilan fayl OCHILADI — tokensiz ham")
        void ticketOpensTheFile() throws Exception {
            String url = ticketUrl();

            // ⚠️ Sarlavhasiz: aynan brauzer `<video>` qiladigan so'rov.
            mockMvc.perform(get(url))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertThat(code)
                                .as("chipta bilan kirish ochiq bo'lsin, 404 emas")
                                .isNotEqualTo(404);
                    });
        }
    }

    @Nested
    @DisplayName("⚠️ Chegaralar")
    class Boundaries {

        /**
         * Chiptasiz va tokensiz — yopiq.
         *
         * ⚠️ Bu holat tuzatishdan OLDIN ham yopiq edi va shunday
         * qolishi kerak: aks holda har qanday video ochiq bo'lardi.
         */
        @Test
        @DisplayName("Chiptasiz va tokensiz — YOPIQ")
        void withoutTicketClosed() throws Exception {
            mockMvc.perform(get("/api/v1/app/media/" + videoId + "/raw"))
                    .andExpect(status().isNotFound());
        }

        /**
         * ⚠️ Chipta BOSHQA media uchun ishlamaydi.
         *
         * Busiz bitta videoga chipta olgan xodim manzildagi raqamni
         * almashtirib, ko'rish huquqi yo'q har qanday videoni ocha
         * olardi.
         */
        @Test
        @DisplayName("Boshqa media'ning chiptasi ISHLAMAYDI")
        void ticketIsBoundToOneMedia() throws Exception {
            String otherKey = storageService.store(
                    new java.io.ByteArrayInputStream(new byte[1024]),
                    "other.mp4", "content");

            MediaAsset other = mediaAssetRepo.save(MediaAsset.builder()
                    .storageKey(otherKey)
                    .originalFilename("other.mp4")
                    .type(MediaType.VIDEO)
                    .mimeType("video/mp4")
                    .sizeBytes(1024L)
                    .status(MediaStatus.READY)
                    .build());

            String ticket = ticketUrl().split("\\?t=")[1];

            mockMvc.perform(get("/api/v1/app/media/" + other.getId() + "/raw?t=" + ticket))
                    .andExpect(status().isNotFound());
        }

        /** Buzuq chipta ham «topilmadi» — media bor-yo'qligi oshkor bo'lmasin. */
        @Test
        @DisplayName("Buzuq chipta — topilmadi")
        void brokenTicketRejected() throws Exception {
            mockMvc.perform(get("/api/v1/app/media/" + videoId + "/raw?t=buzuq.chipta.matni"))
                    .andExpect(status().isNotFound());
        }

        /**
         * ⚠️ Range so'rovi ham chiptani TEKSHIRADI.
         *
         * Pleyer birinchi so'rovni Range'siz, keyingi hammasini Range
         * bilan yuboradi. Chipta faqat birinchisida tekshirilsa video
         * ochilib DARROV to'xtardi — va sababi tushunarsiz bo'lardi.
         */
        @Test
        @DisplayName("Range so'rovi ham chipta bilan ishlaydi")
        void rangeRequestAlsoAcceptsTicket() throws Exception {
            String url = ticketUrl();

            mockMvc.perform(get(url).header("Range", "bytes=0-99"))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertThat(code)
                                .as("Range so'rovi chiptasiz qolib ketmasin")
                                .isNotEqualTo(404);
                    });
        }
    }

    /** Preview chaqirib, javobdagi manzilni oladi. */
    private String ticketUrl() throws Exception {
        String body = mockMvc.perform(get(
                        "/api/v1/app/admin/media/" + videoId + "/preview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int from = body.indexOf("\"url\":\"") + 7;
        return body.substring(from, body.indexOf('"', from)).replace("\\u003d", "=");
    }
}
