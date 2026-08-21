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

    public static CurrencyPackageDto from(CurrencyPackage p) {
        return CurrencyPackageDto.builder()
                .id(p.getId())
                .kind(p.getKind())
                .amount(p.getAmount())
                .price(p.getPrice())
                .active(p.getActive())
                .sortOrder(p.getSortOrder())
                .build();
    }
}
