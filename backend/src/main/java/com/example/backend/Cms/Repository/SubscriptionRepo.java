package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepo extends JpaRepository<Subscription, Long> {

    List<Subscription> findAllByUserIdOrderByEndAtDesc(UUID userId);

    /**
     * Obuna daromadi (§45).
     *
     * ⚠️ FAQAT haqiqiy xaridlar: {@code ADMIN_GIFT} obunalarida
     * {@code paidAmount} bo'sh va ular hisobga kirmaydi — sovg'a daromad
     * emas.
     *
     * Ilgari bu jamlanma {@code findAll()} bilan Java'da hisoblanardi:
     * har bir dashboard ochilishida BUTUN obunalar jadvali xotiraga
     * tortilardi.
     */
    @org.springframework.data.jpa.repository.Query(
            "select coalesce(sum(s.paidAmount), 0) from Subscription s "
                    + "where s.paidAmount is not null and s.revokedAt is null")
    java.math.BigDecimal totalPaidAmount();
}
