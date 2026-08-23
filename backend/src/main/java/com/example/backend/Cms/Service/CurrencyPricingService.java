package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.CurrencyPackage;
import com.example.backend.Cms.Enums.CurrencyKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Yulduz va tanga narxini hisoblaydi (ТЗ §40, §41).
 *
 * <h2>Nima uchun kerak edi</h2>
 * ТЗ: «1 STAR = X UZS admin panel orqali boshqarilishi kerak.» Sozlama
 * ({@code currency.star.rate}) bor edi, admin uni tahrirlay ham olardi —
 * lekin uni HECH QAYERDA o'qilmasdi. Ya'ni kurs bezak edi: admin qiymatni
 * o'zgartirardi va hech narsa o'zgarmasdi.
 *
 * <h2>Ikki manba, aniq tartib</h2>
 * <ol>
 *   <li><b>Paketning o'z narxi</b> — agar belgilangan bo'lsa, u ustun.
 *       Sabab: paketlarda odatda chegirma bo'ladi (1000 ta yulduz
 *       10 tasidan arzonroq), buni kurs bilan ifodalab bo'lmaydi.</li>
 *   <li><b>Kurs × miqdor</b> — paket narxi belgilanmagan bo'lsa.</li>
 * </ol>
 *
 * <h2>⚠️ Ikkalasi ham 0 bo'lsa</h2>
 * Paket SOTIB OLINMAYDI. Bu muhim: V5 barcha paketlarni {@code 0.00}
 * narx bilan qo'shgan (buyurtmachi kursni hali aytmagan) va ular
 * {@code active = true}. Ya'ni ro'yxatda «1000 yulduz — 0 so'm» bo'lib
 * ko'rinardi — bepul yulduz taklifiday. Endi bunday paket ochiq
 * ro'yxatda «narxi belgilanmagan» deb belgilanadi.
 */
@Service
@RequiredArgsConstructor
public class CurrencyPricingService {

    private final SettingsService settingsService;

    /**
     * Paketning haqiqiy narxi.
     *
     * @return narx yoki {@code null} — narx ham, kurs ham belgilanmagan
     */
    public BigDecimal effectivePrice(CurrencyPackage pack) {
        if (pack == null) {
            return null;
        }
        BigDecimal own = pack.getPrice();
        if (own != null && own.compareTo(BigDecimal.ZERO) > 0) {
            return own;
        }
        BigDecimal rate = rateOf(pack.getKind());
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0
                || pack.getAmount() == null || pack.getAmount() <= 0) {
            return null;
        }
        return rate.multiply(BigDecimal.valueOf(pack.getAmount()));
    }

    /**
     * Sotib olish mumkinmi.
     *
     * Narxi noma'lum paketni sotib olish taklif qilinmaydi — aks holda
     * foydalanuvchi «bepul» deb tushunardi.
     */
    public boolean isPurchasable(CurrencyPackage pack) {
        return effectivePrice(pack) != null;
    }

    /** 1 birlik necha so'm. 0 yoki belgilanmagan bo'lsa — {@code null}. */
    public BigDecimal rateOf(CurrencyKind kind) {
        String key = kind == CurrencyKind.STARS
                ? SettingKeys.STAR_RATE : SettingKeys.COIN_RATE;
        BigDecimal rate = settingsService.getMoney(key);
        return rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 ? null : rate;
    }
}
