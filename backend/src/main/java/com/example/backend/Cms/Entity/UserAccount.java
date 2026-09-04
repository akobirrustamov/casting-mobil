package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mobil foydalanuvchining platformaga oid holati.
 *
 * <b>Nega alohida jadval, `users` ga ustun qo'shish emas:</b> `users` jadvalini
 * sayt, Telegram bot va mobil ilova birgalikda ishlatadi. Unga ustun qo'shish
 * uchtala klientni ham xavf ostiga qo'yadi. Bu yerda esa faqat yangi
 * funksiyalar uchun kerak bo'lgan holat saqlanadi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_user_account", indexes = {
        @Index(name = "idx_account_user", columnList = "user_id", unique = true),
        @Index(name = "idx_account_status", columnList = "status"),
        @Index(name = "idx_account_premium", columnList = "premium_until")
})
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    /**
     * Premium qachongacha amal qiladi. null — obuna yo'q.
     *
     * Denormalizatsiya: har safar Subscription jadvalidan hisoblash o'rniga
     * shu yerda saqlanadi, chunki bu qiymat HAR bir kirish tekshiruvida kerak.
     */
    @Column(name = "premium_until")
    private LocalDateTime premiumUntil;

    /**
     * Casting bo'limiga kirish qachongacha. {@code null} — berilmagan.
     *
     * <h2>⚠️ Nima uchun Premiumdan alohida</h2>
     * Ilgari casting huquqi «faol Premium» degan ma'noni anglatardi va
     * uni Premiumsiz berishning yo'li yo'q edi. Buyurtmachi esa aynan
     * shunday promokod so'radi: «casting bo'limiga bepul kirish
     * kunlari». Endi ikki muddat alohida yashaydi.
     *
     * Premium bu maydonni ORTIQCHA qilmaydi: {@code canAccessCasting}
     * ikkalasini ham qaraydi, ya'ni Premium kodi casting kodini qamrab
     * oladi, teskarisi esa yo'q.
     */
    @Column(name = "casting_until")
    private LocalDateTime castingUntil;

    /**
     * Foydalanuvchi tanlagan til (§32, §61 qo'shimchasi).
     *
     * <h2>Nega kerak</h2>
     * Kontent, bildirishnoma va reklama uch tilda saqlanadi, lekin
     * FOYDALANUVCHINING tili hech qayerda yozilmasdi. Bosh sahifa uni
     * so'rov parametridan olardi ({@code ?lang=RU}), push xabar esa
     * umuman hech qayerdan — ya'ni FCM ulangach barcha foydalanuvchiga
     * o'zbekcha matn ketardi. Ruscha so'zlashuvchi foydalanuvchi
     * tushunmaydigan xabar oladi.
     *
     * Default UZ: bu «bilmayman» degani emas, davlat tili — mobil ilova
     * birinchi ochilishda uni yuboradi va qiymat aniqlashadi.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 8, nullable = false)
    @Builder.Default
    private Locale language = Locale.UZ;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Hozir faol Premium bormi. */
    public boolean hasActivePremium() {
        return premiumUntil != null && premiumUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Casting bo'limi ochiqmi — Premiumdan qat'i nazar.
     *
     * ⚠️ Bu «casting huquqi bormi» degan savolning TO'LIQ javobi EMAS:
     * Premium ham casting ochadi. To'liq qaror {@code AccessService} da,
     * bitta joyda (ТЗ §37).
     */
    public boolean hasActiveCastingAccess() {
        return castingUntil != null && castingUntil.isAfter(LocalDateTime.now());
    }

    public UUID userId() {
        return user == null ? null : user.getId();
    }
}
