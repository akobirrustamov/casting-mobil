package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Advertisement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvertisementRepo extends JpaRepository<Advertisement, Long> {

    @EntityGraph(attributePaths = "translations")
    List<Advertisement> findAllByOrderBySortOrderAscIdAsc();
}
