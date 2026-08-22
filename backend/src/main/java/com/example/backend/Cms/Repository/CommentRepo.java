package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Enums.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.time.LocalDateTime;

public interface CommentRepo extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author", "content"})
    Page<Comment> findAllByStatus(CommentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "content"})
    Page<Comment> findAllByContentId(Long contentId, Pageable pageable);

    /** Shikoyat qilinganlar — moderator birinchi shularni ko'radi. */
    @EntityGraph(attributePaths = {"author", "content"})
    Page<Comment> findAllByReportsCountGreaterThanOrderByReportsCountDesc(int min, Pageable pageable);

    /**
     * Moderatsiya ro'yxati — BARCHA filtrlar birga ishlaydi (ТЗ §34).
     *
     * <h2>Nima uchun bitta so'rov</h2>
     * Ilgari filtrlar {@code if/else} zanjiri bilan tanlanardi, ya'ni ular
     * BIR-BIRINI INKOR QILARDI: moderator «yashirilgan izohlar» va «shu
     * kino» ni birga tanlasa, kino bo'yicha filtr status filtrini jimgina
     * yutib yuborardi va ro'yxatda ko'rinadigan izohlar ham chiqardi.
     * Xato ekranda ko'rinmasdi — shunchaki noto'g'ri ro'yxat edi.
     *
     * <h2>Nima uchun {@code :param is null or ...}</h2>
     * Har bir filtr ixtiyoriy. Berilmagani shartni o'chiradi, berilgani esa
     * qo'shiladi — ya'ni ular VA bilan bog'lanadi.
     *
     * <h2>Nima uchun {@code join fetch}</h2>
     * {@code author}, {@code content} va {@code episode} — dangasa
     * {@code @ManyToOne}. DTO uchalasiga ham tegadi, ya'ni 20 qatorlik
     * sahifa 60 tagacha qo'shimcha so'rov yuborardi (N+1).
     *
     * ⚠️ Bu yerda {@code join fetch} XAVFSIZ: {@code @ManyToOne} bitta
     * satrni ko'paytirmaydi, shuning uchun {@code limit} to'g'ri ishlaydi.
     * To'plamlarda ({@code @OneToMany}) esa aksincha bo'lardi
     * ({@code HHH90003004}).
     *
     * Sanoq so'rovi alohida yozilgan: {@code join fetch} bilan
     * {@code count} birga ishlamaydi.
     */
    @Query(value = """
            select c from Comment c
            left join fetch c.author
            left join fetch c.content
            left join fetch c.episode
            where (:status is null or c.status = :status)
              and (:contentId is null or c.content.id = :contentId)
              and (:authorId is null or c.author.id = :authorId)
              and (:from is null or c.createdAt >= :from)
              and (:to is null or c.createdAt <= :to)
              and (:reportedOnly = false or c.reportsCount > 0)
              and (:q is null or lower(c.text) like lower(concat('%', :q, '%')))
            """,
            countQuery = """
            select count(c) from Comment c
            where (:status is null or c.status = :status)
              and (:contentId is null or c.content.id = :contentId)
              and (:authorId is null or c.author.id = :authorId)
              and (:from is null or c.createdAt >= :from)
              and (:to is null or c.createdAt <= :to)
              and (:reportedOnly = false or c.reportsCount > 0)
              and (:q is null or lower(c.text) like lower(concat('%', :q, '%')))
            """)
    Page<Comment> moderationList(@Param("status") CommentStatus status,
                                 @Param("contentId") Long contentId,
                                 @Param("authorId") UUID authorId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 @Param("reportedOnly") boolean reportedOnly,
                                 @Param("q") String q,
                                 Pageable pageable);

    @Query("select c from Comment c where lower(c.text) like lower(concat('%', :q, '%'))")
    @EntityGraph(attributePaths = {"author", "content"})
    Page<Comment> search(@Param("q") String q, Pageable pageable);

    long countByStatus(CommentStatus status);
}
