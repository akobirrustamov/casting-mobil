package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Tariff;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TariffRepo extends JpaRepository<Tariff, Long> {

    Optional<Tariff> findByCode(String code);

    @EntityGraph(attributePaths = "translations")
    List<Tariff> findAllByOrderBySortOrderAscIdAsc();
}
