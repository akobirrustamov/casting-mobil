package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Premiere;
import com.example.backend.Cms.Entity.PremiereTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class PremiereDto {

    private Long id;
    private String name;
    private Long imageMediaId;
    private Long videoMediaId;
    private Long contentId;
    private Boolean buttonEnabled;
    private InternalLinkDto link;
    private PublicationStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer sortOrder;

    /** ТЗ §27 — qachon yaratilgan va oxirgi marta o'zgargan. */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean live;
    private Map<Locale, PremiereTextDto> translations;

    @Data
    @Builder
    public static class PremiereTextDto {
        private String title;
        private String subtitle;
        private String description;
        private String buttonText;
    }

    public static PremiereDto from(Premiere p) {
        Map<Locale, PremiereTextDto> tr = new LinkedHashMap<>();
        for (PremiereTranslation t : p.getTranslations()) {
            tr.put(t.getLocale(), PremiereTextDto.builder()
                    .title(t.getTitle())
                    .subtitle(t.getSubtitle())
                    .description(t.getDescription())
                    .buttonText(t.getButtonText())
                    .build());
        }
        return PremiereDto.builder()
                .id(p.getId())
                .name(p.getName())
                .imageMediaId(p.getImage() == null ? null : p.getImage().getId())
                .videoMediaId(p.getVideo() == null ? null : p.getVideo().getId())
                .contentId(p.getContent() == null ? null : p.getContent().getId())
                .buttonEnabled(p.getButtonEnabled())
                .link(InternalLinkDto.from(p.getLink()))
                .status(p.getStatus())
                .startAt(p.getStartAt())
                .endAt(p.getEndAt())
                .sortOrder(p.getSortOrder())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .live(p.isLiveAt(LocalDateTime.now()))
                .translations(tr)
                .build();
    }
}
