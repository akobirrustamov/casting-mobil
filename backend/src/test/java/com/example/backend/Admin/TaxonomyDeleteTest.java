package com.example.backend.Admin;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.TaxonomySaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Category;
import com.example.backend.Cms.Entity.Genre;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.CategoryRepo;
import com.example.backend.Cms.Repository.GenreRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.TaxonomyService;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kategoriya va janrni o'chirish (ТЗ §16, §17).
 *
 * <h2>Nega alohida fayl</h2>
 * Ilgari o'chirish endpointi umuman yo'q edi — ТЗ auditda aniq
 * yozilgan bo'shliq. Bu fayl aynan shu ikki bandning qabul mezonini
 * tekshiradi: foydalanilayotgan yozuv o'chirilmaydi va sabab ochiq
 * ko'rsatiladi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
@Transactional
class TaxonomyDeleteTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private TestStaffFactory staff;
    @Autowired private TaxonomyService taxonomyService;
    @Autowired private ContentService contentService;
    @Autowired private CategoryRepo categoryRepo;
    @Autowired private GenreRepo genreRepo;

    private String tokenWith(Permission... permissions) {
        return staff.tokenForRole("+998900003" + (100 + SEQ.incrementAndGet()),
                PlatformRole.WORKER, EnumSet.copyOf(java.util.List.of(permissions)));
    }

    /**
     * Uchala tilda tarjima bilan — kategoriya FAOL bo'lsa (sukut
     * qiymat) uchtasi ham majburiy (ТЗ §16, {@code requireUzName}).
     */
    private Category newCategory(String slug) {
        TaxonomySaveRequest req = new TaxonomySaveRequest();
        req.setSlug(slug);
        req.setTranslations(new LinkedHashMap<>(Translations.all(slug)));
        return taxonomyService.saveCategory(null, null, req);
    }

    private Genre newGenre(String slug) {
        TaxonomySaveRequest req = new TaxonomySaveRequest();
        req.setSlug(slug);
        req.setTranslations(new LinkedHashMap<>(Translations.all(slug)));
        return taxonomyService.saveGenre(null, null, req);
    }

    // ---------------------------------------------------------- kategoriya

    @Nested
    @DisplayName("Kategoriya")
    class CategoryDeletion {

        @Test
        @DisplayName("Bo'sh kategoriya o'chadi")
        void emptyIsDeleted() throws Exception {
            Category c = newCategory("bosh-kategoriya-" + SEQ.incrementAndGet());
            String token = tokenWith(Permission.CATEGORY_DELETE);

            mockMvc.perform(delete("/api/v1/app/admin/categories/" + c.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            assertThat(categoryRepo.findById(c.getId())).isEmpty();
        }

        @Test
        @DisplayName("Ishlatilayotgan kategoriya O'CHMAYDI — 409 va sabab")
        void inUseIsRejected() throws Exception {
            Category c = newCategory("band-kategoriya-" + SEQ.incrementAndGet());

            ContentSaveRequest content = new ContentSaveRequest();
            content.setContentType(ContentType.MOVIE);
            content.setStructureType(StructureType.SINGLE);
            content.setAccessPolicy(AccessPolicy.FREE);
            content.setStatus(PublicationStatus.PUBLISHED);
            content.setCategoryId(c.getId());
            content.setTranslations(Translations.all("Band kontent"));
            contentService.create(null, content);

            String token = tokenWith(Permission.CATEGORY_DELETE);

            mockMvc.perform(delete("/api/v1/app/admin/categories/" + c.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CATEGORY_IN_USE"));

            // ⚠️ ASOSIY DALIL: yozuv baribir mavjud.
            assertThat(categoryRepo.findById(c.getId())).isPresent();
        }

        @Test
        @DisplayName("CATEGORY_DELETE ruxsatisiz 403")
        void requiresPermission() throws Exception {
            Category c = newCategory("ruxsatsiz-kategoriya-" + SEQ.incrementAndGet());
            String token = tokenWith(Permission.CATEGORY_VIEW, Permission.CATEGORY_EDIT);

            mockMvc.perform(delete("/api/v1/app/admin/categories/" + c.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());

            assertThat(categoryRepo.findById(c.getId())).isPresent();
        }
    }

    // ---------------------------------------------------------------- janr

    @Nested
    @DisplayName("Janr")
    class GenreDeletion {

        @Test
        @DisplayName("Bo'sh janr o'chadi")
        void emptyIsDeleted() throws Exception {
            Genre g = newGenre("bosh-janr-" + SEQ.incrementAndGet());
            String token = tokenWith(Permission.GENRE_DELETE);

            mockMvc.perform(delete("/api/v1/app/admin/genres/" + g.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            assertThat(genreRepo.findById(g.getId())).isEmpty();
        }

        @Test
        @DisplayName("Ishlatilayotgan janr O'CHMAYDI — 409 va sabab")
        void inUseIsRejected() throws Exception {
            Genre g = newGenre("band-janr-" + SEQ.incrementAndGet());

            ContentSaveRequest content = new ContentSaveRequest();
            content.setContentType(ContentType.MOVIE);
            content.setStructureType(StructureType.SINGLE);
            content.setAccessPolicy(AccessPolicy.FREE);
            content.setStatus(PublicationStatus.PUBLISHED);
            content.setGenreIds(java.util.Set.of(g.getId()));
            content.setTranslations(Translations.all("Band kontent"));
            contentService.create(null, content);

            String token = tokenWith(Permission.GENRE_DELETE);

            mockMvc.perform(delete("/api/v1/app/admin/genres/" + g.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GENRE_IN_USE"));

            assertThat(genreRepo.findById(g.getId())).isPresent();
        }
    }
}
