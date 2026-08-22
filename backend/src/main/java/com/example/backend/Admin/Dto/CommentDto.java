package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Enums.CommentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Moderatsiya ro'yxatining bitta qatori (ТЗ §34).
 *
 * <h2>Telefon raqami nima uchun shartli</h2>
 * Izohni moderatsiya qilish uchun muallifning KIMLIGI yetarli: ismi va
 * ID'si. Telefon raqami esa shaxsiy ma'lumot va u boshqa vazifaga —
 * foydalanuvchini boshqarishga tegishli.
 *
 * Ilgari u har doim qaytarilardi. Natijada FAQAT {@code COMMENT_VIEW}
 * ruxsati berilgan xodim ham har bir izoh muallifining telefonini
 * ko'rardi — ya'ni izoh moderatsiyasi butun foydalanuvchi bazasining
 * telefon raqamlariga ochiq eshik bo'lardi.
 *
 * Endi u faqat {@code USER_VIEW} ruxsati bor xodimga ko'rinadi.
 */
@Data
@Builder
public class CommentDto {

    private Long id;
    private UUID authorId;
    private String authorName;
    /** ⚠️ Faqat {@code USER_VIEW} ruxsati bo'lsa to'ldiriladi. */
    private String authorPhone;
    private Long contentId;
    private String contentSlug;
    private Long episodeId;
    private String text;
    private CommentStatus status;
    private Integer reportsCount;
    private LocalDateTime createdAt;
    private LocalDateTime moderatedAt;

    /**
     * Telefonsiz variant — moderatsiya uchun yetarli.
     *
     * Standart shu: telefonni ko'rsatish ATAYLAB so'ralishi kerak, aks
     * holda yangi chaqiruv joyi uni beixtiyor oshkor qilardi.
     */
    public static CommentDto from(Comment c) {
        return from(c, false);
    }

    /**
     * @param includePhone chaqiruvchida {@code USER_VIEW} ruxsati bormi
     */
    public static CommentDto from(Comment c, boolean includePhone) {
        return CommentDto.builder()
                .id(c.getId())
                .authorId(c.getAuthor() == null ? null : c.getAuthor().getId())
                .authorName(c.getAuthor() == null ? null : c.getAuthor().getName())
                .authorPhone(!includePhone || c.getAuthor() == null
                        ? null : c.getAuthor().getPhone())
                .contentId(c.getContent() == null ? null : c.getContent().getId())
                .contentSlug(c.getContent() == null ? null : c.getContent().getSlug())
                .episodeId(c.getEpisode() == null ? null : c.getEpisode().getId())
                .text(c.getText())
                .status(c.getStatus())
                .reportsCount(c.getReportsCount())
                .createdAt(c.getCreatedAt())
                .moderatedAt(c.getModeratedAt())
                .build();
    }
}
