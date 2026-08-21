package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotificationRepo extends JpaRepository<Notification, Long> {

    /**
     * Bildirishnomalar ro'yxati.
     *
     * {@code @EntityGraph} ATAYLAB yo'q: to-many to'plamni fetch join bilan
     * olib sahifalash so'ralsa, Hibernate SQL {@code limit} ni ishlata olmay
     * butun jadvalni xotiraga tortadi ({@code HHH90003004}).
     *
     * To'plamlar {@link #findAllByIdIn} bilan alohida to'ldiriladi.
     */
    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Sahifadagi elementlar uchun tarjimalarni bitta so'rovda oladi. */
    @EntityGraph(attributePaths = "translations")
    List<Notification> findAllByIdIn(Collection<Long> ids);
}
