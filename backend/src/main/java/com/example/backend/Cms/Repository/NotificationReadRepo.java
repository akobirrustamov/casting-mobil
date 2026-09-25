package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NotificationReadRepo extends JpaRepository<NotificationRead, NotificationRead.Key> {

    /** Berilgan xabarlardan qaysilarini shu odam o'qigan. */
    @Query("select r.notificationId from NotificationRead r "
            + "where r.userId = :userId and r.notificationId in :ids")
    List<Long> findReadIds(@Param("userId") UUID userId,
                           @Param("ids") Collection<Long> ids);

    /** Hisob o'chirilganda — shaxsiy ma'lumot. */
    long deleteByUserId(UUID userId);
}
