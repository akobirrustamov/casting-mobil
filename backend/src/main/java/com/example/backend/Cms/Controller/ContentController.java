package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
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
                    .durationSeconds(e.getDurationSeconds())
                    .thumbnailMediaId(e.getThumbnail() == null ? null : e.getThumbnail().getId())
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
        private Integer durationSeconds;
        private Long thumbnailMediaId;
        /** Qismning o'z siyosati, bo'lmasa kontentniki. */
        private String accessPolicy;

        private boolean allowed;
        private String reason;
        private String requiredAction;
        /** Faqat sotib olish taklif qilinganda to'ladi. */
        private BigDecimal episodePrice;
    }
}
