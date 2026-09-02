package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.WatchProgress;
import com.example.backend.Cms.Enums.WatchTargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchProgressRepo extends JpaRepository<WatchProgress, Long> {

    /**
     * Bitta video uchun joriy holat.
     *
     * Yozishda ham shu ishlatiladi: satr bor bo'lsa ustiga yoziladi,
     * yo'q bo'lsa yaratiladi. Jadvaldagi noyob indeks
     * ({@code uq_watch_progress_user_target}) buni kafolatlaydi.
     */
    Optional<WatchProgress> findByUserIdAndTypeAndTargetId(UUID userId,
                                                           WatchTargetType type,
                                                           Long targetId);

    /**
     * «Ko'rishda davom eting» ro'yxati.
     *
     * <h2>⚠️ Tugatilganlar CHIQARILADI</h2>
     * Oxirigacha ko'rilgan filmni «davom eting» deb taklif qilish
     * xato bo'lardi — odam uni tugatgan.
     *
     * <h2>⚠️ Boshidagi soniyalar ham chiqariladi</h2>
     * Odam videoni ochib darhol yopishi mumkin — noto'g'ri bosdi,
     * tavsifni o'qidi, fikridan qaytdi. Bunday yozuv ro'yxatni
     * tasodifiy ochilgan videolar bilan to'ldirardi va haqiqiy
     * «davom eting» elementlarini pastga surib yuborardi.
     *
     * Chegara chaqiruvchida — u qoida, bu yerda esa so'rov.
     *
     * <h2>⚠️ Yangisi YUQORIDA</h2>
     * {@code id desc} — ikkinchi mezon: bir necha yozuv bir soniyada
     * yangilansa tartib tasodifiy bo'lib qolardi va ro'yxat har
     * so'rovda boshqacha ko'rinardi.
     */
    @Query("select p from WatchProgress p "
            + "where p.user.id = :userId "
            + "and p.completed = false "
            + "and p.positionSeconds >= :minSeconds "
            + "order by p.updatedAt desc, p.id desc")
    List<WatchProgress> findContinueWatching(@Param("userId") UUID userId,
                                             @Param("minSeconds") int minSeconds,
                                             Pageable pageable);
}
