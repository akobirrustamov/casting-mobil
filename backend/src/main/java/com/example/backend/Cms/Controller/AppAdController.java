package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Dto.HomeFeedDto;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Service.HomeFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Butun ekranni yopadigan banner — «Majburiy reklama» (maket, 09.09.2026).
 *
 * <h2>Nima uchun alohida manzil, fid ichida emas</h2>
 * Bu ekranning qismi emas: u ustidan chiqadi va uni yopish kerak. Fidga
 * qo'shilsa, klient uni qatorlar orasidan qidirardi, «ko'rsatildimi»
 * degan holat esa qatorlar bilan aralashib ketardi.
 *
 * <h2>⚠️ Manzil OCHIQ, va bu ataylab</h2>
 * Aynan tizimga kirmagan odam reklamani ko'radi: obuna sotib olgani —
 * yo'q ({@link com.example.backend.Cms.Enums.AdAudience}). Token
 * yuborilsa hisobga olinadi — o'shanda obunachi reklamasiz qoladi.
 * Yopiq qilinsa, reklama faqat kirganlarga ko'rinardi, ya'ni teskarisi.
 */
@RestController
@RequestMapping("/api/v1/app/ads")
@RequiredArgsConstructor
public class AppAdController {

    private final HomeFeedService homeFeedService;

    /**
     * @return banner yoki 204, agar ko'rsatadigan narsa bo'lmasa
     */
    @GetMapping("/interstitial")
    public ResponseEntity<HomeFeedDto.BannerCard> interstitial(
            @RequestParam(defaultValue = "UZ") Locale locale) {

        HomeFeedDto.BannerCard banner =
                homeFeedService.interstitial(CurrentUser.getOrNull(), locale);

        // ⚠️ 204, а не null tanasi bilan 200: klient uchun «hech narsa
        // yo'q» va «javob keldi, lekin bo'sh» bir xil ko'rinmasligi kerak.
        return banner == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(banner);
    }
}
