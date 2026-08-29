package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Enums.PublicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EpisodeRepo extends JpaRepository<Episode, Long> {

    List<Episode> findAllByContentIdOrderBySortOrderAsc(Long contentId);

    List<Episode> findAllBySeasonIdOrderByEpisodeNumberAsc(Long seasonId);

    long countByContentId(Long contentId);

    /**
     * Nechta NASHR QILINGAN qism — bir nechta kontent uchun BITTA so'rovda.
     *
     * <h2>Nima uchun guruhlangan</h2>
     * Bosh sahifada qirqqacha kartochka bo'ladi. Har biriga alohida
     * {@code countByContentId} chaqirilsa — qirq qo'shimcha so'rov, ya'ni
     * klassik N+1. Bu yerda bitta.
     *
     * ⚠️ Qoralama qismlar SANALMAYDI. «12 qism» yozuvi odam ko'ra oladigan
     * qismlar soni bo'lishi kerak: aks holda u kontentni ochib ikkitasini
     * topardi va yozuv yolg'on bo'lib chiqardi.
     *
     * @return {@code [contentId, count]} juftliklari
     */
    @Query("select e.content.id, count(e) from Episode e "
            + "where e.content.id in :contentIds and e.status = :status "
            + "group by e.content.id")
    List<Object[]> countPublishedByContentIds(
            @Param("contentIds") Collection<Long> contentIds,
            @Param("status") PublicationStatus status);
}
