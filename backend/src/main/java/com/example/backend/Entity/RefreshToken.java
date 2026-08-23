package com.example.backend.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Berilgan refresh tokenlar ro'yxati (§61).
 *
 * <h2>Nega kerak</h2>
 * JWT o'z-o'zidan tekshiriladi — server uni bekor qila olmaydi. Ya'ni
 * bu jadvalsiz «chiqish» faqat klient tomonida bo'lardi: o'g'irlangan
 * token muddati tugaguncha ishlayverardi va admin hech narsa qila
 * olmasdi. ТЗ esa {@code logout/revoke} ni aniq talab qiladi.
 *
 * <h2>Rotatsiya va o'g'rilikni aniqlash</h2>
 * Har yangilashda eski token bekor qilinadi va yangisi beriladi
 * ({@code replacedBy} orqali zanjir tuziladi). Agar ALLAQACHON bekor
 * qilingan token qayta ishlatilsa — demak nusxasi birovda: o'sha
 * foydalanuvchining butun zanjiri bekor qilinadi. Bu «token
 * o'g'irlandi» degan yagona ishonchli belgi.
 */
@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refresh_user", columnList = "user_id"),
        @Index(name = "idx_refresh_expires", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    /** Token ichidagi {@code jti} — token matnining o'zi saqlanmaydi. */
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /** Rotatsiyada shu tokendan keyin berilgan token. */
    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Qaysi qurilmadan berilgani — foydalanuvchi «faol sessiyalar»
     * ro'yxatida o'zini taniy olishi uchun.
     *
     * ⚠️ Token matnining o'zi hech qachon saqlanmaydi: baza o'qilsa
     * ham undan sessiyani tiklab bo'lmasin.
     */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(length = 64)
    private String ip;

    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
