package com.example.backend.Cms;

import com.example.backend.Cms.Bootstrap.TaxonomyBootstrap;
import com.example.backend.Cms.Bootstrap.TaxonomyCatalog;
import com.example.backend.Cms.Entity.Genre;
import com.example.backend.Cms.Entity.PlatformSetting;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.CategoryRepo;
import com.example.backend.Cms.Repository.GenreRepo;
import com.example.backend.Cms.Repository.PlatformSettingRepo;
import com.example.backend.Cms.Service.SettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kategoriya/janr katalogi ishga tushishda bazaga tushadimi.
 *
 * <h2>⚠️ Nega runner QO'LDA yasaladi, `@Autowired` emas</h2>
 * Test profilida {@code app.taxonomy.bootstrap=false} — bean umuman
 * yaratilmaydi, ya'ni uni autowire qilib bo'lmaydi.
 *
 * Sozlamani shu test uchun {@code true} qilish ham YARAMAYDI: bu
 * ALOHIDA Spring konteksti demak, uning runner'i esa ishga tushishda
 * ayni o'sha H2 bazasiga yozardi — testlar orasida umumiy. Natijada
 * bu test boshqa testlarni yiqitardi va sabab qaysi test avval
 * yurganiga bog'liq bo'lardi.
 *
 * Qo'lda yasalgan runner esa faqat shu testning tranzaksiyasida
 * ishlaydi va u tugagach hamma narsa qaytariladi.
 */
@SpringBootTest
@ActiveProfiles("test")
class TaxonomyCatalogTest {

    @Autowired private GenreRepo genreRepo;
    @Autowired private CategoryRepo categoryRepo;
    @Autowired private PlatformSettingRepo settingRepo;
    @Autowired private SettingsService settingsService;

    /** Konteksdagi bean emas — sinaladigan obyekt shu yerda yasaladi. */
    private TaxonomyBootstrap bootstrap() {
        return new TaxonomyBootstrap(categoryRepo, genreRepo, settingRepo);
    }

    @Nested
    @DisplayName("Katalog ro'yxati")
    class CatalogData {

        @Test
        @DisplayName("Slug'lar takrorlanmaydi")
        void slugsAreUnique() {
            assertUniqueSlugs(TaxonomyCatalog.CATEGORIES, "kategoriya");
            assertUniqueSlugs(TaxonomyCatalog.GENRES, "janr");
        }

        /**
         * ⚠️ Uch til SHART (ТЗ §14). Bitta tarjima tushib qolsa, o'sha
         * tilda ilova janr nomi o'rniga bo'sh joy ko'rsatardi — va buni
         * faqat rus yoki ingliz tilida ochgan odam sezardi.
         */
        @Test
        @DisplayName("Har bir satr uch tilda va bo'sh emas")
        void everyRowHasThreeLanguages() {
            for (String[][] table : java.util.List.of(TaxonomyCatalog.CATEGORIES, TaxonomyCatalog.GENRES)) {
                for (String[] row : table) {
                    assertThat(row)
                            .as("satr: %s", Arrays.toString(row))
                            .hasSize(4);
                    for (String cell : row) {
                        assertThat(cell)
                                .as("satr: %s", Arrays.toString(row))
                                .isNotBlank();
                    }
                }
            }
        }

        /** Slug URL'ga tushadi va tarjima qilinmaydi — faqat kichik lotin. */
        @Test
        @DisplayName("Slug formati: kichik harf, raqam va defis")
        void slugFormat() {
            for (String[][] table : java.util.List.of(TaxonomyCatalog.CATEGORIES, TaxonomyCatalog.GENRES)) {
                for (String[] row : table) {
                    assertThat(row[0]).matches("[a-z0-9]+(-[a-z0-9]+)*");
                }
            }
        }

        private void assertUniqueSlugs(String[][] table, String what) {
            Set<String> seen = new HashSet<>();
            for (String[] row : table) {
                assertThat(seen.add(row[0]))
                        .as("%s slug'i takrorlangan: %s", what, row[0])
                        .isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Ishga tushirish")
    class Bootstrapping {

        @Test
        @Transactional
        @DisplayName("Katalogdagi hamma janr va kategoriya bazada")
        void catalogIsInDatabase() {
            bootstrap().run();

            for (String[] row : TaxonomyCatalog.GENRES) {
                assertThat(genreRepo.existsBySlug(row[0]))
                        .as("janr yo'q: %s", row[0])
                        .isTrue();
            }
            for (String[] row : TaxonomyCatalog.CATEGORIES) {
                assertThat(categoryRepo.existsBySlug(row[0]))
                        .as("kategoriya yo'q: %s", row[0])
                        .isTrue();
            }
        }

        @Test
        @Transactional
        @DisplayName("Janr uch tilda saqlanadi")
        void genreHasThreeTranslations() {
            bootstrap().run();

            Genre drama = genreRepo.findBySlug("drama").orElseThrow();
            assertThat(drama.getTranslations())
                    .extracting(t -> t.getLocale())
                    .containsExactlyInAnyOrder(Locale.UZ, Locale.RU, Locale.EN);
            assertThat(drama.getActive()).isTrue();
        }

        /**
         * ⚠️ ENG MUHIM tekshiruv: runner har ishga tushishda ishlaydi.
         * Takroriy yurish nusxa yaratsa, janr ro'yxati har qayta ishga
         * tushirishdan keyin uzayib borardi.
         */
        @Test
        @Transactional
        @DisplayName("Takroriy yurish nusxa yaratmaydi")
        void secondRunAddsNothing() {
            bootstrap().run();
            long genresAfterFirst = genreRepo.count();
            long categoriesAfterFirst = categoryRepo.count();

            bootstrap().run();

            assertThat(genreRepo.count()).isEqualTo(genresAfterFirst);
            assertThat(categoryRepo.count()).isEqualTo(categoriesAfterFirst);
        }

        /**
         * Admin o'chirgan janr qayta ishga tushirishda TIKLANMASLIGI kerak —
         * aks holda o'chirish tugmasi ishlamayotgandek ko'rinardi.
         */
        @Test
        @Transactional
        @DisplayName("O'chirilgan janr qaytib kelmaydi")
        void deletedGenreStaysDeleted() {
            bootstrap().run();
            Genre western = genreRepo.findBySlug("western").orElseThrow();
            genreRepo.delete(western);
            genreRepo.flush();

            bootstrap().run();

            assertThat(genreRepo.existsBySlug("western")).isFalse();
        }
    }

    @Nested
    @DisplayName("Versiya belgisi")
    class VersionMarker {

        @Test
        @Transactional
        @DisplayName("Sozlamalar ro'yxatida ko'rinmaydi")
        void hiddenFromSettingsList() {
            bootstrap().run();

            assertThat(settingRepo.findById(TaxonomyBootstrap.VERSION_KEY)).isPresent();
            assertThat(settingsService.all())
                    .extracting(PlatformSetting::getKey)
                    .doesNotContain(TaxonomyBootstrap.VERSION_KEY);
        }

        @Test
        @Transactional
        @DisplayName("Admin uni o'zgartira olmaydi")
        void notWritableByAdmin() {
            bootstrap().run();

            assertThatThrownBy(() ->
                    settingsService.update(null, TaxonomyBootstrap.VERSION_KEY, "0"))
                    .isInstanceOf(com.example.backend.exceptions.BusinessException.class);
        }
    }
}
