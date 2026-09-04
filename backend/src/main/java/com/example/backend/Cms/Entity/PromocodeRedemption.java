package com.example.backend.Cms.Entity;

import com.example.backend.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Promokod ishlatilgani — kim, qachon, nima olgani.
 *
 * <h2>⚠️ «Bitta odam bir marta» qoidasi SHU YERDA</h2>
 * {@code uk_promocode_user} — bazada. Uni xizmat darajasida «avval
 * tekshir, keyin yoz» bilan bajarish mumkin emas: ikkita parallel so'rov
 * ikkalasi ham «hali ishlatilmagan» ni ko'rib, ikkalasi ham yozardi.
 * Cheklov ikkinchisini rad etadi va xizmat buni {@code PROMO_ALREADY_USED}
 * ga aylantiradi.
 *
 * <h2>Obuna yozuviga havola</h2>
 * Berilgan kunlar {@code cms_subscription} da {@code PROMO} manbasi bilan
 * turadi. Havola shu yerda ham saqlanadi: «bu promokod qancha premium
 * tarqatdi» degan hisobot obunalar jadvalini qidirmasdan chiqadi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_promocode_redemption",
        uniqueConstraints = @UniqueConstraint(name = "uk_promocode_user",
                columnNames = {"promocode_id", "user_id"}),
        indexes = @Index(name = "idx_promocode_redemption_user", columnList = "user_id,redeemed_at"))
public class PromocodeRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promocode_id")
    private Promocode promocode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;

    /**
     * Berilgan huquq qachongacha amal qildi.
     *
     * ⚠️ Ilgari bu {@code subscription.endAt} dan olinardi. Casting
     * kodida obuna yozuvi UMUMAN yo'q, shuning uchun javob shu yerda
     * saqlanadi — ikkala tur uchun ham bir xil ishlaydi.
     */
    @Column(name = "granted_until")
    private LocalDateTime grantedUntil;

    @PrePersist
    void onCreate() {
        if (redeemedAt == null) {
            redeemedAt = LocalDateTime.now();
        }
    }
}
