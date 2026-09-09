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

    /**
     * Qism ko'rishlarini oshirish (buyurtmachi 07.09.2026).
     *
     * <h2>⚠️ Ustun bor edi, lekin uni hech kim oshirmasdi</h2>
     * {@code cms_episode.view_count} boshidan mavjud va admin panel uni
     * ko'rsatib turardi — doim nol. Hodisalar {@code episodeId} bilan
     * kelardi, ammo jamlash ularni faqat KONTENT bo'yicha guruhlab,
     * qism raqamini tashlab yuborardi. Ya'ni panel yolg'on nol
     * ko'rsatardi, buzuq ekani esa ko'rinmasdi.
     *
     * ⚠️ {@code clearAutomatically} YO'Q — {@code ContentRepo.addViews}
     * bilan bir sababdan: chaqiruvchi o'sha tranzaksiyada kunlik
     * jamlanma obyektlari bilan ishlaydi.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("update Episode e set e.viewCount = e.viewCount + :delta where e.id = :id")
    void addViews(@Param("id") Long id, @Param("delta") long delta);

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
