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

    /**
     * Sahifalangan ro'yxat, ixtiyoriy qidiruv bilan (ТЗ §51).
     *
     * ⚠️ Mavjud {@link #search} metodi {@code List} qaytaradi va JAMI
     * sonni bilmaydi — panel «3-sahifadan 5-sahifaga» o'tishni ko'rsata
     * olmasdi. Bu esa {@code Page}: jami son bilan.
     */
    @Query(value = """
            select distinct c from Creator c
            left join c.translations t
            where :q is null
               or lower(t.displayName) like lower(concat('%', :q, '%'))
               or lower(t.firstName) like lower(concat('%', :q, '%'))
               or lower(t.lastName) like lower(concat('%', :q, '%'))
            """,
            countQuery = """
            select count(distinct c) from Creator c
            left join c.translations t
            where :q is null
               or lower(t.displayName) like lower(concat('%', :q, '%'))
               or lower(t.firstName) like lower(concat('%', :q, '%'))
               or lower(t.lastName) like lower(concat('%', :q, '%'))
            """)
    org.springframework.data.domain.Page<Creator> searchPage(
            @Param("q") String q, Pageable pageable);
}
