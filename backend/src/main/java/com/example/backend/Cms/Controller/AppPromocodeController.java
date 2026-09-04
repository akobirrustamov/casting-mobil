package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Entity.PromocodeRedemption;
import com.example.backend.Cms.Enums.PromocodeGrantType;
import com.example.backend.Cms.Service.PromocodeService;
import com.example.backend.Entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Promokod — ilova tomoni.
 *
 * <h2>Nima uchun token talab qilinadi</h2>
 * Kod hisobga Premium qo'shadi — hisob bo'lmasa qo'shadigan joy yo'q.
 * Mehmon kodni ko'rsa ham, uni faqat kirgandan keyin ishlata oladi.
 *
 * <h2>⚠️ Terib ko'rishga qarshi</h2>
 * {@code RateLimitFilter} da alohida qoida: bir IP dan daqiqasiga
 * cheklangan urinish. Aks holda 8 belgili kodlarni skript bilan
 * qidirish mumkin bo'lardi — sekin, lekin mumkin.
 */
@RestController
@RequestMapping("/api/v1/app/promocodes")
@RequiredArgsConstructor
public class AppPromocodeController {

    private final PromocodeService promocodeService;

    /**
     * Kodni ishlatish.
     *
     * Xato kodlari ilovada alohida matnga ega: {@code PROMO_NOT_FOUND},
     * {@code PROMO_EXPIRED}, {@code PROMO_ALREADY_USED},
     * {@code PROMO_EXHAUSTED}, {@code PROMO_INACTIVE}.
     */
    @PostMapping("/redeem")
    public ResponseEntity<RedeemResponse> redeem(@RequestBody RedeemRequest body) {
        User user = CurrentUser.get();
        PromocodeService.Redemption result = promocodeService.redeem(
                user, body == null ? null : body.getCode());

        return ResponseEntity.ok(RedeemResponse.builder()
                .code(result.promocode().getCode())
                .grantType(result.promocode().getGrantType())
                .days(result.promocode().getGrantDays())
                .until(result.premiumUntil())
                .build());
    }

    /** Men ishlatgan kodlar. */
    @GetMapping("/my")
    public ResponseEntity<List<RedemptionDto>> mine() {
        User user = CurrentUser.get();
        return ResponseEntity.ok(promocodeService.mine(user.getId()).stream()
                .map(RedemptionDto::from)
                .toList());
    }

    // ------------------------------------------------------------------ DTO

    @Data
    public static class RedeemRequest {
        private String code;
    }

    @Data
    @Builder
    public static class RedeemResponse {
        private String code;

        /**
         * {@code PREMIUM_DAYS} yoki {@code CASTING_DAYS}.
         *
         * ⚠️ Ilova xabar matnini SHU bo'yicha tanlaydi: «30 kun Premium
         * qo'shildi» va «7 kun casting ochildi» — boshqa-boshqa gaplar,
         * va ikkinchisida odam film ochilishini kutmasligi kerak.
         */
        private PromocodeGrantType grantType;

        private int days;

        /** Endi qachongacha — ilova aynan shuni ko'rsatadi. */
        private LocalDateTime until;
    }

    @Data
    @Builder
    public static class RedemptionDto {
        private String code;
        private PromocodeGrantType grantType;
        private int days;
        private LocalDateTime redeemedAt;

        /**
         * Shu kod bergan huquq qachongacha edi.
         *
         * ⚠️ Qatorning O'ZIDAN olinadi, obunadan emas: casting kodida
         * obuna yozuvi umuman yo'q.
         */
        private LocalDateTime until;

        static RedemptionDto from(PromocodeRedemption r) {
            return RedemptionDto.builder()
                    .code(r.getPromocode().getCode())
                    .grantType(r.getPromocode().getGrantType())
                    .days(r.getPromocode().getGrantDays())
                    .redeemedAt(r.getRedeemedAt())
                    .until(r.getGrantedUntil())
                    .build();
        }
    }
}
