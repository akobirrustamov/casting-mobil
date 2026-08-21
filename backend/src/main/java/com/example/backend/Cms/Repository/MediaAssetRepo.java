package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Media kutubxonasi so'rovlari (ТЗ §26).
 *
 * <h2>Nega bitta {@code @Query}</h2>
 * Kutubxonada uchta mustaqil filtr bor: tur, holat va qidiruv. Ularning
 * har bir kombinatsiyasi uchun alohida metod yozilsa 8 ta metod kerak
 * bo'lardi va yangi filtr qo'shilganda soni ikki barobar oshardi.
 *
 * Shuning uchun bitta so'rov: {@code null} berilgan parametr shartni
 * o'tkazib yuboradi.
 */
public interface MediaAssetRepo extends JpaRepository<MediaAsset, Long> {

    Page<MediaAsset> findAllByTypeOrderByCreatedAtDesc(MediaType type, Pageable pageable);

    /**
     * Kutubxona ro'yxati — filtr va qidiruv bilan.
     *
     * Qidiruv ASL fayl nomi bo'yicha: {@code storageKey} UUID bo'lgani
     * uchun undan qidirishning ma'nosi yo'q, admin esa faylni yuklagan
     * nomi bilan eslaydi.
     */
    @Query("""
            select m from MediaAsset m
            where (:type is null or m.type = :type)
              and (:status is null or m.status = :status)
              and (:q is null or lower(m.originalFilename) like lower(concat('%', :q, '%')))
            order by m.createdAt desc
            """)
    Page<MediaAsset> library(@Param("type") MediaType type,
                             @Param("status") MediaStatus status,
                             @Param("q") String q,
                             Pageable pageable);
}
