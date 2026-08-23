package com.example.backend.Admin.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginRequest {

    @NotBlank(message = "Telefon raqami kiritilmagan")
    private String phone;

    @NotBlank(message = "Parol kiritilmagan")
    private String password;
}
