package com.example.backend.Cms.Service;

import com.example.backend.Cms.Dto.HomeFeedDto;
import com.example.backend.Cms.Entity.*;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Repository.*;
import com.example.backend.Entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Mobil ilova bosh sahifasini quradi (ТЗ §31).
 *
 * <h2>Nima uchun kerak</h2>
 * ТЗ: «Mobil app bosh sahifani backenddan oladi, homepage hardcoded
 * bo'lmasin.» Bu endpoint qo'shilgunga qadar bosh sahifani boshqarish
 * FAQAT admin panelida bor edi — ya'ni admin bo'limlarni sozlay olardi,
 * lekin ilova bu sozlamalarni so'raydigan joy yo'q edi.
 *
 * <h2>Bo'sh bo'lim ko'rsatilmaydi</h2>
 * Elementi yo'q bo'lim javobga umuman kirmaydi. Aks holda ilova bo'sh
 * sarlavha chizib qo'yardi. Soxta element o'ylab topilmaydi — ma'lumot
 * yo'q bo'lsa, bo'lim yo'q.
 */
@Service
@RequiredArgsConstructor
public class HomeFeedService {

    /** Bitta qatorda nechta element — bo'limda o'z chegarasi bo'lmasa. */
    private static final int DEFAULT_LIMIT = 20;

    private final HomepageSectionRepo sectionRepo;
    private final HomepageSectionItemRepo sectionItemRepo;
    private final AdvertisementRepo advertisementRepo;
    private final PremiereRepo premiereRepo;
    private final CategoryRepo categoryRepo;
    private final ContentRepo contentRepo;
    private final AccessService accessService;
    private final HomepageService homepageService;

    @Transactional(readOnly = true)
    public HomeFeedDto build(User user, Locale locale) {
        Locale lang = locale == null ? Locale.UZ : locale;
        boolean showAds = accessService.shouldShowAds(user);
        LocalDateTime now = LocalDateTime.now();

        List<HomeFeedDto.Section> sections = new ArrayList<>();
        for (HomepageSection section : sectionRepo.findAllByEnabledTrueOrderBySortOrderAscIdAsc()) {
            HomeFeedDto.Section built = section(section, lang, user, showAds, now);
            if (built != null) {
                sections.add(built);
            }
        }

        return HomeFeedDto.builder()
                .locale(lang)
                .showAds(showAds)
                .sections(sections)
                .build();
    }

    // ------------------------------------------------------------ bo'limlar

    private HomeFeedDto.Section section(HomepageSection section, Locale lang, User user,
                                        boolean showAds, LocalDateTime now) {
        int limit = section.getItemLimit() == null || section.getItemLimit() <= 0
                ? DEFAULT_LIMIT : section.getItemLimit();

        HomeFeedDto.Section.SectionBuilder b = HomeFeedDto.Section.builder()
                .id(section.getId())
                .type(section.getType())
                .sortOrder(section.getSortOrder())
                .title(sectionTitle(section, lang));

        boolean empty;
        switch (section.getType()) {
            case ADVERTISEMENT_CAROUSEL -> {
                List<HomeFeedDto.BannerCard> ads = ads(lang, showAds, now, limit);
                b.banners(ads);
                empty = ads.isEmpty();
            }
            case NEW_PREMIERES -> {
                List<HomeFeedDto.BannerCard> items = premieres(lang, now, limit);
                b.banners(items);
                empty = items.isEmpty();
            }
            case CATEGORIES -> {
                List<HomeFeedDto.CategoryCard> items = categories(lang, limit);
                b.categories(items);
                empty = items.isEmpty();
            }
            case POPULAR_CREATORS -> {
                List<HomeFeedDto.CreatorCard> items = creators(lang, limit);
                b.creators(items);
                empty = items.isEmpty();
            }
            case CUSTOM_ROW -> {
                List<HomeFeedDto.ContentCard> items = curated(section, lang, user, limit);
                b.content(items);
                empty = items.isEmpty();
            }
            default -> {
                List<HomeFeedDto.ContentCard> items = contentFor(section.getType(), lang, user, limit);
                b.content(items);
                empty = items.isEmpty();
            }
        }
        // Bo'sh bo'lim klientda sarlavhasi bor, ichi yo'q qator bo'lib
        // chiqardi. Shuning uchun umuman qaytarilmaydi.
        return empty ? null : b.build();
    }

    private List<HomeFeedDto.BannerCard> ads(Locale lang, boolean showAds,
                                             LocalDateTime now, int limit) {
        return advertisementRepo.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(a -> a.isLiveAt(now))
                // Tijorat reklamasi faol obunasi borlarga ko'rsatilmaydi
                // («Premium — reklamasiz tomosha»), admin e'loni esa hammaga.
                .filter(a -> showAds || a.getAudience() == AdAudience.ADMIN_ANNOUNCEMENT)
                .limit(limit)
                .map(a -> adCard(a, lang))
                .toList();
    }

