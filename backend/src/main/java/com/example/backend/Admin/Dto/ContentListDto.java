package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.ContentMedia;
import com.example.backend.Cms.Entity.ContentTranslation;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Kontent ro'yxati uchun yengil DTO.
 *
 * Entity to'g'ridan-to'g'ri qaytarilmaydi (§65): u lazy bog'lanishlarga to'la
 * va JSON'ga o'girilganda halqa hosil qiladi.
 */
@Data
@Builder
public class ContentListDto {

    private Long id;

    /**
     * Optimistik qulf versiyasi (§60).
     *
     * ⚠️ Bu maydonsiz butun himoya o'lik edi: entity'da {@code @Version}
     * bor, servisda tekshiruv bor, panel formasida {@code version}
     * maydoni bor — lekin API uni QAYTARMAGANI uchun forma doim
     * {@code null} yuborardi va tekshiruv har safar o'tkazib
     * yuborilardi. Ikki admin bir-birining ishini indamay bosib ketardi.
     */
    private Long version;

    private String slug;
    private ContentType contentType;
    private StructureType structureType;
    private ContentOrientation orientation;
    private PublicationStatus status;

    /** PUBLIC · UNLISTED · PRIVATE — katalogda topilishi (ТЗ §15). */
    private ContentVisibility visibility;

    /** Asarning asl tili, ISO 639-1. Tarjimalar bilan aralashtirilmasin. */
    private String language;
    private AccessPolicy accessPolicy;
    private BigDecimal premierePrice;
    private String categorySlug;
    private String ageRating;
    private Boolean featured;
    private Boolean popular;
    private Long viewCount;
    private LocalDateTime publicationDate;
    private LocalDateTime premiereDate;

    /** Uchala til - admin panel ro'yxatda ham tilni almashtira oladi. */
    private Map<Locale, TranslationDto> translations;

    /** Til bo'yicha afisha. Kalit yo'q = umumiy afisha ishlatiladi. */
    private Long posterMediaId;
    private Map<Locale, Long> localePosters;

    /**
     * Quyidagi uch maydon TAHRIRLASH uchun majburiy.
     *
     * <h2>Nega qo'shildi (B17)</h2>
     * Ilgari bu yerda faqat {@code categorySlug} bor edi, muqova va galereya
     * esa umuman yo'q edi. Muharrir ularni YUKLAY olmasdi, saqlashda esa
     * backend media ro'yxatini butunlay almashtiradi
     * ({@code content.getMedia().clear()}).
     *
     * Natijada har qanday tahrirlash kategoriya, muqova va galereyani
     * JIMGINA o'chirib yuborardi — foydalanuvchi faqat sarlavhani
     * tuzatgan bo'lsa ham.
     */
    private Long categoryId;
    private Long coverMediaId;
    private List<Long> gallery;

    /**
     * BARCHA media bog'lanishlari — xom ko'rinishda.
     *
     * <h2>Nima uchun qulaylik maydonlari yetarli emas</h2>
     * {@code posterMediaId}, {@code coverMediaId}, {@code gallery} va
     * {@code localePosters} — panel uchun qulay, lekin ular FAQAT uchta
     * rolni qamraydi. Saqlashda esa {@code ContentService} media ro'yxatini
     * BUTUNLAY almashtiradi ({@code getMedia().clear()}).
     *
     * Natijada muharrir orqali tahrirlash {@code VIDEO}, {@code TRAILER},
     * {@code TEASER} va {@code THUMBNAIL} bog'lanishlarini jimgina
     * o'chirardi — ya'ni filmning asosiy videosi yo'qolardi (B17 ning
     * kengroq ko'rinishi).
     *
     * Bu ro'yxat qulaylik maydonlarini ALMASHTIRMAYDI, ular yonida turadi.
     * Yangi rol qo'shilsa u avtomatik shu yerga tushadi va DTO'ni
     * yangilash esdan chiqmaydi.
     */
    private List<MediaLinkDto> media;

    public static ContentListDto from(Content c) {
        Map<Locale, TranslationDto> tr = new LinkedHashMap<>();
        for (ContentTranslation t : c.getTranslations()) {
            tr.put(t.getLocale(), TranslationDto.builder()
                    .title(t.getTitle())
                    .shortDescription(t.getShortDescription())
                    .description(t.getDescription())
                    .build());
        }

        Long defaultPoster = null;
        Long cover = null;
        Map<Locale, Long> localePosters = new LinkedHashMap<>();
        List<ContentMedia> galleryLinks = new ArrayList<>();

        for (ContentMedia m : c.getMedia()) {
            if (m.getMedia() == null) {
                continue;
            }
            if (m.getRole() == MediaRole.POSTER) {
                if (m.getLocale() == null) {
                    defaultPoster = m.getMedia().getId();
                } else {
                    localePosters.put(m.getLocale(), m.getMedia().getId());
                }
            } else if (m.getRole() == MediaRole.COVER) {
                cover = m.getMedia().getId();
            } else if (m.getRole() == MediaRole.GALLERY) {
                galleryLinks.add(m);
            }
        }

        // Barcha bog'lanishlar - rol va tartib bilan.
        List<MediaLinkDto> allMedia = c.getMedia().stream()
                .filter(m -> m.getMedia() != null)
                .sorted(Comparator
                        .comparing((ContentMedia m) -> m.getRole().name())
                        .thenComparing(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()))
                .map(m -> MediaLinkDto.builder()
                        .role(m.getRole())
                        .locale(m.getLocale())
                        .mediaId(m.getMedia().getId())
                        .sortOrder(m.getSortOrder())
                        .build())
                .toList();

        // Galereya tartibi muhim - admin uni ataylab joylashtirgan.
        galleryLinks.sort(Comparator.comparing(
                m -> m.getSortOrder() == null ? 0 : m.getSortOrder()));
        List<Long> gallery = galleryLinks.stream().map(m -> m.getMedia().getId()).toList();

        return ContentListDto.builder()
                .id(c.getId())
                .version(c.getVersion())
                .slug(c.getSlug())
                .contentType(c.getContentType())
                .structureType(c.getStructureType())
                .orientation(c.getOrientation())
                .status(c.getStatus())
                .visibility(c.getVisibility())
                .language(c.getLanguage())
                .accessPolicy(c.getAccessPolicy())
                .premierePrice(c.getPremierePrice())
                .categorySlug(c.getCategory() == null ? null : c.getCategory().getSlug())
                .categoryId(c.getCategory() == null ? null : c.getCategory().getId())
                .ageRating(c.getAgeRating())
                .featured(c.getFeatured())
                .popular(c.getPopular())
                .viewCount(c.getViewCount())
                .publicationDate(c.getPublicationDate())
                .premiereDate(c.getPremiereDate())
                .translations(tr)
                .posterMediaId(defaultPoster)
                .localePosters(localePosters)
                .coverMediaId(cover)
                .gallery(gallery)
                .media(allMedia)
                .build();
    }

    /** Bitta media bog'lanishi — saqlashdagi {@code MediaLink} bilan bir xil shakl. */
    @Data
    @Builder
    public static class MediaLinkDto {
        private MediaRole role;
        /** {@code null} = barcha tillar uchun. */
        private Locale locale;
        private Long mediaId;
        private Integer sortOrder;
    }
}
