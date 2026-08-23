package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentListDto;
import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.Category;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Entity.Genre;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.CreatorProfession;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.MediaRole;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.CategoryRepo;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Repository.GenreRepo;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.TaxonomyService;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §79 — kontent qabul mezonlari.
 *
 * <h2>Nega alohida fayl</h2>
 * §78 dagi kabi: har bir band boshqa testlarda qamrab olingan, lekin
 * <b>ro'yxat sifatida</b> yig'ilmagan. Buyurtmachi «shu besh tur va shu
 * to'qqiz maydon bilan saqlansin» deganda, javob bitta joydan
 * ko'rinishi kerak.
 *
 * <h2>Nega qayta o'qiladi</h2>
 * Saqlash muvaffaqiyatli qaytishi hech nimani isbotlamaydi: maydon
 * jimgina tashlab yuborilgan bo'lishi mumkin. Shuning uchun har bir
 * tekshiruv bazadan QAYTA O'QIYDI — §66 da aynan shunday nuqson
 * topilgan edi (janr va ijodkorlar saqlanmasdan yo'qolardi).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContentAcceptanceTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private ContentService contentService;
    @Autowired private TaxonomyService taxonomyService;
    @Autowired private ContentRepo contentRepo;
    @Autowired private CategoryRepo categoryRepo;
    @Autowired private GenreRepo genreRepo;
    @Autowired private MediaAssetRepo mediaAssetRepo;

    // ------------------------------------------------------ besh xil tur

    @Nested
    @DisplayName("Besh xil kontent turi yaratiladi")
    class ContentTypes {

        @ParameterizedTest(name = "{2}")
        @CsvSource({
                "SHORT_FILM,  SINGLE,   qisqa film",
                "MOVIE,       SINGLE,   film",
                "MINI_SERIES, EPISODIC, mini serial",
                "SERIES,      SEASONAL, faslli serial",
                "PODCAST,     EPISODIC, podkast",
        })
        @DisplayName("Tur saqlanadi")
        void everyTypeCanBeCreated(ContentType type, StructureType structure, String label) {
            ContentSaveRequest r = base(label);
            r.setContentType(type);
            r.setStructureType(structure);

            Content created = contentService.create(null, r);

            Content stored = contentRepo.findById(created.getId()).orElseThrow();
            assertThat(stored.getContentType()).isEqualTo(type);
            assertThat(stored.getStructureType()).isEqualTo(structure);
        }
    }

    // -------------------------------------------------- to'qqiz maydon

    @Nested
    @DisplayName("ТЗ sanagan barcha maydonlar saqlanadi")
    class AllFields {

        @Test
        @DisplayName("Rasm, galereya, treyler, video, kategoriya, janr, ijodkor, premyera sanasi, kirish siyosati")
        void everyListedFieldIsPersisted() {
            Category category = category();
            Genre genre = genre();
            Creator creator = creator();

            MediaAsset poster = media("afisha", MediaType.IMAGE);
            MediaAsset gallery1 = media("galereya-1", MediaType.IMAGE);
            MediaAsset gallery2 = media("galereya-2", MediaType.IMAGE);
            MediaAsset trailer = media("treyler", MediaType.VIDEO);
            MediaAsset video = media("asosiy-video", MediaType.VIDEO);
            LocalDateTime premiere = LocalDateTime.now().plusDays(30).withNano(0);

            ContentSaveRequest r = base("To'liq film");
            r.setContentType(ContentType.MOVIE);
            r.setStructureType(StructureType.SINGLE);
            r.setAccessPolicy(AccessPolicy.PREMIUM_OR_PURCHASE);
            // Pullik bitta qismlik kontentga narx SHART - loyihaning
            // o'z qoidasi. Narxsiz saqlansa, foydalanuvchi «sotib
            // olish» tugmasini bosib nol so'mga ega bo'lardi.
            r.setPremierePrice(new java.math.BigDecimal("29000.00"));
            r.setCategoryId(category.getId());
            r.setGenreIds(Set.of(genre.getId()));
            r.setPremiereDate(premiere);
            r.setMedia(List.of(
                    link(MediaRole.POSTER, poster.getId(), 0),
                    link(MediaRole.GALLERY, gallery1.getId(), 0),
                    link(MediaRole.GALLERY, gallery2.getId(), 1),
                    link(MediaRole.TRAILER, trailer.getId(), 0),
                    link(MediaRole.VIDEO, video.getId(), 0)));
            r.setCredits(List.of(credit(creator.getId())));

            Content created = contentService.create(null, r);

            // ⚠️ Bazadan qayta o'qiladi: saqlash muvaffaqiyatli qaytishi
            // maydon yozilganini ANGLATMAYDI.
            ContentListDto dto = ContentListDto.from(
                    contentRepo.findById(created.getId()).orElseThrow());

            assertThat(dto.getCategoryId()).as("kategoriya").isEqualTo(category.getId());
            assertThat(dto.getGenreIds()).as("janrlar").containsExactly(genre.getId());
            assertThat(dto.getCredits()).as("ijodkorlar").hasSize(1);
            assertThat(dto.getCredits().get(0).getCreatorId()).isEqualTo(creator.getId());
            assertThat(dto.getPremiereDate()).as("premyera sanasi").isEqualTo(premiere);
            assertThat(dto.getAccessPolicy()).as("kirish siyosati")
                    .isEqualTo(AccessPolicy.PREMIUM_OR_PURCHASE);
            assertThat(dto.getPremierePrice()).as("narx")
                    .isEqualByComparingTo("29000.00");
            assertThat(dto.getPosterMediaId()).as("afisha").isEqualTo(poster.getId());
            assertThat(dto.getGallery()).as("galereya")
                    .containsExactly(gallery1.getId(), gallery2.getId());

            // Treyler va video DTO'ning umumiy media ro'yxatida.
            assertThat(dto.getMedia()).as("treyler")
                    .anyMatch(m -> m.getRole() == MediaRole.TRAILER
                            && m.getMediaId().equals(trailer.getId()));
            assertThat(dto.getMedia()).as("asosiy video")
                    .anyMatch(m -> m.getRole() == MediaRole.VIDEO
                            && m.getMediaId().equals(video.getId()));
        }

        @Test
        @DisplayName("Uchala tilda sarlavha saqlanadi")
        void allThreeLanguagesArePersisted() {
            Content created = contentService.create(null, base("Uch tilli"));

            Content stored = contentRepo.findById(created.getId()).orElseThrow();
            assertThat(stored.getTranslations())
                    .as("mobil ilova uch tilda ishlaydi - uchalasi ham majburiy")
                    .extracting(t -> t.getLocale())
                    .containsExactlyInAnyOrder(Locale.UZ, Locale.RU, Locale.EN);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private ContentSaveRequest base(String title) {
        ContentSaveRequest r = new ContentSaveRequest();
        r.setContentType(ContentType.MOVIE);
        r.setStructureType(StructureType.SINGLE);
        r.setAccessPolicy(AccessPolicy.FREE);
        r.setStatus(PublicationStatus.DRAFT);
        r.setTranslations(Translations.all(title + " " + SEQ.incrementAndGet()));
        return r;
    }

    private ContentSaveRequest.MediaLink link(MediaRole role, Long mediaId, int order) {
        ContentSaveRequest.MediaLink l = new ContentSaveRequest.MediaLink();
        l.setRole(role);
        l.setMediaId(mediaId);
        l.setSortOrder(order);
        return l;
    }

    private ContentSaveRequest.CreditLink credit(Long creatorId) {
        ContentSaveRequest.CreditLink l = new ContentSaveRequest.CreditLink();
        l.setCreatorId(creatorId);
        l.setProfession(CreatorProfession.DIRECTOR);
        l.setSortOrder(0);
        return l;
    }

    private MediaAsset media(String name, MediaType type) {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/qabul-" + name + "-" + SEQ.incrementAndGet())
                .originalFilename(name)
                .type(type)
                .mimeType(type == MediaType.IMAGE ? "image/jpeg" : "video/mp4")
                .sizeBytes(100L)
                .status(MediaStatus.READY)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Category category() {
        return categoryRepo.save(Category.builder()
                .slug("qabul-kategoriya-" + SEQ.incrementAndGet())
                .active(true).sortOrder(0).build());
    }

    private Genre genre() {
        int n = SEQ.incrementAndGet();
        Genre g = new Genre();
        g.setSlug("qabul-janr-" + n);
        g.setActive(true);
        g.setSortOrder(n);
        return genreRepo.save(g);
    }

    private Creator creator() {
        var r = new com.example.backend.Admin.Dto.CreatorSaveRequest();
        r.setActive(true);
        int n = SEQ.incrementAndGet();
        var tr = new java.util.LinkedHashMap<Locale,
                com.example.backend.Admin.Dto.CreatorSaveRequest.NameDto>();
        for (Locale loc : Locale.values()) {
            var name = new com.example.backend.Admin.Dto.CreatorSaveRequest.NameDto();
            name.setDisplayName("Rejissyor " + loc + " " + n);
            tr.put(loc, name);
        }
        r.setTranslations(tr);
        return taxonomyService.saveCreator(null, null, r);
    }
}
