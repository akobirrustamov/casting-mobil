package com.example.backend.Cms.Service;

import com.example.backend.Cms.Dto.ContentDetailDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.ContentCredit;
import com.example.backend.Cms.Entity.ContentMedia;
import com.example.backend.Cms.Entity.ContentTranslation;
import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Entity.CreatorTranslation;
import com.example.backend.Cms.Entity.Genre;
import com.example.backend.Cms.Entity.GenreTranslation;
import com.example.backend.Cms.Enums.CommentStatus;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.MediaRole;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.CommentRepo;
import com.example.backend.Cms.Repository.ContentLikeRepo;
import com.example.backend.Cms.Repository.DonationRepo;
import com.example.backend.Cms.Repository.EpisodeRepo;
import com.example.backend.Cms.Repository.SeasonRepo;
import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Ilovaning kontent sahifasi uchun kartochka (ТЗ §14, §46).
 *
 * <h2>Nima uchun {@link HomeFeedService} dan foydalanilmadi</h2>
 * U bosh sahifa QATORI uchun kartochka yasaydi: bitta afisha, bitta janr,
 * qisqa tavsif. Sahifada esa boshqasi kerak — to'liq tavsif, barcha
 * janrlar, aktyorlar ro'yxati, fon kadri va treyler. Bularni bosh sahifa
 * kartochkasiga qo'shish har bir qatordagi 20 ta filmga ortiqcha
 * so'rovlar olib kelardi.
 *
 * <h2>So'rovlar soni</h2>
 * Kontent to'plamlari (tarjimalar, media, janrlar, kreditlar) dangasa
 * yuklanadi va {@code @BatchSize} bilan belgilangan — bitta kontent uchun
 * bu bir nechta qisqa so'rov. Sanoqlar ({@code izohlar}) alohida
 * {@code count} bilan olinadi: {@code Comment} to'plami {@code Content}
 * da umuman yo'q va uni fetch qilish shart emas.
 */
@Service
@RequiredArgsConstructor
public class ContentDetailService {

    /** Maketda «Top 10 Donatchilar» — o'ntadan ortig'i ro'yxatga sig'maydi. */
    public static final int TOP_DONORS = 10;

    private final EpisodeRepo episodeRepo;
    private final SeasonRepo seasonRepo;
    private final CommentRepo commentRepo;
    private final ContentLikeRepo contentLikeRepo;
    private final DonationRepo donationRepo;
    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    public ContentDetailDto build(Content c, User viewer, Locale lang) {
        ContentTranslation t = TranslationPicker.pick(
                c.getTranslations(), lang, ContentTranslation::getLocale);

        boolean multiPart = c.getStructureType() != StructureType.SINGLE;

        return ContentDetailDto.builder()
                .id(c.getId())
                .slug(c.getSlug())
                .title(t == null ? null : t.getTitle())
                .shortDescription(t == null ? null : t.getShortDescription())
                .description(t == null ? null : t.getDescription())
                .contentType(name(c.getContentType()))
                .structureType(name(c.getStructureType()))
                .orientation(name(c.getOrientation()))
                .accessPolicy(name(c.getAccessPolicy()))
                .ageRating(c.getAgeRating())
                .year(year(c))
                .language(c.getLanguage())
                // Ko'p qismli kontentda o'z davomiyligi yo'q — u yerda
                // qismlar soni ko'rsatiladi.
                .durationSeconds(multiPart || c.getDurationMinutes() == null
                        ? null : c.getDurationMinutes() * 60)
                .episodeCount(multiPart ? publishedEpisodes(c) : null)
                .seasonCount(c.getStructureType() == StructureType.SEASONAL
                        ? publishedSeasons(c) : null)
                .posterMediaId(media(c, lang, MediaRole.POSTER))
                .coverMediaId(media(c, lang, MediaRole.COVER))
                .trailerMediaId(trailer(c, lang))
                .galleryMediaIds(gallery(c, lang))
                .genres(genres(c, lang))
                .viewCount(nz(c.getViewCount()))
                .likeCount(nz(c.getLikeCount()))
                .commentCount(commentRepo.countByContentIdAndStatus(
                        c.getId(), CommentStatus.VISIBLE))
                .starsReceived(nz(c.getStarsReceived()))
                .coinsReceived(donationRepo.sumForTarget(
                        DonationTargetType.CONTENT, c.getId(), CurrencyKind.UZCASTING_COIN))
                .liked(viewer != null
                        && contentLikeRepo.existsByContentIdAndUserId(c.getId(), viewer.getId()))
                .cast(cast(c, lang))
                .build();
    }

