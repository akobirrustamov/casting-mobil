package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.HomepageSection;
import com.example.backend.Cms.Enums.HomepageSectionType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HomepageSectionRepo extends JpaRepository<HomepageSection, Long> {

    @EntityGraph(attributePaths = "translations")
    List<HomepageSection> findAllByOrderBySortOrderAscIdAsc();

    /**
     * Mobil ilova bosh sahifasi uchun — faqat YOQILGAN bo'limlar, tartibda.
     *
     * Tarjimalar birga olinadi: aks holda har bir bo'lim sarlavhasi uchun
     * alohida so'rov ketardi.
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "translations")
    List<HomepageSection> findAllByEnabledTrueOrderBySortOrderAscIdAsc();

    Optional<HomepageSection> findByType(HomepageSectionType type);
}
