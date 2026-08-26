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

    /** SMS-kod yuboradi. Foydalanuvchi topilmasa ham ruxsat — bu ro'yxatdan o'tish oqimi. */
    HttpEntity<?> sendOtp(String phone);

    /** Kodni tekshiradi, kerak bo'lsa hisob yaratadi va JWT qaytaradi. */
    HttpEntity<?> verifyOtp(String phone, String code);
}
