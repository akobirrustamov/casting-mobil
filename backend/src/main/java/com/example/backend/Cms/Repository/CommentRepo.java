package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Enums.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepo extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author", "content"})
    Page<Comment> findAllByStatus(CommentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "content"})
    Page<Comment> findAllByContentId(Long contentId, Pageable pageable);

    /** Shikoyat qilinganlar — moderator birinchi shularni ko'radi. */
    @EntityGraph(attributePaths = {"author", "content"})
    Page<Comment> findAllByReportsCountGreaterThanOrderByReportsCountDesc(int min, Pageable pageable);

    @Query("select c from Comment c where lower(c.text) like lower(concat('%', :q, '%'))")
    @EntityGraph(attributePaths = {"author", "content"})
    Page<Comment> search(@Param("q") String q, Pageable pageable);

    long countByStatus(CommentStatus status);
}
