package com.example.backend.Repository;

import com.example.backend.Entity.CastingUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CastingUserRepo extends JpaRepository<CastingUser,Integer> {

    @Query(value = "SELECT " +
            "(SELECT COUNT(*) FROM casting_user WHERE DATE(created_at) = CURRENT_DATE) AS dailyCount, " +
            "(SELECT COUNT(*) FROM casting_user) AS totalCount, " +
            "(SELECT COUNT(*) FROM casting_user WHERE status = 2) AS rejectedCount, " +
            "(SELECT COUNT(*) FROM casting_user WHERE status = 1) AS acceptedCount, " +
            "(SELECT COUNT(*) FROM casting_user WHERE status = 0) AS pendingCount", nativeQuery = true)
    Object getAdminStatistic();


    @Query(value = "SELECT * FROM casting_user where telegram_id=:telegramId", nativeQuery = true)
    List<CastingUser> findByTelegramId(String telegramId);

    List<CastingUser> findAllByOrderByCreatedAtAsc();

    List<CastingUser> findAllByOrderByCreatedAtDesc();

    List<CastingUser> findAllByIsWebShow(boolean b);

    // ---------------------------------------------------------------------
    //  Mobil ilova anketalari (V40, `/api/v1/app/casting`)
    // ---------------------------------------------------------------------

    /**
     * Foydalanuvchining o'z anketalari, yangisi yuqorida.
     *
     * ⚠️ Rasmlar SHU so'rovning o'zida olinadi: {@code photos} lazy va
     * javobda har bir anketaning rasm id'lari kerak — aks holda har bir
     * anketa uchun alohida so'rov ketardi (N+1).
     */
    @EntityGraph(attributePaths = "photos")
    List<CastingUser> findAllByAppUserIdOrderByCreatedAtDesc(UUID appUserId);

    /** Shu foydalanuvchida berilgan statusdagi anketa bormi (0 — kutilmoqda). */
    boolean existsByAppUserIdAndStatus(UUID appUserId, Integer status);

    /**
     * Berilgan rasmlardan nechtasi allaqachon biror anketaga biriktirilgan.
     *
     * ⚠️ {@code casting_user_photos.photos_id} UNIQUE (V1). Band rasm
     * yangi anketaga qo'shilsa, INSERT cheklovga urilib 500 qaytarardi —
     * shuning uchun oldindan tekshiriladi va aniq xato beriladi.
     */
    @Query(value = "select count(*) from casting_user_photos where photos_id in (:ids)",
            nativeQuery = true)
    long countLinkedPhotos(@Param("ids") Collection<UUID> ids);
}
