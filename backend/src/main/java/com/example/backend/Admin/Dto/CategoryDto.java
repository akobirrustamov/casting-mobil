package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Category;
import com.example.backend.Cms.Entity.CategoryTranslation;
import com.example.backend.Cms.Enums.Locale;
import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class CategoryDto {

    private Long id;
    private String slug;
    private Integer sortOrder;
    private Boolean active;
    private Long iconMediaId;
    private Map<Locale, TranslationDto> translations;

    public static CategoryDto from(Category c) {
        Map<Locale, TranslationDto> tr = new LinkedHashMap<>();
        for (CategoryTranslation t : c.getTranslations()) {
            tr.put(t.getLocale(), TranslationDto.builder()
                    .title(t.getName())
                    .description(t.getDescription())
                    .build());
        }
        return CategoryDto.builder()
                .id(c.getId())
                .slug(c.getSlug())
                .sortOrder(c.getSortOrder())
                .active(c.getActive())
                .iconMediaId(c.getIcon() == null ? null : c.getIcon().getId())
                .translations(tr)
                .build();
    }
}
