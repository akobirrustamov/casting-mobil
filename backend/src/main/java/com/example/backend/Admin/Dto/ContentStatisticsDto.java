package com.example.backend.Admin.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Bitta kontentning tomosha statistikasi (ТЗ §46).
 *
 * <h2>Voronka</h2>
 * <pre>
 *   CONTENT_VIEW  →  CONTENT_PLAY  →  CONTENT_COMPLETE
 *   (sahifa ochildi)  (o'ynatildi)     (oxirigacha ko'rildi)
 * </pre>
 *
 * Uchala bosqich alohida ma'noga ega:
 * <ul>
 *   <li><b>view → play</b> pastligi — afisha yoki tavsif qiziqtirmayapti;</li>
 *   <li><b>play → complete</b> pastligi — kontentning O'ZI ushlab
 *       turolmayapti.</li>
 * </ul>
 * Ularni bitta «ko'rishlar» soniga qo'shish bu farqni yo'q qilardi.
 */
@Data
@Builder
public class ContentStatisticsDto {

    private Long contentId;
    private LocalDate from;
    private LocalDate to;

    private Long views;
    private Long plays;
    private Long completes;

    /**
     * ⚠️ Kunlik unikallar YIG'INDISI, davr bo'yicha distinct EMAS.
     * Bir odam ikki kun ko'rsa — ikki marta sanaladi. Davr bo'yicha aniq
     * distinct millionlab xom hodisani skanerlashni talab qilardi.
     */
    private Long uniqueViewers;

    /** Ochganlarning necha foizi o'ynatgan. */
    private Double playRate;

    /** O'ynatganlarning necha foizi oxirigacha ko'rgan. */
    private Double completionRate;

    private List<DayRow> daily;

    @Data
    @Builder
    public static class DayRow {
        private LocalDate date;
        private Long views;
        private Long plays;
        private Long completes;
        private Long uniqueViewers;
        private Double completionRate;
    }
}
