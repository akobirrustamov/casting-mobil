package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Admin.Dto.SeasonSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.Season;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.support.Translations;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §80 — kontent tuzilishining qabul mezoni.
 *
 * Uch holat ham ishlashi kerak:
 *   SEASONAL — Serial → Fasl → Qism → bir nechta video segment;
 *   EPISODIC — Mini-serial → Qism (faslsiz);
 *   SINGLE   — qismsiz.
 *
 * Bu testlar aynan shu tuzilmalar qurilishini va noto'g'ri kombinatsiyalar
 * rad etilishini qo'riqlaydi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContentStructureTest {

    @Autowired
    private ContentService contentService;

    @Autowired
    private EpisodeService episodeService;

    // ------------------------------------------------------------- yordamchi

    private Map<Locale, TranslationDto> uz(String title) {
        return Translations.all(title);
    }

    private Content newContent(StructureType structure, String title) {
        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(structure == StructureType.SINGLE
                ? ContentType.MOVIE : ContentType.SERIES);
        req.setStructureType(structure);
        // Pullik SINGLE uchun narx majburiy (ContentService.validate) - shu sababli
        // bitta qismlik testlarda FREE ishlatiladi.
        req.setAccessPolicy(structure == StructureType.SINGLE
                ? AccessPolicy.FREE : AccessPolicy.PREMIUM_OR_PURCHASE);
        req.setTranslations(uz(title));
        return contentService.create(null, req);
    }

    private Season newSeason(Content content, int number, String title) {
        SeasonSaveRequest req = new SeasonSaveRequest();
        req.setSeasonNumber(number);
        req.setStatus(PublicationStatus.PUBLISHED);
        req.setTranslations(uz(title));
        return episodeService.saveSeason(null, content.getId(), null, req);
    }

    private EpisodeSaveRequest episodeRequest(Long seasonId, int number, String title) {
        EpisodeSaveRequest req = new EpisodeSaveRequest();
        req.setSeasonId(seasonId);
        req.setEpisodeNumber(number);
        req.setStatus(PublicationStatus.PUBLISHED);
        req.setTranslations(uz(title));
        return req;
    }

    // -------------------------------------------------------------- SEASONAL

    @Nested
    @DisplayName("SEASONAL: Serial → Fasl → Qism → video segmentlar")
    class Seasonal {

        @Test
        @DisplayName("To'liq tuzilish quriladi")
        void fullStructureIsBuilt() {
            Content series = newContent(StructureType.SEASONAL, "Serial A");
            Season s1 = newSeason(series, 1, "1-fasl");
            Season s2 = newSeason(series, 2, "2-fasl");

            episodeService.saveEpisode(null, series.getId(), null,
                    episodeRequest(s1.getId(), 1, "1-qism"));
            episodeService.saveEpisode(null, series.getId(), null,
                    episodeRequest(s1.getId(), 2, "2-qism"));
            episodeService.saveEpisode(null, series.getId(), null,
                    episodeRequest(s2.getId(), 1, "2-fasl 1-qism"));

            assertThat(episodeService.seasonsOf(series.getId())).hasSize(2);

            List<Episode> all = episodeService.episodesOf(series.getId());
            assertThat(all).hasSize(3);
            assertThat(all).allSatisfy(e ->
                    assertThat(e.getSeason()).as("SEASONAL da har qism faslga tegishli").isNotNull());
        }

        @Test
        @DisplayName("Har xil fasllarda bir xil qism raqami bo'lishi MUMKIN")
        void sameEpisodeNumberInDifferentSeasons() {
            Content series = newContent(StructureType.SEASONAL, "Serial B");
            Season s1 = newSeason(series, 1, "1-fasl");
            Season s2 = newSeason(series, 2, "2-fasl");

            episodeService.saveEpisode(null, series.getId(), null,
                    episodeRequest(s1.getId(), 1, "S1E1"));
            // Bu YIQILMASLIGI kerak - raqam fasl ichida unikal
            episodeService.saveEpisode(null, series.getId(), null,
                    episodeRequest(s2.getId(), 1, "S2E1"));

            assertThat(episodeService.episodesOf(series.getId())).hasSize(2);
        }

        @Test
        @DisplayName("Bitta fasl ichida qism raqami takrorlanmaydi")
        void duplicateEpisodeNumberInSameSeasonRejected() {
            Content series = newContent(StructureType.SEASONAL, "Serial C");
            Season s1 = newSeason(series, 1, "1-fasl");
            episodeService.saveEpisode(null, series.getId(), null,
                    episodeRequest(s1.getId(), 1, "1-qism"));

            assertThatThrownBy(() -> episodeService.saveEpisode(null, series.getId(), null,
                    episodeRequest(s1.getId(), 1, "Takror")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("1-qism");
        }

        @Test
        @DisplayName("Faslsiz qism qabul qilinmaydi")
        void episodeWithoutSeasonRejected() {
            Content series = newContent(StructureType.SEASONAL, "Serial D");
            assertThatThrownBy(() -> episodeService.saveEpisode(null, series.getId(), null,
                    episodeRequest(null, 1, "Faslsiz")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("fasl");
        }

        @Test
        @DisplayName("Qismlari bor fasl o'chirilmaydi")
        void nonEmptySeasonCannotBeDeleted() {
            Content series = newContent(StructureType.SEASONAL, "Serial E");
            Season s1 = newSeason(series, 1, "1-fasl");
            episodeService.saveEpisode(null, series.getId(), null,
                    episodeRequest(s1.getId(), 1, "1-qism"));

            assertThatThrownBy(() ->
                    episodeService.deleteSeason(null, series.getId(), s1.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("qism");
        }
    }

    // -------------------------------------------------------------- EPISODIC

    @Nested
    @DisplayName("EPISODIC: Mini-serial → Qism (faslsiz)")
    class Episodic {

        @Test
        @DisplayName("Faslsiz qismlar quriladi")
        void episodesWithoutSeasons() {
            Content mini = newContent(StructureType.EPISODIC, "Mini-serial B");

            for (int i = 1; i <= 3; i++) {
                episodeService.saveEpisode(null, mini.getId(), null,
                        episodeRequest(null, i, i + "-qism"));
            }

            List<Episode> all = episodeService.episodesOf(mini.getId());
            assertThat(all).hasSize(3);
            assertThat(all).allSatisfy(e ->
                    assertThat(e.getSeason()).as("EPISODIC da fasl bo'lmaydi").isNull());
        }

        @Test
        @DisplayName("Fasl qo'shib bo'lmaydi")
        void seasonRejected() {
            Content mini = newContent(StructureType.EPISODIC, "Mini-serial C");
            SeasonSaveRequest req = new SeasonSaveRequest();
            req.setSeasonNumber(1);
            req.setTranslations(uz("Fasl"));

            assertThatThrownBy(() -> episodeService.saveSeason(null, mini.getId(), null, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("SEASONAL");
        }

        @Test
        @DisplayName("Qism raqami butun kontent bo'yicha takrorlanmaydi")
        void duplicateNumberRejected() {
            Content mini = newContent(StructureType.EPISODIC, "Mini-serial D");
            episodeService.saveEpisode(null, mini.getId(), null, episodeRequest(null, 1, "1-qism"));

            assertThatThrownBy(() -> episodeService.saveEpisode(null, mini.getId(), null,
                    episodeRequest(null, 1, "Takror")))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ---------------------------------------------------------------- SINGLE

    @Nested
    @DisplayName("SINGLE: qismsiz")
    class Single {

        @Test
        @DisplayName("Qism ham, fasl ham qo'shilmaydi")
        void noPartsAllowed() {
            Content movie = newContent(StructureType.SINGLE, "Film");

            assertThatThrownBy(() -> episodeService.saveEpisode(null, movie.getId(), null,
                    episodeRequest(null, 1, "Qism")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Bitta qismlik");

            SeasonSaveRequest season = new SeasonSaveRequest();
            season.setSeasonNumber(1);
            season.setTranslations(uz("Fasl"));
            assertThatThrownBy(() -> episodeService.saveSeason(null, movie.getId(), null, season))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ------------------------------------------------------- kirish siyosati

    @Nested
    @DisplayName("Qism darajasidagi kirish siyosati")
    class AccessOverride {

        @Test
        @DisplayName("Override berilmasa kontentdan meros olinadi")
        void inheritsFromContent() {
            Content series = newContent(StructureType.EPISODIC, "Meros sinovi");
            Episode e = episodeService.saveEpisode(null, series.getId(), null,
                    episodeRequest(null, 1, "1-qism"));

            assertThat(e.getAccessPolicyOverride()).isNull();
            assertThat(e.effectiveAccessPolicy()).isEqualTo(AccessPolicy.PREMIUM_OR_PURCHASE);
        }

        @Test
        @DisplayName("Override berilsa kontentnikini bekor qiladi — 1-qism bepul bo'lishi mumkin")
        void overrideWins() {
            Content series = newContent(StructureType.EPISODIC, "Override sinovi");
            EpisodeSaveRequest req = episodeRequest(null, 1, "Bepul 1-qism");
            req.setAccessPolicyOverride(AccessPolicy.FREE);

            Episode e = episodeService.saveEpisode(null, series.getId(), null, req);

            assertThat(e.effectiveAccessPolicy())
                    .as("Reklama uchun 1-qism bepul bo'lishi kerak")
                    .isEqualTo(AccessPolicy.FREE);
        }
    }
}