    private List<HomeFeedDto.BannerCard> premieres(Locale lang, LocalDateTime now, int limit) {
        return premiereRepo.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(p -> p.isLiveAt(now))
                .limit(limit)
                .map(p -> premiereCard(p, lang))
                .toList();
    }

    private List<HomeFeedDto.CategoryCard> categories(Locale lang, int limit) {
        return categoryRepo.findAllByActiveTrueOrderBySortOrderAsc().stream()
                .limit(limit)
                .map(c -> HomeFeedDto.CategoryCard.builder()
                        .id(c.getId())
                        .slug(c.getSlug())
                        .name(pick(c.getTranslations(), lang,
                                CategoryTranslation::getLocale, CategoryTranslation::getName))
                        .iconMediaId(c.getIcon() == null ? null : c.getIcon().getId())
                        .build())
                .toList();
    }

    private List<HomeFeedDto.CreatorCard> creators(Locale lang, int limit) {
        return homepageService.featuredCreators(limit).stream()
                .map(c -> HomeFeedDto.CreatorCard.builder()
                        .id(c.getId())
                        .slug(c.getSlug())
                        .displayName(pick(c.getTranslations(), lang,
                                CreatorTranslation::getLocale, CreatorModuleNames::displayName))
                        .photoMediaId(c.getPhoto() == null ? null : c.getPhoto().getId())
                        .build())
                .toList();
    }

    /** Admin qo'lda yig'gan qator (§31 — «Custom content rows»). */
    private List<HomeFeedDto.ContentCard> curated(HomepageSection section, Locale lang,
                                                  User user, int limit) {
        return sectionItemRepo.findForSection(section.getId()).stream()
                .map(HomepageSectionItem::getContent)
                .filter(c -> isVisible(c, user))
                .limit(limit)
                .map(c -> contentCard(c, lang))
                .toList();
    }

