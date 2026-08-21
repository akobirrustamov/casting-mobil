package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EpisodeRepo extends JpaRepository<Episode, Long> {

    List<Episode> findAllByContentIdOrderBySortOrderAsc(Long contentId);

    List<Episode> findAllBySeasonIdOrderByEpisodeNumberAsc(Long seasonId);

    long countByContentId(Long contentId);

}
