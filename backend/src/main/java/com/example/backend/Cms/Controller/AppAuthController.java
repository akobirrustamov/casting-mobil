package com.example.backend.Cms.Controller;

import com.example.backend.DTO.OtpSendDTO;
import com.example.backend.DTO.OtpVerifyDTO;
import com.example.backend.DTO.RefreshRequestDTO;
import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.Services.AuthService.AuthService;
import com.example.backend.Services.AuthService.RefreshTokenService;
import com.example.backend.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Mobil ilova uchun SMS-kod bilan kirish.
 *
 * <h2>⚠️ Nega ESKI kontrollerdan ko'chirildi</h2>
 * Bu ikki endpoint dastlab {@code /api/v1/auth/otp/**} da,
 * ya'ni ESKI casting modulining kontrollerida yozilgan edi.
 *
 * O'sha makon MUZLATILGAN: uni Telegram bot, eski admin sayti va
 * boshqa mijozlar ishlatadi va u yerdagi har o'zgarish ularga tegadi.
 * {@code OldCastingFrozenTest} shuni qo'riqlaydi — va u aynan shu
 * qo'shilishni ushladi.
 *
 * Yangi funksiya yangi makonga ({@code /api/v1/app/**}) tushadi.
 * Bu yerda eski mijozlar yo'q, ya'ni endpoint erkin o'zgarishi
 * mumkin.
 *
 * <h2>Xatti-harakat O'ZGARMADI</h2>
 * Ayni {@link AuthService} metodlari chaqiriladi, ayni DTO'lar,
 * ayni javob shakli. Faqat manzil boshqa.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/app/auth")
public class AppAuthController {

    private final AuthService service;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserRepo userRepo;

    /**
     * Telefonga SMS-kod so'raydi (Eskiz orqali).
     *
     * Ro'yxatdan o'tish ham shu bilan boshlanadi — alohida
     * «registratsiya» endpointi yo'q.
     */
    @PostMapping(value = "/otp/send", consumes = "application/json")
    public HttpEntity<?> sendOtp(@RequestBody OtpSendDTO dto) {
        return service.sendOtp(dto.getPhone());
    }

    /**
     * Kodni tasdiqlaydi: hisob yo'q bo'lsa yaratadi, bor bo'lsa
     * kirgizadi.
     *
     * Javob {@code /auth/login} va {@code /auth/google} bilan BIR XIL
     * shaklda — klient uchta oqim uchun bitta ishlov yozadi.
     */
    @PostMapping(value = "/otp/verify", consumes = "application/json")
    public HttpEntity<?> verifyOtp(@RequestBody OtpVerifyDTO dto) {
        return service.verifyOtp(dto.getPhone(), dto.getCode());
    }

    /**
     * Access tokenni yangilaydi.
     *
     * <h2>⚠️ Nega bu endpoint kerak bo'ldi</h2>
     * Access token 15 daqiqa yashaydi. Mobil ilova refresh tokenni
     * OLARDI, lekin uni saqlamasdi va ishlatmasdi — ya'ni odam har
     * 15 daqiqada tizimdan chiqib ketardi, hatto film o'rtasida ham.
     *
     * <h2>Nega eski {@code /api/v1/auth/refresh} ishlatilmaydi</h2>
     * Uchta sabab:
     *
     * <ul>
     *   <li>u tokenni URL SO'ROV QATORIDA oladi — token server, proksi
     *       va CDN jurnallariga ochiq tushadi;</li>
     *   <li>u ROTATSIYA qilmaydi: bitta refresh token muddati
     *       tugagunicha cheksiz ishlatilaveradi;</li>
     *   <li>u MUZLATILGAN ({@code OldCastingFrozenTest}) — Telegram bot
     *       va eski admin sayti undan foydalanadi.</li>
     * </ul>
     *
     * <h2>Rotatsiya</h2>
     * Har yangilashda eski token bekor qilinadi va YANGISI beriladi.
     * Ya'ni bitta refresh token bir marta ishlaydi.
     *
     * ⚠️ Klient yangi refresh tokenni SAQLASHI shart. Saqlamasa
     * keyingi yangilash «bekor qilingan token» deb rad etilardi va
     * odam tizimdan chiqib ketardi — aynan tuzatilayotgan nosozlik
     * qaytardi.
     *
     * <h2>Javob shakli</h2>
     * {@code /otp/verify} va {@code /auth/google} bilan bir xil:
     * {@code access_token} va {@code refresh_token}. Klient uchta
     * oqim uchun bitta ishlov yozadi.
     */
    @PostMapping(value = "/refresh", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody RefreshRequestDTO dto,
                                                       HttpServletRequest request) {

        String presented = dto == null ? null : dto.getRefreshToken();

        // ⚠️ Eski token AVVAL bekor qilinadi, keyin yangisi beriladi.
        // Tartib teskari bo'lsa, ikkinchi bosqich yiqilganda eski
        // token ham, yangisi ham amalda qolardi.
        UUID userId = refreshTokenService.rotate(presented, request);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN",
                        "Foydalanuvchi topilmadi", HttpStatus.UNAUTHORIZED));

        String rotated = refreshTokenService.issue(user, request);
        refreshTokenService.linkReplacement(presented, rotated);

        return ResponseEntity.ok(Map.of(
                "access_token", jwtService.generateJwtToken(user),
                "refresh_token", rotated));
    }
}
