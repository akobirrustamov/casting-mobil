package com.example.backend.Cms.Controller;

import com.example.backend.Cms.Dto.OtpCompleteRequest;
import com.example.backend.Cms.Dto.OtpSendRequest;
import com.example.backend.Cms.Dto.OtpVerifyRequest;
import com.example.backend.Cms.Service.AppAccountService;
import com.example.backend.DTO.RefreshRequestDTO;
import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.Services.AuthService.RefreshTokenService;
import com.example.backend.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Mobil ilovaning kirish endpointlari: telefon raqam va SMS-kod.
 *
 * <h2>Bitta yo'l</h2>
 * <ol>
 *   <li>{@code /otp/send} — raqamga kod (hisob bor-yo'qligidan qat'i
 *       nazar);</li>
 *   <li>{@code /otp/verify} — kod. Hisobi bori shu yerda kiradi,
 *       yangisiga {@code name_required=true} qaytadi;</li>
 *   <li>{@code /otp/complete} — ism. Hisob shu yerda yaratiladi va
 *       sessiya beriladi.</li>
 * </ol>
 *
 * <h2>⚠️ Parolli endpointlar O'CHIRILDI</h2>
 * {@code /register/start}, {@code /register/confirm},
 * {@code /register/complete} va {@code /login} bu yerda edi
 * (buyurtmachi 01.09.2026 so'ragan ikki bo'lim). 04.09.2026 da
 * buyurtmachi parolni butunlay bekor qildi — ular olib tashlandi.
 *
 * Ularni ishlatadigan boshqa mijoz yo'q edi: bu makon
 * ({@code /api/v1/app/**}) faqat mobil ilovaniki, eski mijozlar esa
 * MUZLATILGAN {@code /api/v1/auth/**} da qoladi
 * ({@code OldCastingFrozenTest} shuni qo'riqlaydi). Admin panelning
 * paroli boshqa yerda — {@code /api/v1/app/admin/auth/login}.
 *
 * <h2>⚠️ Nega ESKI kontrollerdan ko'chirilgan edi</h2>
 * OTP endpointlari dastlab {@code /api/v1/auth/otp/**} da, ya'ni eski
 * casting modulining kontrollerida yozilgan edi. O'sha makon
 * MUZLATILGAN: uni Telegram bot, eski admin sayti va boshqa mijozlar
 * ishlatadi. Yangi funksiya yangi makonga tushadi — bu yerda endpoint
 * erkin o'zgarishi mumkin, va aynan shuning uchun bugungi o'zgarish
 * hech kimni sindirmaydi.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/app/auth")
public class AppAuthController {

    private final AppAccountService accountService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserRepo userRepo;

    /**
     * 1-qadam: telefonga SMS-kod (Eskiz orqali).
     *
     * Kirish ham, ro'yxatdan o'tish ham shu bilan boshlanadi — ular
     * ajratilmagan.
     */
    @PostMapping(value = "/otp/send", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> sendOtp(@Valid @RequestBody OtpSendRequest dto) {
        int expiresIn = accountService.startLogin(dto.getPhone());
        return ResponseEntity.ok(Map.of("sent", true, "expiresInSeconds", expiresIn));
    }

    /**
     * 2-qadam: kodni tekshiradi.
     *
     * <p>Javob ikki xil bo'ladi:
     * <ul>
     *   <li>ismli hisobi bor odam — to'liq sessiya va
     *       {@code name_required=false};</li>
     *   <li>yangi (yoki ismsiz) odam — faqat {@code name_required=true}
     *       va {@code expiresInSeconds}. Token BERILMAYDI.</li>
     * </ul>
     *
     * ⚠️ Klient aynan {@code name_required} ga qarab yo'l tanlaydi,
     * {@code access_token} bor-yo'qligiga emas: bayroq aniq, tokenning
     * yo'qligi esa tasodifga o'xshaydi.
     */
    @PostMapping(value = "/otp/verify", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest dto,
                                                         HttpServletRequest request) {
        return ResponseEntity.ok(accountService.verifyLogin(dto.getPhone(), dto.getCode(), request));
    }

    /**
     * 3-qadam: yangi foydalanuvchining ismi.
     *
     * Muvaffaqiyatda hisob yaratiladi va sessiya beriladi — odam
     * qaytadan kirmaydi, to'g'ri bosh sahifaga o'tadi.
     *
     * ⚠️ Faqat 2-qadamdan o'tgan raqam uchun ishlaydi. Tasdiqlash
     * muddati o'tgan bo'lsa — {@code PHONE_NOT_VERIFIED}.
     */
    @PostMapping(value = "/otp/complete", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> completeOtp(@Valid @RequestBody OtpCompleteRequest dto,
                                                           HttpServletRequest request) {
        return ResponseEntity.ok(accountService.completeLogin(dto.getPhone(), dto.getName(), request));
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
