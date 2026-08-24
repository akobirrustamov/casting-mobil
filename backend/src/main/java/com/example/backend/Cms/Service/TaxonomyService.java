package com.example.backend.Cms.Service;

import com.example.backend.Admin.Dto.CreatorSaveRequest;
import com.example.backend.Admin.Dto.TaxonomySaveRequest;
import com.example.backend.Cms.Entity.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.*;
import com.example.backend.Entity.User;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Kategoriya, janr va ijodkorlarni yaratish/tahrirlash.
 *
 * Uchtasi ham bir xil naqshda: slug + tartib + faollik + uch tilli tarjimalar.
 */
@Service
@RequiredArgsConstructor
public class TaxonomyService {

    private final CategoryRepo categoryRepo;
    private final GenreRepo genreRepo;
    private final CreatorRepo creatorRepo;
    private final MediaAssetRepo mediaAssetRepo;
    private final com.example.backend.Cms.Repository.ContentRepo contentRepo;
    private final AuditService auditService;

    // -------------------------------------------------------------- category

    @Transactional
    public Category saveCategory(User actor, Long id, TaxonomySaveRequest request) {
        String uzName = requireUzName(request.getTranslations(), request.getActive());

        Category category = id == null ? new Category()
                : categoryRepo.findById(id)
                        .orElseThrow(() -> BusinessException.notFound("Category", id));

        String newSlug = slugToApply(id, request.getSlug(), uzName,
                slug -> categoryRepo.findBySlug(slug)
                        .filter(c -> id == null || !c.getId().equals(id)).isPresent());
        if (newSlug != null) {
            category.setSlug(newSlug);
        }
        category.setSortOrder(nz(request.getSortOrder()));
        category.setActive(!Boolean.FALSE.equals(request.getActive()));
        category.setIcon(request.getIconMediaId() == null ? null
                : mediaAssetRepo.findById(request.getIconMediaId())
                        .orElseThrow(() -> BusinessException.notFound("Media", request.getIconMediaId())));

        // Joyida yangilanadi - clear()+add UNIQUE(parent, locale) ni buzadi
        Map<Locale, CategoryTranslation> existingCat = new HashMap<>();
        category.getTranslations().forEach(t -> existingCat.put(t.getLocale(), t));
        Set<Locale> keepCat = new HashSet<>();
        request.getTranslations().forEach((locale, dto) -> {
            if (dto == null || isBlank(dto.getTitle())) {
                return;
            }
            keepCat.add(locale);
            CategoryTranslation row = existingCat.get(locale);
            if (row == null) {
                row = CategoryTranslation.builder().locale(locale).build();
                category.addTranslation(row);
            }
            row.setName(dto.getTitle().trim());
            row.setDescription(dto.getDescription());
        });
        category.getTranslations().removeIf(t -> !keepCat.contains(t.getLocale()));

        Category saved = categoryRepo.save(category);
        auditService.log(actor, id == null ? AuditAction.CATEGORY_CREATED : AuditAction.CATEGORY_UPDATED,
                "Category", saved.getId());
        return saved;
    }

    /**
     * Kategoriyani o'chiradi (ТЗ §16).
     *
     * <h2>Nega HAQIQIY o'chirish, arxivlash emas</h2>
     * Kategoriya va janrda {@code deleted_at} yo'q — ular kontent kabi
     * soft-delete arxitekturasiga kirmaydi (§58 faqat kontent, ijodkor
     * va media uchun). Ular sof taksonomiya: kontentni guruhlash uchun
     * yorliq, o'zining tarixi yoki analitikasi yo'q.
     *
     * <h2>Nega tekshiruv shart</h2>
     * Kategoriya kontentga {@code category_id} orqali BOG'LANGAN
     * (majburiy emas, lekin bog'lansa FK bor). O'chirish jim
     * bajarilsa, o'sha kontent kategoriyasiz qolib, katalogdagi
     * filtrlardan tushib qolardi — admin buni sezmasdi, chunki
     * frontendda xato chiqmasdi.
     *
     * Faqat TIRIK kontent hisobga olinadi: arxivlangan (soft-deleted)
     * kontent kategoriyani qulflab qo'ymasligi kerak.
     */
    @Transactional
    public void deleteCategory(User actor, Long id) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Category", id));

