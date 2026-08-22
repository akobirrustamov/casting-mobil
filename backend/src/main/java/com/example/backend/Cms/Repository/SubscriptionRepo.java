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

    /** Bitta tarif bo'yicha daromad (ТЗ §47 filtri). */
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(s.paidAmount), 0) from Subscription s
            where s.paidAmount is not null and s.revokedAt is null
              and s.tariff.id = :tariffId
            """)
    java.math.BigDecimal totalPaidAmountByTariff(
            @org.springframework.data.repository.query.Param("tariffId") Long tariffId);

    /**
     * Obuna daromadi — kunlik (ТЗ §48 grafigi).
     *
     * ⚠️ Sovg'a obunalar ({@code paidAmount is null}) kirmaydi: ular
     * grafikni ko'tarib ko'rsatardi, lekin hech qanday pul kelmagan.
     */
    @org.springframework.data.jpa.repository.Query("""
            select cast(s.startAt as date) as day, coalesce(sum(s.paidAmount), 0) as value
            from Subscription s
            where s.paidAmount is not null and s.revokedAt is null
              and s.startAt >= :from
            group by cast(s.startAt as date)
            order by cast(s.startAt as date)
            """)
    List<DayMoney> revenueByDay(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from);

    /** Kunlik pul — grafik uchun proyeksiya. */
    interface DayMoney {
        java.time.LocalDate getDay();
        java.math.BigDecimal getValue();
    }
}
