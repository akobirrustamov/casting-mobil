package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CategoryRepo extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findAllByActiveTrueOrderBySortOrderAsc();


    /**
     * Sahifalangan ro'yxat, ixtiyoriy qidiruv bilan (ТЗ §51).
     *
     * <h2>Nima uchun kerak</h2>
     * Ilgari {@code findAll()} chaqirilardi — ya'ni panel har ochilganda
     * BUTUN jadval kelardi. Kategoriya soni cheklanmagan: platforma
     * o'sgani sari ro'yxat uzayadi va sahifa sekinlashadi.
     *
     * ⚠️ Qidiruv TARJIMALARDA: kategoriya nomi uch tilda saqlanadi va
     * admin qaysi tilda yozishi noma'lum. Faqat bitta tilda qidirish
     * «Drama» ni topib, «Драма» ni topmasdi.
     */
    @Query(value = """
            select distinct c from Category c
            left join c.translations t
            where :q is null or lower(t.name) like lower(concat('%', :q, '%'))
            """,
            countQuery = """
            select count(distinct c) from Category c
            left join c.translations t
            where :q is null or lower(t.name) like lower(concat('%', :q, '%'))
            """)
    org.springframework.data.domain.Page<Category> searchPage(
            @org.springframework.data.repository.query.Param("q") String q,
            org.springframework.data.domain.Pageable pageable);
}
