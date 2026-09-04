package com.example.backend.Services.AuthService;

import com.example.backend.DTO.UserDTO;
import com.example.backend.Entity.User;
import org.springframework.http.HttpEntity;

import java.util.UUID;

public interface AuthService {
    HttpEntity<?> login(UserDTO dto);

    /** Google ID token'ni tekshirib, o'z JWT'imizni qaytaradi. */
    HttpEntity<?> googleLogin(String idToken);

    HttpEntity<?> refreshToken(String refreshToken);
    User decode(String token);

    User password(UUID adminId, String password);

    // ⚠️ sendOtp/verifyOtp BU YERDAN olib tashlandi (04.09.2026).
    //
    // Telefon orqali kirishning butun oqimi endi `AppAccountService` da:
    // u bitta joyda kodni ham tekshiradi, ismni ham so'raydi va hisobni
    // ham yaratadi. Bu yerda qolgan nusxa ikkinchi, ismsiz hisob
    // yaratadigan yo'l bo'lib turardi — undan foydalanadigan endpoint
    // esa yo'q edi.
}
