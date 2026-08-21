package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.HomepageSectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HomepageSectionItemRepo extends JpaRepository<HomepageSectionItem, Long> {

    /**
     * Bo'lim elementlari — kontent bilan birga.
     *
     * {@code join fetch} bo'lmasa har bir element uchun alohida so'rov
     * ketardi (N+1): 20 elementli qator 21 ta so'rovga aylanardi.
     */
    @Query("""
            select i from HomepageSectionItem i
            join fetch i.content c
            where i.section.id = :sectionId and c.deletedAt is null
            order by i.sortOrder asc, i.id asc
            """)
    List<HomepageSectionItem> findForSection(@Param("sectionId") Long sectionId);

    void deleteAllBySectionId(Long sectionId);

    List<HomepageSectionItem> findAllByContentId(Long contentId);
}
