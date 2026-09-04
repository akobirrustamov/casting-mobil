package com.example.backend.Cms.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Kirishning 3-qadami: yangi foydalanuvchining ismi.
 *
 * ⚠️ Uzunlik chegarasi bu yerda EMAS —
 * {@code AppAccountService.validateName} da. Sabab: xato kodi
 * ({@code NAME_INVALID}) ilovada tarjima qilinadi, {@code @Size} esa
 * boshqa shakldagi javob berardi va ilova uni tanimasdi.
 */
@Data
public class OtpCompleteRequest {

    @NotBlank(message = "Telefon raqami kiritilmagan")
    private String phone;

    @NotBlank(message = "Ism kiritilmagan")
    private String name;
}
