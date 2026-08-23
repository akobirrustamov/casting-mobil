package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.ContentMedia;
import com.example.backend.Cms.Entity.ContentTranslation;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.EpisodeTranslation;
import com.example.backend.Cms.Entity.EpisodeVideo;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.MediaRole;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Repository.EpisodeRepo;
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

/**
 * Qismni ko'rish - klient uchun yagona kirish nuqtasi.
 *
 * <h2>Nega bitta endpoint</h2>
 * Klient "ko'ra olamanmi" va "qaysi faylni o'ynatay" degan savollarni ALOHIDA
 * so'rasa, ikkalasining orasida holat o'zgarishi mumkin (obuna tugadi, xarid
 * qaytarildi). Bu yerda qaror va manba bitta tranzaksiyada olinadi.
 *
 * <h2>Asosiy qoida</h2>
 * Ruxsat bo'lmasa {@code sources} BO'SH qaytadi. Hech qanday holatda video
 * havolasi rad javobi bilan birga yuborilmaydi - aks holda klientni chetlab
 * o'tib faylni olish mumkin bo'lardi. Fayl endpointining o'zi ham alohida
 * tekshiradi ({@code AccessService.canReadMedia}), ya'ni himoya ikki qavatli.
 *
 * <h2>Til</h2>
 * Video {@code locale} maydoni bo'sh bo'lsa - u BARCHA tillar uchun. Aks holda
 * so'ralgan til, bo'lmasa {@link Locale#DEFAULT}, bo'lmasa mavjud bo'lgani.
 */
@RestController
@RequestMapping("/api/v1/app/watch")
@RequiredArgsConstructor
public class WatchController {

    private final EpisodeRepo episodeRepo;
    private final ContentRepo contentRepo;
    private final AccessService accessService;

    @GetMapping("/{episodeId}")
    @Transactional(readOnly = true)
    public ResponseEntity<WatchResponse> watch(
            @PathVariable Long episodeId,
            @RequestParam(defaultValue = "UZ") Locale locale) {

        Episode episode = episodeRepo.findById(episodeId)
                .orElseThrow(() -> BusinessException.notFound("Episode", episodeId));

        User user = CurrentUser.getOrNull();
        AccessDecision decision = accessService.canWatch(user, episode);

        WatchResponse.WatchResponseBuilder body = WatchResponse.builder()
                .episodeId(episode.getId())
                .contentId(episode.getContent() != null ? episode.getContent().getId() : null)
                .episodeNumber(episode.getEpisodeNumber())
                .durationSeconds(episode.getDurationSeconds())
                .title(title(episode, locale))
                .allowed(decision.isAllowed())
                .reason(decision.getReason().name())
                .requiredAction(decision.getRequiredAction().name())
                .episodePrice(decision.getEpisodePrice())
                .premierePrice(decision.getPremierePrice())
                .showAds(decision.isAllowed() && accessService.shouldShowAds(user))
                // Rad etilganda ham ro'yxat bo'sh emas, BO'SH RO'YXAT - null emas,
                // klientda "null.length" xatosi chiqmasligi uchun.
                .sources(decision.isAllowed() ? sources(episode, locale) : List.of());

        return ResponseEntity.ok(body.build());
    }

