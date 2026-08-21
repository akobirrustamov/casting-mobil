package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.TaxonomySaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Category;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Genre;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.support.Translations;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.TaxonomyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kontent tasnifi — UCH MUSTAQIL O'LCHOV (ТЗ §13).
 *
 * <h2>Qoida</h2>
 * {@code contentType}, {@code category} va {@code genre} — bir xil narsa EMAS.
 * Buyurtmachining misoli aynan shunday:
 *
 * <pre>
 *   Content type : MINI_SERIES   ← kontentning SHAKLI (enum)
 *   Category     : Drama         ← katalog bo'limi (jadval, tarjimalar bilan)
 *   Genre        : Romance       ← janr (jadval, ko'p-ko'pga)
 * </pre>
 *
 * <h2>Nega bu test kerak</h2>
 * Bu qoida jimgina buzilishi mumkin. Eng ehtimolli yo'l — kimdir «bir xil
 * ro'yxatga o'xshaydi» deb kategoriyani enum'ga aylantirishi yoki
 * {@code contentType} ni kategoriyaga bog'lab qo'yishi. Natijada
 * «MINI_SERIES» va «Drama» bitta maydonga tushib qolardi va ulardan biri
 * yo'qolardi — masalan drama janridagi podkastni ifodalab bo'lmasdi.
 *
 * Uchtasi mustaqil bo'lgani uchun ular ERKIN kombinatsiyalanadi, va aynan
 * shu test buni tekshiradi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContentClassificationTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private ContentService contentService;
    @Autowired private TaxonomyService taxonomyService;
    @Autowired private ContentRepo contentRepo;

    private TaxonomySaveRequest named(String uz, String ru, String en) {
        TaxonomySaveRequest req = new TaxonomySaveRequest();
        req.setTranslations(Map.of(
                Locale.UZ, TranslationDto.ofTitle(uz + " " + SEQ.incrementAndGet()),
                Locale.RU, TranslationDto.ofTitle(ru),
                Locale.EN, TranslationDto.ofTitle(en)));
        return req;
    }

    private Content newContent(ContentType type, Long categoryId, Set<Long> genreIds) {
        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(type);
        req.setStructureType(type == ContentType.MOVIE || type == ContentType.SHORT_FILM
                ? StructureType.SINGLE : StructureType.EPISODIC);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(PublicationStatus.DRAFT);
        req.setCategoryId(categoryId);
        req.setGenreIds(genreIds);
        req.setTranslations(Translations.all("Tasnif sinovi " + SEQ.incrementAndGet()));
        return contentService.create(null, req);
    }

    // ------------------------------------------------------------ ТЗ misoli

    @Nested
    @DisplayName("ТЗ §13 misoli")
    class SpecExample {

        @Test
        @DisplayName("MINI_SERIES + Drama + Romance — uchtasi birga yashaydi")
        void typeCategoryAndGenreCoexist() {
            Category drama = taxonomyService.saveCategory(null, null,
                    named("Drama", "Драма", "Drama"));
            Genre romance = taxonomyService.saveGenre(null, null,
                    named("Romantika", "Романтика", "Romance"));

            Content content = newContent(ContentType.MINI_SERIES,
                    drama.getId(), Set.of(romance.getId()));

            Content saved = contentRepo.findById(content.getId()).orElseThrow();

            // Uchtasi ham o'z joyida va bir-birini almashtirmagan.
            assertThat(saved.getContentType()).isEqualTo(ContentType.MINI_SERIES);
            assertThat(saved.getCategory().getId()).isEqualTo(drama.getId());
            assertThat(saved.getGenres()).extracting(Genre::getId)
                    .containsExactly(romance.getId());
        }
    }

    // -------------------------------------------------------- mustaqillik

    @Nested
    @DisplayName("Uch o'lchov mustaqil")
    class Independence {

        @Test
        @DisplayName("Bitta kategoriya turli TURDAGI kontentlarda bo'ladi")
        void oneCategoryManyTypes() {
            Category drama = taxonomyService.saveCategory(null, null,
                    named("Drama", "Драма", "Drama"));

            // «Drama» kategoriyasidagi podkast ham, serial ham, film ham bo'lishi mumkin.
            Content podcast = newContent(ContentType.PODCAST, drama.getId(), Set.of());
            Content series = newContent(ContentType.SERIES, drama.getId(), Set.of());
            Content movie = newContent(ContentType.MOVIE, drama.getId(), Set.of());

            assertThat(podcast.getContentType()).isEqualTo(ContentType.PODCAST);
            assertThat(series.getContentType()).isEqualTo(ContentType.SERIES);
            assertThat(movie.getContentType()).isEqualTo(ContentType.MOVIE);

            // Kategoriya uchalasida ham BIR XIL - ya'ni tur uni belgilamaydi.
            assertThat(podcast.getCategory().getId()).isEqualTo(drama.getId());
            assertThat(series.getCategory().getId()).isEqualTo(drama.getId());
            assertThat(movie.getCategory().getId()).isEqualTo(drama.getId());
        }

        @Test
        @DisplayName("Kategoriyani o'zgartirish TURGA tegmaydi")
        void changingCategoryKeepsType() {
            Category drama = taxonomyService.saveCategory(null, null,
                    named("Drama", "Драма", "Drama"));
            Category comedy = taxonomyService.saveCategory(null, null,
                    named("Komediya", "Комедия", "Comedy"));

            Content content = newContent(ContentType.MINI_SERIES, drama.getId(), Set.of());

            ContentSaveRequest update = new ContentSaveRequest();
            update.setContentType(ContentType.MINI_SERIES);
            update.setStructureType(StructureType.EPISODIC);
            update.setAccessPolicy(AccessPolicy.FREE);
            update.setStatus(PublicationStatus.DRAFT);
            update.setCategoryId(comedy.getId());
            update.setTranslations(Translations.all("Kategoriya almashdi"));
            contentService.update(null, content.getId(), update);

            Content after = contentRepo.findById(content.getId()).orElseThrow();
            assertThat(after.getCategory().getId()).isEqualTo(comedy.getId());
            assertThat(after.getContentType())
                    .as("Kategoriya almashganda tur o'zgarmasligi kerak")
                    .isEqualTo(ContentType.MINI_SERIES);
        }

        @Test
        @DisplayName("Kategoriyasiz kontent bo'lishi mumkin, turi baribir bor")
        void categoryIsOptionalTypeIsNot() {
            Content content = newContent(ContentType.INTERVIEW, null, Set.of());

            assertThat(content.getCategory()).isNull();
            assertThat(content.getContentType()).isEqualTo(ContentType.INTERVIEW);
        }

        @Test
        @DisplayName("Bir kontentda bir NECHTA janr bo'ladi — kategoriya esa bitta")
        void manyGenresOneCategory() {
            Category drama = taxonomyService.saveCategory(null, null,
                    named("Drama", "Драма", "Drama"));
            Genre romance = taxonomyService.saveGenre(null, null,
                    named("Romantika", "Романтика", "Romance"));
            Genre thriller = taxonomyService.saveGenre(null, null,
                    named("Triller", "Триллер", "Thriller"));

            Content content = newContent(ContentType.SERIES, drama.getId(),
                    Set.of(romance.getId(), thriller.getId()));

            // Aynan shu sabab janr ALOHIDA jadval: kategoriya bitta, janr ko'p.
            assertThat(content.getGenres()).hasSize(2);
            assertThat(content.getCategory()).isNotNull();
        }
    }

    // -------------------------------------------------- namuna ma'lumot

    @Nested
    @DisplayName("Namuna ma'lumot to'g'ri naqsh o'rgatadi")
    class SeedData {

        private static final java.nio.file.Path SEEDER = java.nio.file.Paths.get(
                "src/main/java/com/example/backend/Cms/Dev/DevDataSeeder.java");

        /**
         * Namuna kategoriyalari kontent TURINI takrorlamasin.
         *
         * <h2>Nega bu topildi</h2>
         * Dev bazasida «Podkast», «Mini seriallar», «Intervyu» KATEGORIYA
         * sifatida turardi — holbuki ular {@code ContentType} qiymatlari.
         * Model to'g'ri edi, lekin namuna ma'lumot noto'g'ri naqsh
         * o'rgatardi: demo'ga qaragan odam kategoriyani tur bilan bir xil
         * deb o'ylardi va prodda ham shunday to'ldirardi.
         *
         * <h2>Nega bazadan emas, MANBADAN o'qiladi</h2>
         * Seeder faqat {@code dev} profilida ishlaydi. Test profilida
         * kategoriyalar jadvali BO'SH, ya'ni bazani tekshiradigan test
         * hech qachon hech narsa topmasdi va bekorga o'tardi — bu
         * tekshirilgan.
         *
         * ⚠️ Bu faqat NAMUNA ma'lumotni qo'riqlaydi. Buyurtmachi prodda
         * qanday kategoriya yaratishi — uning ishi, backend to'sqinlik
         * qilmaydi.
         */
        @Test
        @DisplayName("Seeder kategoriyalari ContentType nomini takrorlamaydi")
        void seededCategoriesAreNotContentTypes() throws java.io.IOException {
            assertThat(java.nio.file.Files.isRegularFile(SEEDER))
                    .as("Seeder ko'chirilgan yoki o'chirilgan: %s", SEEDER)
                    .isTrue();

            String source = java.nio.file.Files.readString(SEEDER);

            // seedCategories() ichidagi {"slug", "UZ", "RU", "EN"} qatorlari.
            int from = source.indexOf("private List<Category> seedCategories()");
            assertThat(from).as("seedCategories() metodi topilmadi").isGreaterThan(0);
            int to = source.indexOf("private List<Genre> seedGenres()", from);
            String block = to > from ? source.substring(from, to) : source.substring(from);

            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\{\\s*\"([a-z0-9-]+)\"")
                    .matcher(block);

            java.util.Set<String> typeNames = new java.util.HashSet<>();
            for (ContentType type : ContentType.values()) {
                // SHORT_FILM → short-film va shortfilm
                typeNames.add(type.name().toLowerCase().replace('_', '-'));
                typeNames.add(type.name().toLowerCase().replace("_", ""));
            }

            java.util.List<String> slugs = new java.util.ArrayList<>();
            java.util.List<String> conflicts = new java.util.ArrayList<>();
            while (m.find()) {
                String slug = m.group(1);
                slugs.add(slug);
                if (typeNames.contains(slug)) {
                    conflicts.add(slug);
                }
            }

            // Avval blok haqiqatan topilganiga ishonch hosil qilamiz -
            // aks holda bo'sh ro'yxat "muammo yo'q" deb ko'rinardi.
            assertThat(slugs)
                    .as("seedCategories() dan bitta ham slug o'qilmadi — "
                            + "format o'zgargan bo'lsa test bekorga o'tadi")
                    .isNotEmpty();

            assertThat(conflicts)
                    .as("Bu namuna kategoriyalari aslida kontent TURI: %s. "
                            + "Kategoriya — katalog bo'limi (kelib chiqishi, "
                            + "auditoriya, mavzu), tur emas (ТЗ §13).", conflicts)
                    .isEmpty();
        }
    }

    // ------------------------------------------------------- tur ro'yxati

    @Nested
    @DisplayName("Tur ro'yxati")
    class Types {

        @Test
        @DisplayName("ТЗ §13 dagi barcha turlar mavjud")
        void specTypesExist() {
            // Ro'yxat ТЗ dan so'zma-so'z. Biror qiymat olib tashlansa,
            // o'sha turdagi mavjud kontent yuklanmay qoladi.
            for (String required : new String[]{
                    "SHORT_FILM", "MOVIE", "MINI_SERIES", "SERIES",
                    "PODCAST", "SHOW", "INTERVIEW", "OTHER"}) {
                assertThat(ContentType.valueOf(required)).isNotNull();
            }
        }

        @Test
        @DisplayName("Doskadan qo'shilgan STREAM va CLIP ham bor")
        void boardTypesExist() {
            // Miro doskasidagi bosh sahifa bo'limlari (R2): Streamlar, Kliplar.
            assertThat(ContentType.valueOf("STREAM")).isNotNull();
            assertThat(ContentType.valueOf("CLIP")).isNotNull();
        }
    }
}
