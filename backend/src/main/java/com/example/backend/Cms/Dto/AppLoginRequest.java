package com.example.backend.Cms.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Mobil ilovaga kirish: telefon + parol.
 *
 * ⚠️ Eski {@code /api/v1/auth/login} dan farqi:
 * <ul>
 *   <li>refresh token HAR DOIM beriladi — mobil ilovada «meni eslab
 *       qol» degan tanlov yo'q, sessiya uzoq yashashi kerak;</li>
 *   <li>javob shakli {@code /otp/verify} va {@code /auth/google} bilan
 *       bir xil — klient uchta oqim uchun bitta ishlov yozadi;</li>
 *   <li>eski endpoint MUZLATILGAN makonda ({@code OldCastingFrozenTest}).</li>
 * </ul>
 */
@Data
public class AppLoginRequest {

    @NotBlank(message = "Telefon raqami kiritilmagan")
    private String phone;

    @NotBlank(message = "Parol kiritilmagan")
    private String password;
}
