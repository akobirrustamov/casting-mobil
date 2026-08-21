package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Premiere;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PremiereRepo extends JpaRepository<Premiere, Long> {

    @EntityGraph(attributePaths = "translations")
    List<Premiere> findAllByOrderBySortOrderAscIdAsc();
}
