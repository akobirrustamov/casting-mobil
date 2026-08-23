package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Admin.Dto.TaxonomySaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.Cms.Service.TaxonomyService;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * «Hamma ma'lumot 3 ta tilda» qoidasi.
 *
 * <h2>Buyurtmachi talabi</h2>
 * Foydalanuvchi ko'radigan har bir sarlavha UZ, RU va EN da bo'lishi shart.
 *
 * <h2>Chegara qayerda</h2>
 * Tekshiruv har saqlashda emas, <b>nashr qilishda</b> ishlaydi:
 *
 * <ul>
 *   <li>{@code DRAFT} · {@code IN_REVIEW} — o'zbekchasi yetarli;</li>
 *   <li>{@code PUBLISHED} · {@code SCHEDULED} — uchala til majburiy;</li>
 *   <li>faol kategoriya/janr — uchala til majburiy (ular mobil ilova bosh
 *       menyusida chiqadi, ТЗ §16).</li>
 * </ul>
 *
 * Aks holda admin qoralamani ham saqlay olmasdi: kontent odatda bitta
 * tilda yoziladi, keyin tarjima qilinadi. Har saqlashda uchala tilni
 * talab qilish odamlarni bo'sh joyga nuqta yozishga majbur qilardi —
 * ya'ni qoida amalda buzilardi.
 *
 * Natija: bazada tarjimasiz NASHR QILINGAN kontent bo'lmaydi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ThreeLanguageRuleTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private ContentService contentService;
    @Autowired private EpisodeService episodeService;
    @Autowired private TaxonomyService taxonomyService;

    private ContentSaveRequest content(PublicationStatus status,
                                       Map<Locale, TranslationDto> translations) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.SERIES);
        c.setStructureType(StructureType.EPISODIC);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(status);
        c.setTranslations(translations);
        return c;
    }

    // ----------------------------------------------------------- kontent

    @Nested
    @DisplayName("Kontent")
    class ContentRule {

        @Test
        @DisplayName("Qoralama — o'zbekchasi yetarli")
        void draftAcceptsBaseLanguageOnly() {
            assertThatCode(() -> contentService.create(null,
                    content(PublicationStatus.DRAFT,
                            Translations.uzOnly("Qoralama " + SEQ.incrementAndGet()))))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("NASHR — uchala til majburiy")
        void publishRequiresAllThree() {
            assertThatThrownBy(() -> contentService.create(null,
                    content(PublicationStatus.PUBLISHED,
                            Translations.uzOnly("Chala " + SEQ.incrementAndGet()))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RU")
                    .hasMessageContaining("EN");
        }

        @Test
        @DisplayName("Xato xabarida AYNAN qaysi til yetishmayotgani aytiladi")
        void errorNamesTheMissingLanguage() {
            // Faqat EN yetishmaydi.
            Map<Locale, TranslationDto> partial = Map.of(
                    Locale.UZ, TranslationDto.ofTitle("Sarlavha"),
                    Locale.RU, TranslationDto.ofTitle("Заголовок"));

            assertThatThrownBy(() -> contentService.create(null,
                    content(PublicationStatus.PUBLISHED, partial)))
                    .hasMessageContaining("EN")
                    // Bor bo'lganlari sanalmasin - admin chalkashmasin.
                    .hasMessageNotContaining("RU,");
        }

        @Test
        @DisplayName("SCHEDULED ham majburiy — u avtomatik nashr bo'ladi")
        void scheduledIsTreatedAsPublished() {
            assertThatThrownBy(() -> contentService.create(null,
                    content(PublicationStatus.SCHEDULED,
                            Translations.uzOnly("Rejalashtirilgan"))))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Uchala til bo'lsa nashr o'tadi")
        void completeTranslationsPublishFine() {
            Content c = contentService.create(null,
                    content(PublicationStatus.PUBLISHED,
                            Translations.all("To'liq " + SEQ.incrementAndGet())));
            assertThat(c.getTranslations()).hasSize(3);
        }

        @Test
        @DisplayName("Qoralamani nashrga o'tkazishda tekshiriladi")
        void promotingDraftToPublishedIsChecked() {
            Content draft = contentService.create(null,
                    content(PublicationStatus.DRAFT,
                            Translations.uzOnly("Keyin nashr " + SEQ.incrementAndGet())));

            // Aynan shu joy muhim: kontent qoralama sifatida chala saqlangan,
            // endi nashr qilinmoqchi - shu paytda to'siq turishi kerak.
            assertThatThrownBy(() -> contentService.update(null, draft.getId(),
                    content(PublicationStatus.PUBLISHED,
                            Translations.uzOnly("Keyin nashr"))))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ------------------------------------------------------- kategoriya

    @Nested
    @DisplayName("Kategoriya va janr (ТЗ §16 — mobil bosh menyu)")
    class TaxonomyRule {

        private TaxonomySaveRequest taxonomy(Boolean active,
                                             Map<Locale, TranslationDto> translations) {
            TaxonomySaveRequest t = new TaxonomySaveRequest();
            t.setActive(active);
            t.setTranslations(translations);
            return t;
        }

        @Test
        @DisplayName("Faol kategoriya — uchala til majburiy")
        void activeCategoryNeedsAllThree() {
            assertThatThrownBy(() -> taxonomyService.saveCategory(null, null,
                    taxonomy(true, Translations.uzOnly("Chala kategoriya"))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RU");
        }

        @Test
        @DisplayName("Faolsizlantirilgan — o'zbekchasi yetarli")
        void inactiveCategoryAcceptsBaseOnly() {
            assertThatCode(() -> taxonomyService.saveCategory(null, null,
                    taxonomy(false, Translations.uzOnly("Tayyorlanmoqda "
                            + SEQ.incrementAndGet()))))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Faol janr ham uchala tilni talab qiladi")
        void activeGenreNeedsAllThree() {
            assertThatThrownBy(() -> taxonomyService.saveGenre(null, null,
                    taxonomy(true, Translations.uzOnly("Chala janr"))))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ------------------------------------------------------------- qism

    @Nested
    @DisplayName("Fasl va qism")
    class EpisodeRule {

        @Test
        @DisplayName("Nashr qilingan qism — uchala til majburiy")
        void publishedEpisodeNeedsAllThree() {
            Content series = contentService.create(null,
                    content(PublicationStatus.DRAFT,
                            Translations.all("Serial " + SEQ.incrementAndGet())));

            EpisodeSaveRequest e = new EpisodeSaveRequest();
            e.setEpisodeNumber(1);
            e.setStatus(PublicationStatus.PUBLISHED);
            e.setTranslations(Translations.uzOnly("Chala qism"));

            assertThatThrownBy(() ->
                    episodeService.saveEpisode(null, series.getId(), null, e))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RU");
        }

        @Test
        @DisplayName("Qoralama qism — o'zbekchasi yetarli")
        void draftEpisodeAcceptsBaseOnly() {
            Content series = contentService.create(null,
                    content(PublicationStatus.DRAFT,
                            Translations.all("Serial " + SEQ.incrementAndGet())));

            EpisodeSaveRequest e = new EpisodeSaveRequest();
            e.setEpisodeNumber(1);
            e.setStatus(PublicationStatus.DRAFT);
            e.setTranslations(Translations.uzOnly("Qoralama qism"));

            assertThatCode(() ->
                    episodeService.saveEpisode(null, series.getId(), null, e))
                    .doesNotThrowAnyException();
        }
    }
}
