package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Purchase;
import com.example.backend.Cms.Enums.PurchaseType;
import org.springframework.data.jpa.repository.JpaRepository;

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

    List<Purchase> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByTypeAndRefundedAtIsNull(PurchaseType type);
}
