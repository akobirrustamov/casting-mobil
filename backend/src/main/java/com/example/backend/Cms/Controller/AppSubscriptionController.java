package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Entity.Subscription;
import com.example.backend.Cms.Entity.Tariff;
import com.example.backend.Cms.Entity.TariffTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.SubscriptionSource;
import com.example.backend.Cms.Repository.SubscriptionRepo;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.HomeFeedService;
import com.example.backend.Cms.Service.TranslationPicker;
import com.example.backend.Entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * «Mening obunam» — holat va tarix.
 *
 * <h2>Nima uchun {@code /app/me} dagi {@code premium} yetarli emas</h2>
 * U ikki maydon beradi: faolmi va qachongacha. Ekranda esa boshqa
 * savollar ham bor: qaysi tarif edi, qancha to'landi, qachondan
 * boshlangan. Ular bo'lmasa «to'lovlar tarixi» bo'limi bo'sh qolardi —
 * garchi ma'lumot bazada yotgan bo'lsa ham.
 *
 * <h2>⚠️ Holat SHU YERDA hisoblanmaydi</h2>
 * «Obuna faolmi» degan qaror bitta joyda turishi shart (ТЗ §37) —
 * {@code AccessService.premiumStatus}. Bu kontroller uni o'zi
 * hisoblasa, qoidaning ikkinchi nusxasi paydo bo'lardi: masalan bu
 * yerda {@code revokedAt} unutilsa, tortib olingan obuna ekranda faol
 * bo'lib ko'rinardi.
 *
 * Aynan shu sabab {@code AppProfileController} da ham yozilgan.
 *
 * <h2>Nima uchun sahifalash yo'q</h2>
 * Bitta odamning obunalari — o'nlab yozuv, yuzlab emas. Sahifalash
 * bu yerda faqat klientga qo'shimcha ish qo'shardi.
 */
@RestController
@RequestMapping("/api/v1/app/me/subscription")
@RequiredArgsConstructor
public class AppSubscriptionController {

    private final SubscriptionRepo subscriptionRepo;
    private final AccessService accessService;
    private final HomeFeedService homeFeedService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<SubscriptionResponse> mine(@RequestParam(required = false) Locale locale) {
        User user = CurrentUser.get();
        Locale resolved = homeFeedService.resolveLanguage(user, locale);
        LocalDateTime now = LocalDateTime.now();

        AccessService.PremiumStatus status = accessService.premiumStatus(user);

        List<SubscriptionDto> history = subscriptionRepo
                .findAllByUserIdOrderByEndAtDesc(user.getId()).stream()
                .map(s -> map(s, resolved, now))
                .toList();

        return ResponseEntity.ok(SubscriptionResponse.builder()
                .active(status.active())
                .until(status.until())
                .history(history)
                .build());
    }

    private SubscriptionDto map(Subscription s, Locale locale, LocalDateTime now) {
        Tariff tariff = s.getTariff();

        return SubscriptionDto.builder()
                .id(s.getId())
                .tariffCode(tariff == null ? null : tariff.getCode())
                .tariffName(tariff == null ? null : TranslationPicker.pickValue(
                        tariff.getTranslations(), locale,
                        TariffTranslation::getLocale, TariffTranslation::getName))
                .startAt(s.getStartAt())
                .endAt(s.getEndAt())
                .source(s.getSource())
                .paidAmount(s.getPaidAmount())
                .currency(tariff == null ? null : tariff.getCurrency())
                .revokedAt(s.getRevokedAt())
                .active(s.isActiveAt(now))
                .build();
    }

    // ------------------------------------------------------------------ DTO

    @Data
    @Builder
    public static class SubscriptionResponse {
        /** Hozir Premium ochiqmi. */
        private boolean active;

        /**
         * Qachongacha. {@code null} — obuna umuman bo'lmagan.
         *
         * ⚠️ Muddati o'tgan obunada sana SAQLANADI va {@code active =
         * false} bo'ladi: ilova «obunangiz tugadi» deb aniq ayta oladi,
         * «obuna yo'q» emas. Ular odam uchun boshqa-boshqa narsa —
         * birinchisida u pul to'lagan va uzaytirishi mumkin.
         */
        private LocalDateTime until;

        /** Yangi tugaydiganidan boshlab. Bo'sh bo'lishi mumkin. */
        private List<SubscriptionDto> history;
    }

    @Data
    @Builder
    public static class SubscriptionDto {
        private Long id;

        /** Barqaror kod: {@code m1}, {@code y1}. Klient shunga tayanadi. */
        private String tariffCode;

        /**
         * Tanlangan tildagi nom.
         *
         * ⚠️ Tarif {@code null} bo'lishi mumkin: {@code ADMIN_GIFT}
         * obunasi hech qanday tarifga bog'lanmagan bo'lishi mumkin.
         * Klient bunda manbani ko'rsatadi — «Sovg'a».
         */
        private String tariffName;

        private LocalDateTime startAt;
        private LocalDateTime endAt;

        /** {@code PURCHASE} yoki {@code ADMIN_GIFT}. */
        private SubscriptionSource source;

        /**
         * To'langan summa. {@code null} — bu daromad EMAS (sovg'a).
         *
         * ⚠️ 0 emas, aynan {@code null}: nol «bepul sotib olindi»
         * degan ma'no berardi.
         */
        private BigDecimal paidAmount;

        private String currency;

        /** Muddatidan oldin tortib olingan bo'lsa. */
        private LocalDateTime revokedAt;

        /** Shu yozuv AYNI PAYTDA amal qilyaptimi. */
        private boolean active;
    }
}
