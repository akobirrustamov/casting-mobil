package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.CurrencyKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Valyuta paketini saqlash so'rovi.
 *
 * <h2>Nima uchun entity'ni to'g'ridan-to'g'ri qabul qilmaymiz</h2>
 * Ilgari controller {@code @RequestBody CurrencyPackage} qabul qilardi va
 * hech qanday tekshiruv yo'q edi. {@code kind} bo'sh yuborilsa, xato faqat
 * BAZADA chiqardi ({@code not null} cheklovi) va admin panelida
 * «500 Internal Server Error» ko'rinardi — ya'ni foydalanuvchi nimani
 * to'ldirmaganini bilmasdi.
 */
@Data
public class CurrencyPackageSaveRequest {

    @NotNull(message = "Valyuta turi tanlanmagan (STARS yoki UZCASTING_COIN)")
    private CurrencyKind kind;

    @NotNull(message = "Miqdor kiritilmagan")
    @Min(value = 1, message = "Miqdor noldan katta bo'lishi kerak")
    private Long amount;

    /** ⚠️ Pul {@code BigDecimal} da — floating point pul uchun yaramaydi. */
    @NotNull(message = "Narx kiritilmagan")
    @DecimalMin(value = "0.0", message = "Narx manfiy bo'lishi mumkin emas")
    private BigDecimal price;

    private Boolean active = true;
    private Integer sortOrder = 0;
}
