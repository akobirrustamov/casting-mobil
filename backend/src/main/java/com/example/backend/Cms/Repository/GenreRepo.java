package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GenreRepo extends JpaRepository<Genre, Long> {

    Optional<Genre> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Genre> findAllByActiveTrueOrderBySortOrderAsc();

}
