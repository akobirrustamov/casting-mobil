package com.example.backend.Cms.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Ro'yxatdan o'tishning 1-qadami: telefon raqam.
 *
 * ⚠️ {@code /otp/send} dan farqi — bu yerda raqam BAND emasligi
 * tekshiriladi. Parolli hisobi bor raqamga SMS umuman yuborilmaydi:
 * ilova odamni «kirish» bo'limiga qaytaradi.
 */
@Data
public class RegisterStartRequest {

    @NotBlank(message = "Telefon raqami kiritilmagan")
    private String phone;
}
