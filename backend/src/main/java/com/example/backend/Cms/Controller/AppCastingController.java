package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Dto.CastingApplicationRequest;
import com.example.backend.Cms.Dto.MyCastingApplicationDto;
import com.example.backend.Cms.Service.AppCastingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Casting anketasi — mobil ilova tomoni.
 *
 * <h2>Nega eski {@code /api/v1/casting-user} emas</h2>
 * U MUZLATILGAN ({@code OldCastingFrozenTest}): Telegram bot va eski admin
 * sayti undan foydalanadi, egasini esa {@code telegramId} orqali taniydi va
 * {@code /my/{telegramId}} TOKENSIZ ochiq — ya'ni id'ni bilgan har kim
 * begona anketani o'qiy oladi. Ilova uchun bu yaramaydi: bu yerda odam JWT
 * orqali tanilgan va faqat O'Z anketalarini ko'radi.
 *
 * Ma'lumot esa o'sha {@code casting_user} jadvaliga tushadi — admin uni
 * eski saytda bot anketalari bilan bir ro'yxatda ko'rib chiqadi
 * ({@link AppCastingService}).
 *
 * <h2>Kirish</h2>
 * Token talab qilinadi: yo'l {@code /api/**} qoidasiga tushadi
 * ({@code SecurityConfig}) va alohida ochilmagan.
 */
@RestController
@RequestMapping("/api/v1/app/casting")
@RequiredArgsConstructor
public class AppCastingController {

    private final AppCastingService castingService;

    /**
     * Yangi anketa.
     *
     * Xato kodlari:
     * <ul>
     *   <li>{@code VALIDATION_ERROR} (422) — maydon xatosi, {@code errors[]} bilan;</li>
     *   <li>{@code CASTING_APPLICATION_PENDING} (409) — ko'rib chiqilayotgan
     *       ariza bor;</li>
     *   <li>{@code CASTING_PHOTO_NOT_FOUND} (422) — rasm id'si topilmadi;</li>
     *   <li>{@code CASTING_PHOTO_IN_USE} (409) — rasm boshqa arizaga biriktirilgan.</li>
     * </ul>
     */
    @PostMapping("/applications")
    public ResponseEntity<MyCastingApplicationDto> submit(
            @Valid @RequestBody CastingApplicationRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(castingService.submit(CurrentUser.get(), body));
    }

    /**
     * Mening arizalarim — yangisi yuqorida.
     *
     * Sahifalanmagan: ro'yxat faqat admin javob bergandan keyin o'sadi
     * (bir vaqtda bitta kutilayotgan ariza), sababi {@code ApiConventionTest}
     * da yozilgan.
     */
    @GetMapping("/applications/my")
    public ResponseEntity<List<MyCastingApplicationDto>> myCastingApplications() {
        return ResponseEntity.ok(castingService.mine(CurrentUser.get()));
    }
}
