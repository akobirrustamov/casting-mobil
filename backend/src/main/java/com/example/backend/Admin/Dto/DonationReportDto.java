package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Donat hisoboti (ТЗ §42).
 *
 * <h2>Nima uchun valyutalar QO'SHILMAYDI</h2>
 * STARS va COIN — ikki xil valyuta va ularning kursi admin panelida
 * alohida belgilanadi (§40, §41). Ularni bitta «jami» ga qo'shish
 * 10 so'm va 10 dollarni qo'shishday bo'lardi.
 *
 * Kurs hozircha 0 (buyurtmachi aytmagan), shuning uchun so'mdagi ekvivalent
 * ham hisoblanmaydi — soxta raqam chiqmasin.
 */
@Data
@Builder
public class DonationReportDto {

    /** Jami tranzaksiyalar soni — valyutadan qat'i nazar. */
    private Long totalTransactions;

    /** Valyuta bo'yicha jamlanma: STARS va COIN alohida. */
    private List<KindTotal> byKind;

    private List<TargetRow> topCreators;
    private List<TargetRow> topContent;

    /** Kunlik summalar — grafik uchun. */
    private List<DayRow> daily;

    /**
     * Oylik summalar (ТЗ §42).
     *
     * Kunlikdan hisoblab bo'lmaydi: kunlik kesim qisqa oyna uchun,
     * oylik esa uzoq tendensiyani ko'rsatadi.
     */
    private List<MonthRow> monthly;

    @Data
    @Builder
    public static class KindTotal {
        private CurrencyKind kind;
        private Long total;
        private Long transactions;
    }

    @Data
    @Builder
    public static class TargetRow {
        private DonationTargetType targetType;
        private Long targetId;

        /** Ijodkor ismi yoki kontent sarlavhasi; topilmasa {@code null}. */
        private String targetName;
        private CurrencyKind kind;
        private Long total;
        private Long transactions;
    }

    @Data
    @Builder
    public static class MonthRow {
        private Integer year;
        private Integer month;
        private CurrencyKind kind;
        private Long total;
        private Long transactions;
    }

    @Data
    @Builder
    public static class DayRow {
        private LocalDate date;
        private CurrencyKind kind;
        private Long total;
        private Long transactions;
    }
}
