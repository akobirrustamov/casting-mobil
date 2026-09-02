package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.WatchTargetType;
import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Foydalanuvchi videoni qayerda to'xtatgani.
 *
 * <h2>Bitta odam + bitta video = bitta satr</h2>
 * Yozuv tarixiy emas, JORIY holat: har yangilanishda ustiga yoziladi.
 * Progress har 15 soniyada keladi va tarix saqlansa ikki soatlik film
 * bitta ko'rish uchun ~480 satr qoldirardi.
 *
 * ⚠️ Aynan shu sabab {@code cms_purchase} dan farq qiladi: u
 * O'ZGARMAS moliyaviy yozuv, bu esa o'zgaruvchan holat.
 *
 * <h2>Nega {@code targetId} ga chet el kaliti yo'q</h2>
 * U turga qarab ikki xil jadvalga ishora qiladi — {@code cms_episode}
 * yoki {@code cms_content}. Bitta ustunni ikkala jadvalga bog'lab
 * bo'lmaydi.
 *
 * Osilib qolgan yozuv zararsiz: o'qishda qism yoki kontent baribir
 * yuklanadi va topilmasa satr ro'yxatdan tushib qoladi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_watch_progress", indexes = {
        @Index(name = "uq_watch_progress_user_target",
               columnList = "user_id,type,target_id", unique = true),
        @Index(name = "idx_watch_progress_continue",
               columnList = "user_id,completed,updated_at")
})
public class WatchProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WatchTargetType type;

    /** EPISODE → qism id, CONTENT → kontent id. */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** Qayerda to'xtadi, soniyada. */
    @Column(name = "position_seconds", nullable = false)
    private Integer positionSeconds;

    /**
     * Videoning to'liq davomiyligi — foizni hisoblash uchun nusxa.
     *
     * Bo'sh bo'lishi mumkin: transkodlash tugamagan videoda davomiylik
     * hali noma'lum. Unda foiz ko'rsatilmaydi, davom ettirish esa
     * baribir ishlaydi.
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Odam QO'LDA tanlagan sifat: {@code 1080p}, {@code 720p},
     * {@code 480p} yoki {@code auto}.
     *
     * ⚠️ Joriy sifat EMAS. Auto rejimda pleyer uni doim o'zgartiradi
     * va oxirgi qiymatni saqlash keyingi safar tanlovni yolg'on
     * qilardi: odam «Auto» qo'ygan bo'lsa ham video qat'iy 480p da
     * ochilardi.
     */
    @Column(length = 16)
    private String quality;

    /**
     * Oxirigacha ko'rilgan.
     *
     * «Ko'rishda davom eting» ro'yxatidan chiqariladi — tugatilgan
     * filmni qayta taklif qilish xato bo'lardi.
     */
    @Column(nullable = false)
    private boolean completed;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
