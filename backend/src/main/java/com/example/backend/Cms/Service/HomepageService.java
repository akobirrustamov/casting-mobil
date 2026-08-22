package com.example.backend.Cms.Service;

import com.example.backend.Admin.Dto.*;
import com.example.backend.Cms.Entity.*;
import com.example.backend.Cms.Enums.HomepageSectionType;
import com.example.backend.Cms.Enums.CreatorRanking;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Repository.*;
import com.example.backend.Entity.User;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Bosh sahifa: bo'limlar, reklama bannerlari va premyera kartochkalari.
 *
 * Uchtasi bir servisda, chunki ular bitta ekranni tashkil qiladi va bir xil
 * naqshlarga ega: vaqt oynasi, tartib, uch tilli matn, umumiy havola.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomepageService {

    private final AdvertisementRepo advertisementRepo;
    private final PremiereRepo premiereRepo;
    private final HomepageSectionRepo homepageSectionRepo;
    private final MediaAssetRepo mediaAssetRepo;
    private final ContentRepo contentRepo;
    private final AuditService auditService;
    private final CreatorRepo creatorRepo;
    private final SettingsService settingsService;
    private final HomepageSectionItemRepo sectionItemRepo;
    private final InternalLinkValidator linkValidator;

    // ---------------------------------------------------------------- reklama

    @Transactional(readOnly = true)
    public List<Advertisement> advertisements() {
        // Arxivlangan reklama — «o'chirilgan» degani (§58 soft delete).
        // Yozuv hisobotlar uchun saqlanadi, lekin panel ro'yxatini to'ldirmaydi.
        return advertisementRepo.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(a -> a.getStatus() != PublicationStatus.ARCHIVED)
                .toList();
    }

    @Transactional
    public Advertisement saveAdvertisement(User actor, Long id, AdvertisementSaveRequest request) {
        validateWindow(request.getStartAt(), request.getEndAt());
        validateLink(request.getLink());

        Advertisement ad = id == null ? new Advertisement()
                : advertisementRepo.findById(id)
                        .orElseThrow(() -> BusinessException.notFound("Advertisement", id));

        // Controller darajasida @NotBlank bor, lekin servis to'g'ridan-to'g'ri
        // chaqirilsa NPE chiqardi. Tushunarli xabar arzonroq.
        if (request.getName() == null || request.getName().isBlank()) {
            throw BusinessException.validation("Ichki nom kiritilmagan");
        }
        ad.setName(request.getName().trim());
        ad.setImage(mediaOrNull(request.getImageMediaId()));
        ad.setMobileImage(mediaOrNull(request.getMobileImageMediaId()));
        ad.setButtonEnabled(Boolean.TRUE.equals(request.getButtonEnabled()));
        ad.setLink(request.getLink() == null ? new InternalLink() : request.getLink().toEntity());
        ad.setAudience(request.getAudience());
        ad.setStatus(request.getStatus());
        ad.setStartAt(request.getStartAt());
        ad.setEndAt(request.getEndAt());
        ad.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        if (id == null) {
            ad.setCreatedBy(actor == null ? null : actor.getId());
        }

        // Joyida yangilanadi - clear()+add UNIQUE(ad, locale) ni buzadi
        // Reklama matni ixtiyoriy — faqat rasmdan iborat banner ham to'g'ri.
        // Lekin matn yozilgan bo'lsa, u uchala tilda ham bo'lsin: aks holda
        // rus tilidagi foydalanuvchi o'zbekcha bannerni ko'rardi.
        boolean adVisible = isUserVisible(request.getStatus());
        TranslationRules.requireAllIfAny(request.getTranslations(),
                AdvertisementDto.AdTextDto::getTitle, "Reklama sarlavhasi", adVisible);
        if (Boolean.TRUE.equals(request.getButtonEnabled())) {
            TranslationRules.requireAllIfAny(request.getTranslations(),
                    AdvertisementDto.AdTextDto::getButtonText, "Tugma matni", adVisible);
        }

        Map<Locale, AdvertisementTranslation> existing = new HashMap<>();
        ad.getTranslations().forEach(t -> existing.put(t.getLocale(), t));
        Set<Locale> keep = new HashSet<>();
        request.getTranslations().forEach((locale, dto) -> {
            if (dto == null) {
                return;
            }
            boolean empty = isBlank(dto.getTitle()) && isBlank(dto.getDescription())
                    && isBlank(dto.getButtonText());
            if (empty) {
                return;
            }
            keep.add(locale);
            AdvertisementTranslation row = existing.get(locale);
            if (row == null) {
                row = AdvertisementTranslation.builder().locale(locale).build();
                ad.addTranslation(row);
            }
            row.setTitle(dto.getTitle());
            row.setDescription(dto.getDescription());
            row.setButtonText(dto.getButtonText());
        });
        ad.getTranslations().removeIf(t -> !keep.contains(t.getLocale()));

        Advertisement saved = advertisementRepo.save(ad);
        auditService.log(actor,
                id == null ? AuditAction.ADVERTISEMENT_CREATED : AuditAction.ADVERTISEMENT_UPDATED,
                "Advertisement", saved.getId(), null,
                Map.of("name", saved.getName(), "audience", saved.getAudience(),
                        "status", saved.getStatus()));
        return saved;
    }

    @Transactional
    public void deleteAdvertisement(User actor, Long id) {
        Advertisement ad = advertisementRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Advertisement", id));
        // Hard delete emas: reklama statistikasi (ko'rsatish/bosish) shu id'ga
        // bog'langan, yozuv yo'qolsa hisobotda egasiz raqamlar qolardi.
        ad.setStatus(PublicationStatus.ARCHIVED);
        advertisementRepo.save(ad);
        auditService.log(actor, AuditAction.ADVERTISEMENT_ARCHIVED, "Advertisement", id);
    }

    // --------------------------------------------------------------- premyera

    @Transactional(readOnly = true)
    public List<Premiere> premieres() {
        return premiereRepo.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(p -> p.getStatus() != PublicationStatus.ARCHIVED)
                .toList();
    }

    @Transactional
    public Premiere savePremiere(User actor, Long id, PremiereSaveRequest request) {
        validateWindow(request.getStartAt(), request.getEndAt());
        validateLink(request.getLink());

        // ⚠️ ТЗ §30: kartochkadagi BARCHA matnlar uch tilda bo'lishi kerak.
        //
        // Kartochka bosh sahifada har bir foydalanuvchiga ko'rinadi va u
        // uch qatordan iborat:
        //
        //     Qalbing egasi        <- title
        //     Tez kunda            <- subtitle (ТЗ dagi "text")
        //     Treylerni ko'rish    <- description / tugma matni
        //
        // Bittasi tarjimasiz qolsa, rus tilidagi ekranda o'zbekcha qator
        // turardi — ya'ni kartochka yarim tarjima bo'lib chiqardi.
        boolean visible = isUserVisible(request.getStatus());

        // Sarlavha MAJBURIY: usiz kartochkani umuman chizib bo'lmaydi.
        TranslationRules.require(request.getTranslations(),
                PremiereDto.PremiereTextDto::getTitle, "Premyera sarlavhasi", visible);

        // Qolgan matnlar IXTIYORIY — kartochka faqat sarlavhadan iborat
        // bo'lishi ham mumkin. Lekin bittasi to'ldirila boshlagan bo'lsa,
        // uchala tilda ham to'ldirilsin.
        TranslationRules.requireAllIfAny(request.getTranslations(),
                PremiereDto.PremiereTextDto::getSubtitle, "Ustki matn", visible);
        TranslationRules.requireAllIfAny(request.getTranslations(),
                PremiereDto.PremiereTextDto::getDescription, "Tavsif", visible);

        if (Boolean.TRUE.equals(request.getButtonEnabled())) {
            // Tugma yoqilgan bo'lsa uning matni ham tarjima qilinsin —
            // aks holda rus tilidagi ekranda o'zbekcha tugma turardi.
            TranslationRules.requireAllIfAny(request.getTranslations(),
                    PremiereDto.PremiereTextDto::getButtonText, "Tugma matni", visible);
        }

        Premiere p = id == null ? new Premiere()
                : premiereRepo.findById(id)
                        .orElseThrow(() -> BusinessException.notFound("Premiere", id));

        p.setName(request.getName().trim());
        p.setImage(mediaOrNull(request.getImageMediaId()));
        p.setVideo(mediaOrNull(request.getVideoMediaId()));
        p.setContent(request.getContentId() == null ? null
                : contentRepo.findById(request.getContentId())
                        .orElseThrow(() -> BusinessException.notFound("Content", request.getContentId())));
        p.setButtonEnabled(Boolean.TRUE.equals(request.getButtonEnabled()));
        p.setLink(request.getLink() == null ? new InternalLink() : request.getLink().toEntity());
        p.setStatus(request.getStatus());
        p.setStartAt(request.getStartAt());
        p.setEndAt(request.getEndAt());
        p.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        if (id == null) {
            p.setCreatedBy(actor == null ? null : actor.getId());
        }

        // ⚠️ Sarlavhasiz til qatori JIMGINA TASHLANARDI.
        //
        // Admin rus tabida "Tez kunda" va tavsifni yozib, sarlavhani
        // to'ldirmasa — pastdagi sikl butun qatorni o'tkazib yuborardi va
        // yozilgan matn izsiz yo'qolardi. Saqlash muvaffaqiyatli
        // ko'rinardi, ma'lumot esa yo'q edi.
        //
        // Sarlavhani NULL qilib saqlab ham bo'lmaydi — ustun `not null`.
        // Shuning uchun jimgina yo'qotish o'rniga aniq xato.
        request.getTranslations().forEach((locale, dto) -> {
            if (dto == null || !isBlank(dto.getTitle())) {
                return;
            }
            boolean hasOtherText = !isBlank(dto.getSubtitle())
                    || !isBlank(dto.getDescription())
                    || !isBlank(dto.getButtonText());
            if (hasOtherText) {
                throw BusinessException.validation(locale.name()
                        + " tilida matn kiritilgan, lekin sarlavha bo'sh. "
                        + "Sarlavha kartochkaning asosi — usiz qolgan matnlar saqlanmaydi");
            }
        });

        Map<Locale, PremiereTranslation> existing = new HashMap<>();
        p.getTranslations().forEach(t -> existing.put(t.getLocale(), t));
        Set<Locale> keep = new HashSet<>();
        request.getTranslations().forEach((locale, dto) -> {
            if (dto == null || isBlank(dto.getTitle())) {
                return;
            }
            keep.add(locale);
            PremiereTranslation row = existing.get(locale);
            if (row == null) {
                row = PremiereTranslation.builder().locale(locale).build();
                p.addTranslation(row);
            }
            row.setTitle(dto.getTitle().trim());
            row.setSubtitle(dto.getSubtitle());
            row.setDescription(dto.getDescription());
            row.setButtonText(dto.getButtonText());
        });
        p.getTranslations().removeIf(t -> !keep.contains(t.getLocale()));

        Premiere saved = premiereRepo.save(p);
        auditService.log(actor, id == null ? AuditAction.PREMIERE_CREATED : AuditAction.PREMIERE_UPDATED,
                "Premiere", saved.getId(), null,
                Map.of("name", saved.getName(), "status", saved.getStatus()));
        return saved;
    }

    @Transactional
    public void deletePremiere(User actor, Long id) {
        Premiere p = premiereRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Premiere", id));
        // Premyeraga PurchaseType.PREMIERE xaridlari bog'lanadi — yozuv
        // o'chsa, to'lov tarixi nima uchun to'langanini ko'rsatolmay qolardi.
        p.setStatus(PublicationStatus.ARCHIVED);
        premiereRepo.save(p);
        auditService.log(actor, AuditAction.PREMIERE_ARCHIVED, "Premiere", id);
    }

    // ------------------------------------------------------------ bosh sahifa

    /**
     * Bo'limlar ro'yxati. Yangi tur qo'shilgan bo'lsa avtomatik yaratiladi —
     * shunda enum'ga qiymat qo'shish uchun migration yozish shart emas.
     */
    @Transactional
    public List<HomepageSection> sections() {
        List<HomepageSection> existing = homepageSectionRepo.findAllByOrderBySortOrderAscIdAsc();
        Set<HomepageSectionType> known = new HashSet<>();
        existing.forEach(s -> known.add(s.getType()));

        List<HomepageSection> created = new ArrayList<>();
        int order = existing.size();
        for (HomepageSectionType type : HomepageSectionType.values()) {
            if (known.contains(type)) {
                continue;
            }
            HomepageSection s = HomepageSection.builder()
                    .type(type)
                    // "Mashhur ijodkorlar" eng pastda - buyurtmachi talabi
                    .sortOrder(type == HomepageSectionType.POPULAR_CREATORS ? 999 : order++)
                    .enabled(true)
                    .build();
            String[] titles = defaultTitles(type);
            Locale[] locales = {Locale.UZ, Locale.RU, Locale.EN};
            for (int i = 0; i < locales.length; i++) {
                s.addTranslation(HomepageSectionTranslation.builder()
                        .locale(locales[i])
                        .title(titles[i])
                        .build());
            }
            created.add(homepageSectionRepo.save(s));
        }
        return created.isEmpty() ? existing
                : homepageSectionRepo.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional
    public HomepageSection saveSection(User actor, Long id, HomepageSectionSaveRequest request) {
        HomepageSection s = homepageSectionRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("HomepageSection", id));

        s.setEnabled(!Boolean.FALSE.equals(request.getEnabled()));
        s.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        s.setItemLimit(request.getItemLimit());

        Map<Locale, HomepageSectionTranslation> existing = new HashMap<>();
        s.getTranslations().forEach(t -> existing.put(t.getLocale(), t));
        Set<Locale> keep = new HashSet<>();
        request.getTranslations().forEach((locale, dto) -> {
            if (dto == null || isBlank(dto.getTitle())) {
                return;
            }
            keep.add(locale);
            HomepageSectionTranslation row = existing.get(locale);
            if (row == null) {
                row = HomepageSectionTranslation.builder().locale(locale).build();
                s.addTranslation(row);
            }
            row.setTitle(dto.getTitle().trim());
        });
        s.getTranslations().removeIf(t -> !keep.contains(t.getLocale()));

        HomepageSection saved = homepageSectionRepo.save(s);
        auditService.log(actor, AuditAction.HOMEPAGE_SECTION_UPDATED, "HomepageSection", id, null,
                Map.of("type", saved.getType(), "enabled", saved.getEnabled(),
                        "sortOrder", saved.getSortOrder()));
        return saved;
    }

    // ------------------------------------------------------------------ ichki

    /** Vaqt oynasi mantiqan to'g'ri bo'lishi kerak. */
    private void validateWindow(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw BusinessException.validation("Tugash sanasi boshlanish sanasidan oldin bo'lishi mumkin emas");
        }
    }

    /**
     * Havolani tekshirish {@link InternalLinkValidator} ga topshiriladi.
     *
     * Ilgari bu yerda faqat maydonlar bo'sh emasligi tekshirilardi — nishon
     * bazada bor-yo'qligi esa umuman tekshirilmasdi. Endi mavjudlik ham
     * tekshiriladi va bir xil qoida bildirishnoma modulida ham ishlaydi (§28).
     */
    private void validateLink(InternalLinkDto link) {
        linkValidator.validate(link);
    }

    /**
     * SCHEDULED ham kiradi — u belgilangan vaqtda avtomatik PUBLISHED
     * bo'ladi va o'shanda tarjima yo'qligi bilinardi, kech bo'lardi.
     */
    private boolean isUserVisible(PublicationStatus status) {
        return status == PublicationStatus.PUBLISHED
                || status == PublicationStatus.SCHEDULED;
    }

    /**
     * Qatorga kiradigan kontent ro'yxati (ТЗ §31).
     *
     * <h2>Nima uchun to'liq almashtirish</h2>
     * Admin panelida qator sudrab qayta tartiblanadi va elementlar
     * qo'shilib-olib tashlanadi. Har bir harakat uchun alohida endpoint
     * o'rniga bitta «yakuniy ro'yxat» yuboriladi — shunda panel va baza
     * bir-biridan chetga chiqmaydi.
     *
     * <h2>Nima uchun ro'yxat tartibi = ko'rinish tartibi</h2>
     * Alohida {@code sortOrder} so'ralsa, panel uni o'zi hisoblab yuborishi
     * kerak bo'lardi va bitta xato raqam qatorni aralashtirib yuborardi.
     */
    @Transactional
    public List<HomepageSectionItem> replaceSectionItems(User actor, Long sectionId,
                                                         List<Long> contentIds) {
        HomepageSection section = homepageSectionRepo.findById(sectionId)
                .orElseThrow(() -> BusinessException.notFound("HomepageSection", sectionId));

        List<Long> ids = contentIds == null ? List.of() : contentIds;

        // Takror ID qatorda bir filmni ikki marta ko'rsatardi va
        // uk_homepage_item ni buzardi - xato baza darajasida emas, shu
        // yerda tushunarli xabar bilan chiqsin.
        Set<Long> seen = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && !seen.add(id)) {
                throw BusinessException.validation("Kontent qatorda takrorlangan: #" + id);
            }
        }

        Map<Long, HomepageSectionItem> existing = new HashMap<>();
        sectionItemRepo.findForSection(sectionId)
                .forEach(i -> existing.put(i.getContent().getId(), i));

        int order = 0;
        List<HomepageSectionItem> result = new ArrayList<>();
        for (Long contentId : seen) {
            HomepageSectionItem item = existing.remove(contentId);
            if (item == null) {
                Content content = contentRepo.findById(contentId)
                        .orElseThrow(() -> BusinessException.notFound("Content", contentId));
                item = HomepageSectionItem.builder()
                        .section(section)
                        .content(content)
                        .build();
            }
            item.setSortOrder(order++);
            result.add(sectionItemRepo.save(item));
        }
        // Ro'yxatdan tushib qolganlar olib tashlanadi. O'chirish faqat
        // BOG'LANISHNI yo'q qiladi - kontentning o'ziga tegmaydi.
        existing.values().forEach(sectionItemRepo::delete);

        auditService.log(actor, AuditAction.HOMEPAGE_SECTION_ITEMS_UPDATED,
                "HomepageSection", sectionId, null,
                Map.of("type", section.getType(), "count", result.size()));
        return result;
    }

    /**
     * Bo'limlar tartibini BITTA tranzaksiyada o'rnatadi (ТЗ §31).
     *
     * <h2>Nima uchun kerak</h2>
     * Tartibni bittalab o'zgartirish oraliq holat yaratadi: admin
     * bo'limni yuqoriga sudrasa, panel 8 ta so'rov yuboradi va ular
     * orasida ikkita bo'lim bir xil raqamda turadi. O'sha lahzada
     * {@code /app/home} ni so'ragan foydalanuvchi aralashib ketgan bosh
     * sahifani ko'rardi.
     *
     * <h2>Ro'yxatga kirmagan bo'limlar</h2>
     * Tartibi o'zgarmaydi va ular ro'yxatdagilardan KEYIN turadi. Shunda
     * panel faqat ko'rinib turgan bo'limlarni yuborsa ham, qolganlari
     * yo'qolib qolmaydi.
     */
    @Transactional
    public List<HomepageSection> reorderSections(User actor, List<Long> sectionIds) {
        List<Long> ids = sectionIds == null ? List.of() : sectionIds;

        // Takror ID tartibni aniqsiz qilardi: bitta bo'lim ikkita
        // raqamga da'vogar bo'lardi.
        Set<Long> seen = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && !seen.add(id)) {
                throw BusinessException.validation("Bo'lim ro'yxatda takrorlangan: #" + id);
            }
        }

        int order = 0;
        for (Long id : seen) {
            HomepageSection section = homepageSectionRepo.findById(id)
                    .orElseThrow(() -> BusinessException.notFound("HomepageSection", id));
            section.setSortOrder(order++);
            homepageSectionRepo.save(section);
        }

        // Ro'yxatga kirmaganlar oxiriga suriladi — tartibi o'zaro saqlanadi.
        int tail = order;
        for (HomepageSection rest : homepageSectionRepo.findAllByOrderBySortOrderAscIdAsc()) {
            if (!seen.contains(rest.getId())) {
                rest.setSortOrder(tail++);
                homepageSectionRepo.save(rest);
            }
        }

        auditService.log(actor, AuditAction.HOMEPAGE_SECTIONS_REORDERED, "HomepageSection", null, null,
                Map.of("count", seen.size()));
        return homepageSectionRepo.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<HomepageSectionItem> sectionItems(Long sectionId) {
        return sectionItemRepo.findForSection(sectionId);
    }

    private MediaAsset mediaOrNull(Long id) {
        return id == null ? null : mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Yangi bo'lim yaratilganda o'zbekcha sarlavha - admin keyin tahrirlaydi. */
    /**
     * Bo'lim sarlavhasi — UCHALA tilda.
     *
     * ⚠️ Ilgari faqat o'zbekcha yaratilardi. Bo'lim sarlavhasi mobil ilova
     * bosh sahifasida chiqadi, ya'ni rus va ingliz tilidagi foydalanuvchi
     * o'zbekcha matn ko'rardi yoki bo'sh katak olardi.
     *
     * @return {@code [UZ, RU, EN]}
     */
    /**
     * «Mashhur ijodkorlar» bo'limi uchun ijodkorlar (ТЗ §25).
     *
     * <h2>Nega alohida metod</h2>
     * Bu — bo'lim tartibi HAL QILINADIGAN YAGONA joy. Ilgari
     * {@code CreatorRepo.findAllByFeaturedTrueAndActiveTrueOrderBySortOrderAsc}
     * mavjud edi, lekin uni HECH KIM chaqirmasdi: bo'lim sozlama satri
     * sifatida bor edi, ma'lumot manbai esa yo'q.
     *
     * <h2>Analitikaga tayyorlik</h2>
     * ТЗ: «Hozir manual featured/sort yetarli, ammo arxitektura analytics
     * rankingga mos bo'lsin.» Tartib {@code homepage.creators.ranking}
     * sozlamasidan o'qiladi va admin uni kod o'zgartirmasdan almashtiradi.
     * Yangi strategiya qo'shish — shu yerga bitta {@code case}.
     *
     * @param limit nechta ijodkor kerak; {@code null} yoki 0 bo'lsa 12 ta
     */
    @Transactional(readOnly = true)
    public List<Creator> featuredCreators(Integer limit) {
        int max = (limit == null || limit <= 0) ? 12 : limit;
        CreatorRanking ranking = rankingStrategy();

        List<Creator> ordered = switch (ranking) {
            case MANUAL -> creatorRepo.findAllByFeaturedTrueAndActiveTrueOrderBySortOrderAsc();
            case STARS -> creatorRepo.findAllByActiveTrueOrderByStarsReceivedDescIdAsc();
        };

        return ordered.size() > max ? ordered.subList(0, max) : ordered;
    }

    /**
     * Sozlamadagi tartib. Noma'lum qiymat bo'lsa MANUAL — bosh sahifa
     * buzuq sozlama tufayli bo'sh qolmasin.
     */
    private CreatorRanking rankingStrategy() {
        String value = settingsService.get(SettingKeys.CREATOR_RANKING);
        if (value == null || value.isBlank()) {
            return CreatorRanking.MANUAL;
        }
        try {
            CreatorRanking ranking = CreatorRanking.valueOf(value.trim().toUpperCase());
            if (ranking == CreatorRanking.STARS && !starsAreCollected()) {
                // Ochiq aytamiz: tanlov qabul qilindi, lekin signal bo'sh.
                // Jim qolsak tartib tasodifiy chiqib, "analitika ishlayapti"
                // degan noto'g'ri taassurot qolardi.
                log.warn("Ijodkor reytingi STARS ga qo'yilgan, lekin hech kimda "
                        + "Stars yo'q — tartib amalda tasodifiy bo'ladi. "
                        + "Donat oqimi ulanmaguncha MANUAL tavsiya etiladi.");
            }
            return ranking;
        } catch (IllegalArgumentException e) {
            log.warn("Noma'lum ijodkor reytingi '{}', MANUAL ishlatiladi", value);
            return CreatorRanking.MANUAL;
        }
    }

    /** Kamida bitta ijodkorda Stars bormi. */
    private boolean starsAreCollected() {
        return creatorRepo.findAllByActiveTrueOrderByStarsReceivedDescIdAsc().stream()
                .findFirst()
                .map(c -> c.getStarsReceived() != null && c.getStarsReceived() > 0)
                .orElse(false);
    }

    private String[] defaultTitles(HomepageSectionType type) {
        return switch (type) {
            case ADVERTISEMENT_CAROUSEL -> new String[]{"Reklama", "Реклама", "Advertising"};
            case NEW_PREMIERES -> new String[]{"Yangi premyeralar", "Новые премьеры", "New premieres"};
            case CATEGORIES -> new String[]{"Kategoriyalar", "Категории", "Categories"};
            case MINI_SERIES -> new String[]{"Mini seriallar", "Мини-сериалы", "Mini series"};
            case REELS_SERIES -> new String[]{"Reels seriallar", "Reels-сериалы", "Reels series"};
            case PODCASTS -> new String[]{"Podkastlar", "Подкасты", "Podcasts"};
            case SHOWS -> new String[]{"Shoular", "Шоу", "Shows"};
            case STREAMS -> new String[]{"Streamlar", "Стримы", "Streams"};
            case CLIPS -> new String[]{"Kliplar", "Клипы", "Clips"};
            case FEATURED_CONTENT -> new String[]{"Tanlangan", "Избранное", "Featured"};
            case POPULAR_CONTENT -> new String[]{"Mashhur", "Популярное", "Popular"};
            case POPULAR_CREATORS -> new String[]{"Mashhur ijodkorlar", "Популярные авторы", "Popular creators"};
            case CUSTOM_ROW -> new String[]{"Maxsus qator", "Особый ряд", "Custom row"};
        };
    }
}