    /**
     * Kontent qatori.
     *
     * FEATURED va POPULAR bayroq bilan ishlaydi (§25 dagi kabi: hozir qo'lda,
     * arxitektura keyinchalik analitika reytingiga mos). Qolgan turlar esa
     * kontent TURI bo'yicha yig'iladi — kategoriya bo'yicha emas (§13:
     * tur va kategoriya bir xil narsa emas).
     */
    private List<HomeFeedDto.ContentCard> contentFor(HomepageSectionType type, Locale lang,
                                                     User user, int limit) {
        List<Content> pool = switch (type) {
            case FEATURED_CONTENT -> contentRepo.findAllByDeletedAtIsNullAndFeaturedTrueAndStatus(
                    PublicationStatus.PUBLISHED);
            case POPULAR_CONTENT -> contentRepo.findAllByDeletedAtIsNullAndPopularTrueAndStatus(
                    PublicationStatus.PUBLISHED);
            case MINI_SERIES -> byType(ContentType.MINI_SERIES);
            case REELS_SERIES -> reels();
            case PODCASTS -> byType(ContentType.PODCAST);
            case SHOWS -> byType(ContentType.SHOW);
            case STREAMS -> byType(ContentType.STREAM);
            case CLIPS -> byType(ContentType.CLIP);
            default -> List.of();
        };
        return pool.stream()
                .filter(c -> isVisible(c, user))
                .sorted(Comparator.comparing(Content::getPublicationDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(c -> contentCard(c, lang))
                .toList();
    }

    private List<Content> byType(ContentType type) {
        return contentRepo.findAllByDeletedAtIsNullAndContentTypeAndStatus(
                type, PublicationStatus.PUBLISHED);
    }

    /** Reels — tik formatdagi seriallar: alohida tur emas, YO'NALISH. */
    private List<Content> reels() {
        return contentRepo.findAllByDeletedAtIsNullAndOrientationAndStatus(
                ContentOrientation.VERTICAL, PublicationStatus.PUBLISHED);
    }

    // ------------------------------------------------------------ yordamchi

    /**
     * Katalogda ko'rinadimi.
     *
     * PRIVATE — faqat xodimlarga, UNLISTED — havola orqali, ya'ni qatorlarda
     * chiqmaydi. Nashr qilinmagan kontent ham chiqmaydi.
     */
    private boolean isVisible(Content c, User user) {
        if (c == null || c.getDeletedAt() != null) {
            return false;
        }
        if (c.getStatus() != PublicationStatus.PUBLISHED) {
            return false;
        }
        return c.getVisibility() == null || c.getVisibility() == ContentVisibility.PUBLIC;
    }

    private HomeFeedDto.ContentCard contentCard(Content c, Locale lang) {
        ContentTranslation t = translation(c, lang);
        return HomeFeedDto.ContentCard.builder()
                .id(c.getId())
                .slug(c.getSlug())
                .title(t == null ? null : t.getTitle())
                .shortDescription(t == null ? null : t.getShortDescription())
                .contentType(c.getContentType() == null ? null : c.getContentType().name())
                .orientation(c.getOrientation() == null ? null : c.getOrientation().name())
                .accessPolicy(c.getAccessPolicy() == null ? null : c.getAccessPolicy().name())
                .ageRating(c.getAgeRating())
                .posterMediaId(poster(c, lang))
                .build();
    }

    /** Til bo'yicha afisha bo'lsa o'sha, bo'lmasa umumiysi. */
    private Long poster(Content c, Locale lang) {
        Long localised = null;
        Long shared = null;
        for (ContentMedia m : c.getMedia()) {
            if (m.getRole() != MediaRole.POSTER || m.getMedia() == null) {
                continue;
            }
            if (m.getLocale() == lang) {
                localised = m.getMedia().getId();
            } else if (m.getLocale() == null) {
                shared = m.getMedia().getId();
            }
        }
        return localised != null ? localised : shared;
    }

    private ContentTranslation translation(Content c, Locale lang) {
        ContentTranslation base = null;
        for (ContentTranslation t : c.getTranslations()) {
            if (t.getLocale() == lang) {
                return t;
            }
            if (t.getLocale() == Locale.UZ) {
                base = t;
            }
        }
        return base;
    }

    private HomeFeedDto.BannerCard adCard(Advertisement a, Locale lang) {
        AdvertisementTranslation t = pickRow(a.getTranslations(), lang,
                AdvertisementTranslation::getLocale);
        return HomeFeedDto.BannerCard.builder()
                .id(a.getId())
                .title(t == null ? null : t.getTitle())
                .description(t == null ? null : t.getDescription())
                .buttonText(t == null ? null : t.getButtonText())
                .buttonEnabled(a.getButtonEnabled())
                .imageMediaId(a.getImage() == null ? null : a.getImage().getId())
                .linkType(linkType(a.getLink()))
                .linkUrl(a.getLink() == null ? null : a.getLink().getLinkUrl())
                .internalTargetType(a.getLink() == null || a.getLink().getInternalTargetType() == null
                        ? null : a.getLink().getInternalTargetType().name())
                .internalTargetId(a.getLink() == null ? null : a.getLink().getInternalTargetId())
                .build();
    }

    private HomeFeedDto.BannerCard premiereCard(Premiere p, Locale lang) {
        PremiereTranslation t = pickRow(p.getTranslations(), lang, PremiereTranslation::getLocale);
        return HomeFeedDto.BannerCard.builder()
                .id(p.getId())
                .title(t == null ? null : t.getTitle())
                .subtitle(t == null ? null : t.getSubtitle())
                .description(t == null ? null : t.getDescription())
                .buttonText(t == null ? null : t.getButtonText())
                .buttonEnabled(p.getButtonEnabled())
                .imageMediaId(p.getImage() == null ? null : p.getImage().getId())
                .videoMediaId(p.getVideo() == null ? null : p.getVideo().getId())
                .linkType(linkType(p.getLink()))
                .linkUrl(p.getLink() == null ? null : p.getLink().getLinkUrl())
                .internalTargetType(p.getLink() == null || p.getLink().getInternalTargetType() == null
                        ? null : p.getLink().getInternalTargetType().name())
                .internalTargetId(p.getLink() == null ? null : p.getLink().getInternalTargetId())
                .build();
    }

    private String linkType(InternalLink link) {
        return link == null || link.getLinkType() == null ? null : link.getLinkType().name();
    }

    private String sectionTitle(HomepageSection section, Locale lang) {
        return pick(section.getTranslations(), lang,
                HomepageSectionTranslation::getLocale, HomepageSectionTranslation::getTitle);
    }

    /**
     * So'ralgan til, bo'lmasa o'zbekcha.
     *
     * Nashr qilingan kontentda uchala til ham bo'lishi shart
     * ({@link TranslationRules}), lekin eski satrlar shu qoidadan oldin
     * yaratilgan bo'lishi mumkin — shunda bo'sh sarlavha o'rniga
     * o'zbekchasi ko'rinadi.
     */
    private <T> String pick(List<T> rows, Locale lang,
                            java.util.function.Function<T, Locale> localeOf,
                            java.util.function.Function<T, String> valueOf) {
        T row = pickRow(rows, lang, localeOf);
        return row == null ? null : valueOf.apply(row);
    }

    private <T> T pickRow(List<T> rows, Locale lang,
                          java.util.function.Function<T, Locale> localeOf) {
        T base = null;
        for (T row : rows) {
            Locale l = localeOf.apply(row);
            if (l == lang) {
                return row;
            }
            if (l == Locale.UZ) {
                base = row;
            }
        }
        return base == null && !rows.isEmpty() ? rows.get(0) : base;
    }

    /** Ijodkor ismi — tarjima qatoridan. */
    private static final class CreatorModuleNames {
        static String displayName(CreatorTranslation t) {
            if (t == null) {
                return null;
            }
            if (t.getDisplayName() != null && !t.getDisplayName().isBlank()) {
                return t.getDisplayName();
            }
            return java.util.stream.Stream.of(t.getFirstName(), t.getLastName())
                    .filter(Objects::nonNull)
                    .filter(s -> !s.isBlank())
                    .reduce((a, b) -> a + " " + b)
                    .orElse(null);
        }
    }
}
