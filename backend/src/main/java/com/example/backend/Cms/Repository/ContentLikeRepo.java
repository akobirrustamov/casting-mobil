package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.ContentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContentLikeRepo extends JpaRepository<ContentLike, Long> {

    boolean existsByContentIdAndUserId(Long contentId, UUID userId);

    /** @return o'chirilgan yozuvlar soni — 0 yoki 1, chunki juftlik unikal. */
    long deleteByContentIdAndUserId(Long contentId, UUID userId);

    /** Hisoblagichni noldan qayta yig'ish uchun. */
    long countByContentId(Long contentId);
}
