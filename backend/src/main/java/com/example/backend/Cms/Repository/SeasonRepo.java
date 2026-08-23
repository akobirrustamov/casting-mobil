package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeasonRepo extends JpaRepository<Season, Long> {

    List<Season> findAllByContentIdOrderBySortOrderAsc(Long contentId);

}
