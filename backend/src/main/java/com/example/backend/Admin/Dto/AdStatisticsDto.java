package com.example.backend.Admin.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Bitta reklamaning statistikasi (ТЗ §29).
 *
 * Talab qilingan beshta ko'rsatkich: impressions · clicks ·
 * unique impressions · unique clicks · CTR — jami va kunlik kesimda.
 */
@Data
@Builder
public class AdStatisticsDto {

    private Long advertisementId;
    private LocalDate from;
    private LocalDate to;

    private Long impressions;
    private Long clicks;

    /**
     * ⚠️ Kunlik unikallar YIG'INDISI, davr bo'yicha distinct EMAS (D25).
     * Bir odam ikki kun ko'rsa — ikki marta sanaladi. Davr bo'yicha aniq
     * distinct millionlab xom hodisani skanerlashni talab qilardi.
     */
    private Long uniqueImpressions;
    private Long uniqueClicks;

    /** Foizda, ikki xonagacha yaxlitlangan. */
    private Double ctr;

    /** Kunlik kesim — grafik chizish uchun. */
    private List<DayRow> daily;

    @Data
    @Builder
    public static class DayRow {
        private LocalDate date;
        private Long impressions;
        private Long clicks;
        private Long uniqueImpressions;
        private Long uniqueClicks;
        private Double ctr;
    }
}
