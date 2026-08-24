package com.example.backend.Admin.Controller;

import com.example.backend.Admin.Dto.*;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Service.PageHydrator;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.TaxonomyService;
import jakarta.validation.Valid;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Repository.*;
import com.example.backend.Enums.Permission;
import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.RequirePermission;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Kategoriya, janr, ijodkor va kontent ro'yxatlari.
 *
 * Har bir metod ruxsatni MUSTAQIL tekshiradi - frontend menyuni yashirishi
 * xavfsizlik hisoblanmaydi (§11).
 */
@RestController
@RequestMapping("/api/v1/app/admin")
@RequiredArgsConstructor
public class CmsCatalogController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CategoryRepo categoryRepo;
    private final GenreRepo genreRepo;
    private final CreatorRepo creatorRepo;
    private final ContentRepo contentRepo;
    private final ContentService contentService;
    private final TaxonomyService taxonomyService;
    private final PermissionService permissionService;

    private void require(Permission permission) {
        if (!permissionService.hasPermission(CurrentUser.get(), permission)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
        }
    }

    /** Sahifa hajmi cheklanadi - 100 000 satrni bitta javobda qaytarish mumkin emas (§95). */
    private Pageable pageable(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE), sort);
    }

    // ------------------------------------------------------------ categories

    /**
     * Kategoriyalar — sahifalangan va qidiruvli (ТЗ §51).
     *
     * ⚠️ Ilgari {@code findAll()} edi: panel har ochilganda BUTUN jadval
     * kelardi. Kategoriya soni cheklanmagan va platforma o'sgani sari
     * ro'yxat uzayardi.
     *
     * ⚠️ Sahifasiz eski chaqiruv ham ishlashi kerak — panelning boshqa
     * joylarida (kontent muharriri, filtrlar) to'liq ro'yxat kerak.
     * Shuning uchun {@code size} kattaroq bo'lishi mumkin.
     */
    @GetMapping("/categories")
    public ResponseEntity<PageResponse<CategoryDto>> categories(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        require(Permission.CATEGORY_VIEW);
        return ResponseEntity.ok(PageResponse.of(
                categoryRepo.searchPage(query(q),
                        pageable(page, size, Sort.by("sortOrder"))),
                CategoryDto::from));
    }

    // ---------------------------------------------------------------- genres

    /** Janrlar — sahifalangan va qidiruvli (ТЗ §51). */
    @GetMapping("/genres")
    public ResponseEntity<PageResponse<GenreDto>> genres(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        require(Permission.GENRE_VIEW);
        return ResponseEntity.ok(PageResponse.of(
                genreRepo.searchPage(query(q),
                        pageable(page, size, Sort.by("sortOrder"))),
                GenreDto::from));
    }

    // -------------------------------------------------------------- creators

    /**
     * Ijodkorlar — sahifalangan va qidiruvli (ТЗ §51).
     *
     * ⚠️ Ilgari qidiruv bor edi, lekin natija {@code List} bo'lib
     * qaytardi va JAMI son noma'lum edi — panel «3-sahifadan 5-sahifaga»
     * o'tishni ko'rsata olmasdi. Qidiruvsiz esa BUTUN jadval kelardi.
     */
    @GetMapping("/creators")
    public ResponseEntity<PageResponse<CreatorDto>> creators(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        require(Permission.CREATOR_VIEW);
        return ResponseEntity.ok(PageResponse.of(
                creatorRepo.searchPage(query(q),
                        pageable(page, size, Sort.by("sortOrder"))),
                CreatorDto::from));
    }

    /**
     * Qidiruv matnini tayyorlaydi.
     *
     * Kamida 2 belgi: bitta harf butun jadvalni skanerlashiga arzimaydi
     * va natija ham foydasiz bo'lardi.
     */
    private String query(String q) {
        return q == null || q.trim().length() < 2 ? null : q.trim();
    }

    // --------------------------------------------------------------- content

    /**
     * ⚠️ {@code @Transactional} SHART: sahifa va uni to'ldiruvchi so'rov
     * bitta persistence context'da bo'lishi kerak. Aks holda to'ldirish
     * boshqa kontekstga tushadi va sahifadagi obyektlar bo'sh qoladi.
     *
     * open-in-view sozlamasiga tayanmaymiz — u o'chirilishi mumkin.
     */
    @GetMapping("/content")
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<ContentListDto>> content(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) PublicationStatus status,
            @RequestParam(required = false) ContentType type,
            @RequestParam(required = false) String q) {

        require(Permission.CONTENT_VIEW);
        Pageable p = pageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Content> result;
        if (q != null && q.trim().length() >= 2) {
            result = contentRepo.search(q.trim(), p);
        } else if (status != null) {
            result = contentRepo.findAllByDeletedAtIsNullAndStatus(status, p);
        } else if (type != null) {
            result = contentRepo.findAllByDeletedAtIsNullAndContentType(type, p);
        } else {
            result = contentRepo.findAllByDeletedAtIsNull(p);
        }

        // Sahifa toza limit bilan olindi; endi bitta so'rov bilan tarjimalar
        // va kategoriya to'ldiriladi. Batafsil: PageHydrator.
        PageHydrator.warm(result, Content::getId, contentRepo::findAllByIdIn);

        return ResponseEntity.ok(PageResponse.of(result, ContentListDto::from));
    }

    @GetMapping("/content/{id}")
    public ResponseEntity<ContentListDto> contentDetail(@PathVariable Long id) {
        require(Permission.CONTENT_VIEW);
        Content c = contentRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Content", id));
        return ResponseEntity.ok(ContentListDto.from(c));
    }

    // ------------------------------------------------------- yozish amallari

    @PostMapping("/content")
    @RequirePermission(Permission.CONTENT_CREATE)
    public ResponseEntity<ContentListDto> createContent(@Valid @RequestBody ContentSaveRequest request) {
        require(Permission.CONTENT_CREATE);
        requirePublishRights(request);
        Content saved = contentService.create(CurrentUser.get(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContentListDto.from(saved));
    }

    @PutMapping("/content/{id}")
    @RequirePermission(Permission.CONTENT_EDIT)
    public ResponseEntity<ContentListDto> updateContent(@PathVariable Long id,
                                                        @Valid @RequestBody ContentSaveRequest request) {
        require(Permission.CONTENT_EDIT);
        requirePublishRights(request);
        Content saved = contentService.update(CurrentUser.get(), id, request);
        return ResponseEntity.ok(ContentListDto.from(saved));
    }

    /** Soft delete: yozuv saqlanadi, ro'yxatlardan yo'qoladi (§58). */
    @DeleteMapping("/content/{id}")
    public ResponseEntity<Void> archiveContent(@PathVariable Long id) {
        require(Permission.CONTENT_DELETE);
        contentService.archive(CurrentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Nashr qilish alohida ruxsat.
     * Worker kontent yarata olsa ham, uni chop etishi uchun CONTENT_PUBLISH kerak.
     */
    private void requirePublishRights(ContentSaveRequest request) {
        if (request.getStatus() == PublicationStatus.PUBLISHED) {
            require(Permission.CONTENT_PUBLISH);
        }
    }

    @PostMapping("/categories")
    @RequirePermission(Permission.CATEGORY_CREATE)
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody TaxonomySaveRequest request) {
        require(Permission.CATEGORY_CREATE);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CategoryDto.from(taxonomyService.saveCategory(CurrentUser.get(), null, request)));
    }

    @PutMapping("/categories/{id}")
    @RequirePermission(Permission.CATEGORY_EDIT)
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long id,
                                                      @Valid @RequestBody TaxonomySaveRequest request) {
        require(Permission.CATEGORY_EDIT);
        return ResponseEntity.ok(
                CategoryDto.from(taxonomyService.saveCategory(CurrentUser.get(), id, request)));
    }

    /**
     * Kategoriyani o'chiradi (ТЗ §16).
     *
     * Kontentga bog'langan bo'lsa backend 409 {@code CATEGORY_IN_USE}
     * qaytaradi va NECHTA kontentda ekanini aytadi — panel buni
     * ko'rsatib, admin avval o'sha kontentlarni ko'chirishi kerakligini
     * bilib oladi.
     */
    @DeleteMapping("/categories/{id}")
    @RequirePermission(Permission.CATEGORY_DELETE)
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        require(Permission.CATEGORY_DELETE);
        taxonomyService.deleteCategory(CurrentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/genres")
    @RequirePermission(Permission.GENRE_CREATE)
    public ResponseEntity<GenreDto> createGenre(@Valid @RequestBody TaxonomySaveRequest request) {
        require(Permission.GENRE_CREATE);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenreDto.from(taxonomyService.saveGenre(CurrentUser.get(), null, request)));
    }

    @PutMapping("/genres/{id}")
    @RequirePermission(Permission.GENRE_EDIT)
    public ResponseEntity<GenreDto> updateGenre(@PathVariable Long id,
                                                @Valid @RequestBody TaxonomySaveRequest request) {
        require(Permission.GENRE_EDIT);
        return ResponseEntity.ok(
                GenreDto.from(taxonomyService.saveGenre(CurrentUser.get(), id, request)));
    }

    /** Janrni o'chiradi (ТЗ §17). Foydalanilayotgan bo'lsa 409 {@code GENRE_IN_USE}. */
    @DeleteMapping("/genres/{id}")
    @RequirePermission(Permission.GENRE_DELETE)
    public ResponseEntity<Void> deleteGenre(@PathVariable Long id) {
        require(Permission.GENRE_DELETE);
        taxonomyService.deleteGenre(CurrentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/creators")
    @RequirePermission(Permission.CREATOR_CREATE)
    public ResponseEntity<CreatorDto> createCreator(@Valid @RequestBody CreatorSaveRequest request) {
        require(Permission.CREATOR_CREATE);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreatorDto.from(taxonomyService.saveCreator(CurrentUser.get(), null, request)));
    }

    @PutMapping("/creators/{id}")
    @RequirePermission(Permission.CREATOR_EDIT)
    public ResponseEntity<CreatorDto> updateCreator(@PathVariable Long id,
                                                    @Valid @RequestBody CreatorSaveRequest request) {
        require(Permission.CREATOR_EDIT);
        return ResponseEntity.ok(
                CreatorDto.from(taxonomyService.saveCreator(CurrentUser.get(), id, request)));
    }
}
