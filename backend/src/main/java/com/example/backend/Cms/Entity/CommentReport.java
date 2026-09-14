package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.CommentReportReason;
import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Izohga shikoyat (13.09.2026).
 *
 * <h2>Nima uchun kerak</h2>
 * Google Play'ning UGC siyosati: foydalanuvchilar kontent yozadigan
 * ilovada begona yozuvga SHIKOYAT QILISH yo'li bo'lishi shart.
 * Tekshiruvchi izohlar ro'yxatini ochib, aynan shu tugmani qidiradi —
 * topmasa, rad javobi keladi.
 *
 * <h2>Nima uchun alohida jadval</h2>
 * {@link Comment#getReportsCount()} ustuni boshidan bor edi va admin
 * paneldagi moderatsiya ro'yxati u bo'yicha filtrlaydi. Faqat
 * hisoblagichni oshirish yetarli ko'rinadi — lekin u holda bir odam
 * bitta izohga necha marta xohlasa shuncha shikoyat qilib, uni
 * navbatning tepasiga sun'iy ravishda chiqarib qo'yardi. Jadval «kim
 * shikoyat qilgan» ni eslab qoladi, shuning uchun takroriy so'rov
 * hisoblagichga ta'sir qilmaydi.
 *
 * Shikoyat O'CHIRILMAYDI: moderator qarori qabul qilingandan keyin ham
 * «bu izohga necha kishi shikoyat qilgan» tarixi qoladi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_comment_report",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_comment_report_user",
                columnNames = {"comment_id", "user_id"}))
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    /** Kim shikoyat qildi. Moderatorga ko'rsatilmaydi — faqat takrorni to'sish uchun. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CommentReportReason reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
