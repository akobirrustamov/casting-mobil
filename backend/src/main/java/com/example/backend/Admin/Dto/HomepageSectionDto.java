package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.HomepageSection;
import com.example.backend.Cms.Entity.HomepageSectionTranslation;
import com.example.backend.Cms.Enums.HomepageSectionType;
import com.example.backend.Cms.Enums.Locale;
import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class HomepageSectionDto {

    private Long id;
    private HomepageSectionType type;
    private Boolean enabled;
    private Integer sortOrder;
    private Integer itemLimit;
    private Map<Locale, TranslationDto> translations;

    public static HomepageSectionDto from(HomepageSection s) {
        Map<Locale, TranslationDto> tr = new LinkedHashMap<>();
        for (HomepageSectionTranslation t : s.getTranslations()) {
            tr.put(t.getLocale(), TranslationDto.ofTitle(t.getTitle()));
        }
        return HomepageSectionDto.builder()
                .id(s.getId())
                .type(s.getType())
                .enabled(s.getEnabled())
                .sortOrder(s.getSortOrder())
                .itemLimit(s.getItemLimit())
                .translations(tr)
                .build();
    }
}
