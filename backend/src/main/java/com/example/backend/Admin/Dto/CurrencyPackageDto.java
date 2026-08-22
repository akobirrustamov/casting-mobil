package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.CurrencyPackage;
import com.example.backend.Cms.Enums.CurrencyKind;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Valyuta paketi — javob uchun (ТЗ §40, §41).
 *
 * <h2>Nima uchun DTO</h2>
 * Ilgari controller {@code CurrencyPackage} entity'sini to'g'ridan-to'g'ri
 * qaytarardi. Bu API'ni baza sxemasiga bog'lab qo'yadi: jadvalga ustun
 * qo'shilsa u avtomatik javobga chiqadi va aksincha, ustun nomini
 * o'zgartirish mijozni buzadi.
 */
@Data
@Builder
public class CurrencyPackageDto {

    private Long id;
    private CurrencyKind kind;
    private Long amount;

    /** ⚠️ Pul — {@code BigDecimal}. Floating point pul uchun yaramaydi. */
    private BigDecimal price;

    private Boolean active;
    private Integer sortOrder;

    /**
     * Haqiqiy narx: paketning o'z narxi, bo'lmasa kurs × miqdor (ТЗ §40).
     *
     * {@code null} — narx ham, kurs ham belgilanmagan.
     */
    private BigDecimal effectivePrice;

    /**
     * ⚠️ Sotib olish mumkinmi.
     *
     * V5 barcha paketlarni {@code 0.00} narx bilan qo'shgan (buyurtmachi
     * kursni hali aytmagan) va ular {@code active = true}. Bu bayroqsiz
     * ro'yxatda «1000 yulduz — 0 so'm» bo'lib ko'rinardi, ya'ni bepul
     * yulduz taklifiday.
     */
    private Boolean purchasable;

    public static CurrencyPackageDto from(CurrencyPackage p) {
        return from(p, null, false);
    }

    public static CurrencyPackageDto from(CurrencyPackage p,
                                          BigDecimal effectivePrice,
                                          boolean purchasable) {
        return CurrencyPackageDto.builder()
                .id(p.getId())
                .kind(p.getKind())
                .amount(p.getAmount())
                .price(p.getPrice())
                .active(p.getActive())
                .sortOrder(p.getSortOrder())
                .effectivePrice(effectivePrice)
                .purchasable(purchasable)
                .build();
    }
}
