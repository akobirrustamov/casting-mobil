package com.example.backend.Cms;

import com.example.backend.Admin.Dto.AdvertisementDto;
import com.example.backend.Admin.Dto.AdvertisementSaveRequest;
import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.HomepageSectionSaveRequest;
import com.example.backend.Admin.Dto.PremiereDto;
import com.example.backend.Admin.Dto.PremiereSaveRequest;
import com.example.backend.Cms.Dto.HomeFeedDto;
import com.example.backend.Cms.Entity.Category;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.HomepageSection;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Repository.HomepageSectionRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.HomeFeedService;
import com.example.backend.Cms.Service.HomepageService;
import com.example.backend.support.CapturingStatementInspector;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §31 — bosh sahifa backenddan olinadi.
 *
 * <h2>Nima uchun bu endpoint kerak</h2>
 * «Homepage hardcoded bo'lmasin» — ya'ni qaysi bo'limlar bor, ular qanday
 * tartibda va nima deb ataladi, hammasi serverda. Aks holda bo'lim
 * qo'shish uchun ilovaning yangi versiyasini do'konga chiqarish kerak
 * bo'lardi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HomeFeedTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private HomeFeedService homeFeedService;
    @Autowired private HomepageService homepageService;
    @Autowired private HomepageSectionRepo sectionRepo;
    @Autowired private ContentService contentService;
    @Autowired private com.example.backend.Cms.Service.TaxonomyService taxonomyService;
    @Autowired private com.example.backend.Cms.Repository.CategoryRepo categoryRepo;
    @Autowired private com.example.backend.Cms.Repository.ContentRepo contentRepo;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

    @BeforeEach
    void ensureSections() {
        // Bo'limlar birinchi murojaatda avtomatik yaratiladi.
        homepageService.sections();
    }

    private HomeFeedDto feed() {
        return homeFeedService.build(null, Locale.UZ);
    }

    private HomepageSection section(HomepageSectionType type) {
        return homepageService.sections().stream()
                .filter(s -> s.getType() == type)
                .findFirst().orElseThrow();
    }

    private HomeFeedDto.Section inFeed(HomeFeedDto feed, HomepageSectionType type) {
        return feed.getSections().stream()
                .filter(s -> s.getType() == type)
                .findFirst().orElse(null);
    }

    // -------------------------------------------------------- bo'lim tartibi

    @Nested
    @DisplayName("Bo'limlar")
    class Sections {

        @Test
        @DisplayName("Tartib serverdan keladi — klientda qotirilmaydi")
        void sectionsAreOrderedByServer() {
            // Bir nechta xil bo'lim to'lsin — tartibni tekshirish uchun
            // kamida ikkitasi kerak.
            newPremiere();
            content(ContentType.MINI_SERIES, PublicationStatus.PUBLISHED,
                    ContentVisibility.PUBLIC);

            HomeFeedDto feed = feed();
            assertThat(feed.getSections()).hasSizeGreaterThanOrEqualTo(2);

            assertThat(feed.getSections()).isNotEmpty();
            assertThat(feed.getSections())
                    .isSortedAccordingTo((a, b) ->
                            Integer.compare(a.getSortOrder(), b.getSortOrder()));
        }

        @Test
        @DisplayName("O'chirilgan bo'lim javobga tushmaydi")
        void disabledSectionIsHidden() {
            newPremiere();
            assertThat(inFeed(feed(), HomepageSectionType.NEW_PREMIERES)).isNotNull();

            HomepageSection s = section(HomepageSectionType.NEW_PREMIERES);
            HomepageSectionSaveRequest req = new HomepageSectionSaveRequest();
            req.setEnabled(false);
            req.setSortOrder(s.getSortOrder());
            req.setTranslations(new LinkedHashMap<>());
            homepageService.saveSection(null, s.getId(), req);

            assertThat(inFeed(feed(), HomepageSectionType.NEW_PREMIERES)).isNull();
        }

        @Test
        @DisplayName("Bo'sh bo'lim ko'rsatilmaydi — soxta element o'ylab topilmaydi")
        void emptySectionIsOmitted() {
            // Hech qanday mashhur kontent yo'q. ТЗ qoidasi: ma'lumot
            // bo'lmasa bo'sh holat, o'ylab topilgan qator emas.
            assertThat(inFeed(feed(), HomepageSectionType.POPULAR_CONTENT)).isNull();
        }

        @Test
        @DisplayName("Sarlavha so'ralgan tilda keladi")
        void titleIsLocalised() {
            newPremiere();

            assertThat(inFeed(homeFeedService.build(null, Locale.UZ),
                    HomepageSectionType.NEW_PREMIERES).getTitle())
                    .isEqualTo("Yangi premyeralar");
            assertThat(inFeed(homeFeedService.build(null, Locale.RU),
                    HomepageSectionType.NEW_PREMIERES).getTitle())
                    .isEqualTo("Новые премьеры");
            assertThat(inFeed(homeFeedService.build(null, Locale.EN),
                    HomepageSectionType.NEW_PREMIERES).getTitle())
                    .isEqualTo("New premieres");
        }

        @Test
        @DisplayName("ТЗ §31 dagi «Mini seriallar» bo'limi bor")
        void miniSeriesSectionExists() {
            assertThat(homepageService.sections())
                    .extracting(HomepageSection::getType)
                    .contains(HomepageSectionType.MINI_SERIES);
        }
    }

    // ------------------------------------------------------------- reklama

    @Nested
    @DisplayName("Reklama karuseli")
    class Ads {

        @Test
        @DisplayName("Faol banner mehmonga ko'rinadi")
        void liveAdIsShownToGuest() {
            newAd(AdAudience.ADVERTISEMENT);

            HomeFeedDto.Section s = inFeed(feed(), HomepageSectionType.ADVERTISEMENT_CAROUSEL);

            assertThat(s).isNotNull();
            assertThat(s.getBanners()).hasSize(1);
            assertThat(feed().getShowAds()).isTrue();
        }

        @Test
        @DisplayName("Muddati o'tgan banner chiqmaydi")
        void expiredAdIsHidden() {
            AdvertisementSaveRequest r = adRequest(AdAudience.ADVERTISEMENT);
            r.setStartAt(LocalDateTime.now().minusDays(10));
            r.setEndAt(LocalDateTime.now().minusDays(1));
            homepageService.saveAdvertisement(null, null, r);

            assertThat(inFeed(feed(), HomepageSectionType.ADVERTISEMENT_CAROUSEL)).isNull();
        }

        @Test
        @DisplayName("Qoralama banner chiqmaydi")
        void draftAdIsHidden() {
            AdvertisementSaveRequest r = adRequest(AdAudience.ADVERTISEMENT);
            r.setStatus(PublicationStatus.DRAFT);
            homepageService.saveAdvertisement(null, null, r);

            assertThat(inFeed(feed(), HomepageSectionType.ADVERTISEMENT_CAROUSEL)).isNull();
        }
    }

    // ------------------------------------------------------------- kontent

    @Nested
    @DisplayName("Kontent qatorlari")
    class ContentRows {

        @Test
        @DisplayName("Nashr qilinmagan kontent qatorga tushmaydi")
        void draftContentIsHidden() {
            Content draft = content(ContentType.MINI_SERIES, PublicationStatus.DRAFT,
                    ContentVisibility.PUBLIC);

            assertThat(draft.getStatus()).isEqualTo(PublicationStatus.DRAFT);
            assertThat(inFeed(feed(), HomepageSectionType.MINI_SERIES)).isNull();
        }

        @Test
        @DisplayName("Nashr qilingan mini serial qatorda ko'rinadi")
        void publishedMiniSeriesIsShown() {
            content(ContentType.MINI_SERIES, PublicationStatus.PUBLISHED,
                    ContentVisibility.PUBLIC);

            HomeFeedDto.Section s = inFeed(feed(), HomepageSectionType.MINI_SERIES);

            assertThat(s).isNotNull();
            assertThat(s.getContent()).hasSize(1);
            assertThat(s.getContent().get(0).getTitle()).isNotBlank();
        }

        @Test
        @DisplayName("UNLISTED va PRIVATE katalogda chiqmaydi")
        void hiddenVisibilityIsNotListed() {
            content(ContentType.MINI_SERIES, PublicationStatus.PUBLISHED,
                    ContentVisibility.UNLISTED);
            content(ContentType.MINI_SERIES, PublicationStatus.PUBLISHED,
                    ContentVisibility.PRIVATE);

            // UNLISTED faqat havola orqali ochiladi, PRIVATE esa xodimlarga.
            assertThat(inFeed(feed(), HomepageSectionType.MINI_SERIES)).isNull();
        }

        @Test
        @DisplayName("Bo'lim chegarasi hurmat qilinadi")
        void itemLimitIsHonoured() {
            for (int i = 0; i < 4; i++) {
                content(ContentType.MINI_SERIES, PublicationStatus.PUBLISHED,
                        ContentVisibility.PUBLIC);
            }
            HomepageSection s = section(HomepageSectionType.MINI_SERIES);
            HomepageSectionSaveRequest req = new HomepageSectionSaveRequest();
            req.setEnabled(true);
            req.setSortOrder(s.getSortOrder());
            req.setItemLimit(2);
            req.setTranslations(new LinkedHashMap<>());
            homepageService.saveSection(null, s.getId(), req);

            assertThat(inFeed(feed(), HomepageSectionType.MINI_SERIES).getContent()).hasSize(2);
        }
    }

    // -------------------------------------------------------- maxsus qator

    @Nested
    @DisplayName("Maxsus qator (Custom content rows)")
    class CustomRow {

        @Test
        @DisplayName("Admin tanlagan tartibda chiqadi")
        void curatedOrderIsPreserved() {
            Content first = content(ContentType.MOVIE, PublicationStatus.PUBLISHED,
                    ContentVisibility.PUBLIC);
            Content second = content(ContentType.MOVIE, PublicationStatus.PUBLISHED,
                    ContentVisibility.PUBLIC);

            HomepageSection row = section(HomepageSectionType.CUSTOM_ROW);
            homepageService.replaceSectionItems(null, row.getId(),
                    List.of(second.getId(), first.getId()));

            List<HomeFeedDto.ContentCard> cards =
                    inFeed(feed(), HomepageSectionType.CUSTOM_ROW).getContent();

            // Ro'yxat tartibi = ko'rinish tartibi.
            assertThat(cards).extracting(HomeFeedDto.ContentCard::getId)
                    .containsExactly(second.getId(), first.getId());
        }

        @Test
        @DisplayName("Ro'yxatdan olib tashlangan kontent qatordan chiqadi")
        void removedItemDisappears() {
            Content c = content(ContentType.MOVIE, PublicationStatus.PUBLISHED,
                    ContentVisibility.PUBLIC);
            HomepageSection row = section(HomepageSectionType.CUSTOM_ROW);

            homepageService.replaceSectionItems(null, row.getId(), List.of(c.getId()));
            assertThat(inFeed(feed(), HomepageSectionType.CUSTOM_ROW)).isNotNull();

            homepageService.replaceSectionItems(null, row.getId(), List.of());
            assertThat(inFeed(feed(), HomepageSectionType.CUSTOM_ROW)).isNull();
        }

        @Test
        @DisplayName("Qoralama kontent qatorga qo'shilsa ham ko'rinmaydi")
        void draftCuratedItemIsStillHidden() {
            Content draft = content(ContentType.MOVIE, PublicationStatus.DRAFT,
                    ContentVisibility.PUBLIC);
            HomepageSection row = section(HomepageSectionType.CUSTOM_ROW);

            // Admin qo'lda qo'shishi mumkin — bu tayyorgarlik. Lekin
            // nashr qilinmaguncha foydalanuvchi uni ko'rmasligi kerak.
            homepageService.replaceSectionItems(null, row.getId(), List.of(draft.getId()));

            assertThat(inFeed(feed(), HomepageSectionType.CUSTOM_ROW)).isNull();
        }
    }

    // ------------------------------------------------------- qator tartibi

    @Nested
    @DisplayName("Qatorlar tartibi boshqariladi (ТЗ §31)")
    class RowOrdering {

        @Test
        @DisplayName("Admin tanlagan tartib avtomatik qoidadan ustun")
        void manualOrderOverridesAutomatic() {
            Content eski = content(ContentType.MINI_SERIES, PublicationStatus.PUBLISHED,
                    ContentVisibility.PUBLIC);
            Content yangi = content(ContentType.MINI_SERIES, PublicationStatus.PUBLISHED,
                    ContentVisibility.PUBLIC);

            // ⚠️ Avtomatik qoida - publicationDate DESC, ya'ni [yangi, eski].
            // Qo'lda esa TESKARI tartib beriladi. Aks holda ikkala tartib
            // bir xil natija berardi va test qo'lda tartiblash ishlayotganini
            // umuman tekshirmasdi.
            eski.setPublicationDate(LocalDateTime.now().minusDays(10));
            yangi.setPublicationDate(LocalDateTime.now());
            contentRepo.save(eski);
            contentRepo.save(yangi);

            HomepageSection row = section(HomepageSectionType.MINI_SERIES);
            homepageService.replaceSectionItems(null, row.getId(),
                    List.of(eski.getId(), yangi.getId()));

            List<HomeFeedDto.ContentCard> cards =
                    inFeed(feed(), HomepageSectionType.MINI_SERIES).getContent();

            // Ilgari qo'lda tartiblash faqat CUSTOM_ROW da bor edi, qolgan
            // qatorlar qat'iy publicationDate desc bilan chiqardi — admin
            // «Mini seriallar» da qaysi film birinchi turishini hal qila
            // olmasdi.
            assertThat(cards).extracting(HomeFeedDto.ContentCard::getId)
                    .containsExactly(eski.getId(), yangi.getId());
        }

        @Test
        @DisplayName("Ro'yxat bo'sh bo'lsa avtomatik qoida ishlaydi")
        void emptyListFallsBackToAutomatic() {
            content(ContentType.MINI_SERIES, PublicationStatus.PUBLISHED,
                    ContentVisibility.PUBLIC);

            // Admin har bir qatorni qo'lda to'ldirishga majbur emas —
            // aks holda yangi kontent bosh sahifaga umuman tushmasdi.
            assertThat(inFeed(feed(), HomepageSectionType.MINI_SERIES).getContent())
                    .hasSize(1);
        }

        @Test
        @DisplayName("Bo'limlar tartibi bitta so'rovda o'rnatiladi")
        void sectionsAreReorderedAtomically() {
            newPremiere();
            content(ContentType.MINI_SERIES, PublicationStatus.PUBLISHED,
                    ContentVisibility.PUBLIC);

            Long premieres = section(HomepageSectionType.NEW_PREMIERES).getId();
            Long mini = section(HomepageSectionType.MINI_SERIES).getId();

            homepageService.reorderSections(null, List.of(mini, premieres));

            List<HomepageSectionType> order = feed().getSections().stream()
                    .map(HomeFeedDto.Section::getType).toList();

            assertThat(order.indexOf(HomepageSectionType.MINI_SERIES))
                    .isLessThan(order.indexOf(HomepageSectionType.NEW_PREMIERES));
        }

        @Test
        @DisplayName("Ro'yxatga kirmagan bo'lim yo'qolmaydi va ZIDDIYAT yaratmaydi")
        void sectionsOutsideTheListSurvive() {
            newPremiere();
            content(ContentType.MINI_SERIES, PublicationStatus.PUBLISHED,
                    ContentVisibility.PUBLIC);

            Long premyeraId = section(HomepageSectionType.NEW_PREMIERES).getId();
            Long miniId = section(HomepageSectionType.MINI_SERIES).getId();

            // Avval premyera birinchi o'ringa (sortOrder = 0) qo'yiladi.
            homepageService.reorderSections(null, List.of(premyeraId));

            // Endi FAQAT mini seriallar yuboriladi — panel ko'rinib turgan
            // bo'limlarnigina yuborgan holat.
            homepageService.reorderSections(null, List.of(miniId));

            List<HomeFeedDto.Section> sections = feed().getSections();

            // Bo'lim yo'qolmasligi kerak.
            assertThat(sections).extracting(HomeFeedDto.Section::getType)
                    .contains(HomepageSectionType.NEW_PREMIERES);

            // ⚠️ ASOSIY TEKSHIRUV: mini ham, premyera ham 0 raqamiga
            // da'vogar. Ro'yxatdan tashqaridagilar surilmasa, ikkalasi
            // bir xil raqamda qolib, tartib ID bo'yicha tasodifiy hal
            // bo'lardi — ya'ni admin sudragan tartib buzilardi.
            HomeFeedDto.Section mini = sections.stream()
                    .filter(x -> x.getType() == HomepageSectionType.MINI_SERIES)
                    .findFirst().orElseThrow();
            HomeFeedDto.Section premyera = sections.stream()
                    .filter(x -> x.getType() == HomepageSectionType.NEW_PREMIERES)
                    .findFirst().orElseThrow();

            assertThat(mini.getSortOrder())
                    .as("Ro'yxatdagi bo'lim tashqaridagidan QAT'IY oldin turishi kerak")
                    .isLessThan(premyera.getSortOrder());
        }

        @Test
        @DisplayName("Takror ID rad etiladi")
        void duplicateIdIsRejected() {
            Long mini = section(HomepageSectionType.MINI_SERIES).getId();

            // Bitta bo'lim ikkita raqamga da'vogar bo'lardi.
            assertThatThrownBy(() ->
                    homepageService.reorderSections(null, List.of(mini, mini)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("takrorlangan");
        }

        @Test
        @DisplayName("Kategoriya qatori tartibi boshqariladi")
        void categoryRowOrderIsManaged() {
            // ⚠️ `if (...)` bilan tekshirish YARAMAYDI: test profilida
            // kategoriya yo'q, shart bajarilmay test bo'sh o'tib ketardi.
            // Shuning uchun kategoriyalar shu yerda yaratiladi.
            Category ikkinchi = category("Ikkinchi", 20);
            Category birinchi = category("Birinchi", 10);

            HomeFeedDto.Section categories = inFeed(feed(), HomepageSectionType.CATEGORIES);

            assertThat(categories).isNotNull();
            // Tartib Category.sortOrder bilan boshqariladi (ТЗ §31).
            assertThat(categories.getCategories())
                    .extracting(HomeFeedDto.CategoryCard::getId)
                    .containsExactly(birinchi.getId(), ikkinchi.getId());
        }

        @Test
        @DisplayName("Nofaol kategoriya qatorda ko'rinmaydi")
        void inactiveCategoryIsHidden() {
            Category faol = category("Faol", 10);
            Category nofaol = category("Nofaol", 20);
            nofaol.setActive(false);
            categoryRepo.save(nofaol);

            assertThat(inFeed(feed(), HomepageSectionType.CATEGORIES).getCategories())
                    .extracting(HomeFeedDto.CategoryCard::getId)
                    .containsExactly(faol.getId());
        }
    }

    // ------------------------------------------------------------ N+1 guard

    @Nested
    @DisplayName("So'rovlar soni")
    class QueryCount {

        @Test
        @DisplayName("Kontent soni ortganda so'rovlar soni mutanosib o'smaydi")
        void doesNotScaleWithItemCount() {
            for (int i = 0; i < 12; i++) {
                content(ContentType.MINI_SERIES, PublicationStatus.PUBLISHED,
                        ContentVisibility.PUBLIC);
            }

            // ⚠️ MAJBURIY: kontekstni tozalash.
            //
            // Testda yaratilgan obyektlar o'sha tranzaksiyada kontekstda
            // turadi va ularning to'plamlari uchun SQL umuman yubormaydi.
            // Tozalamasdan o'lchansa, so'rovlar soni doim 0 chiqadi va
            // test HAR QANDAY holatda o'tardi - ya'ni hech narsani
            // tekshirmasdi.
            em.flush();
            em.clear();

            CapturingStatementInspector.clear();
            HomeFeedDto built = homeFeedService.build(null, Locale.UZ);
            int selects = CapturingStatementInspector.selectsFrom("cms_content_translation").size();

            assertThat(inFeed(built, HomepageSectionType.MINI_SERIES).getContent()).hasSize(12);

            // @BatchSize(50) bo'lgani uchun 12 ta film tarjimasi BITTA
            // so'rovda kelishi kerak. Batch bo'lmasa har bir film uchun
            // alohida so'rov ketardi (N+1) va bosh sahifa yuzlab so'rovga
            // aylanardi.
            // Kamida bitta so'rov BO'LISHI kerak - aks holda o'lchov
            // ishlamayapti va test bo'sh joyni tekshirayotgan bo'lardi.
            assertThat(selects)
                    .as("Tarjimalar bazadan o'qilishi kerak edi")
                    .isGreaterThanOrEqualTo(1);
            assertThat(selects)
                    .as("12 ta kontent tarjimasi uchun yuborilgan so'rovlar: " + selects)
                    .isLessThanOrEqualTo(3);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private void newPremiere() {
        PremiereSaveRequest r = new PremiereSaveRequest();
        r.setName("Premyera " + SEQ.incrementAndGet());
        r.setStatus(PublicationStatus.PUBLISHED);
        Map<Locale, PremiereDto.PremiereTextDto> tr = new LinkedHashMap<>();
        for (Locale l : List.of(Locale.UZ, Locale.RU, Locale.EN)) {
            tr.put(l, PremiereDto.PremiereTextDto.builder()
                    .title("Premyera " + l.name()).build());
        }
        r.setTranslations(tr);
        r.setButtonEnabled(false);
        homepageService.savePremiere(null, null, r);
    }

    private AdvertisementSaveRequest adRequest(AdAudience audience) {
        AdvertisementSaveRequest r = new AdvertisementSaveRequest();
        r.setName("Banner " + SEQ.incrementAndGet());
        r.setAudience(audience);
        r.setStatus(PublicationStatus.PUBLISHED);
        r.setButtonEnabled(false);
        Map<Locale, AdvertisementDto.AdTextDto> tr = new LinkedHashMap<>();
        r.setTranslations(tr);
        return r;
    }

    private void newAd(AdAudience audience) {
        homepageService.saveAdvertisement(null, null, adRequest(audience));
    }

    /** Faol kategoriya — berilgan tartib raqami bilan. */
    private Category category(String name, int sortOrder) {
        com.example.backend.Admin.Dto.TaxonomySaveRequest req =
                new com.example.backend.Admin.Dto.TaxonomySaveRequest();
        req.setSortOrder(sortOrder);
        req.setActive(true);
        req.setTranslations(Translations.all(name + " " + SEQ.incrementAndGet()));
        return taxonomyService.saveCategory(null, null, req);
    }

    private Content content(ContentType type, PublicationStatus status,
                            ContentVisibility visibility) {
        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(type);
        req.setStructureType(type == ContentType.MOVIE
                ? StructureType.SINGLE : StructureType.EPISODIC);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(status);
        req.setVisibility(visibility);
        req.setTranslations(Translations.all("Bosh sahifa kontenti " + SEQ.incrementAndGet()));
        return contentService.create(null, req);
    }
}
