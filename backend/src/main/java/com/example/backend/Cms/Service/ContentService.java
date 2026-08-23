package com.example.backend.Cms.Service;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Repository.*;
import com.example.backend.Entity.User;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Kontentni yaratish va tahrirlash.
 *
 * Controller'da emas, alohida servisda: bu yerda tranzaksiya, tarjimalar,
 * media va kreditlarni almashtirish hamda audit bir joyda turadi (§103).
 */
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepo contentRepo;
    private final CategoryRepo categoryRepo;
    private final GenreRepo genreRepo;
    private final CreatorRepo creatorRepo;
    private final MediaAssetRepo mediaAssetRepo;
    private final AuditService auditService;

    @Transactional
    public Content create(User actor, ContentSaveRequest request) {
        validate(request);

        Content content = new Content();
        content.setSlug(resolveSlug(request, null));
        content.setCreatedBy(actor == null ? null : actor.getId());
        apply(content, request);

        Content saved = contentRepo.save(content);
        auditService.log(actor, AuditAction.CONTENT_CREATED, "Content", saved.getId(),
                null, Map.of("slug", saved.getSlug(), "status", saved.getStatus()));
        return saved;
    }

    @Transactional
    public Content update(User actor, Long id, ContentSaveRequest request) {
        validate(request);

        Content content = contentRepo.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.notFound("Content", id));

        // Ikki admin bir vaqtda tahrirlaganda ikkinchisi birinchisining ishini
        // indamay bosib ketmasligi kerak (§60).
        ConcurrencyGuard.check(request.getVersion(), content.getVersion(), "Kontent");

        // Qatorni ataylab «o'zgargan» deb belgilaymiz.
        //
        // ⚠️ HOZIR bu ortiqcha: `apply()` har safar `media.clear()` va
        // `credits.clear()` chaqiradi, kolleksiya iflos bo'ladi va
        // Hibernate egasining versiyasini o'zi oshiradi. Ya'ni himoya
        // TASODIFAN ishlayapti. Kimdir `apply()` ni optimallashtirsa
        // («ro'yxat o'zgarmagan bo'lsa tegmaymiz»), versiya oshmay
        // qolardi va ikki admin bir-birini indamay bosib ketardi —
        // xuddi qismda bo'lgani kabi (u yerda buzuq edi, §60 da
        // tuzatildi). Shuning uchun niyat aniq yozib qo'yiladi.
        content.touch();

        PublicationStatus before = content.getStatus();
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            content.setSlug(resolveSlug(request, content));
        }
        content.setUpdatedBy(actor == null ? null : actor.getId());
        apply(content, request);

        Content saved = contentRepo.save(content);

        String action = before != PublicationStatus.PUBLISHED
                && saved.getStatus() == PublicationStatus.PUBLISHED
                ? AuditAction.CONTENT_PUBLISHED
                : AuditAction.CONTENT_UPDATED;
        auditService.log(actor, action, "Content", saved.getId(),
                Map.of("status", before), Map.of("status", saved.getStatus()));
        return saved;
    }

    /** Soft delete - yozuv saqlanadi, ro'yxatlardan yo'qoladi (§58). */
    @Transactional
    public void archive(User actor, Long id) {
        Content content = contentRepo.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.notFound("Content", id));

        content.setDeletedAt(LocalDateTime.now());
        content.setStatus(PublicationStatus.ARCHIVED);
        contentRepo.save(content);

        auditService.log(actor, AuditAction.CONTENT_ARCHIVED, "Content", id);
    }

    // ------------------------------------------------------------------ ichki

    private void validate(ContentSaveRequest request) {
        Map<Locale, com.example.backend.Admin.Dto.TranslationDto> tr = request.getTranslations();
        if (tr == null || tr.isEmpty()) {
            throw BusinessException.validation("Kamida bitta tilda sarlavha kiritilishi kerak");
        }
        // Qoralamada o'zbekchasi yetarli, NASHR qilinganda uchala til ham
        // majburiy. Sabab va chegara: TranslationRules.
        //
        // SCHEDULED ham kiradi: u belgilangan vaqtda avtomatik PUBLISHED
        // bo'ladi va o'shanda tarjimasi yo'qligi bilinardi — kech bo'lardi.
        boolean userVisible = request.getStatus() == PublicationStatus.PUBLISHED
                || request.getStatus() == PublicationStatus.SCHEDULED;
        TranslationRules.require(tr,
                com.example.backend.Admin.Dto.TranslationDto::getTitle,
                "Sarlavha", userVisible);
        if (request.getAccessPolicy() != null && request.getAccessPolicy().requiresPayment()
                && request.getStructureType() == com.example.backend.Cms.Enums.StructureType.SINGLE
                && request.getPremierePrice() == null) {
            throw BusinessException.validation(
                    "Pullik bitta qismlik kontent uchun narx kiritilishi kerak");
        }
    }

    private String resolveSlug(ContentSaveRequest request, Content existing) {
        String requested = request.getSlug();
        String fromTitle = request.getTranslations().get(Locale.UZ).getTitle();
        String source = (requested != null && !requested.isBlank()) ? requested : fromTitle;

        return SlugGenerator.unique(source, fromTitle, candidate ->
                contentRepo.findBySlug(candidate)
                        .filter(c -> existing == null || !c.getId().equals(existing.getId()))
                        .isPresent());
    }

    /** Barcha maydonlarni so'rovdan entity'ga ko'chiradi. */
    private void apply(Content content, ContentSaveRequest request) {
        content.setContentType(request.getContentType());
        // Berilmasa mavjud qiymat saqlanadi - tahrirlash ko'rinuvchanlikni
        // tasodifan PUBLIC ga qaytarib yubormasin.
        if (request.getVisibility() != null) {
            content.setVisibility(request.getVisibility());
        }
        content.setLanguage(request.getLanguage());
        content.setStructureType(request.getStructureType());
        content.setOrientation(request.getOrientation());
        content.setStatus(request.getStatus());
        content.setAccessPolicy(request.getAccessPolicy());
        content.setPremierePrice(request.getPremierePrice());
        content.setAgeRating(request.getAgeRating());
        content.setDurationMinutes(request.getDurationMinutes());
        content.setPremiereDate(request.getPremiereDate());
        content.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        content.setPopular(Boolean.TRUE.equals(request.getPopular()));

        // PUBLISHED bo'lganda sana o'zi qo'yiladi - admin unutib qoldirmasin
        if (request.getPublicationDate() != null) {
            content.setPublicationDate(request.getPublicationDate());
        } else if (request.getStatus() == PublicationStatus.PUBLISHED
                && content.getPublicationDate() == null) {
            content.setPublicationDate(LocalDateTime.now());
        }

        content.setCategory(request.getCategoryId() == null ? null
                : categoryRepo.findById(request.getCategoryId())
                        .orElseThrow(() -> BusinessException.notFound("Category", request.getCategoryId())));

        Set<Genre> genres = new LinkedHashSet<>();
        for (Long genreId : request.getGenreIds()) {
            genres.add(genreRepo.findById(genreId)
                    .orElseThrow(() -> BusinessException.notFound("Genre", genreId)));
        }
        content.setGenres(genres);

        replaceTranslations(content, request);
        replaceMedia(content, request);
        replaceCredits(content, request);
    }

    /**
     * Tarjimalarni JOYIDA yangilaydi.
     *
     * ⚠️ Bu yerda `clear()` + qayta qo'shish ISHLAMAYDI: Hibernate bitta flush
     * ichida eski satrni o'chirishdan OLDIN yangisini qo'shadi va
     * `UNIQUE(content_id, locale)` cheklovi buziladi. Shuning uchun mavjud
     * satrlar qayta ishlatiladi, ortiqchasi o'chiriladi, yetishmagani qo'shiladi.
     */
    private void replaceTranslations(Content content, ContentSaveRequest request) {
        Map<Locale, ContentTranslation> existing = new HashMap<>();
        for (ContentTranslation t : content.getTranslations()) {
            existing.put(t.getLocale(), t);
        }

        Set<Locale> keep = new HashSet<>();
        request.getTranslations().forEach((locale, dto) -> {
            if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
                return; // bo'sh tarjima saqlanmaydi
            }
            keep.add(locale);
            ContentTranslation row = existing.get(locale);
            if (row == null) {
                row = ContentTranslation.builder().locale(locale).build();
                content.addTranslation(row);
            }
            row.setTitle(dto.getTitle().trim());
            row.setShortDescription(dto.getShortDescription());
            row.setDescription(dto.getDescription());
        });

        // Yuborilmagan tillar o'chiriladi (orphanRemoval buni bajaradi)
        content.getTranslations().removeIf(t -> !keep.contains(t.getLocale()));
    }

    private void replaceMedia(Content content, ContentSaveRequest request) {
        content.getMedia().clear();
        for (ContentSaveRequest.MediaLink link : request.getMedia()) {
            MediaAsset asset = mediaAssetRepo.findById(link.getMediaId())
                    .orElseThrow(() -> BusinessException.notFound("Media", link.getMediaId()));
            content.addMedia(ContentMedia.builder()
                    .role(link.getRole())
                    .locale(link.getLocale())
                    .media(asset)
                    .sortOrder(link.getSortOrder() == null ? 0 : link.getSortOrder())
                    .build());
        }
    }

    private void replaceCredits(Content content, ContentSaveRequest request) {
        content.getCredits().clear();
        for (ContentSaveRequest.CreditLink link : request.getCredits()) {
            Creator creator = creatorRepo.findById(link.getCreatorId())
                    .orElseThrow(() -> BusinessException.notFound("Creator", link.getCreatorId()));
            content.addCredit(ContentCredit.builder()
                    .creator(creator)
                    .profession(link.getProfession())
                    .characterName(link.getCharacterName())
                    .sortOrder(link.getSortOrder() == null ? 0 : link.getSortOrder())
                    .build());
        }
    }
}
