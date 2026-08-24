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
    /*
     * ⚠️ `cast(:param as ...)` — bezak emas, MAJBURIY.
     *
     * `:param is null` shartida parametr boshqa hech qayerda
     * ishlatilmaydi: Hibernate har bir nomlangan parametr uchun
     * ALOHIDA pozitsion `?` yasaydi, ya'ni `$N` faqat `is null`
     * kontekstida qoladi. PostgreSQL 18 uchun bunday parametrning
     * turi noma'lum va u so'rovni butunlay rad etadi:
     *
     *   ERROR: could not determine data type of parameter $N
     *
     * Xato FAQAT o'sha filtr bo'sh bo'lganda chiqadi. Audit sahifasi
     * esa odatda filtrsiz ochiladi — ya'ni bu eng ko'p uchraydigan
     * holat edi.
     *
     * Matnli parametrlar (`:action`, `:entityType`) uchun kerak emas:
     * PostgreSQL noma'lum turni `text` deb qabul qila oladi. UUID va
     * sana uchun esa qila olmaydi.
     */
    @Query(value = """
            select a from AuditLog a
            where (:action is null or lower(a.action) like lower(concat('%', :action, '%')))
              and (cast(:actorId as String) is null or a.actorId = :actorId)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (cast(:from as LocalDateTime) is null or a.createdAt >= :from)
              and (cast(:to as LocalDateTime) is null or a.createdAt <= :to)
            order by a.createdAt desc
            """,
            countQuery = """
            select count(a) from AuditLog a
            where (:action is null or lower(a.action) like lower(concat('%', :action, '%')))
              and (cast(:actorId as String) is null or a.actorId = :actorId)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (cast(:from as LocalDateTime) is null or a.createdAt >= :from)
              and (cast(:to as LocalDateTime) is null or a.createdAt <= :to)
            """)
    Page<AuditLog> search(@Param("action") String action,
                          @Param("actorId") UUID actorId,
                          @Param("entityType") String entityType,
                          @Param("entityId") String entityId,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);
}
