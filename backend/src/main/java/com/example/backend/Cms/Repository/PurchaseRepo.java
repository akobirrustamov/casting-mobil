package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Purchase;
import com.example.backend.Cms.Enums.PurchaseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PurchaseRepo extends JpaRepository<Purchase, Long> {

    /**
     * Foydalanuvchining aniq nishonga xaridi.
     *
     * Qaytarilganlar ham qaytadi — filtrlash {@code isValid()} orqali
     * servisda bo'ladi, chunki qaytarish qoidalari hali aniqlanmagan.
     */
    List<Purchase> findAllByUserIdAndTypeAndTargetId(UUID userId, PurchaseType type, Long targetId);

    /**
     * Bir nechta maqsad uchun xaridlar — qismlar ro'yxatida N+1 dan qochish uchun
     * ({@code AccessService.canWatchAll}).
     */
    List<Purchase> findAllByUserIdAndTypeAndTargetIdIn(UUID userId, PurchaseType type,
                                                       Collection<Long> targetIds);

    List<Purchase> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByTypeAndRefundedAtIsNull(PurchaseType type);

    /**
     * Nishonga bog'langan xaridlar soni (§58).
     *
     * Qaytarilganlar ham sanaladi: qaytarilgan xarid ham moliyaviy tarix,
     * nishoni o'chsa u ham «nima uchun pul qaytarildi»ni ko'rsatolmay qoladi.
     */
    long countByTypeAndTargetId(PurchaseType type, Long targetId);

    /**
     * Bir martalik kontent xaridlari daromadi (§45).
     *
     * Qaytarilganlar chiqarib tashlanadi — qaytarilgan pul daromad emas.
     *
     * ⚠️ Valyuta paketlari BU YERGA KIRMAYDI: ular alohida hisoblanadi.
     * «Single purchase revenue» kontent xaridini bildiradi, paket esa
     * pulni virtual valyutaga o'girish — ikkalasini qo'shish qaysi
     * ko'rsatkich nimani anglatishini chalkashtirardi.
     */
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(p.amount), 0) from Purchase p
            where p.refundedAt is null
              and p.type <> com.example.backend.Cms.Enums.PurchaseType.CURRENCY_PACKAGE
            """)
    java.math.BigDecimal contentPurchaseRevenue();

    /** Valyuta paketlari daromadi — alohida ko'rsatkich (§45). */
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(p.amount), 0) from Purchase p
            where p.refundedAt is null
              and p.type = com.example.backend.Cms.Enums.PurchaseType.CURRENCY_PACKAGE
            """)
    java.math.BigDecimal currencyPackageRevenue();
}
