package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.PlatformSetting;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Platforma sozlamasi — javob uchun.
 *
 * Entity o'rniga DTO: API baza sxemasiga bog'lanib qolmasin.
 */
@Data
@Builder
public class PlatformSettingDto {

    private String key;
    private String value;
    private String description;

    /** Kim o'zgartirgani — audit uchun; sozlamalarni ko'ra oladigan xodimga. */
    private UUID updatedBy;
    private LocalDateTime updatedAt;

    public static PlatformSettingDto from(PlatformSetting s) {
        return PlatformSettingDto.builder()
                .key(s.getKey())
                .value(s.getValue())
                .description(s.getDescription())
                .updatedBy(s.getUpdatedBy())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
