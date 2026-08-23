package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.AnalyticsEvent;
import com.example.backend.Cms.Enums.AnalyticsEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsEventRepo extends JpaRepository<AnalyticsEvent, Long> {

    /**
     * Agregatlanmagan hodisalarni sana + tur + nishon bo'yicha jamlaydi.
     *
     * {@code COUNT(DISTINCT ...)} shu yerda hisoblanadi — bu bir marta,
     * agregatlash paytida bo'ladi. Dashboard esa tayyor natijani o'qiydi.
     *
     * Unikal kalit: ro'yxatdan o'tgan foydalanuvchida user_id, anonimda
     * device_key. {@code coalesce} ikkalasini birlashtiradi.
     */
    @Query("""
            select e.eventDate as day, e.type as type, e.targetId as targetId,
                   count(e) as total,
                   count(distinct coalesce(cast(e.userId as string), e.deviceKey)) as uniques
            from AnalyticsEvent e
            where e.processed = false
            group by e.eventDate, e.type, e.targetId
            """)
    List<AggregateRow> aggregateUnprocessed();

    /**
     * Bir KUN ichidagi unikal foydalanuvchilar — qayta ishlanganidan qat'i nazar.
     *
     * <h2>Nima uchun kerak</h2>
     * Agregatsiya har 5 daqiqada ishlaydi va faqat YANGI hodisalarni ko'radi.
     * Agar unikal sanoq har to'plamda QO'SHIB borilsa, bir soat kontent
     * ko'rgan foydalanuvchi 12 ta «unikal» bo'lib hisoblanardi.
     *
     * Shuning uchun unikal sanoq qo'shilmaydi, balki shu so'rov bilan
     * BUTUN KUN uchun qayta hisoblanadi va ustidan yoziladi.
     *
     * ⚠️ Faqat joriy to'plamda uchragan kunlar uchun chaqiriladi, ya'ni
     * so'rovlar soni to'plam hajmi bilan chegaralangan. Bu fon vazifasi,
     * dashboard emas.
     */
    @Query("""
            select count(distinct coalesce(cast(e.userId as string), e.deviceKey))
            from AnalyticsEvent e
            where e.type = :type and e.targetId = :targetId and e.eventDate = :day
            """)
    long countUniquesForDay(@Param("type") AnalyticsEventType type,
                            @Param("targetId") Long targetId,
                            @Param("day") LocalDate day);

    @Modifying
    @Query("update AnalyticsEvent e set e.processed = true where e.processed = false")
    int markAllProcessed();

    long countByProcessedFalse();

    long countByTypeAndEventDateBetween(AnalyticsEventType type, LocalDate from, LocalDate to);

    /** Bitta nishon bo'yicha hodisalar soni — bildirishnoma hisoboti uchun (§33). */
    long countByTypeAndTargetId(AnalyticsEventType type, Long targetId);

    /**
     * Bitta nishon bo'yicha UNIKAL odamlar soni.
     *
     * Bir odam xabarni ikki qurilmada ochsa ham bitta sanaladi. Ro'yxatdan
     * o'tmaganlar {@code deviceKey} bo'yicha farqlanadi.
     */
    @Query("""
            select count(distinct coalesce(cast(e.userId as string), e.deviceKey))
            from AnalyticsEvent e
            where e.type = :type and e.targetId = :targetId
            """)
    long countUniquesForTarget(@Param("type") AnalyticsEventType type,
                               @Param("targetId") Long targetId);

    /** Proyeksiya — entity yuklamasdan faqat kerakli ustunlar (§66). */
    interface AggregateRow {
        LocalDate getDay();
        AnalyticsEventType getType();
        Long getTargetId();
        Long getTotal();
        Long getUniques();
    }
}
