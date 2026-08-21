package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Season;
import com.example.backend.Cms.Entity.SeasonTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class SeasonDto {

    private Long id;
    private Integer seasonNumber;
    private Long posterMediaId;
    private LocalDateTime premiereDate;
    private PublicationStatus status;
    private Integer sortOrder;
    private Map<Locale, TranslationDto> translations;

    /** Shu faslga tegishli qismlar soni - ro'yxatda ko'rsatish uchun. */
    private Integer episodeCount;

    public static SeasonDto from(Season s, int episodeCount) {
        Map<Locale, TranslationDto> tr = new LinkedHashMap<>();
        for (SeasonTranslation t : s.getTranslations()) {
            tr.put(t.getLocale(), TranslationDto.builder()
                    .title(t.getTitle())
                    .description(t.getDescription())
                    .build());
        }
        return SeasonDto.builder()
                .id(s.getId())
                .seasonNumber(s.getSeasonNumber())
                .posterMediaId(s.getPoster() == null ? null : s.getPoster().getId())
                .premiereDate(s.getPremiereDate())
                .status(s.getStatus())
                .sortOrder(s.getSortOrder())
                .translations(tr)
                .episodeCount(episodeCount)
                .build();
    }
}