    /**
     * Kontentni ko'p qo'llab-quvvatlaganlar (ТЗ §42).
     *
     * <h2>Nima uchun alohida chaqiriladi</h2>
     * Reyting kartochkaga kirmaydi: u sahifaning eng pastida turadi va
     * har bir kontent ochilganda kerak emas. Ilova uni sahifa
     * ochilgandan keyin alohida so'raydi — kartochka esa darhol chiqadi.
     *
     * <h2>⚠️ Valyuta CHAQIRUVCHIDAN keladi</h2>
     * Ilovada ikkita alohida reyting bor: «Yulduzlar» va «Uzcasting».
     * Ular BIR ro'yxatga qo'shilmaydi — {@link DonationRepo#topSenders}
     * dagi sabab bilan bir xil: kurs boshqa, ma'no boshqa, va 100 tanga
     * yuborgan odam 100 yulduz yuborgandan yuqori turib qolardi.
     * {@code null} — eskicha xatti-harakat, ya'ni yulduzlar.
     */
    @Transactional(readOnly = true)
    public List<ContentDetailDto.Donor> topDonors(Long contentId, int limit, CurrencyKind kind) {
        int safe = Math.min(Math.max(limit, 1), 50);

        List<DonationRepo.SenderTotal> totals = donationRepo.topSenders(
                DonationTargetType.CONTENT, contentId,
                kind == null ? CurrencyKind.STARS : kind,
                PageRequest.of(0, safe));

        if (totals.isEmpty()) {
            return List.of();
        }

        // ⚠️ Ismlar BITTA so'rov bilan olinadi. Har qator uchun alohida
        // so'rash o'nta so'rov degani bo'lardi — reyting uchun juda qimmat.
        List<UUID> ids = totals.stream()
                .map(DonationRepo.SenderTotal::getSenderId)
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, User> users = new LinkedHashMap<>();
        for (User u : userRepo.findAllById(ids)) {
            users.put(u.getId(), u);
        }

        List<ContentDetailDto.Donor> donors = new ArrayList<>();
        int rank = 1;
        for (DonationRepo.SenderTotal row : totals) {
            User u = row.getSenderId() == null ? null : users.get(row.getSenderId());
            donors.add(ContentDetailDto.Donor.builder()
                    .rank(rank++)
                    // O'chirilgan yoki ismsiz hisob ham reytingda qoladi:
                    // yulduzlar haqiqatan yuborilgan, qatorni tashlab
                    // yuborish yig'indini yashirardi.
                    .name(u == null ? null : u.getName())
                    .avatarUrl(u == null ? null : u.getAvatarUrl())
                    .stars(row.getTotal() == null ? 0L : row.getTotal())
                    .build());
        }
        return donors;
    }

    // --------------------------------------------------------------- ichki

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    /**
     * Chiqarilgan yili.
     *
     * Premyera sanasi ustunroq: nashr sanasi — bu kontent SAYTGA qachon
     * qo'yilgani, ya'ni 1995 yilgi film ham «2026» bo'lib chiqardi.
     */
    private static Integer year(Content c) {
        LocalDateTime date = c.getPremiereDate() != null
                ? c.getPremiereDate() : c.getPublicationDate();
        return date == null ? null : date.getYear();
    }

    private int publishedEpisodes(Content c) {
        return (int) episodeRepo.findAllByContentIdOrderBySortOrderAsc(c.getId()).stream()
                .filter(e -> e.getStatus().isVisibleToUsers())
                .count();
    }

    private int publishedSeasons(Content c) {
        return (int) seasonRepo.findAllByContentIdOrderBySortOrderAsc(c.getId()).stream()
                .filter(s -> s.getStatus().isVisibleToUsers())
                .count();
    }

