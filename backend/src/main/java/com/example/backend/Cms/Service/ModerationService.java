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

    @Transactional(readOnly = true)
    public Page<Comment> comments(CommentStatus status, Long contentId, String query,
                                  boolean reportedOnly, Pageable pageable) {
        if (query != null && query.trim().length() >= 2) {
            return commentRepo.search(query.trim(), pageable);
        }
        if (reportedOnly) {
            return commentRepo.findAllByReportsCountGreaterThanOrderByReportsCountDesc(0, pageable);
        }
        if (contentId != null) {
            return commentRepo.findAllByContentId(contentId, pageable);
        }
        if (status != null) {
            return commentRepo.findAllByStatus(status, pageable);
        }
        return commentRepo.findAll(pageable);
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