    /**
     * SINGLE kontentni ko'rish — film, qisqa metraj, klip, shou.
     *
     * <h2>Nima uchun alohida endpoint</h2>
     * {@code /watch/{episodeId}} qism identifikatorini talab qiladi, SINGLE
     * kontentda esa qism BO'LMAYDI (ТЗ §14). Bu endpoint qo'shilgunga qadar
     * filmni tomosha qilish oqimi umuman yo'q edi: video saqlanadigan joy
     * ham, uni so'raydigan endpoint ham.
     *
     * <h2>Video qayerda</h2>
     * ТЗ §22 (Step 2 — Media) videolarni kontent darajasida sanaydi.
     * Shuning uchun ular {@code ContentMedia} da {@code role = VIDEO}
     * sifatida yotadi: {@code sortOrder} — segment tartibi (§19),
     * {@code locale} — dublyaj tili.
     */
    @GetMapping("/content/{contentId}")
    @Transactional(readOnly = true)
    public ResponseEntity<WatchResponse> watchContent(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "UZ") Locale locale) {

        Content content = contentRepo.findById(contentId)
                .orElseThrow(() -> BusinessException.notFound("Content", contentId));

        // Ko'p qismli kontentni bu yerdan ko'rib bo'lmaydi - klient qaysi
        // qismni so'rayotganini aytishi kerak. Aks holda "qaysi qism?"
        // degan savolga server o'zi javob berib qo'yardi.
        if (content.getStructureType() != StructureType.SINGLE) {
            throw BusinessException.validation(
                    "Bu kontent ko'p qismli. /watch/{episodeId} dan foydalaning");
        }

        User user = CurrentUser.getOrNull();
        AccessDecision decision = accessService.canWatch(user, content);

        return ResponseEntity.ok(WatchResponse.builder()
                .contentId(content.getId())
                .durationSeconds(content.getDurationMinutes() == null
                        ? null : content.getDurationMinutes() * 60)
                .title(contentTitle(content, locale))
                .allowed(decision.isAllowed())
                .reason(decision.getReason().name())
                .requiredAction(decision.getRequiredAction().name())
                .episodePrice(decision.getEpisodePrice())
                .premierePrice(decision.getPremierePrice())
                .showAds(decision.isAllowed() && accessService.shouldShowAds(user))
                .sources(decision.isAllowed() ? contentSources(content, locale) : List.of())
                .build());
    }

    // ------------------------------------------------------------- ichki qism

    /** So'ralgan til, bo'lmasa standart til, bo'lmasa bori. */
    private String contentTitle(Content content, Locale locale) {
        List<ContentTranslation> all = content.getTranslations();
        if (all == null || all.isEmpty()) {
            return null;
        }
        return all.stream().filter(t -> t.getLocale() == locale).findFirst()
                .or(() -> all.stream().filter(t -> t.getLocale() == Locale.DEFAULT).findFirst())
                .or(() -> all.stream().findFirst())
                .map(ContentTranslation::getTitle)
                .orElse(null);
    }

    /**
     * Kontentning asosiy video segmentlari.
     *
     * TRAILER va TEASER ATAYLAB kirmaydi — ular reklama roliklari, kontentning
     * o'zi emas. Ularni bu yerga qo'shsak, pullik filmni sotib olmagan odam
     * treylerni «film» deb olib ketardi.
     */
    private List<VideoSource> contentSources(Content content, Locale locale) {
        List<ContentMedia> videos = content.getMedia() == null ? List.of()
                : content.getMedia().stream()
                        .filter(m -> m.getRole() == MediaRole.VIDEO)
                        .filter(m -> m.getMedia() != null)
                        .toList();

        if (videos.isEmpty()) {
            return List.of();
        }

        List<ContentMedia> picked = forContentLocale(videos, locale);
        if (picked.isEmpty()) {
            picked = forContentLocale(videos, Locale.DEFAULT);
        }
        if (picked.isEmpty()) {
            picked = new ArrayList<>(videos);
        }

        return picked.stream()
                .sorted(Comparator.comparing(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()))
                .map(m -> VideoSource.builder()
                        // Segment raqami 1 dan boshlanadi - klientda "1-qism" deb ko'rsatiladi.
                        .partNumber((m.getSortOrder() == null ? 0 : m.getSortOrder()) + 1)
                        .mediaId(m.getMedia().getId())
                        .url("/api/v1/app/media/" + m.getMedia().getId() + "/raw")
                        .durationSeconds(m.getMedia().getDurationSeconds())
                        .build())
                .toList();
    }

    /** Locale bo'sh = barcha tillar uchun mos. */
    private List<ContentMedia> forContentLocale(List<ContentMedia> videos, Locale locale) {
        return videos.stream()
                .filter(m -> m.getLocale() == null || m.getLocale() == locale)
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

    /**
     * Shu til uchun video bo'laklari.
     *
     * Kontent YouTube formatida bitta bo'lak, Reels formatida bir nechta
     * bo'lishi mumkin - shuning uchun ro'yxat, {@code partNumber} bo'yicha
     * tartiblangan.
     */
    private List<VideoSource> sources(Episode episode, Locale locale) {
        List<EpisodeVideo> videos = episode.getVideos();
        if (videos == null || videos.isEmpty()) {
            return List.of();
        }

        List<EpisodeVideo> picked = forLocale(videos, locale);
        if (picked.isEmpty()) {
            picked = forLocale(videos, Locale.DEFAULT);
        }
        if (picked.isEmpty()) {
            picked = new ArrayList<>(videos);
        }

        return picked.stream()
                .filter(v -> v.getMedia() != null)
                .sorted(Comparator
                        .comparing((EpisodeVideo v) ->
                                v.getPartNumber() == null ? 1 : v.getPartNumber())
                        .thenComparing(v -> v.getSortOrder() == null ? 0 : v.getSortOrder()))
                .map(v -> VideoSource.builder()
                        .partNumber(v.getPartNumber())
                        .mediaId(v.getMedia().getId())
                        .url("/api/v1/app/media/" + v.getMedia().getId() + "/raw")
                        .durationSeconds(v.getMedia().getDurationSeconds())
                        .build())
                .toList();
    }

    /** Locale bo'sh = barcha tillar uchun mos, shuning uchun u ham kiradi. */
    private List<EpisodeVideo> forLocale(List<EpisodeVideo> videos, Locale locale) {
        return videos.stream()
                .filter(v -> v.getLocale() == null || v.getLocale() == locale)
                .toList();
    }

    // ------------------------------------------------------------------- DTO

    @Data
    @Builder
    public static class WatchResponse {
        private Long episodeId;
        private Long contentId;
        private Integer episodeNumber;
        private Integer durationSeconds;
        private String title;

        private boolean allowed;
        /** FREE, PREMIUM, PAYMENT_REQUIRED ... */
        private String reason;
        /** NONE, SIGN_IN, SUBSCRIBE, BUY_EPISODE, BUY_OR_SUBSCRIBE. */
        private String requiredAction;
        private BigDecimal episodePrice;
        private BigDecimal premierePrice;

        /** Premium obunachiga reklama ko'rsatilmaydi. */
        private boolean showAds;

        /** Ruxsat bo'lmasa - bo'sh. */
        private List<VideoSource> sources;
    }

    @Data
    @Builder
    public static class VideoSource {
        private Integer partNumber;
        private Long mediaId;
        private String url;
        private Integer durationSeconds;
    }
}