    /**
     * Roli bo'yicha bitta media — tilga mos bo'lsa o'sha, bo'lmasa umumiysi.
     *
     * ⚠️ Til bo'yicha tanlov shunchaki qulaylik emas: ruscha afishada
     * sarlavha ruscha yozilgan bo'ladi, va uni o'zbek tilidagi ilovada
     * ko'rsatish sahifani ikki tilli qilib qo'yardi.
     */
    private Long media(Content c, Locale lang, MediaRole role) {
        Long localised = null;
        Long shared = null;
        for (ContentMedia m : c.getMedia()) {
            if (m.getRole() != role || m.getMedia() == null) {
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

    /** Treyler, bo'lmasa tizer: ikkalasi ham «sotib olishdan oldin ko'rsa bo'ladigan» rolik. */
    private Long trailer(Content c, Locale lang) {
        Long t = media(c, lang, MediaRole.TRAILER);
        return t != null ? t : media(c, lang, MediaRole.TEASER);
    }

    /** Galereya kadrlari — admin bergan tartibda. */
    private List<Long> gallery(Content c, Locale lang) {
        return c.getMedia().stream()
                .filter(m -> m.getRole() == MediaRole.GALLERY && m.getMedia() != null)
                .filter(m -> m.getLocale() == null || m.getLocale() == lang)
                .sorted(Comparator.comparing(
                        m -> m.getSortOrder() == null ? 0 : m.getSortOrder()))
                .map(m -> m.getMedia().getId())
                .toList();
    }

    /**
     * Barcha janrlar so'ralgan tilda.
     *
     * Kartochkada bitta janr chiqadi ({@code HomeFeedService.firstGenre}) —
     * u yerda bitta qatorgina joy bor. Sahifada esa hammasi ko'rinadi:
     * maketda ular alohida teglar sifatida turadi.
     */
    private List<String> genres(Content c, Locale lang) {
        if (c.getGenres() == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Genre g : c.getGenres()) {
            String n = TranslationPicker.pickValue(g.getTranslations(), lang,
                    GenreTranslation::getLocale, GenreTranslation::getName);
            if (n != null && !n.isBlank()) {
                names.add(n);
            }
        }
        return names;
    }

    /**
     * Aktyorlar va boshqa ijodkorlar.
     *
     * ⚠️ Tartib admin belgilagani bo'yicha ({@code sortOrder}), alifbo
     * bo'yicha EMAS: bosh rol ro'yxatning boshida turishi kerak, va buni
     * faqat muharrir biladi.
     */
    private List<ContentDetailDto.CastMember> cast(Content c, Locale lang) {
        if (c.getCredits() == null) {
            return List.of();
        }
        return c.getCredits().stream()
                .filter(cr -> cr.getCreator() != null)
                .sorted(Comparator
                        .comparing((ContentCredit cr) ->
                                cr.getSortOrder() == null ? 0 : cr.getSortOrder())
                        .thenComparing(ContentCredit::getId))
                .map(cr -> {
                    Creator creator = cr.getCreator();
                    return ContentDetailDto.CastMember.builder()
                            .creatorId(creator.getId())
                            .slug(creator.getSlug())
                            .name(displayName(creator, lang))
                            .photoMediaId(creator.getPhoto() == null
                                    ? null : creator.getPhoto().getId())
                            .profession(name(cr.getProfession()))
                            .characterName(cr.getCharacterName())
                            .build();
                })
                .toList();
    }

    /** To'liq ism bo'lsa o'sha, bo'lmasa ism va familiyadan yig'iladi. */
    private String displayName(Creator creator, Locale lang) {
        CreatorTranslation t = TranslationPicker.pick(
                creator.getTranslations(), lang, CreatorTranslation::getLocale);
        if (t == null) {
            return null;
        }
        if (t.getDisplayName() != null && !t.getDisplayName().isBlank()) {
            return t.getDisplayName();
        }
        return Stream.of(t.getFirstName(), t.getLastName())
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse(null);
    }
}
