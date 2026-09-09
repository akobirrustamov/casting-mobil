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

    /**
     * Davr ichida qo'yilgan «yoqdi».
     *
     * ⚠️ BU KO'RSATKICH ORQAGA QARAB O'ZGARADI. «Yoqdi» olib
     * tashlanganda yozuv jadvaldan o'chadi, ya'ni dushanba qo'yilib
     * juma kuni yechilgan «yoqdi» dushanba sonidan ham yo'qoladi.
     * Ko'rishlar bilan solishtirib bo'lmaydi: ko'rish — sodir bo'lgan
     * voqea, «yoqdi» esa hozirgi HOLAT.
     */
    private Long likes;

    /**
     * Hozir turgan JAMI «yoqdi» — davrdan qat'i nazar.
     *
     * Aynan shu son ilovada kontent ostida turadi
     * ({@code cms_content.like_count}). Davr bo'yicha songa qarab
     * «ilovada boshqacha ko'rsatilyapti» degan savol tug'ilmasin uchun
     * ikkalasi yonma-yon beriladi.
     */
    private Long likesTotal;

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

        /** Shu kuni qo'yilgan va HOZIRGACHA turgan «yoqdi». */
        private Long likes;
    }
}
