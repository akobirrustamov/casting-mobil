package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.DonationTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import org.springframework.data.repository.query.Param;
import com.example.backend.Cms.Enums.DonationTargetType;

import java.util.List;

public interface DonationRepo extends JpaRepository<DonationTransaction, Long> {

    Page<DonationTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Nishonlar bo'yicha jamlanma — reyting uchun.
     * Har bir nishon va valyuta juftligi uchun bitta qator.
     */
    @Query("""
            select d.targetType as targetType, d.targetId as targetId,
                   d.kind as kind, sum(d.amount) as total, count(d) as transactions
            from DonationTransaction d
            group by d.targetType, d.targetId, d.kind
            order by sum(d.amount) desc
            """)
    List<TargetTotal> topTargets(Pageable pageable);

    /**
     * Nishon TURI bo'yicha reyting — «top ijodkorlar» va «top kontent»
     * alohida ro'yxat sifatida kerak (ТЗ §42).
     */
    @Query("""
            select d.targetType as targetType, d.targetId as targetId,
                   d.kind as kind, sum(d.amount) as total, count(d) as transactions
            from DonationTransaction d
            where d.targetType = :targetType
            group by d.targetType, d.targetId, d.kind
            order by sum(d.amount) desc
            """)
    List<TargetTotal> topTargetsOfType(@Param("targetType") DonationTargetType targetType,
                                       Pageable pageable);

    /** Valyuta bo'yicha umumiy jamlanma: STARS va COIN alohida (ТЗ §42). */
    @Query("""
            select d.kind as kind, sum(d.amount) as total, count(d) as transactions
            from DonationTransaction d
            group by d.kind
            """)
    List<KindTotal> totalsByKind();

    /**
     * Kunlik summalar (ТЗ §42).
     *
     * ⚠️ Xom tranzaksiyalar ustidan guruhlash. Donat hodisalari reklama
     * ko'rsatishlariga qaraganda ancha kam bo'ladi (har biri pul harakati),
     * shuning uchun bu yerda alohida kunlik jamlanma jadvali qurilmadi.
     * Hajm o'ssa — {@code AdDailyStatistic} kabi jamlanma qo'shiladi.
     */
    @Query("""
            select cast(d.createdAt as date) as day, d.kind as kind,
                   sum(d.amount) as total, count(d) as transactions
            from DonationTransaction d
            where d.createdAt >= :from and d.createdAt < :to
            group by cast(d.createdAt as date), d.kind
            order by cast(d.createdAt as date)
            """)
    List<PeriodTotal> dailyTotals(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    /** Proyeksiya — entity o'rniga faqat kerakli ustunlar (§66). */
    interface KindTotal {
        com.example.backend.Cms.Enums.CurrencyKind getKind();
        Long getTotal();
        Long getTransactions();
    }

    /** Kunlik kesim. */
    interface PeriodTotal {
        java.time.LocalDate getDay();
        com.example.backend.Cms.Enums.CurrencyKind getKind();
        Long getTotal();
        Long getTransactions();
    }

    /** Proyeksiya — entity o'rniga faqat kerakli ustunlar (§66). */
    interface TargetTotal {
        com.example.backend.Cms.Enums.DonationTargetType getTargetType();
        Long getTargetId();
        com.example.backend.Cms.Enums.CurrencyKind getKind();
        Long getTotal();
        Long getTransactions();
    }
}
