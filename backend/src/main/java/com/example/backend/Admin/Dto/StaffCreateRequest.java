package com.example.backend.Admin.Dto;

import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class StaffCreateRequest {

    @NotBlank(message = "Ism kiritilmagan")
    @Size(min = 2, max = 100, message = "Ism 2-100 belgi bo'lishi kerak")
    private String name;

    /** ТЗ formati: +998 XX XXX XX XX (bo'shliqlarsiz ham qabul qilinadi). */
    @NotBlank(message = "Telefon raqami kiritilmagan")
    @Pattern(regexp = "^\\+998\\s?\\d{2}\\s?\\d{3}\\s?\\d{2}\\s?\\d{2}$",
            message = "Telefon formati: +998 XX XXX XX XX")
    private String phone;

    @NotBlank(message = "Parol kiritilmagan")
    @Size(min = 8, message = "Parol kamida 8 belgi bo'lishi kerak")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Parolda kamida bitta harf va bitta raqam bo'lishi kerak")
    private String password;

    @NotNull(message = "Rol tanlanmagan")
    private PlatformRole role;

    /** Faqat WORKER uchun ma'noli. Yuqori rollarda e'tiborga olinmaydi. */
    private Set<Permission> permissions;
}
