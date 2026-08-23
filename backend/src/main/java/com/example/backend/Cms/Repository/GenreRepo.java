package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface GenreRepo extends JpaRepository<Genre, Long> {

    Optional<Genre> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Genre> findAllByActiveTrueOrderBySortOrderAsc();


    /**
     * Sahifalangan ro'yxat, ixtiyoriy qidiruv bilan (ТЗ §51).
     *
     * Qidiruv tarjimalarda — janr nomi uch tilda saqlanadi.
     */
    @Query(value = """
            select distinct g from Genre g
            left join g.translations t
            where :q is null or lower(t.name) like lower(concat('%', :q, '%'))
            """,
            countQuery = """
            select count(distinct g) from Genre g
            left join g.translations t
            where :q is null or lower(t.name) like lower(concat('%', :q, '%'))
            """)
    org.springframework.data.domain.Page<Genre> searchPage(
            @org.springframework.data.repository.query.Param("q") String q,
            org.springframework.data.domain.Pageable pageable);
}
