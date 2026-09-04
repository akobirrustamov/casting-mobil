package com.example.backend.Cms.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Kirishning 2-qadami: SMS'dan kelgan kod. */
@Data
public class OtpVerifyRequest {

    @NotBlank(message = "Telefon raqami kiritilmagan")
    private String phone;

    @NotBlank(message = "Kod kiritilmagan")
    private String code;
}
