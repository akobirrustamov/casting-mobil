package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.DonationTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /** Proyeksiya — entity o'rniga faqat kerakli ustunlar (§66). */
    interface TargetTotal {
        com.example.backend.Cms.Enums.DonationTargetType getTargetType();
        Long getTargetId();
        com.example.backend.Cms.Enums.CurrencyKind getKind();
        Long getTotal();
        Long getTransactions();
    }
}
