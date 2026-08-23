package com.example.backend.Repository;

import com.example.backend.Entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {

    /**
     * Jurnal filtri (§59).
     *
     * <b>Nega bitta so'rov.</b> Ilgari filtrlar ichma-ich ternary bilan
     * tanlanardi va faqat bittasi ishlardi: tergovchi «PREMIUM_GRANTED
     * + aktyor X» deb qidirsa, action jimgina tashlab yuborilar, javobda
     * esa X ning barcha amallari chiqardi. Audit vositasida bu shunchaki
     * noqulaylik emas — noto'g'ri xulosa.
     *
     * <b>Action bo'yicha qismiy moslik.</b> Panelda bu maydon qidiruv
     * darchasi. Aniq tenglik bo'lsa «content» deb yozgan admin bo'sh
     * ro'yxat ko'rib, «bunday hodisa bo'lmagan» deb o'ylardi.
     */
    @Query(value = """
            select a from AuditLog a
            where (:action is null or lower(a.action) like lower(concat('%', :action, '%')))
              and (:actorId is null or a.actorId = :actorId)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
            order by a.createdAt desc
            """,
            countQuery = """
            select count(a) from AuditLog a
            where (:action is null or lower(a.action) like lower(concat('%', :action, '%')))
              and (:actorId is null or a.actorId = :actorId)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
            """)
    Page<AuditLog> search(@Param("action") String action,
                          @Param("actorId") UUID actorId,
                          @Param("entityType") String entityType,
                          @Param("entityId") String entityId,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);
}
