package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.AdAudience;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AdvertisementSaveRequest {

    @NotBlank(message = "Ichki nom kiritilmagan")
    @Size(max = 255)
    private String name;

    private Long imageMediaId;
    private Long mobileImageMediaId;

    private Boolean buttonEnabled = false;
    private InternalLinkDto link = new InternalLinkDto();

    private AdAudience audience = AdAudience.ADVERTISEMENT;
    private PublicationStatus status = PublicationStatus.DRAFT;

    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer sortOrder = 0;

    private Map<Locale, AdvertisementDto.AdTextDto> translations = new LinkedHashMap<>();
}
