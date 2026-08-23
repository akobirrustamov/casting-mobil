package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Genre;
import com.example.backend.Cms.Entity.GenreTranslation;
import com.example.backend.Cms.Enums.Locale;
import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class GenreDto {

    private Long id;
    private String slug;
    private Integer sortOrder;
    private Boolean active;
    private Map<Locale, TranslationDto> translations;

    public static GenreDto from(Genre g) {
        Map<Locale, TranslationDto> tr = new LinkedHashMap<>();
        for (GenreTranslation t : g.getTranslations()) {
            tr.put(t.getLocale(), TranslationDto.ofTitle(t.getName()));
        }
        return GenreDto.builder()
                .id(g.getId())
                .slug(g.getSlug())
                .sortOrder(g.getSortOrder())
                .active(g.getActive())
                .translations(tr)
                .build();
    }
}
