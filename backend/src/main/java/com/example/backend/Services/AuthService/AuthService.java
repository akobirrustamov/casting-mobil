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
}
