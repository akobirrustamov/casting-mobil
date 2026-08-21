package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Advertisement;
import com.example.backend.Cms.Entity.AdvertisementTranslation;
import com.example.backend.Cms.Enums.AdAudience;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class AdvertisementDto {

    private Long id;
    private String name;
    private Long imageMediaId;
    private Long mobileImageMediaId;
    private Boolean buttonEnabled;
    private InternalLinkDto link;
    private AdAudience audience;
    private PublicationStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer sortOrder;

    /** ТЗ §27 — qachon yaratilgan va oxirgi marta o'zgargan. */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Hozir foydalanuvchiga ko'rinadimi — admin ro'yxatda darhol ko'rsin. */
    private Boolean live;

    private Map<Locale, AdTextDto> translations;

    @Data
    @Builder
    public static class AdTextDto {
        private String title;
        private String description;
        private String buttonText;
    }

    public static AdvertisementDto from(Advertisement a) {
        Map<Locale, AdTextDto> tr = new LinkedHashMap<>();
        for (AdvertisementTranslation t : a.getTranslations()) {
            tr.put(t.getLocale(), AdTextDto.builder()
                    .title(t.getTitle())
                    .description(t.getDescription())
                    .buttonText(t.getButtonText())
                    .build());
        }
        return AdvertisementDto.builder()
                .id(a.getId())
                .name(a.getName())
                .imageMediaId(a.getImage() == null ? null : a.getImage().getId())
                .mobileImageMediaId(a.getMobileImage() == null ? null : a.getMobileImage().getId())
                .buttonEnabled(a.getButtonEnabled())
                .link(InternalLinkDto.from(a.getLink()))
                .audience(a.getAudience())
                .status(a.getStatus())
                .startAt(a.getStartAt())
                .endAt(a.getEndAt())
                .sortOrder(a.getSortOrder())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .live(a.isLiveAt(LocalDateTime.now()))
                .translations(tr)
                .build();
    }
}
