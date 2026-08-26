package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Cms.Dto.HomeFeedDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentOrientation;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.HomepageSectionType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.Cms.Service.HomeFeedService;
import com.example.backend.Cms.Service.HomepageService;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tik format (Reels) — yo'nalish ilovagacha yetib boradimi.
 *
 * <h2>Qanday bo'shliq yopildi</h2>
 * {@link ContentOrientation} allaqachon bor edi va admin panelida
 * tanlanardi, lekin u FAQAT bosh sahifa feedida chiqardi. Ko'rish javobida
 * ham, qismlar ro'yxatida ham yo'q edi — ya'ni ilova pleyerni 16:9 qilib
 * chizardi va tik rolik keng qora quti ichida ip bo'lib qolardi.
 *
 * <h2>Nima uchun faylning o'zidan aniqlab bo'lmaydi</h2>
 * Qulflangan kontentda video UMUMAN berilmaydi ({@code sources} bo'sh) —
 * demak o'lchamni o'qish uchun fayl yo'q. Afisha esa aynan o'sha paytda
 * chiziladi. Shuning uchun yo'nalishni server aytadi, hatto rad javobida
 * ham.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VerticalFormatTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentService contentService;
    @Autowired private EpisodeService episodeService;
    @Autowired private HomeFeedService homeFeedService;
    @Autowired private HomepageService homepageService;

    @BeforeEach
    void ensureSections() {
        homepageService.sections();
    }

    // ------------------------------------------------------------- yordamchi

    private Content content(ContentType type, StructureType structure,
                            ContentOrientation orientation, AccessPolicy policy) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(type);
        c.setStructureType(structure);
        c.setOrientation(orientation);
        c.setAccessPolicy(policy);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setDurationMinutes(structure == StructureType.SINGLE ? 3 : null);
        if (policy != AccessPolicy.FREE) {
            c.setPremierePrice(new BigDecimal("15000"));
        }
        c.setTranslations(Translations.all("Format " + SEQ.incrementAndGet()));
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

    // ------------------------------------------------------------ ko'rish

    @Nested
    @DisplayName("Ko'rish javobi")
    class Watch {

        @Test
        @DisplayName("Tik klip VERTICAL deb qaytadi")
        void verticalSingleReportsOrientation() throws Exception {
            Content clip = content(ContentType.CLIP, StructureType.SINGLE,
                    ContentOrientation.VERTICAL, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/watch/content/" + clip.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orientation").value("VERTICAL"));
        }

        @Test
        @DisplayName("Odatiy film LANDSCAPE deb qaytadi")
        void landscapeSingleReportsOrientation() throws Exception {
            Content movie = content(ContentType.MOVIE, StructureType.SINGLE,
                    ContentOrientation.LANDSCAPE, AccessPolicy.FREE);

            mockMvc.perform(get("/api/v1/app/watch/content/" + movie.getId()))
                    .andExpect(jsonPath("$.orientation").value("LANDSCAPE"));
        }

        @Test
        @DisplayName("Qism o'z kontentining yo'nalishini oladi")
        void episodeInheritsContentOrientation() throws Exception {
            Content series = content(ContentType.MINI_SERIES, StructureType.EPISODIC,
                    ContentOrientation.VERTICAL, AccessPolicy.FREE);
            Episode first = episode(series, 1);

            mockMvc.perform(get("/api/v1/app/watch/" + first.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orientation").value("VERTICAL"));
        }

        /**
         * Eng muhim holat: rad javobida video yo'q, demak yo'nalishni
         * fayldan aniqlab bo'lmaydi — lekin afisha aynan shu paytda
         * chiziladi.
         */
        @Test
        @DisplayName("Qulflangan kontentda ham yo'nalish aytiladi")
        void lockedContentStillReportsOrientation() throws Exception {
            Content paid = content(ContentType.MINI_SERIES, StructureType.SINGLE,
                    ContentOrientation.VERTICAL, AccessPolicy.PREMIUM_OR_PURCHASE);

            mockMvc.perform(get("/api/v1/app/watch/content/" + paid.getId()))
                    .andExpect(jsonPath("$.allowed").value(false))
                    .andExpect(jsonPath("$.sources").isEmpty())
                    .andExpect(jsonPath("$.orientation").value("VERTICAL"));
        }
    }

    // -------------------------------------------------------- qismlar ro'yxati

    @Nested
    @DisplayName("Qismlar ro'yxati")
    class EpisodeList {

        @Test
        @DisplayName("Ro'yxat yo'nalishni bir marta, kontent darajasida beradi")
        void listReportsOrientationOnce() throws Exception {
            Content series = content(ContentType.MINI_SERIES, StructureType.EPISODIC,
                    ContentOrientation.VERTICAL, AccessPolicy.FREE);
            episode(series, 1);
            episode(series, 2);

            mockMvc.perform(get("/api/v1/app/content/" + series.getId() + "/episodes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orientation").value("VERTICAL"))
                    .andExpect(jsonPath("$.episodes.length()").value(2))
                    // Qismda o'z yo'nalishi YO'Q: bitta serialning qismlari
                    // har xil formatda bo'lmaydi, takrorlash esa ular
                    // ajralib ketishi mumkin degan va'da bo'lardi.
                    .andExpect(jsonPath("$.episodes[0].orientation").doesNotExist());
        }
    }

    // ------------------------------------------------------------ bosh sahifa

    @Nested
    @DisplayName("Reels qatori")
    class ReelsRow {

        /**
         * Yo'nalish TUR emas: tik formatda ham mini-serial, ham klip
         * bo'lishi mumkin (ТЗ §13 — o'qlar mustaqil).
         */
        @Test
        @DisplayName("Qatorga faqat tik kontent tushadi")
        void onlyVerticalContentInReelsRow() {
            Content vertical = content(ContentType.MINI_SERIES, StructureType.SINGLE,
                    ContentOrientation.VERTICAL, AccessPolicy.FREE);
            Content landscape = content(ContentType.MINI_SERIES, StructureType.SINGLE,
                    ContentOrientation.LANDSCAPE, AccessPolicy.FREE);

            HomeFeedDto.Section reels = row(HomepageSectionType.REELS_SERIES);
            assertThat(reels).isNotNull();

            List<Long> ids = reels.getContent().stream()
                    .map(HomeFeedDto.ContentCard::getId).toList();
            assertThat(ids).contains(vertical.getId());
            assertThat(ids).doesNotContain(landscape.getId());
        }

        @Test
        @DisplayName("Kartochka o'z formatini aytadi — ilova shakli shundan")
        void cardCarriesOrientation() {
            Content vertical = content(ContentType.CLIP, StructureType.SINGLE,
                    ContentOrientation.VERTICAL, AccessPolicy.FREE);

            HomeFeedDto.ContentCard card = row(HomepageSectionType.REELS_SERIES)
                    .getContent().stream()
                    .filter(c -> c.getId().equals(vertical.getId()))
                    .findFirst().orElseThrow();

            assertThat(card.getOrientation()).isEqualTo("VERTICAL");
        }

        private HomeFeedDto.Section row(HomepageSectionType type) {
            return homeFeedService.build(null, Locale.UZ).getSections().stream()
                    .filter(s -> s.getType() == type)
                    .findFirst().orElse(null);
        }
    }
}
