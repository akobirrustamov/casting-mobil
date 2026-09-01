package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.VideoProcessingStatus;
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

    /**
     * Berilgan kalitlar bo'yicha — ombor ko'rinishi uchun.
     *
     * ⚠️ `findAll()` O'RNIGA. Papka ochilganda faqat SHU DARAJADAGI
     * kalitlar kerak (ko'pi bilan 1000 ta), butun jadval emas —
     * o'n minglab yozuvli kutubxonada har papka ochilishi butun
     * jadvalni xotiraga tortardi.
     *
     * ⚠️ Kalit ikki shaklda saqlanishi mumkin: `/content/x.mp4` va
     * `content/x.mp4`. Chaqiruvchi ikkalasini ham beradi.
     */
    java.util.List<MediaAsset> findByStorageKeyIn(java.util.Collection<String> keys);

    Page<MediaAsset> findAllByTypeOrderByCreatedAtDesc(MediaType type, Pageable pageable);

    /**
     * Kutubxona ro'yxati — filtr va qidiruv bilan.
     *
     * Qidiruv ASL fayl nomi bo'yicha: {@code storageKey} UUID bo'lgani
     * uchun undan qidirishning ma'nosi yo'q, admin esa faylni yuklagan
     * nomi bilan eslaydi.
     *
     * <h2>{@code transcoding} filtri</h2>
     * ⚠️ {@code exists} ishlatiladi, {@code join} EMAS. {@code join}
     * bilan ishi YO'Q media umuman chiqmay qolardi — ya'ni filtrsiz
     * ham eski fayllar ro'yxatdan yo'qolardi.
     *
     * Bu filtr adminning asosiy savoliga javob beradi: «qaysi videolar
     * yiqildi». Usiz u yiqilganlarni sahifalab qidirishga majbur
     * bo'lardi.
     */
    @Query("""
            select m from MediaAsset m
            where (:type is null or m.type = :type)
              and (:status is null or m.status = :status)
              and (:q is null or lower(m.originalFilename) like lower(concat('%', :q, '%')))
              and (:transcoding is null or exists (
                    select 1 from TranscodingJob j
                    where j.media = m and j.status = :transcoding))
            """)
    Page<MediaAsset> library(@Param("type") MediaType type,
                             @Param("status") MediaStatus status,
                             @Param("q") String q,
                             @Param("transcoding") VideoProcessingStatus transcoding,
                             Pageable pageable);
}