        long contentCount = contentRepo.countByCategoryIdAndDeletedAtIsNull(id);
        if (contentCount > 0) {
            throw new BusinessException("CATEGORY_IN_USE",
                    "Kategoriya ishlatilmoqda: " + contentCount + " ta kontentda. "
                            + "Avval o'sha kontentlarning kategoriyasini almashtiring.",
                    org.springframework.http.HttpStatus.CONFLICT);
        }

        categoryRepo.delete(category);
        auditService.log(actor, AuditAction.CATEGORY_DELETED, "Category", id);
    }

    // ----------------------------------------------------------------- genre

    @Transactional
    public Genre saveGenre(User actor, Long id, TaxonomySaveRequest request) {
        String uzName = requireUzName(request.getTranslations(), request.getActive());

        Genre genre = id == null ? new Genre()
                : genreRepo.findById(id).orElseThrow(() -> BusinessException.notFound("Genre", id));

        String newSlug = slugToApply(id, request.getSlug(), uzName,
                slug -> genreRepo.findBySlug(slug)
                        .filter(g -> id == null || !g.getId().equals(id)).isPresent());
        if (newSlug != null) {
            genre.setSlug(newSlug);
        }
        genre.setSortOrder(nz(request.getSortOrder()));
        genre.setActive(!Boolean.FALSE.equals(request.getActive()));

        Map<Locale, GenreTranslation> existingGenre = new HashMap<>();
        genre.getTranslations().forEach(t -> existingGenre.put(t.getLocale(), t));
        Set<Locale> keepGenre = new HashSet<>();
        request.getTranslations().forEach((locale, dto) -> {
            if (dto == null || isBlank(dto.getTitle())) {
                return;
            }
            keepGenre.add(locale);
            GenreTranslation row = existingGenre.get(locale);
            if (row == null) {
                row = GenreTranslation.builder().locale(locale).build();
                genre.addTranslation(row);
            }
            row.setName(dto.getTitle().trim());
        });
        genre.getTranslations().removeIf(t -> !keepGenre.contains(t.getLocale()));

        Genre saved = genreRepo.save(genre);
        auditService.log(actor, id == null ? AuditAction.GENRE_CREATED : AuditAction.GENRE_UPDATED,
                "Genre", saved.getId());
        return saved;
    }

    /**
     * Janrni o'chiradi (ТЗ §17).
     *
     * Kategoriyadan farqi: janr kontentga ko'p-ko'pga bog'lanadi
     * ({@code content_genre}), ya'ni bitta kontentda bir nechta janr
     * bo'lishi mumkin. Tekshiruv shu sababli boshqa so'rov —
     * {@code countByGenres_IdAndDeletedAtIsNull} — ishlatadi.
     *
     * Qolgan mantiq {@link #deleteCategory} bilan bir xil: faqat tirik
     * kontent hisobga olinadi, foydalanilayotgan janr o'chirilmaydi.
     */
    @Transactional
    public void deleteGenre(User actor, Long id) {
        Genre genre = genreRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Genre", id));

        long contentCount = contentRepo.countByGenres_IdAndDeletedAtIsNull(id);
        if (contentCount > 0) {
            throw new BusinessException("GENRE_IN_USE",
                    "Janr ishlatilmoqda: " + contentCount + " ta kontentda. "
                            + "Avval o'sha kontentlardan bu janrni olib tashlang.",
                    org.springframework.http.HttpStatus.CONFLICT);
        }

        genreRepo.delete(genre);
        auditService.log(actor, AuditAction.GENRE_DELETED, "Genre", id);
    }

    // --------------------------------------------------------------- creator

    @Transactional
    public Creator saveCreator(User actor, Long id, CreatorSaveRequest request) {
        CreatorSaveRequest.NameDto uz = request.getTranslations().get(Locale.UZ);
        if (uz == null) {
            throw BusinessException.validation("O'zbekcha ism majburiy - u asosiy til");
        }
        String uzDisplay = displayNameOf(uz);
        if (isBlank(uzDisplay)) {
            throw BusinessException.validation("O'zbekcha ism majburiy - u asosiy til");
        }

        // Faol ijodkor kontent sahifasida va «Mashhur ijodkorlar» bo'limida
        // chiqadi - ismi uchala tilda bo'lishi kerak.
        if (request.getActive() == null || Boolean.TRUE.equals(request.getActive())) {
            TranslationRules.requireAll(request.getTranslations(),
                    this::displayNameOf, "Ijodkor ismi");
        }

        Creator creator = id == null ? new Creator()
                : creatorRepo.findById(id).orElseThrow(() -> BusinessException.notFound("Creator", id));

        String newSlug = slugToApply(id, request.getSlug(), uzDisplay,
                slug -> creatorRepo.findBySlug(slug)
                        .filter(c -> id == null || !c.getId().equals(id)).isPresent());
        if (newSlug != null) {
            creator.setSlug(newSlug);
        }
        creator.setBirthDate(request.getBirthDate());
        creator.setActive(!Boolean.FALSE.equals(request.getActive()));
        creator.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        creator.setSortOrder(nz(request.getSortOrder()));
        creator.setPhoto(mediaOrNull(request.getPhotoMediaId()));
        creator.setCover(mediaOrNull(request.getCoverMediaId()));

        Map<Locale, CreatorTranslation> existingCr = new HashMap<>();
        creator.getTranslations().forEach(t -> existingCr.put(t.getLocale(), t));
        Set<Locale> keepCr = new HashSet<>();
        request.getTranslations().forEach((locale, dto) -> {
            if (dto == null) {
                return;
            }
            String display = displayNameOf(dto);
            if (isBlank(display)) {
                return;
            }
            keepCr.add(locale);
            CreatorTranslation row = existingCr.get(locale);
            if (row == null) {
                row = CreatorTranslation.builder().locale(locale).build();
                creator.addTranslation(row);
            }
            row.setFirstName(dto.getFirstName());
            row.setLastName(dto.getLastName());
            row.setMiddleName(dto.getMiddleName());
            row.setDisplayName(display);
            row.setBio(dto.getBio());
        });
        creator.getTranslations().removeIf(t -> !keepCr.contains(t.getLocale()));

        Creator saved = creatorRepo.save(creator);
        auditService.log(actor, id == null ? AuditAction.CREATOR_CREATED : AuditAction.CREATOR_UPDATED,
                "Creator", saved.getId());
        return saved;
    }

    // ------------------------------------------------------------------ ichki

    /** displayName bo'sh bo'lsa ism va familiyadan yig'iladi. */
    private String displayNameOf(CreatorSaveRequest.NameDto dto) {
        if (!isBlank(dto.getDisplayName())) {
            return dto.getDisplayName().trim();
        }
        String joined = ((dto.getFirstName() == null ? "" : dto.getFirstName()) + " "
                + (dto.getLastName() == null ? "" : dto.getLastName())).trim();
        return joined;
    }

    /**
     * Nom tekshiruvi.
     *
     * ⚠️ Kategoriya va janr FAOL bo'lsa uchala tilda ham majburiy: ТЗ §16
     * bo'yicha kategoriya «foydalanuvchi mobil ilovasining bosh menyusida
     * chiqadi». Ruscha tarjimasi yo'q kategoriya rus tilidagi menyuda bo'sh
     * katak bo'lib ko'rinardi.
     *
     * Faolsizlantirilgan (active = false) uchun asosiy til yetarli — u
     * hech qayerda ko'rinmaydi, ya'ni tayyorlanayotgan bo'lishi mumkin.
     */
    private String requireUzName(Map<Locale, com.example.backend.Admin.Dto.TranslationDto> translations,
                                 Boolean active) {
        boolean userVisible = active == null || Boolean.TRUE.equals(active);
        TranslationRules.require(translations,
                com.example.backend.Admin.Dto.TranslationDto::getTitle,
                "Nom", userVisible);
        return translations.get(Locale.UZ).getTitle().trim();
    }

    private MediaAsset mediaOrNull(Long id) {
        return id == null ? null : mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String pick(String preferred, String fallback) {
        return isBlank(preferred) ? fallback : preferred;
    }

    /**
     * Slug qachon qayta yasaladi.
     *
     * <b>Faqat yaratishda yoki so'rovda ATAYLAB berilganda.</b> Tahrirlashda
     * nomni o'zgartirish slug'ni O'ZGARTIRMAYDI: unga URL'lar, havolalar va
     * mobil ilovadagi keshlar bog'langan bo'lishi mumkin.
     *
     * @return yangi slug, yoki {@code null} - o'zgartirmaslik kerak bo'lsa
     */
    private String slugToApply(Long id, String requestedSlug, String nameForSlug,
                               java.util.function.Predicate<String> isTaken) {
        boolean creating = id == null;
        boolean explicit = !isBlank(requestedSlug);
        if (!creating && !explicit) {
            return null;
        }
        return SlugGenerator.unique(pick(requestedSlug, nameForSlug), nameForSlug, isTaken);
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
