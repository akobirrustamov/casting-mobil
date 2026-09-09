package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.ContentCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContentCreditRepo extends JpaRepository<ContentCredit, Long> {

    /**
     * Kontentda qatnashganlar — ekran uchun tayyor ro'yxat.
     *
     * <h2>⚠️ Nima uchun fetch join</h2>
     * Har bir yozuv uchun ijodkor, uning surati va tarjimasi kerak. Ularsiz
     * ro'yxat ochilishi bilan N+1 boshlanardi: 12 aktyor — 37 ta so'rov,
     * va bu kontent ekrani har ochilganda.
     *
     * Bitta to'plam ({@code translations}) fetch qilinadi — ikkitasi
     * {@code MultipleBagFetchException} berardi.
     */
    @Query("""
            select distinct cc from ContentCredit cc
            join fetch cc.creator c
            left join fetch c.photo
            left join fetch c.translations
            where cc.content.id = :contentId
            order by cc.sortOrder asc, cc.id asc
            """)
    List<ContentCredit> findForContent(@Param("contentId") Long contentId);
}
