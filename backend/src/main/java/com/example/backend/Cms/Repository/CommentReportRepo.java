package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentReportRepo extends JpaRepository<CommentReport, Long> {

    /** Shu odam bu izohga allaqachon shikoyat qilganmi — takrori 409 oladi. */
    boolean existsByCommentIdAndUserId(Long commentId, UUID userId);

    /**
     * Hisobini o'chirgan odamning shikoyatlari.
     *
     * ⚠️ Shikoyatlar o'chiriladi, lekin {@code reports_count} KAMAYTIRILMAYDI:
     * moderator uchun «bu izohga shikoyat bo'lgan» fakti hisob o'chirilgandan
     * keyin ham o'z kuchida qoladi. Aks holda hisobini o'chirib, shikoyatini
     * ham olib tashlash mumkin bo'lardi.
     */
    long deleteByUserId(UUID userId);
}
