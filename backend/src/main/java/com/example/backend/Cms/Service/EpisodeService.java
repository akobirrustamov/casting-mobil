package com.example.backend.Cms.Service;

import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Admin.Dto.SeasonSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.*;
import com.example.backend.Entity.User;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.Cms.Enums.PurchaseType;
import com.example.backend.Cms.Repository.PurchaseRepo;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Fasllar va qismlarni boshqarish.
 *
 * <b>Tuzilish qoidalari qat'iy tekshiriladi</b> — aks holda ma'lumot bazasi
 * bir-biriga zid holatga tushadi:
 * <ul>
 *   <li>{@code SINGLE} kontentda qism ham, fasl ham bo'lmaydi;</li>
 *   <li>{@code EPISODIC} da qismlar bor, fasllar yo'q ({@code seasonId = null});</li>
 *   <li>{@code SEASONAL} da har bir qism faslga tegishli bo'lishi SHART.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final ContentRepo contentRepo;
    private final SeasonRepo seasonRepo;
    private final EpisodeRepo episodeRepo;
    private final MediaAssetRepo mediaAssetRepo;
    private final PurchaseRepo purchaseRepo;
    private final AuditService auditService;

    // ---------------------------------------------------------------- fasllar

    @Transactional(readOnly = true)
    public List<Season> seasonsOf(Long contentId) {
        requireContent(contentId);
        return seasonRepo.findAllByContentIdOrderBySortOrderAsc(contentId);
    }

    @Transactional
    public Season saveSeason(User actor, Long contentId, Long seasonId, SeasonSaveRequest request) {
        Content content = requireContent(contentId);

        if (content.getStructureType() != StructureType.SEASONAL) {
            throw BusinessException.validation(
                    "Fasl faqat SEASONAL tuzilishdagi kontentda bo'ladi. Hozirgi tuzilish: "
                            + content.getStructureType());
        }

        String uzTitle = requireUzTitle(request.getTranslations(), "Fasl nomi",
                isUserVisible(request.getStatus()));

        Season season = seasonId == null ? new Season()
                : seasonRepo.findById(seasonId)
                        .orElseThrow(() -> BusinessException.notFound("Season", seasonId));

        if (seasonId != null && !season.getContent().getId().equals(contentId)) {
            throw BusinessException.validation("Bu fasl boshqa kontentga tegishli");
        }

        // Fasl raqami takrorlanmasin — DB cheklovidan oldin tushunarli xato beramiz
        boolean numberTaken = seasonRepo.findAllByContentIdOrderBySortOrderAsc(contentId).stream()
                .anyMatch(s -> s.getSeasonNumber().equals(request.getSeasonNumber())
                        && (seasonId == null || !s.getId().equals(seasonId)));
        if (numberTaken) {
            throw BusinessException.duplicate("DUPLICATE_SEASON_NUMBER",
                    request.getSeasonNumber() + "-fasl allaqachon mavjud");
        }

        season.setContent(content);
        season.setSeasonNumber(request.getSeasonNumber());
        season.setStatus(request.getStatus());
        season.setPremiereDate(request.getPremiereDate());
        season.setSortOrder(request.getSortOrder() == null
                ? request.getSeasonNumber() : request.getSortOrder());
        season.setPoster(mediaOrNull(request.getPosterMediaId()));

        mergeSeasonTranslations(season, request.getTranslations());

        Season saved = seasonRepo.save(season);
        auditService.log(actor, seasonId == null ? AuditAction.SEASON_CREATED : AuditAction.SEASON_UPDATED,
                "Season", saved.getId(), null,
                Map.of("contentId", contentId, "number", saved.getSeasonNumber(), "title", uzTitle));
        return saved;
    }

    @Transactional
    public void deleteSeason(User actor, Long contentId, Long seasonId) {
        Season season = seasonRepo.findById(seasonId)
                .orElseThrow(() -> BusinessException.notFound("Season", seasonId));
        if (!season.getContent().getId().equals(contentId)) {
            throw BusinessException.validation("Bu fasl boshqa kontentga tegishli");
        }

        // Qismlari bor faslni jim o'chirib yubormaymiz — avval qismlar ko'chirilsin
        long episodes = episodeRepo.findAllBySeasonIdOrderByEpisodeNumberAsc(seasonId).size();
        if (episodes > 0) {
            throw new BusinessException("SEASON_NOT_EMPTY",
                    "Faslda " + episodes + " ta qism bor. Avval ularni o'chiring yoki boshqa faslga ko'chiring.",
                    HttpStatus.CONFLICT);
        }

        seasonRepo.delete(season);
        auditService.log(actor, AuditAction.SEASON_DELETED, "Season", seasonId);
    }

    // ---------------------------------------------------------------- qismlar

    @Transactional(readOnly = true)
    public List<Episode> episodesOf(Long contentId) {
        requireContent(contentId);
        return episodeRepo.findAllByContentIdOrderBySortOrderAsc(contentId);
    }

    @Transactional
    public Episode saveEpisode(User actor, Long contentId, Long episodeId, EpisodeSaveRequest request) {
        Content content = requireContent(contentId);

        if (content.getStructureType() == StructureType.SINGLE) {
            throw BusinessException.validation(
                    "Bitta qismlik kontentga qism qo'shilmaydi. Tuzilishni EPISODIC yoki SEASONAL ga o'zgartiring.");
        }

        Season season = resolveSeason(content, request.getSeasonId());
        String uzTitle = requireUzTitle(request.getTranslations(), "Qism nomi",
                isUserVisible(request.getStatus()));

        Episode episode = episodeId == null ? new Episode()
                : episodeRepo.findById(episodeId)
                        .orElseThrow(() -> BusinessException.notFound("Episode", episodeId));

        if (episodeId != null) {
            if (!episode.getContent().getId().equals(contentId)) {
                throw BusinessException.validation("Bu qism boshqa kontentga tegishli");
            }
            if (request.getVersion() != null && !request.getVersion().equals(episode.getVersion())) {
                throw new BusinessException("CONCURRENT_MODIFICATION",
                        "Bu qismni boshqa foydalanuvchi o'zgartirdi. Sahifani yangilang.",
                        HttpStatus.CONFLICT);
            }
        }

        requireFreeEpisodeNumber(contentId, episodeId, season, request.getEpisodeNumber());

        episode.setContent(content);
        episode.setSeason(season);
        episode.setEpisodeNumber(request.getEpisodeNumber());
        episode.setDurationSeconds(request.getDurationSeconds());
        episode.setPremiereDate(request.getPremiereDate());
        episode.setStatus(request.getStatus());
        episode.setAccessPolicyOverride(request.getAccessPolicyOverride());
        episode.setPrice(request.getPrice());
        episode.setSortOrder(request.getSortOrder() == null
                ? request.getEpisodeNumber() : request.getSortOrder());
        episode.setThumbnail(mediaOrNull(request.getThumbnailMediaId()));

        // PUBLISHED bo'lganda sana o'zi qo'yiladi — admin unutib qoldirmasin
        if (request.getPublicationDate() != null) {
            episode.setPublicationDate(request.getPublicationDate());
        } else if (request.getStatus() != null && request.getStatus().isVisibleToUsers()
                && episode.getPublicationDate() == null) {
            episode.setPublicationDate(java.time.LocalDateTime.now());
        }

        mergeEpisodeTranslations(episode, request.getTranslations());
        replaceVideos(episode, request.getVideos());

        Episode saved = episodeRepo.save(episode);
        auditService.log(actor, episodeId == null ? AuditAction.EPISODE_CREATED : AuditAction.EPISODE_UPDATED,
                "Episode", saved.getId(), null,
                Map.of("contentId", contentId, "number", saved.getEpisodeNumber(), "title", uzTitle));
        return saved;
    }

    @Transactional
    public void deleteEpisode(User actor, Long contentId, Long episodeId) {
        Episode episode = episodeRepo.findById(episodeId)
                .orElseThrow(() -> BusinessException.notFound("Episode", episodeId));
        if (!episode.getContent().getId().equals(contentId)) {
            throw BusinessException.validation("Bu qism boshqa kontentga tegishli");
        }
        // Sotilgan qismni o'chirib bo'lmaydi: xarid yozuvi qism id'siga
        // bog'langan, qism yo'qolsa foydalanuvchi pul to'lagan-u, nimaga
        // to'laganini na u, na qo'llab-quvvatlash ko'ra oladi.
        long purchases = purchaseRepo.countByTypeAndTargetId(PurchaseType.EPISODE, episodeId);
        if (purchases > 0) {
            throw new BusinessException("EPISODE_PURCHASED",
                    "Bu qism " + purchases + " marta sotib olingan, o'chirib bo'lmaydi. "
                            + "Uni arxivga o'tkazing (status = ARCHIVED).",
                    HttpStatus.CONFLICT);
        }

        episodeRepo.delete(episode);
        auditService.log(actor, AuditAction.EPISODE_DELETED, "Episode", episodeId);
    }

    // ------------------------------------------------------------------ ichki

    private Content requireContent(Long contentId) {
        return contentRepo.findById(contentId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.notFound("Content", contentId));
    }

    /** Tuzilishga qarab fasl majburiy yoki taqiqlangan. */
    private Season resolveSeason(Content content, Long seasonId) {
        if (content.getStructureType() == StructureType.SEASONAL) {
            if (seasonId == null) {
                throw BusinessException.validation(
                        "Faslli serialda qism qaysi faslga tegishli ekani ko'rsatilishi shart");
            }
            Season season = seasonRepo.findById(seasonId)
                    .orElseThrow(() -> BusinessException.notFound("Season", seasonId));
            if (!season.getContent().getId().equals(content.getId())) {
                throw BusinessException.validation("Tanlangan fasl boshqa kontentga tegishli");
            }
            return season;
        }
        // EPISODIC — fasl bo'lmaydi
        if (seasonId != null) {
            throw BusinessException.validation(
                    "Faslsiz tuzilishda qism faslga bog'lanmaydi");
        }
        return null;
    }

    /**
     * Qism raqami takrorlanmasin.
     * Faslli serialda — fasl ichida, faslsizda — butun kontent bo'yicha.
     */
    private void requireFreeEpisodeNumber(Long contentId, Long episodeId, Season season, Integer number) {
        List<Episode> siblings = season == null
                ? episodeRepo.findAllByContentIdOrderBySortOrderAsc(contentId).stream()
                        .filter(e -> e.getSeason() == null).toList()
                : episodeRepo.findAllBySeasonIdOrderByEpisodeNumberAsc(season.getId());

        boolean taken = siblings.stream()
                .anyMatch(e -> Objects.equals(e.getEpisodeNumber(), number)
                        && (episodeId == null || !e.getId().equals(episodeId)));
        if (taken) {
            throw BusinessException.duplicate("DUPLICATE_EPISODE_NUMBER",
                    number + "-qism bu yerda allaqachon mavjud");
        }
    }

    /**
     * Sarlavha tekshiruvi.
     *
     * Nashr qilingan fasl va qism uchun uchala til ham majburiy — ular
     * foydalanuvchi ro'yxatida chiqadi. Qoralamada asosiy til yetarli.
     */
    /**
     * SCHEDULED ham kiradi — u belgilangan vaqtda avtomatik PUBLISHED
     * bo'ladi va o'shanda tarjima yo'qligi bilinardi, kech bo'lardi.
     */
    private boolean isUserVisible(PublicationStatus status) {
        return status == PublicationStatus.PUBLISHED
                || status == PublicationStatus.SCHEDULED;
    }

    private String requireUzTitle(Map<Locale, TranslationDto> translations, String what,
                                  boolean userVisible) {
        TranslationRules.require(translations, TranslationDto::getTitle, what, userVisible);
        return translations.get(Locale.UZ).getTitle().trim();
    }

    /** Joyida yangilanadi — clear()+add UNIQUE(parent, locale) ni buzadi. */
    private void mergeSeasonTranslations(Season season, Map<Locale, TranslationDto> incoming) {
        Map<Locale, SeasonTranslation> existing = new HashMap<>();
        season.getTranslations().forEach(t -> existing.put(t.getLocale(), t));

        Set<Locale> keep = new HashSet<>();
        incoming.forEach((locale, dto) -> {
            if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
                return;
            }
            keep.add(locale);
            SeasonTranslation row = existing.get(locale);
            if (row == null) {
                row = SeasonTranslation.builder().locale(locale).build();
                season.addTranslation(row);
            }
            row.setTitle(dto.getTitle().trim());
            row.setDescription(dto.getDescription());
        });
        season.getTranslations().removeIf(t -> !keep.contains(t.getLocale()));
    }

    private void mergeEpisodeTranslations(Episode episode, Map<Locale, TranslationDto> incoming) {
        Map<Locale, EpisodeTranslation> existing = new HashMap<>();
        episode.getTranslations().forEach(t -> existing.put(t.getLocale(), t));

        Set<Locale> keep = new HashSet<>();
        incoming.forEach((locale, dto) -> {
            if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
                return;
            }
            keep.add(locale);
            EpisodeTranslation row = existing.get(locale);
            if (row == null) {
                row = EpisodeTranslation.builder().locale(locale).build();
                episode.addTranslation(row);
            }
            row.setTitle(dto.getTitle().trim());
            row.setShortDescription(dto.getShortDescription());
            row.setDescription(dto.getDescription());
        });
        episode.getTranslations().removeIf(t -> !keep.contains(t.getLocale()));
    }

    /** Video qismlarda unikal cheklov yo'q, shuning uchun to'liq almashtiriladi. */
    private void replaceVideos(Episode episode, List<EpisodeSaveRequest.VideoLink> links) {
        episode.getVideos().clear();
        if (links == null) {
            return;
        }
        int index = 0;
        for (EpisodeSaveRequest.VideoLink link : links) {
            MediaAsset media = mediaAssetRepo.findById(link.getMediaId())
                    .orElseThrow(() -> BusinessException.notFound("Media", link.getMediaId()));
            episode.addVideo(EpisodeVideo.builder()
                    .media(media)
                    .locale(link.getLocale())
                    .partNumber(link.getPartNumber() == null ? index + 1 : link.getPartNumber())
                    .sortOrder(link.getSortOrder() == null ? index : link.getSortOrder())
                    .build());
            index++;
        }
    }

    private MediaAsset mediaOrNull(Long id) {
        return id == null ? null : mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));
    }
}
