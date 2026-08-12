package com.example.backend.Repository;

import com.example.backend.Entity.CastingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

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
}
