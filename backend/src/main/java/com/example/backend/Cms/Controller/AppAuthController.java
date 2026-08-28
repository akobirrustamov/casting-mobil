package com.example.backend.Cms.Controller;

import com.example.backend.DTO.OtpSendDTO;
import com.example.backend.DTO.OtpVerifyDTO;
import com.example.backend.Services.AuthService.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
