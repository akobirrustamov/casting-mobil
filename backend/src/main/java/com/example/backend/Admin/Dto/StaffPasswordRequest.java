package com.example.backend.Admin.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Xodim parolini tiklash.
 *
 * <h2>Nega parol ADMIN tomonidan beriladi, tasodifiy generatsiya emas</h2>
 * Generatsiya qilingan parolni foydalanuvchiga yetkazish kerak bo'lardi —
 * ya'ni u javob tanasida, keyin esa ehtimol log yoki chatda paydo bo'lardi.
 * Admin uni o'zi kiritsa, parol faqat bitta so'rovda yuriydi va javobda
 * umuman qaytarilmaydi.
 *
 * Talab {@code StaffCreateRequest} bilan bir xil (ТЗ R15).
 */
@Data
public class StaffPasswordRequest {

    @NotBlank(message = "Parol kiritilmagan")
    @Size(min = 8, message = "Parol kamida 8 belgi bo'lishi kerak")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Parolda kamida bitta harf va bitta raqam bo'lishi kerak")
    private String password;
}
