package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.Locale;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bo'limni yangilash. Tur ({@code type}) o'zgartirilmaydi — u tizim
 * konstantasi, faqat yoqish/o'chirish, tartib va sarlavha tahrirlanadi.
 */
@Data
public class HomepageSectionSaveRequest {

    private Boolean enabled = true;
    private Integer sortOrder = 0;
    private Integer itemLimit;
    private Map<Locale, TranslationDto> translations = new LinkedHashMap<>();
}
