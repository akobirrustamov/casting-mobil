package com.example.backend.Cms.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Ro'yxatdan o'tishning 2-qadami: SMS kod.
 *
 * ⚠️ {@code /otp/verify} dan farqi — bu yerda TOKEN BERILMAYDI. Hisob
 * hali yaratilmagan: odam avval parol qo'yishi kerak (3-qadam).
 */
@Data
public class RegisterConfirmRequest {

    @NotBlank(message = "Telefon raqami kiritilmagan")
    private String phone;

    @NotBlank(message = "Kod kiritilmagan")
    private String code;
}
