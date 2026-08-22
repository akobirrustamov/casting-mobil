package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.Locale;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CreatorSaveRequest {

    @Size(max = 128)
    private String slug;

    private Long photoMediaId;
    private Long coverMediaId;
    private LocalDate birthDate;

    private Boolean active = true;
    private Boolean featured = false;
    private Integer sortOrder = 0;

    /**
     * ⚠️ {@code @NotEmpty}, {@code @NotNull} EMAS.
     *
     * Maydonda standart qiymat bor ({@code new LinkedHashMap<>()}),
     * shuning uchun u HECH QACHON null bo'lmaydi va {@code @NotNull}
     * hech qachon ishlamasdi. Annotatsiya himoya qilayotgandek
     * ko'rinardi, aslida o'lik edi: bo'sh tana bemalol o'tib,
     * xato servisda — maydon nomisiz — chiqardi.
     */
    @NotEmpty(message = "Ismlar kiritilmagan")
    private Map<Locale, NameDto> translations = new LinkedHashMap<>();

    @Data
    public static class NameDto {
        private String firstName;
        private String lastName;
        private String middleName;
        /** Bo'sh bo'lsa firstName + lastName dan yasaladi. */
        private String displayName;
        private String bio;
    }
}
