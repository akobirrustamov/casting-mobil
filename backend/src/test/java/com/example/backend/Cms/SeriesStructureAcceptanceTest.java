package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Admin.Dto.SeasonSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.Season;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.EpisodeRepo;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.support.Translations;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §80 — serial tuzilmasi qabul mezoni.
 *
 * ТЗ ikkita aniq tuzilmani talab qiladi va ikkalasi ham shu yerda
 * <b>to'liq</b> quriladi:
 *
 * <pre>
 *   Serial A                   Mini Serial B
 *     Fasl 1                     1-qism
 *       1-qism                   2-qism
 *         video 1-bo'lak         3-qism
 *         video 2-bo'lak
 *       2-qism
 *         video
 *     Fasl 2
 *       1-qism
 *         video
 * </pre>
 *
 * <h2>Eng nozik joyi</h2>
 * «Fasl 1 → 1-qism» va «Fasl 2 → 1-qism» — bir xil raqam. Agar raqam
 * butun kontent bo'yicha noyob bo'lsa, ТЗ dagi tuzilmani umuman
 * yaratib bo'lmasdi. Shuning uchun bu alohida tekshiriladi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeriesStructureAcceptanceTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private ContentService contentService;
    @Autowired private EpisodeService episodeService;
    @Autowired private EpisodeRepo episodeRepo;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private EntityManager em;

    // ------------------------------------------------------ faslli serial

    @Nested
    @DisplayName("Faslli serial")
    class Seasonal {

        @Test
        @DisplayName("ТЗ dagi to'liq daraxt quriladi")
        void fullTreeIsBuilt() {
            Content series = series();

            Season s1 = season(series, 1, "Birinchi fasl");
            Season s2 = season(series, 2, "Ikkinchi fasl");

            // 1-fasl, 1-qism — IKKI bo'lakli video.
            Episode s1e1 = episode(series, s1, 1, "Boshlanish",
                    List.of(videoPart(1), videoPart(2)));
            // 1-fasl, 2-qism — bitta video.
            Episode s1e2 = episode(series, s1, 2, "Davomi", List.of(videoPart(1)));
            // 2-fasl, 1-qism — yana 1-raqam.
            Episode s2e1 = episode(series, s2, 1, "Yangi fasl", List.of(videoPart(1)));

            flush();

            assertThat(reload(s1e1).getVideos())
                    .as("bitta qism bir nechta video bo'lagidan iborat bo'lishi mumkin")
                    .hasSize(2)
                    .extracting(v -> v.getPartNumber())
                    .containsExactlyInAnyOrder(1, 2);

            assertThat(reload(s1e2).getVideos()).hasSize(1);
            assertThat(reload(s2e1).getVideos()).hasSize(1);

            assertThat(episodeRepo.findAllBySeasonIdOrderByEpisodeNumberAsc(s1.getId()))
                    .as("birinchi faslda ikkita qism")
                    .hasSize(2);
            assertThat(episodeRepo.findAllBySeasonIdOrderByEpisodeNumberAsc(s2.getId()))
                    .as("ikkinchi faslda bitta qism")
                    .hasSize(1);
        }

        @Test
        @DisplayName("Har xil fasllarda bir xil qism raqami mumkin")
        void sameNumberInDifferentSeasons() {
            Content series = series();
            Season s1 = season(series, 1, "Fasl 1");
            Season s2 = season(series, 2, "Fasl 2");

            // ⚠️ Raqam butun kontent bo'yicha noyob bo'lsa, ТЗ dagi
            // tuzilmani qurish MUMKIN BO'LMASDI: har bir yangi faslning
            // birinchi qismi rad etilardi.
            episode(series, s1, 1, "S1E1", List.of(videoPart(1)));
            episode(series, s2, 1, "S2E1", List.of(videoPart(1)));

            flush();

            assertThat(episodeRepo.findAllBySeasonIdOrderByEpisodeNumberAsc(s1.getId()))
                    .hasSize(1);
            assertThat(episodeRepo.findAllBySeasonIdOrderByEpisodeNumberAsc(s2.getId()))
                    .hasSize(1);
        }

        @Test
        @DisplayName("Bitta fasl ichida raqam takrorlanmaydi")
        void numberIsUniqueWithinSeason() {
            Content series = series();
            Season s1 = season(series, 1, "Fasl 1");
            episode(series, s1, 1, "Birinchi", List.of(videoPart(1)));

            // Bu esa xato: bitta faslda ikkita «1-qism» tomoshabinni
            // chalkashtirardi va tartib bazaga bog'liq bo'lib qolardi.
            assertThatThrownBy(() ->
                    episode(series, s1, 1, "Takroriy", List.of(videoPart(1))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("allaqachon mavjud");
        }
    }

    // ------------------------------------------------------- mini serial

    @Nested
    @DisplayName("Mini serial — fasilsiz")
    class Episodic {

        @Test
        @DisplayName("Uch qism fasl yaratmasdan qo'shiladi")
        void threeEpisodesWithoutSeason() {
            Content mini = miniSeries();

            episode(mini, null, 1, "Birinchi", List.of(videoPart(1)));
            episode(mini, null, 2, "Ikkinchi", List.of(videoPart(1)));
            episode(mini, null, 3, "Uchinchi", List.of(videoPart(1)));

            flush();

            List<Episode> all = episodeRepo.findAllByContentIdOrderBySortOrderAsc(mini.getId());
            assertThat(all).hasSize(3);
            assertThat(all).as("fasl talab qilinmasligi kerak")
                    .allMatch(e -> e.getSeason() == null);
            assertThat(all).extracting(Episode::getEpisodeNumber)
                    .containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("Fasilsiz qismlarda ham raqam takrorlanmaydi")
        void numberIsUniqueWithoutSeason() {
            Content mini = miniSeries();
            episode(mini, null, 1, "Birinchi", List.of(videoPart(1)));

            assertThatThrownBy(() ->
                    episode(mini, null, 1, "Takroriy", List.of(videoPart(1))))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private void flush() {
        em.flush();
        em.clear();
    }

    private Episode reload(Episode e) {
        return episodeRepo.findById(e.getId()).orElseThrow();
    }

    private Content series() {
        return content(ContentType.SERIES, StructureType.SEASONAL, "Serial A");
    }

    private Content miniSeries() {
        return content(ContentType.MINI_SERIES, StructureType.EPISODIC, "Mini Serial B");
    }

    private Content content(ContentType type, StructureType structure, String title) {
        ContentSaveRequest r = new ContentSaveRequest();
        r.setContentType(type);
        r.setStructureType(structure);
        r.setAccessPolicy(AccessPolicy.FREE);
        r.setStatus(PublicationStatus.DRAFT);
        r.setTranslations(Translations.all(title + " " + SEQ.incrementAndGet()));
        Content c = contentService.create(null, r);
        flush();
        return c;
    }

    private Season season(Content series, int number, String title) {
        SeasonSaveRequest r = new SeasonSaveRequest();
        r.setSeasonNumber(number);
        r.setTranslations(Translations.all(title + " " + SEQ.incrementAndGet()));
        Season s = episodeService.saveSeason(null, series.getId(), null, r);
        flush();
        return s;
    }

    private Episode episode(Content parent, Season season, int number, String title,
                            List<EpisodeSaveRequest.VideoLink> videos) {
        EpisodeSaveRequest r = new EpisodeSaveRequest();
        r.setSeasonId(season == null ? null : season.getId());
        r.setEpisodeNumber(number);
        r.setStatus(PublicationStatus.DRAFT);
        r.setTranslations(Translations.all(title + " " + SEQ.incrementAndGet()));
        r.setVideos(videos);
        return episodeService.saveEpisode(null, parent.getId(), null, r);
    }

    /** Bitta video bo'lagi — «1-qism, 2-bo'lak» kabi. */
    private EpisodeSaveRequest.VideoLink videoPart(int partNumber) {
        EpisodeSaveRequest.VideoLink v = new EpisodeSaveRequest.VideoLink();
        v.setMediaId(video().getId());
        v.setLocale(Locale.UZ);
        v.setPartNumber(partNumber);
        v.setSortOrder(partNumber - 1);
        return v;
    }

    private MediaAsset video() {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/serial-" + SEQ.incrementAndGet())
                .originalFilename("qism.mp4")
                .type(MediaType.VIDEO)
                .mimeType("video/mp4")
                .sizeBytes(1000L)
                .status(MediaStatus.READY)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
