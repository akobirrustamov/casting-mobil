package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.ContentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ContentLikeRepo extends JpaRepository<ContentLike, Long> {

    boolean existsByContentIdAndUserId(Long contentId, UUID userId);

    /** @return o'chirilgan yozuvlar soni — 0 yoki 1, chunki juftlik unikal. */
    long deleteByContentIdAndUserId(Long contentId, UUID userId);

    /** Hisoblagichni noldan qayta yig'ish uchun. */
    long countByContentId(Long contentId);

    // ------------------------------------------------------- hisobotlar
    //
    // ⚠️ QUYIDAGI SO'ROVLAR HAMMASI BITTA NARSANI SANAYDI:
    // HOZIR TURGAN «yoqdi» larni, ular qo'yilgan kun bo'yicha.
    //
    // Ko'rishlardan tubdan farq qiladi. Ko'rish — sodir bo'lgan voqea,
    // uni ortga qaytarib bo'lmaydi. «Yoqdi» esa OLIB TASHLANADI, va
    // olib tashlanganda qator jadvaldan o'chadi ({@code deleteBy...}).
    //
    // Ya'ni dushanba qo'yilib juma kuni yechilgan «yoqdi» dushanba
    // ustunidan ham YO'QOLADI — o'tmish grafigi orqaga qarab
    // o'zgaradi. Bu jadval tuzilishining oqibati, xato emas; hisobotda
    // shu haqda ogohlantirish yozilgan.
    //
    // Buni to'g'irlash uchun «yoqdi» hodisalari alohida jurnalga
    // yozilishi kerak bo'lardi — hozircha bunga ehtiyoj yo'q.

    /**
     * Bitta kontent bo'yicha kunlik «yoqdi» — grafik uchun.
     *
     * Bo'sh kunlar qatorda BO'LMAYDI: chaqiruvchi ularni kunlik
     * ko'rish qatoriga moslab to'ldiradi.
     */
    @Query("""
            select cast(l.createdAt as date) as day, count(l) as total
            from ContentLike l
            where l.content.id = :contentId
              and cast(l.createdAt as date) between :from and :to
            group by cast(l.createdAt as date)
            order by cast(l.createdAt as date)
            """)
    List<DailyLikes> dailyForContent(@Param("contentId") Long contentId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    /** Davr ichida qo'yilgan barcha «yoqdi» — butun platforma bo'yicha. */
    @Query("""
            select count(l) from ContentLike l
            where cast(l.createdAt as date) between :from and :to
            """)
    long countBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** O'sha son, lekin faqat tanlangan kontentlar bo'yicha (hisobot filtri). */
    @Query("""
            select count(l) from ContentLike l
            where l.content.id in :contentIds
              and cast(l.createdAt as date) between :from and :to
            """)
    long countBetweenForContents(@Param("contentIds") Collection<Long> contentIds,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);

    /**
     * Hisobot jadvalidagi o'nta qator uchun «yoqdi» — BITTA so'rovda.
     *
     * ⚠️ Har qator uchun alohida {@code countByContentId} chaqirish
     * klassik N+1 bo'lardi (§66).
     */
    @Query("""
            select l.content.id as contentId, count(l) as total
            from ContentLike l
            where l.content.id in :contentIds
              and cast(l.createdAt as date) between :from and :to
            group by l.content.id
            """)
    List<ContentLikes> likesByContentBetween(@Param("contentIds") Collection<Long> contentIds,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    interface DailyLikes {
        LocalDate getDay();
        Long getTotal();
    }

    interface ContentLikes {
        Long getContentId();
        Long getTotal();
    }
}
