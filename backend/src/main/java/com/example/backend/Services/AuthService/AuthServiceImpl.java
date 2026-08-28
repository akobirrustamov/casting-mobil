package com.example.backend.Services.AuthService;

import com.example.backend.DTO.UserDTO;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.GoogleTokenVerifier;
import com.example.backend.Security.JwtService;
import com.example.backend.Sms.OtpService;
import com.example.backend.exceptions.InvalidCredentialsException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final OtpService otpService;

    /**
     * Refresh tokenlarni ro'yxatga oladi (§61).
     *
     * <h2>⚠️ Nega bu qo'shildi — jimgina buzilgan himoya</h2>
     * {@link RefreshTokenService} bekor qilish, rotatsiya va o'g'rilikni
     * aniqlash bilan birga allaqachon YOZILGAN edi. Lekin bu yerdagi
     * kirish oqimlari uni CHETLAB o'tib, {@code jti} siz eski token
     * berardi.
     *
     * Natijada:
     * <ul>
     *   <li>token bazada YO'Q — o'g'irlansa bekor qilib bo'lmasdi;</li>
     *   <li>rotatsiya uni «eski formatda» deb rad etardi — ya'ni
     *       yangilash oqimi mobil ilova uchun umuman ishlamasdi.</li>
     * </ul>
     *
     * Infratuzilma bor edi, undan foydalanuvchi yo'q edi.
     */
    private final RefreshTokenService refreshTokenService;

    /**
     * Qurilma izi uchun — {@code User-Agent} va IP.
     *
     * ⚠️ {@code ObjectProvider}: bu servis so'rovdan TASHQARIDA ham
     * chaqirilishi mumkin (masalan kelajakdagi rejalashtirilgan
     * vazifada). To'g'ridan-to'g'ri {@code HttpServletRequest}
     * kiritilsa, u yerda {@code IllegalStateException} bilan
     * yiqilardi.
     */
    private final ObjectProvider<HttpServletRequest> requestProvider;

    /**
     * Ro'yxatga olingan refresh token.
     *
     * Ilgari bu {@code jwtService.generateJwtRefreshToken(user)} edi —
     * imzolangan, lekin hech qayerda qayd etilmagan token.
     */
    private String issueRefreshToken(User user) {
        return refreshTokenService.issue(user, requestProvider.getIfAvailable());
    }
    @Override
    public HttpEntity<Map<String, Object>> login(UserDTO userDTO) {
        Optional<User> userOpt = userRepo.findByPhone(userDTO.getPhone());
        if (userOpt.isEmpty()) throw new InvalidCredentialsException();

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDTO.getPhone(), userDTO.getPassword()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }

        User user = userOpt.get();

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", jwtService.generateJwtToken(user));

        if (userDTO.isRememberMe()) {
            response.put("refresh_token", issueRefreshToken(user));
        }

        response.put("roles", user.getRoles());
        return ResponseEntity.ok(response);
    }

    /**
     * Google login.
     *
     * Ilova Google'dan olgan ID token'ni yuboradi -> imzo va audience tekshiriladi
     * -> "sub" bo'yicha foydalanuvchi topiladi yoki yaratiladi -> o'z JWT'imiz qaytariladi.
     *
     * Telefon bu bosqichda so'ralmaydi: yangi foydalanuvchida u null bo'ladi.
     * Javobdagi "phone_required" bayrog'i ilovaga telefon so'rash kerakligini bildiradi.
     */
    @Override
    @Transactional
    public HttpEntity<?> googleLogin(String idToken) {
        GoogleIdToken.Payload payload;
        try {
            payload = googleTokenVerifier.verify(idToken);
        } catch (IllegalStateException e) {
            // Server sozlanmagan - bu mijozning aybi emas
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }

        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        User user = userRepo.findByGoogleSub(googleSub)
                .or(() -> userRepo.findByEmail(email))
                .map(existing -> linkGoogle(existing, googleSub, name, picture))
                .orElseGet(() -> createGoogleUser(googleSub, email, name, picture));

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", jwtService.generateJwtToken(user));
        response.put("refresh_token", issueRefreshToken(user));
        response.put("roles", user.getRoles());
        response.put("phone_required", user.getPhone() == null);
        response.put("user", Map.of(
                "id", user.getId().toString(),
                "name", user.getName() == null ? "" : user.getName(),
                "email", user.getEmail() == null ? "" : user.getEmail(),
                "avatarUrl", user.getAvatarUrl() == null ? "" : user.getAvatarUrl()
        ));
        return ResponseEntity.ok(response);
    }

    /** Email bo'yicha topilgan eski hisobga Google'ni bog'laymiz. */
    private User linkGoogle(User user, String googleSub, String name, String picture) {
        boolean changed = false;

        if (user.getGoogleSub() == null) {
            user.setGoogleSub(googleSub);
            changed = true;
        }
        if (user.getName() == null && name != null) {
            user.setName(name);
            changed = true;
        }
        if (user.getAvatarUrl() == null && picture != null) {
            user.setAvatarUrl(picture);
            changed = true;
        }

        return changed ? userRepo.save(user) : user;
    }

    private User createGoogleUser(String googleSub, String email, String name, String picture) {
        Role userRole = roleRepo.findByName(UserRoles.ROLE_USER);

        User user = User.builder()
                .googleSub(googleSub)
                .email(email)
                .name(name)
                .avatarUrl(picture)
                // Parol yo'q: bu hisobga faqat Google orqali kiriladi.
                // Tasodifiy qiymat - hech qachon mos kelmasligi uchun.
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .roles(userRole == null ? List.of() : List.of(userRole))
                .build();

        return userRepo.save(user);
    }

    /** SMS-kod so'raladi. Telefon hali ro'yxatda bo'lmasligi mumkin — bu ro'yxatdan o'tish oqimi ham. */
    @Override
    public HttpEntity<?> sendOtp(String phone) {
        int ttlSeconds = otpService.send(phone);
        return ResponseEntity.ok(Map.of("sent", true, "expiresInSeconds", ttlSeconds));
    }

    /**
     * Kod tekshiriladi va hisob find-or-create qilinadi — xuddi
     * {@link #googleLogin(String)} kabi, faqat Google sub o'rniga telefon
     * kalit bo'ladi. Ro'yxatdan o'tish va kirish shu bitta endpoint.
     */
    @Override
    @Transactional
    public HttpEntity<?> verifyOtp(String phone, String code) {
        otpService.verify(phone, code);
        String normalizedPhone = OtpService.normalize(phone);

        User user = userRepo.findByPhone(normalizedPhone)
                .orElseGet(() -> createPhoneUser(normalizedPhone));

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", jwtService.generateJwtToken(user));
        response.put("refresh_token", issueRefreshToken(user));
        response.put("roles", user.getRoles());
        response.put("user", Map.of(
                "id", user.getId().toString(),
                "name", user.getName() == null ? "" : user.getName(),
                "phone", user.getPhone() == null ? "" : user.getPhone()
        ));
        return ResponseEntity.ok(response);
    }

    /** Google orqali kirgan yangi foydalanuvchi kabi - parol yo'q, tasodifiy hash. */
    private User createPhoneUser(String phone) {
        Role userRole = roleRepo.findByName(UserRoles.ROLE_USER);

        User user = User.builder()
                .phone(phone)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .roles(userRole == null ? List.of() : List.of(userRole))
                .build();

        return userRepo.save(user);
    }

    @Override
    public HttpEntity<?> refreshToken(String refreshToken) {
        String userId = jwtService.extractSubjectFromJwt(refreshToken);
        User user = userRepo.findById(UUID.fromString(userId)).orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtService.generateJwtToken(user);
        return ResponseEntity.ok(Map.of("access_token", newAccessToken));
    }

    @Override
    public User decode(String token) {
        if (!jwtService.validateToken(token)) {
            throw new RuntimeException("Token is expired or invalid");
        }
        String userId = jwtService.extractSubjectFromJwt(token);
        return userRepo.findById(UUID.fromString(userId)).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public User password(UUID adminId, String password) {
        User userNotFound = userRepo.findById(adminId).orElseThrow(() -> new RuntimeException("User not found"));
        userNotFound.setPassword(passwordEncoder.encode(password));
        return userRepo.save(userNotFound);


    }
}
