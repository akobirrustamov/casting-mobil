package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Entity.CreatorTranslation;
import com.example.backend.Cms.Enums.Locale;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class CreatorDto {

    private Long id;
    private String slug;
    private Long photoMediaId;
    private Long coverMediaId;
    private LocalDate birthDate;
    private Boolean active;
    private Boolean featured;
    private Integer sortOrder;
    private Long starsReceived;

    /** ТЗ §24 — profil qachon yaratilgan va oxirgi marta o'zgargan. */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<Locale, CreatorNameDto> translations;

    @Data
    @Builder
    public static class CreatorNameDto {
        private String firstName;
        private String lastName;
        private String middleName;
        private String displayName;
        private String bio;
    }

    public static CreatorDto from(Creator c) {
        Map<Locale, CreatorNameDto> tr = new LinkedHashMap<>();
        for (CreatorTranslation t : c.getTranslations()) {
            tr.put(t.getLocale(), CreatorNameDto.builder()
                    .firstName(t.getFirstName())
                    .lastName(t.getLastName())
                    .middleName(t.getMiddleName())
                    .displayName(t.getDisplayName())
                    .bio(t.getBio())
                    .build());
        }
        return CreatorDto.builder()
                .id(c.getId())
                .slug(c.getSlug())
                .photoMediaId(c.getPhoto() == null ? null : c.getPhoto().getId())
                .coverMediaId(c.getCover() == null ? null : c.getCover().getId())
                .birthDate(c.getBirthDate())
                .active(c.getActive())
                .featured(c.getFeatured())
                .sortOrder(c.getSortOrder())
                .starsReceived(c.getStarsReceived())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .translations(tr)
                .build();
    }
}
