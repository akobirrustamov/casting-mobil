package com.example.backend.Cms.Entity;

import com.example.backend.Entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bitta odamning bitta kontentga qo'ygan «yoqdi» si.
 *
 * <h2>⚠️ Nega bu «Saqlanganlar» dan alohida jadval</h2>
 * {@link UserFavorite} — odamning SHAXSIY ro'yxati: uni faqat egasi
 * ko'radi, va u «keyin ko'raman» degani. «Yoqdi» esa OMMAVIY sanoq: uni
 * hamma ko'radi, va u kontent haqida gapiradi, odam haqida emas.
 *
 * Ikkalasini bitta yozuv qilib qo'yilsa, ro'yxatdan olib tashlagan odam
 * kontentdan «yoqdi» ni ham yulib olardi — va aksincha, «yoqdi» bosgan
 * odamning shaxsiy ro'yxati o'zi so'ramagan yozuv bilan to'lardi.
 *
 * <h2>Yagona himoya — baza</h2>
 * Takroriy bosish yoki qayta yuborilgan so'rov ikkinchi yozuv yaratmaydi:
 * {@code uq_content_like_user} buni bazada to'xtatadi. Tekshiruvni faqat
 * koddagi «avval qarab olamiz» ga tashlab bo'lmaydi — ikkita so'rov bir
 * vaqtda kelsa, ikkalasi ham «yo'q ekan» deb ko'radi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_content_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_content_like_user",
                columnNames = {"content_id", "user_id"}),
        indexes = {
                @Index(name = "idx_content_like_content", columnList = "content_id"),
                @Index(name = "idx_content_like_user", columnList = "user_id,created_at")
        })
public class ContentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
