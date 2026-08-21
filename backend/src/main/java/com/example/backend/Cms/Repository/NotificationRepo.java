package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
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

    /**
     * Vaqti kelgan rejalashtirilgan bildirishnomalar.
     *
     * Chegaralangan ro'yxat qaytadi: bir vaqtning o'zida yuzlab
     * bildirishnoma vaqti kelsa ham, ularning hammasi bitta tranzaksiyada
     * ishlanmasin.
     */
    List<Notification> findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            NotificationStatus status, LocalDateTime moment, Pageable pageable);

    long countByStatus(NotificationStatus status);
}
