package com.example.backend.Cms.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Kirishning 1-qadami: telefon raqam.
 *
 * ⚠️ Raqam band-emasligi TEKSHIRILMAYDI: kirish va ro'yxatdan o'tish
 * bitta oqim, band raqam — bu shunchaki kirayotgan odam.
 */
@Data
public class OtpSendRequest {

    @NotBlank(message = "Telefon raqami kiritilmagan")
    private String phone;
}
