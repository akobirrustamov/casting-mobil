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
import com.example.backend.Cms.Service.StorageService;
import com.example.backend.Cms.Service.Video.PlaybackTicketService;
import com.example.backend.Cms.Service.Video.SignedUrlProvider;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pullik kontent HLS orqali sizib chiqmasligini qo'riqlaydi (§4.10).
 *
 * <h2>Nega bu test kerak</h2>
 * {@code /raw} yo'li uchun {@link PaidContentLeakTest} bor. HLS esa
 * BUTUNLAY BOSHQA yo'l: segmentlar Spring Boot'dan o'tmaydi, ya'ni
 * {@code AccessService} tekshiruvi o'z-o'zidan qo'llanmaydi.
 *
 * Kimdir kelajakda playlistni chiptasiz ochiq qilsa, yoki segmentni
 * ham shu endpointdan bersa, yoki {@code ..} ni o'tkazib yuborsa —
 * test darhol yiqiladi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(HlsProtectionTest.FakeSigner.class)
class HlsProtectionTest {

    /**
     * Imzolovchining o'rnini bosuvchi.
     *
     * ⚠️ Haqiqiy {@code PresignedUrlProvider} faqat S3 sozlanganda
     * ko'tariladi. Test muhitida S3 yo'q, shuning uchun proksi yo'lini
     * umuman tekshirib bo'lmasdi — bean qo'lda beriladi.
     *
     * Imzo o'rniga ko'rinadigan belgi qo'yiladi: playlistda AYNAN
     * shu qator paydo bo'lganini tekshirish mumkin.
     */
    @TestConfiguration
    static class FakeSigner {
        @Bean
        SignedUrlProvider signedUrlProvider() {
            return new SignedUrlProvider() {
                @Override
                public String sign(String storageKey) {
                    return "https://s3.test" + storageKey + "?X-Amz-Signature=imzo";
                }

                @Override
                public boolean isAvailable() {
                    return true;
                }
            };
        }
    }

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentService contentService;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private StorageService storageService;
    @Autowired private PlaybackTicketService ticketService;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------- yordamchi

