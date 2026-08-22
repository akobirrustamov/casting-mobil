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

    /**
     * Sarlavha va matn bo'yicha qidiruv (ТЗ §51).
     *
     * ⚠️ Qidiruv TARJIMALARDA: bildirishnoma matni uch tilda saqlanadi
     * va admin qaysi tilda yozishi noma'lum.
     *
     * ⚠️ `@EntityGraph` bu yerda YO'Q: `distinct` bilan birga to-many
     * fetch join sahifalashni buzardi. Tarjimalar `findAllByIdIn` bilan
     * alohida to'ldiriladi.
     */
    @org.springframework.data.jpa.repository.Query(value = """
            select distinct n from Notification n
            left join n.translations t
            where lower(t.title) like lower(concat('%', :q, '%'))
               or lower(t.body) like lower(concat('%', :q, '%'))
            order by n.createdAt desc
            """,
            countQuery = """
            select count(distinct n) from Notification n
            left join n.translations t
            where lower(t.title) like lower(concat('%', :q, '%'))
               or lower(t.body) like lower(concat('%', :q, '%'))
            """)
    Page<Notification> search(
            @org.springframework.data.repository.query.Param("q") String q,
            Pageable pageable);

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
