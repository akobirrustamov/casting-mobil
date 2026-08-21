package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.AdDailyStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdDailyStatisticRepo extends JpaRepository<AdDailyStatistic, Long> {

    Optional<AdDailyStatistic> findByAdvertisementIdAndStatDate(Long advertisementId, LocalDate statDate);

    List<AdDailyStatistic> findAllByStatDateBetweenOrderByStatDateAsc(LocalDate from, LocalDate to);

    /**
     * BITTA reklamaning kunlik ko'rsatkichlari.
     *
     * ТЗ §29: «Har bir reklama uchun Admin ko'ra olishi kerak». Umumiy
     * hisobotda faqat top-10 chiqadi, ya'ni 30 ta banneri bor admin
     * 25-chisining natijasini umuman ko'ra olmasdi.
     */
    List<AdDailyStatistic> findAllByAdvertisementIdAndStatDateBetweenOrderByStatDateAsc(
            Long advertisementId, LocalDate from, LocalDate to);

    /** Reklama bo'yicha davr jamlanmasi — hisobot uchun. */
    @Query("""
            select s.advertisementId as advertisementId,
                   sum(s.impressions) as impressions,
                   sum(s.clicks) as clicks,
                   sum(s.uniqueImpressions) as uniqueImpressions,
                   sum(s.uniqueClicks) as uniqueClicks
            from AdDailyStatistic s
            where s.statDate between :from and :to
            group by s.advertisementId
            order by sum(s.impressions) desc
            """)
    List<AdTotals> totalsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    interface AdTotals {
        Long getAdvertisementId();
        Long getImpressions();
        Long getClicks();
        Long getUniqueImpressions();
        Long getUniqueClicks();
    }
}