    private static final String MASTER = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=854x480
            480p/index.m3u8
            """;

    private static final String VARIANT = """
            #EXTM3U
            #EXT-X-MAP:URI="init.mp4"
            #EXTINF:6.000,
            segment_00001.m4s
            #EXT-X-ENDLIST
            """;

    /** Media yozuvi + omborda haqiqiy playlist fayllari. */
    private MediaAsset transcodedVideo() {
        int n = SEQ.incrementAndGet();
        String dir = "/videos/hls-test-" + n + "/hls";

        put(dir + "/master.m3u8", MASTER);
        put(dir + "/480p/index.m3u8", VARIANT);
        // ⚠️ Segment ham HAQIQATDAN yoziladi.
        //
        // Bo'lmasa `segmentsNotProxied` testi bekorga yashil bo'lardi:
        // so'rov 404 qaytarardi, lekin «segment berilmaydi» degani
        // uchun emas, «fayl yo'q» degani uchun. Kengaytma tekshiruvi
        // olib tashlansa ham test buni sezmasdi.
        put(dir + "/480p/segment_00001.m4s", "segment mazmuni");

        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/video-" + n + ".mp4")
                .originalFilename("kino.mp4")
                .type(MediaType.VIDEO)
                .mimeType("video/mp4")
                .sizeBytes(1024L)
                .durationSeconds(600)
                .status(MediaStatus.READY)
                .hlsMasterKey(dir + "/master.m3u8")
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void put(String key, String body) {
        storageService.storeAt(
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
                key, "application/vnd.apple.mpegurl");
    }

    private Content movie(MediaAsset file, AccessPolicy policy) {
        ContentSaveRequest.MediaLink link = new ContentSaveRequest.MediaLink();
        link.setRole(MediaRole.VIDEO);
        link.setMediaId(file.getId());
        link.setSortOrder(0);

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
        c.setMedia(List.of(link));
        return contentService.create(null, c);
    }

    /** Oddiy tomoshabin — panel xodimi EMAS. */
    private User viewer() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99890" + (9700000 + SEQ.incrementAndGet()));
        u.setPassword(passwordEncoder.encode("Parol123!"));
        u.setName("Tomoshabin " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    // ------------------------------------------------------- tomosha javobi

    @Nested
    @DisplayName("`/watch` javobidagi manzil")
    class WatchResponse {

        /**
         * ⚠️ NISBIY yo'l — mobil uning oldiga o'z {@code BASE_URL} ini
         * qo'yadi. Mutlaq manzil yozilsa server o'z tashqi domenini
         * taxmin qilishi kerak bo'lardi.
         */
        @Test
        @DisplayName("Imzolash bor bo'lsa — proksi yo'li, chipta bilan")
        void hlsUrlPointsAtProxy() throws Exception {
            MediaAsset file = transcodedVideo();
            Content film = movie(file, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sources[0].hlsUrl")
                            .value(org.hamcrest.Matchers.startsWith(
                                    "/api/v1/app/media/" + file.getId()
                                            + "/hls/master.m3u8?t=")));
        }

        @Test
        @DisplayName("Eski `url` maydoni o'zgarmaydi")
        void rawUrlUnchanged() throws Exception {
            MediaAsset file = transcodedVideo();
            Content film = movie(file, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(jsonPath("$.sources[0].url")
                            .value("/api/v1/app/media/" + file.getId() + "/raw"));
        }

        /**
         * ⚠️ Rad javobida chipta ham berilmasligi kerak — u berilsa
         * himoyaning butun ma'nosi yo'qolardi.
         */
        @Test
        @DisplayName("Ruxsat yo'q — manbalar ham, chipta ham yo'q")
        void deniedGivesNothing() throws Exception {
            MediaAsset file = transcodedVideo();
            Content film = movie(file, AccessPolicy.PREMIUM_ONLY);

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(jsonPath("$.allowed").value(false))
                    .andExpect(jsonPath("$.sources").isEmpty());
        }
    }

    // ------------------------------------------------------------ playlist

    @Nested
    @DisplayName("Playlist qaytarilishi")
    class Playlist {

        @Test
        @DisplayName("Master ichidagi variant BIZNING endpointga ishora qiladi")
        void masterPointsAtProxy() throws Exception {
            MediaAsset file = transcodedVideo();
            movie(file, AccessPolicy.FREE);
            String ticket = ticketService.issue(null, file.getId());

            mockMvc.perform(get("/api/v1/app/media/" + file.getId() + "/hls/master.m3u8")
                            .param("t", ticket))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(
                            "/api/v1/app/media/" + file.getId()
                                    + "/hls/480p/index.m3u8?t=")));
        }

        /**
         * ⚠️ Segment manzili OMBORGA ishora qilishi shart.
         *
         * Bizga ishora qilsa gigabaytlar Spring Boot orqali oqardi —
         * aynan shundan qochish uchun butun HLS ishi qilingan.
         */
        @Test
        @DisplayName("Variant ichidagi segment imzolangan havolaga aylanadi")
        void segmentsAreSigned() throws Exception {
            MediaAsset file = transcodedVideo();
            movie(file, AccessPolicy.FREE);
            String ticket = ticketService.issue(null, file.getId());

            String body = mockMvc.perform(
                            get("/api/v1/app/media/" + file.getId() + "/hls/480p/index.m3u8")
                                    .param("t", ticket))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body)
                    .contains("https://s3.test")
                    .contains("segment_00001.m4s?X-Amz-Signature=imzo")
                    // `init.mp4` unutilsa video umuman ochilmasdi.
                    .contains("init.mp4?X-Amz-Signature=imzo");
            assertThat(body).doesNotContain("/api/v1/app/media/" + file.getId() + "/hls/480p/seg");
        }

        /**
         * Playlist ichida muddati cheklangan imzolar bor. Keshlansa,
         * imzolar eskirgach «video ochilmadi» bo'lardi.
         */
        @Test
        @DisplayName("Playlist keshlanmaydi")
        void notCached() throws Exception {
            MediaAsset file = transcodedVideo();
            movie(file, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/media/" + file.getId() + "/hls/master.m3u8")
                            .param("t", ticketService.issue(null, file.getId())))
                    .andExpect(header -> assertThat(
                            header.getResponse().getHeader("Cache-Control"))
                            .contains("no-store"));
        }
    }

    // ------------------------------------------------------------ himoya

    @Nested
    @DisplayName("Kirish nazorati")
    class Protection {

        @Test
        @DisplayName("Chiptasiz so'rov o'tmaydi")
        void ticketRequired() throws Exception {
            MediaAsset file = transcodedVideo();
            movie(file, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/media/" + file.getId() + "/hls/master.m3u8"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Soxta chipta o'tmaydi")
        void forgedTicketRejected() throws Exception {
            MediaAsset file = transcodedVideo();
            movie(file, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/media/" + file.getId() + "/hls/master.m3u8")
                            .param("t", "soxta.chipta.imzo"))
                    .andExpect(status().isNotFound());
        }

        /**
         * ⚠️ Chipta AYNAN bitta media uchun.
         *
         * Tekshirilmasa bepul klipning chiptasi bilan pullik filmni
         * ochish mumkin bo'lardi — imzo ikkalasida ham to'g'ri.
         */
        @Test
        @DisplayName("Boshqa media chiptasi o'tmaydi")
        void ticketIsBoundToMedia() throws Exception {
            // ⚠️ IKKALASI HAM BEPUL. Biri pullik bo'lsa, so'rovni
            // `AccessService` to'xtatardi va bog'lanish tekshiruvi
            // olib tashlansa ham test yashil qolardi — ya'ni hech
            // narsani qo'riqlamasdi.
            MediaAsset first = transcodedVideo();
            movie(first, AccessPolicy.FREE);

            MediaAsset second = transcodedVideo();
            movie(second, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/media/" + second.getId() + "/hls/master.m3u8")
                            .param("t", ticketService.issue(null, first.getId())))
                    .andExpect(status().isNotFound());
        }

        /**
         * ⚠️ Chipta va kirish tokeni BIR XIL kalit bilan imzolanadi,
         * ya'ni imzo tekshiruvidan ikkalasi ham o'tadi. Farqni faqat
         * tur belgisi qiladi.
         */
        @Test
        @DisplayName("Kirish tokeni chipta o'rniga ishlamaydi")
        void accessTokenIsNotATicket() throws Exception {
            MediaAsset file = transcodedVideo();
            movie(file, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/media/" + file.getId() + "/hls/master.m3u8")
                            .param("t", jwtService.generateJwtToken(viewer())))
                    .andExpect(status().isNotFound());
        }

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV.
         *
         * Chipta faqat KIMLIGINI aytadi. Huquq har so'rovda
         * {@code AccessService} dan qayta so'raladi — shuning uchun
         * chiptasi bor, lekin obunasi yo'q odam playlistni ololmaydi.
         */
        @Test
        @DisplayName("Chipta HUQUQ bermaydi — pullik film baribir yopiq")
        void ticketDoesNotGrantAccess() throws Exception {
            MediaAsset file = transcodedVideo();
            movie(file, AccessPolicy.PREMIUM_ONLY);

            // Chipta to'g'ri imzolangan va aynan shu media uchun.
            mockMvc.perform(get("/api/v1/app/media/" + file.getId() + "/hls/master.m3u8")
                            .param("t", ticketService.issue(viewer(), file.getId())))
                    .andExpect(status().isNotFound());
        }

        /**
         * Boshqa papkaga chiqish IMKONI yo'q.
         *
         * ⚠️ Bu yerda hozir Spring'ning {@code StrictHttpFirewall} ishlaydi
         * — so'rov kontrollerga umuman yetib kelmaydi. Test AYNAN shu
         * xatti-harakatni qo'riqlaydi: qaysi qavat rad etishidan qat'i
         * nazar, {@code ..} o'tmasligi kerak.
         *
         * Kontrollerdagi tekshiruv ikkinchi qavat bo'lib qoladi —
         * firewall sozlamasi yumshatilsa u yagona to'siq bo'ladi.
         */
        @Test
        @DisplayName("`..` bilan boshqa papkaga chiqib bo'lmaydi")
        void pathTraversalBlocked() throws Exception {
            MediaAsset file = transcodedVideo();
            movie(file, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/media/" + file.getId()
                            + "/hls/../../other/hls/master.m3u8")
                            .param("t", ticketService.issue(null, file.getId())))
                    .andExpect(status().is4xxClientError());
        }

        /**
         * ⚠️ Bu endpoint FAQAT playlist uchun. Segmentni ham bersa,
         * butun trafik serverimizdan oqib, HLS'ga o'tishning ma'nosi
         * qolmasdi.
         */
        @Test
        @DisplayName("Segment bu endpointdan berilmaydi")
        void segmentsNotProxied() throws Exception {
            MediaAsset file = transcodedVideo();
            movie(file, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/media/" + file.getId()
                            + "/hls/480p/segment_00001.m4s")
                            .param("t", ticketService.issue(null, file.getId())))
                    .andExpect(status().is4xxClientError());
        }

        /**
         * Transcoding tugamagan media uchun HLS yo'q — «xato» emas,
         * «hali yo'q». Klient eski {@code /raw} yo'liga qaytadi.
         */
        @Test
        @DisplayName("Transcoding qilinmagan media — 404")
        void notTranscodedIsNotFound() throws Exception {
            MediaAsset file = mediaAssetRepo.save(MediaAsset.builder()
                    .storageKey("/test/xom-" + SEQ.incrementAndGet() + ".mp4")
                    .originalFilename("xom.mp4")
                    .type(MediaType.VIDEO)
                    .mimeType("video/mp4")
                    .sizeBytes(1024L)
                    .status(MediaStatus.READY)
                    .createdAt(LocalDateTime.now())
                    .build());
            movie(file, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/media/" + file.getId() + "/hls/master.m3u8")
                            .param("t", ticketService.issue(null, file.getId())))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------- chipta

    @Nested
    @DisplayName("Chipta")
    class Ticket {

        @Test
        @DisplayName("Egasini qaytaradi")
        void resolvesHolder() {
            User u = viewer();
            String ticket = ticketService.issue(u, 42L);

            assertThat(ticketService.holderOf(ticket, 42L))
                    .extracting(User::getId).isEqualTo(u.getId());
        }

        @Test
        @DisplayName("Anonim chipta — egasi yo'q")
        void anonymousHasNoHolder() {
            assertThat(ticketService.holderOf(ticketService.issue(null, 42L), 42L)).isNull();
        }

        /**
         * ⚠️ Hisob o'chirilgan bo'lsa ham chipta imzosi to'g'ri
         * qolaveradi. Bunday holatda anonim sifatida qaraladi — ya'ni
         * faqat bepul kontent ochiladi.
         */
        @Test
        @DisplayName("O'chirilgan hisob egasi topilmaydi")
        void deletedHolderIsAnonymous() {
            User u = viewer();
            String ticket = ticketService.issue(u, 42L);
            userRepo.deleteById(u.getId());

            assertThat(ticketService.holderOf(ticket, 42L)).isNull();
        }

        @Test
        @DisplayName("Boshqa media uchun chipta rad etiladi")
        void wrongMediaRejected() {
            String ticket = ticketService.issue(null, 42L);

            org.assertj.core.api.Assertions
                    .assertThatThrownBy(() -> ticketService.holderOf(ticket, 43L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Ma'nosiz satr rad etiladi")
        void garbageRejected() {
            org.assertj.core.api.Assertions
                    .assertThatThrownBy(() -> ticketService.holderOf("a.b.c", 42L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Kirish tokeni chipta sifatida rad etiladi")
        void accessTokenRejected() {
            User u = viewer();
            String access = jwtService.generateJwtToken(u);

            org.assertj.core.api.Assertions
                    .assertThatThrownBy(() -> ticketService.holderOf(access, 42L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Har chipta o'z media raqamiga bog'langan")
        void ticketsDifferPerMedia() {
            assertThat(ticketService.issue(null, 1L))
                    .isNotEqualTo(ticketService.issue(null, 2L));
        }
    }
}
