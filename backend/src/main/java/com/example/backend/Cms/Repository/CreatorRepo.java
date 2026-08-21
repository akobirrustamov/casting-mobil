package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Creator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CreatorRepo extends JpaRepository<Creator, Long> {

    Optional<Creator> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Creator> findAllByFeaturedTrueAndActiveTrueOrderBySortOrderAsc();

    /**
     * Olingan Stars bo'yicha tartib — analitika asosidagi reyting uchun
     * (ТЗ §25).
     *
     * {@code featured} bayrog'i bu yerda TEKSHIRILMAYDI: avtomatik reyting
     * mohiyatan «kim mashhur» degan savolga o'zi javob beradi va admin
     * qo'lda belgilashini talab qilmaydi. Faqat faol profillar kiradi.
     */
    List<Creator> findAllByActiveTrueOrderByStarsReceivedDescIdAsc();

    /**
     * Ijodkor qidiruvi - kontent muharriridagi "Ijodkor qo'shish" uchun.
     * Uchala tilda ham qidiradi, chunki ism tarjimalarda saqlanadi.
     */
    @Query("""
            select distinct c from Creator c
            join c.translations t
            where lower(t.displayName) like lower(concat('%', :q, '%'))
               or lower(coalesce(t.firstName, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(t.lastName, '')) like lower(concat('%', :q, '%'))
            """)
    List<Creator> search(@Param("q") String q, Pageable pageable);
}
