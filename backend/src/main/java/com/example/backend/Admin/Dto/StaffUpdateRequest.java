package com.example.backend.Admin.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Xodim ma'lumotlarini tahrirlash.
 *
 * ⚠️ Rol va ruxsatlar bu yerda YO'Q — ular alohida endpointlar orqali
 * o'zgaradi ({@code /role}, {@code /permissions}). Sabab: ular boshqa
 * xavfsizlik qoidalariga bo'ysunadi (ierarxiya, «o'zida bo'lmaganini
 * bera olmaydi») va ularni oddiy tahrirlash bilan aralashtirish
 * tekshiruvlarni yashirib qo'yardi.
 *
 * Parol ham bu yerda emas — {@code /password} endpointi orqali.
 */
@Data
public class StaffUpdateRequest {

    @NotBlank(message = "Ism kiritilmagan")
    @Size(min = 2, max = 100, message = "Ism 2-100 belgi bo'lishi kerak")
    private String name;

    /** ТЗ formati: +998 XX XXX XX XX */
    @NotBlank(message = "Telefon raqami kiritilmagan")
    @Pattern(regexp = "^\\+998\\s?\\d{2}\\s?\\d{3}\\s?\\d{2}\\s?\\d{2}$",
            message = "Telefon formati: +998 XX XXX XX XX")
    private String phone;

    @Email(message = "Email formati noto'g'ri")
    private String email;

    private String avatarUrl;
}
