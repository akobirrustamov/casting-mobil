package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.ContentDailyStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContentDailyStatisticRepo extends JpaRepository<ContentDailyStatistic, Long> {

    Optional<ContentDailyStatistic> findByContentIdAndStatDate(Long contentId, LocalDate statDate);

    List<ContentDailyStatistic> findAllByStatDateBetweenOrderByStatDateAsc(LocalDate from, LocalDate to);

    /**
     * BITTA kontentning kunlik ko'rsatkichlari (ТЗ §46).
     *
     * Umumiy hisobotda faqat top-10 chiqadi — 200 ta filmi bor admin
     * 150-chisining raqamlarini umuman ko'ra olmasdi. Reklamada bu
     * bo'shliq §29 da tuzatilgan edi, kontentda esa qolib ketgan.
     */
    List<ContentDailyStatistic> findAllByContentIdAndStatDateBetweenOrderByStatDateAsc(
            Long contentId, LocalDate from, LocalDate to);

    @Query("""
            select s.contentId as contentId,
                   sum(s.views) as views,
                   sum(s.plays) as plays,
                   sum(s.completes) as completes,
                   sum(s.uniqueViewers) as uniqueViewers
            from ContentDailyStatistic s
            where s.statDate between :from and :to
            group by s.contentId
            order by sum(s.views) desc
            """)
    List<ContentTotals> totalsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * FILTRLANGAN kunlik qator (ТЗ §47).
     *
     * <h2>Nima uchun kerak</h2>
     * Filtr qo'llanganda grafik ham torayishi shart. Aks holda admin
     * bitta kategoriyani tanlaydi, ro'yxat torayadi — lekin grafik va
     * umumiy son BUTUN platformaniki bo'lib qoladi. Ya'ni hisobot
     * o'z-o'ziga zid bo'lardi va bunga sabab ekranda ko'rinmasdi.
     */
    @Query("""
            select s.statDate as day,
                   sum(s.views) as views,
                   sum(s.plays) as plays,
                   sum(s.completes) as completes
            from ContentDailyStatistic s
            where s.statDate between :from and :to
              and s.contentId in :contentIds
            group by s.statDate
            order by s.statDate
            """)
    List<DailyPoint> dailySeriesForContents(@Param("from") LocalDate from,
                                            @Param("to") LocalDate to,
                                            @Param("contentIds") Collection<Long> contentIds);

    /** Kunlik qator — grafik uchun. */
    @Query("""
            select s.statDate as day,
                   sum(s.views) as views,
                   sum(s.plays) as plays,
                   sum(s.completes) as completes
            from ContentDailyStatistic s
            where s.statDate between :from and :to
            group by s.statDate
            order by s.statDate
            """)
    List<DailyPoint> dailySeries(@Param("from") LocalDate from, @Param("to") LocalDate to);

    interface ContentTotals {
        Long getContentId();
        Long getViews();
        Long getPlays();
        Long getCompletes();
        Long getUniqueViewers();
    }

    interface DailyPoint {
        LocalDate getDay();
        Long getViews();
        Long getPlays();
        Long getCompletes();
    }
}
