package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.FavoriteType;
import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Foydalanuvchining sevimlilar ro'yxatidagi bitta yozuv.
 *
 * <h2>⚠️ Nega server tomonda</h2>
 * Ilgari sevimlilar FAQAT telefonda saqlanardi
 * ({@code mobile/src/features/favorites/store.ts}). Ya'ni:
 *
 * <ul>
 *   <li>ilovani qayta o'rnatsa — ro'yxat yo'qolardi;</li>
 *   <li>ikkinchi qurilmada — bo'sh;</li>
 *   <li>telefon almashsa — hammasi ketardi.</li>
 * </ul>
 *
 * Foydalanuvchi buni ma'lumot yo'qolishi deb his qiladi, dastur
 * xatosi deb emas.
 *
 * <h2>{@code targetId} — qaysi jadval</h2>
 * Turga qarab boshqa jadvalga ishora qiladi: {@code CREATOR} uchun
 * eski moduldagi {@code CastingUser.id}, {@code CONTENT} uchun
 * {@code Content.id}.
 *
 * ⚠️ Chet el kaliti ATAYLAB qo'yilmagan. {@code CastingUser} —
 * muzlatilgan eski modul; unga bog'lanish yangi jadvalni uning
 * hayotiy sikliga bog'lab qo'yardi. Ishora qilingan yozuv o'chirilsa
 * sevimli «osilib» qoladi va bu zararsiz: klient baribir mavjud
 * ro'yxat bo'yicha filtrlaydi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_user_favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_favorite_user_target",
                columnNames = {"user_id", "type", "target_id"}),
        indexes = {
                @Index(name = "idx_favorite_user_type", columnList = "user_id,type,created_at")
        })
public class UserFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FavoriteType type;

    /**
     * ⚠️ {@code bigint} — {@code CastingUser.id} hozir {@code Integer},
     * {@code Content.id} esa {@code Long}. Ikkalasi bitta ustunga
     * tushadi, shuning uchun kengrog'i olinadi.
     */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /**
     * Qachon qo'shilgan.
     *
     * Ro'yxat shu bo'yicha tartiblanadi: oxirgi qo'shilgani yuqorida —
     * odam aynan uni qidiradi.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
