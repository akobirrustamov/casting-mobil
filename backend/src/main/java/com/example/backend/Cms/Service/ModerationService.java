package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Enums.CommentStatus;
import com.example.backend.Cms.Repository.CommentRepo;
import com.example.backend.Entity.User;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Izohlar moderatsiyasi.
 *
 * Admin panel izoh YARATMAYDI — uni foydalanuvchi mobil ilovadan yozadi.
 * Bu yerda faqat ko'rish, yashirish, tiklash va o'chirilgan deb belgilash.
 *
 * Hard delete yo'q (§58): moderator qarori va shikoyat tarixi saqlanadi.
 */
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final CommentRepo commentRepo;
    private final AuditService auditService;

    /**
     * Moderatsiya ro'yxati (ТЗ §34).
     *
     * <h2>Filtrlar BIRGA ishlaydi</h2>
     * Ilgari bu yerda {@code if/else} zanjiri turardi va filtrlar
     * bir-birini inkor qilardi: moderator «yashirilgan» + «shu kino» ni
     * birga tanlasa, kino filtri status filtrini jimgina yutib yuborardi va
     * ro'yxatda ko'rinadigan izohlar ham chiqardi. Ekranda hech qanday xato
     * ko'rinmasdi — shunchaki noto'g'ri ro'yxat edi.
     *
     * <h2>Foydalanuvchi va sana filtri</h2>
     * ТЗ ro'yxatida bor edi, kodda umuman yo'q edi.
     *
     * @param query kamida 2 belgi — bitta harf butun bazani skanerlashiga
     *              arzimaydi
     */
    @Transactional(readOnly = true)
    public Page<Comment> comments(CommentStatus status, Long contentId, UUID authorId,
                                  LocalDateTime from, LocalDateTime to,
                                  String query, boolean reportedOnly, Pageable pageable) {
        String q = query == null || query.trim().length() < 2 ? null : query.trim();
        return commentRepo.moderationList(status, contentId, authorId, from, to,
                reportedOnly, q, pageable);
    }

    @Transactional
    public Comment changeStatus(User actor, Long id, CommentStatus target) {
        Comment c = commentRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Comment", id));

        CommentStatus before = c.getStatus();
        if (before == target) {
            return c;
        }

        c.setStatus(target);
        c.setModeratedBy(actor == null ? null : actor.getId());
        c.setModeratedAt(LocalDateTime.now());
        Comment saved = commentRepo.save(c);

        String action = target == CommentStatus.HIDDEN
                ? AuditAction.COMMENT_HIDDEN
                : "COMMENT_" + target.name();
        auditService.log(actor, action, "Comment", id,
                Map.of("status", before), Map.of("status", target));
        return saved;
    }

    @Transactional(readOnly = true)
    public long countByStatus(CommentStatus status) {
        return commentRepo.countByStatus(status);
    }
}
