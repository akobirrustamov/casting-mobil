package com.example.backend.Admin.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminLoginResponse {

    private String accessToken;
    private String refreshToken;
    private AdminUserDto user;
}
