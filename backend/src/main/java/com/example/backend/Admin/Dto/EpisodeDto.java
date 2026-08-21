package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.EpisodeTranslation;
import com.example.backend.Cms.Entity.EpisodeVideo;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class EpisodeDto {

    private Long id;
    private Long seasonId;
    private Integer episodeNumber;
    private Long thumbnailMediaId;
    private Integer durationSeconds;
    private LocalDateTime premiereDate;
    private LocalDateTime publicationDate;
    private PublicationStatus status;

    /** null - kontent siyosati meros olinadi. */
    private AccessPolicy accessPolicyOverride;

    /** Amaldagi siyosat: o'zinikini, bo'lmasa kontentnikini ko'rsatadi. */
    private AccessPolicy effectiveAccessPolicy;

    private BigDecimal price;
    private Long viewCount;
    private Integer sortOrder;
    private Long version;

    private Map<Locale, TranslationDto> translations;
    private List<VideoDto> videos;

    @Data
    @Builder
    public static class VideoDto {
        private Long id;
        private Long mediaId;
        private Locale locale;
        private Integer partNumber;
        private Integer sortOrder;
    }

    public static EpisodeDto from(Episode e) {
        Map<Locale, TranslationDto> tr = new LinkedHashMap<>();
        for (EpisodeTranslation t : e.getTranslations()) {
            tr.put(t.getLocale(), TranslationDto.builder()
                    .title(t.getTitle())
                    .shortDescription(t.getShortDescription())
                    .description(t.getDescription())
                    .build());
        }

        List<VideoDto> videos = new ArrayList<>();
        for (EpisodeVideo v : e.getVideos()) {
            videos.add(VideoDto.builder()
                    .id(v.getId())
                    .mediaId(v.getMedia() == null ? null : v.getMedia().getId())
                    .locale(v.getLocale())
                    .partNumber(v.getPartNumber())
                    .sortOrder(v.getSortOrder())
                    .build());
        }
        videos.sort((a, b) -> {
            int byPart = Integer.compare(
                    a.getPartNumber() == null ? 0 : a.getPartNumber(),
                    b.getPartNumber() == null ? 0 : b.getPartNumber());
            return byPart != 0 ? byPart : Integer.compare(
                    a.getSortOrder() == null ? 0 : a.getSortOrder(),
                    b.getSortOrder() == null ? 0 : b.getSortOrder());
        });

        return EpisodeDto.builder()
                .id(e.getId())
                .seasonId(e.getSeason() == null ? null : e.getSeason().getId())
                .episodeNumber(e.getEpisodeNumber())
                .thumbnailMediaId(e.getThumbnail() == null ? null : e.getThumbnail().getId())
                .durationSeconds(e.getDurationSeconds())
                .premiereDate(e.getPremiereDate())
                .publicationDate(e.getPublicationDate())
                .status(e.getStatus())
                .accessPolicyOverride(e.getAccessPolicyOverride())
                .effectiveAccessPolicy(e.effectiveAccessPolicy())
                .price(e.getPrice())
                .viewCount(e.getViewCount())
                .sortOrder(e.getSortOrder())
                .version(e.getVersion())
                .translations(tr)
                .videos(videos)
                .build();
    }
}
