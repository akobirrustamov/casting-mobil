package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Dto.ContentDetailDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.EpisodeTranslation;
import com.example.backend.Cms.Entity.Season;
import com.example.backend.Cms.Entity.SeasonTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Repository.EpisodeRepo;
import com.example.backend.Cms.Repository.SeasonRepo;
import com.example.backend.Cms.Service.AccessDecision;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.ContentDetailService;
import com.example.backend.Cms.Service.TranslationPicker;
import com.example.backend.Entity.User;
import com.example.backend.exceptions.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Kontent qismlari — ilova uchun.
 *
 * <h2>Nima uchun kerak</h2>
 * {@code /watch/content/{id}} faqat YAXLIT kontentni ochadi, ko'p qismlisiga
 * «qaysi qism?» deb javob beradi. Ilovada esa qism identifikatorini oladigan
 * joy YO'Q edi: serial, mini-serial va podkast ochilmasdi. Bu endpoint aynan
 * shu bo'shliqni yopadi.
 *
 * <h2>Video havolalari bu yerda YO'Q</h2>
 * Ro'yxat kimga nima ochiqligini aytadi, lekin fayl manzilini bermaydi —
 * u faqat {@code /watch/{episodeId}} dan, huquq tasdiqlangandan keyin
 * chiqadi. Aks holda ro'yxatning o'zi pullik qismni berib yuborardi.
 *
 * <h2>Huquqni kim hisoblaydi</h2>
 * {@link AccessService#canWatchAll} — ya'ni ro'yxatdagi qulf va ochish
 * sahifasidagi qulf BIR xil qoidadan chiqadi (ТЗ §37).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/app/content")
public class ContentController {

    private final ContentRepo contentRepo;
    private final EpisodeRepo episodeRepo;
    private final SeasonRepo seasonRepo;
    private final AccessService accessService;
    private final ContentDetailService contentDetailService;

    /**
     * Kontent kartochkasi — ilovaning kontent sahifasi (ТЗ §14, §46).
     *
     * <h2>Nima uchun qo'shildi</h2>
     * Ilovada bu sahifa ma'lumotni bosh sahifa KESHIDAN olardi: afisha va
     * qisqa tavsifdan boshqa hech narsa yo'q edi, chunki qatorlar
     * kartochkasida boshqasi bo'lmaydi. Natijada yil, janrlar, to'liq
     * tavsif va aktyorlar — bazada bori — ilovaga umuman yetib
     * bormasdi, to'g'ridan-to'g'ri havola bilan kirilganda esa sahifa
     * afishasiz ochilardi.
     *
     * <h2>⚠️ Video havolasi bu yerda YO'Q</h2>
     * Kartochka mehmonga ham ochiq. Fayl manzili faqat {@code /watch/**}
     * dan, huquq tasdiqlangandan keyin chiqadi.
     */
    @GetMapping("/{contentId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ContentDetailDto> detail(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "UZ") Locale locale) {

        Content content = contentRepo.findById(contentId)
                .orElseThrow(() -> BusinessException.notFound("Content", contentId));

        User user = CurrentUser.getOrNull();

        // Nashr qilinmagan yoki o'chirilgan kontent umuman yo'q — «bor,
        // lekin yopiq» deyish uning mavjudligini oshkor qilardi.
        if (!accessService.isVisible(user, content)) {
            throw BusinessException.notFound("Content", contentId);
        }

        return ResponseEntity.ok(contentDetailService.build(content, user, locale));
    }

    /**
     * Kontentni ko'p qo'llab-quvvatlaganlar — maketdagi «Top Donatchilar».
     *
     * <h2>Nima uchun kartochkadan alohida</h2>
     * Reyting sahifaning eng pastida turadi va uni ko'rish uchun odam
     * hali pastga tushishi kerak. Kartochkaga qo'shilsa, har bir sahifa
     * ochilishi ortiqcha guruhlash so'roviga aylanardi.
     */
    @GetMapping("/{contentId}/donors")
    @Transactional(readOnly = true)
    public ResponseEntity<DonorListResponse> donors(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "10") int limit) {

        Content content = contentRepo.findById(contentId)
                .orElseThrow(() -> BusinessException.notFound("Content", contentId));

        if (!accessService.isVisible(CurrentUser.getOrNull(), content)) {
            throw BusinessException.notFound("Content", contentId);
        }

        return ResponseEntity.ok(DonorListResponse.builder()
                .contentId(content.getId())
                .starsReceived(content.getStarsReceived() == null
                        ? 0L : content.getStarsReceived())
                .donors(contentDetailService.topDonors(contentId, limit))
                .build());
    }

    @GetMapping("/{contentId}/episodes")
    @Transactional(readOnly = true)
    public ResponseEntity<EpisodeListResponse> episodes(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "UZ") Locale locale) {

        Content content = contentRepo.findById(contentId)
                .orElseThrow(() -> BusinessException.notFound("Content", contentId));

        User user = CurrentUser.getOrNull();

        // Nashr qilinmagan yoki o'chirilgan kontent umuman yo'q — «bor, lekin
        // yopiq» deyish uning mavjudligini oshkor qilardi.
        if (!accessService.isVisible(user, content)) {
            throw BusinessException.notFound("Content", contentId);
        }

        // Nashr qilinmagan qism ro'yxatga KIRMAYDI. Uni qulf bilan ko'rsatish
        // «tez orada 5-qism chiqadi» degan va'da bo'lardi, buni esa muharrir
        // aytmagan.
        List<Episode> visible = episodeRepo.findAllByContentIdOrderBySortOrderAsc(contentId)
                .stream()
                .filter(e -> e.getStatus().isVisibleToUsers())
                .sorted(Comparator
                        .comparing((Episode e) -> e.getSeason() == null
                                ? Integer.MIN_VALUE : e.getSeason().getSortOrder())
                        .thenComparing(Episode::getSortOrder)
                        .thenComparing(Episode::getEpisodeNumber))
                .toList();

        Map<Long, AccessDecision> decisions = accessService.canWatchAll(user, visible);

        List<EpisodeCard> cards = new ArrayList<>();
        for (Episode e : visible) {
            AccessDecision d = decisions.get(e.getId());
            cards.add(EpisodeCard.builder()
                    .id(e.getId())
                    .episodeNumber(e.getEpisodeNumber())
                    .seasonId(e.getSeason() == null ? null : e.getSeason().getId())
                    .seasonNumber(e.getSeason() == null ? null : e.getSeason().getSeasonNumber())
                    .title(title(e, locale))
                    .shortDescription(shortDescription(e, locale))
                    .durationSeconds(e.getDurationSeconds())
                    .thumbnailMediaId(e.getThumbnail() == null ? null : e.getThumbnail().getId())
                    // Eski qatorlarda null bo'lishi mumkin — nol yuboramiz:
                    // ilova «server aytmadi» ni «hech kim ko'rmagan» dan
                    // ajratadi, va bu yerda javob aniq.
                    .viewCount(e.getViewCount() == null ? 0L : e.getViewCount())
                    .accessPolicy(e.effectiveAccessPolicy().name())
                    .allowed(d != null && d.isAllowed())
                    .reason(d == null ? null : d.getReason().name())
                    .requiredAction(d == null ? null : d.getRequiredAction().name())
                    .episodePrice(d == null ? null : d.getEpisodePrice())
                    .build());
        }

        return ResponseEntity.ok(EpisodeListResponse.builder()
                .contentId(content.getId())
                .structureType(content.getStructureType().name())
                .orientation(content.getOrientation() == null
                        ? null : content.getOrientation().name())
                .seasons(seasons(content, locale))
                .episodes(cards)
                .build());
    }

    /**
     * Mavsumlar — faqat nomlari uchun.
     *
     * Qismlar tekis ro'yxatda qoladi: klient ularni {@code seasonId} bo'yicha
     * guruhlaydi. Ichma-ich tuzilma bo'lsa, mavsumsiz kontent uchun soxta
     * «0-mavsum» o'ylab topishga to'g'ri kelardi.
     */
    private List<SeasonCard> seasons(Content content, Locale locale) {
        if (content.getStructureType() != StructureType.SEASONAL) {
            return List.of();
        }
        return seasonRepo.findAllByContentIdOrderBySortOrderAsc(content.getId()).stream()
                .filter(s -> s.getStatus().isVisibleToUsers())
                .map(s -> SeasonCard.builder()
                        .id(s.getId())
                        .seasonNumber(s.getSeasonNumber())
                        .title(title(s, locale))
                        .posterMediaId(s.getPoster() == null ? null : s.getPoster().getId())
                        .build())
                .toList();
    }

    /**
     * Qism tavsifi — ro'yxatdagi uchinchi qator.
     *
     * <h2>Nima uchun QISQA tavsif</h2>
     * Ro'yxat qatoriga ikki qatorlik matn sig'adi. To'liq tavsif o'n
     * qatorlik bo'lishi mumkin va u qismning O'Z sahifasiga tegishli;
     * bu yerga qo'yilsa, ro'yxat qismlar ro'yxati bo'lishdan to'xtardi.
     */
    private String shortDescription(Episode episode, Locale locale) {
        return TranslationPicker.pickValue(episode.getTranslations(), locale,
                EpisodeTranslation::getLocale, EpisodeTranslation::getShortDescription);
    }

    /** So'ralgan til, bo'lmasa standart til, bo'lmasa bori. */
    private String title(Episode episode, Locale locale) {
        List<EpisodeTranslation> all = episode.getTranslations();
        if (all == null || all.isEmpty()) {
            return null;
        }
        return all.stream().filter(t -> t.getLocale() == locale).findFirst()
                .or(() -> all.stream().filter(t -> t.getLocale() == Locale.DEFAULT).findFirst())
                .or(() -> all.stream().findFirst())
                .map(EpisodeTranslation::getTitle)
                .orElse(null);
    }

    private String title(Season season, Locale locale) {
        List<SeasonTranslation> all = season.getTranslations();
        if (all == null || all.isEmpty()) {
            return null;
        }
        return all.stream().filter(t -> t.getLocale() == locale).findFirst()
                .or(() -> all.stream().filter(t -> t.getLocale() == Locale.DEFAULT).findFirst())
                .or(() -> all.stream().findFirst())
                .map(SeasonTranslation::getTitle)
                .orElse(null);
    }

    // ------------------------------------------------------------------- DTO

    @Data
    @Builder
    public static class DonorListResponse {
        private Long contentId;
        /**
         * Kontentga tushgan yulduzlarning UMUMIY yig'indisi.
         *
         * ⚠️ Ro'yxatdagi o'nta qatorning yig'indisidan KATTA bo'lishi
         * normal: ro'yxatda faqat eng yuqori o'rindagilar bor. Ilova
         * ikkalasini qo'shmasligi kerak.
         */
        private Long starsReceived;
        private List<ContentDetailDto.Donor> donors;
    }

    @Data
    @Builder
    public static class EpisodeListResponse {
        private Long contentId;
        /** SINGLE, EPISODIC, SEASONAL. */
        private String structureType;
        /**
         * LANDSCAPE yoki VERTICAL — ro'yxatdagi kadrchalar shakli shundan.
         *
         * Yo'nalish KONTENTniki: bitta serialning qismlari har xil
         * formatda bo'lmaydi, shuning uchun har bir qismda takrorlanmaydi.
         */
        private String orientation;
        /** Faqat SEASONAL da to'ladi. */
        private List<SeasonCard> seasons;
        private List<EpisodeCard> episodes;
    }

    @Data
    @Builder
    public static class SeasonCard {
        private Long id;
        private Integer seasonNumber;
        private String title;
        private Long posterMediaId;
    }

    @Data
    @Builder
    public static class EpisodeCard {
        private Long id;
        private Integer episodeNumber;
        private Long seasonId;
        private Integer seasonNumber;
        private String title;
        /** Ro'yxatdagi ikki qatorlik izoh. To'lig'i qismning o'z sahifasida. */
        private String shortDescription;
        private Integer durationSeconds;
        private Long thumbnailMediaId;

        /**
         * Shu QISM necha marta ochilgan.
         *
         * ⚠️ Kontentning umumiy sanog'i bilan aralashtirilmasin:
         * {@code /watch/**} javobidagi {@code viewCount} butun film yoki
         * serial haqida («buni 12 ming kishi ko'rgan»), bu esa aynan shu
         * qism haqida. Ro'yxatda kontent sanog'ini har qatorga yozish
         * barcha qismlarni bir xil ko'rsatardi.
         */
        private Long viewCount;

        /** Qismning o'z siyosati, bo'lmasa kontentniki. */
        private String accessPolicy;

        private boolean allowed;
        private String reason;
        private String requiredAction;
        /** Faqat sotib olish taklif qilinganda to'ladi. */
        private BigDecimal episodePrice;
    }
}
