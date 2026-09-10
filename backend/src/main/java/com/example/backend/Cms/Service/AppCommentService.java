package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.CommentStatus;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Cms.Repository.CommentRepo;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Entity.User;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ilovadagi izohlar — ro'yxat, yozish, o'z izohini o'chirish (10.09.2026).
 *
 * <h2>Nima uchun kerak</h2>
 * Izoh modeli ({@link Comment}) va moderatsiya paneli boshidan bor edi,
 * kartochkada izohlar SONI ham chiqardi — lekin ilovada izohni o'qish
 * yoki yozishning yo'li yo'q edi. «Izohlar» plitkasi hech qayerga olib
 * bormasdi.
 *
 * <h2>Moderatsiya bilan bog'liqlik</h2>
 * Yangi izoh darhol {@link CommentStatus#VISIBLE} — oldindan tekshiruv
 * yo'q, moderator keyin yashiradi (panelning o'zi shunday qurilgan:
 * yashirish va shikoyatlar ro'yxati bor, «tasdiqlash» navbati yo'q).
 *
 * O'chirish ham HARD DELETE emas — {@link CommentStatus#DELETED}: shikoyat
 * tarixi va moderator qarori saqlanadi (§58).
 */
@Service
@RequiredArgsConstructor
public class AppCommentService {

    /** Ustun uzunligi bilan bir xil ({@code cms_comment.text}). */
    public static final int MAX_LENGTH = 2000;

    /** Bitta sahifada eng ko'pi bilan — ilova 20 tadan so'raydi. */
    static final int MAX_PAGE_SIZE = 50;

    private final CommentRepo commentRepo;
    private final ContentRepo contentRepo;
    private final UserAccountRepo accountRepo;
    private final AccessService accessService;

    /**
     * Kontent izohlari, yangisi tepada.
     *
     * @param viewer kirgan foydalanuvchi yoki {@code null} — mehmon. Kirgan
     *               odamga moderator yashirgan o'z izohlari ham ko'rinadi.
     */
    @Transactional(readOnly = true)
    public Page<Comment> list(User viewer, Long contentId, int page, int size) {
        visibleContent(viewer, contentId);

        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        return viewer == null
                ? commentRepo.findForApp(contentId, CommentStatus.VISIBLE, pageable)
                : commentRepo.findForAppWithOwnHidden(contentId, CommentStatus.VISIBLE,
                        CommentStatus.HIDDEN, viewer.getId(), pageable);
    }

    @Transactional
    public Comment post(User author, Long contentId, String text) {
        Content content = visibleContent(author, contentId);

        // Bloklangan hisob tomosha ham qila olmaydi (AccessService) —
        // izoh yozishi ham mumkin emas, aks holda blok ochiq eshik bo'lardi.
        UserAccount account = accountRepo.findByUserId(author.getId()).orElse(null);
        if (account != null && account.getStatus() == UserStatus.BLOCKED) {
            throw BusinessException.accessDenied("Hisobingiz bloklangan");
        }

        String clean = text == null ? "" : text.strip();
        if (clean.isEmpty()) {
            throw BusinessException.validation("Izoh bo'sh bo'lishi mumkin emas");
        }
        if (clean.length() > MAX_LENGTH) {
            throw BusinessException.validation(
                    "Izoh " + MAX_LENGTH + " belgidan oshmasligi kerak");
        }

        return commentRepo.save(Comment.builder()
                .author(author)
                .content(content)
                .text(clean)
                .status(CommentStatus.VISIBLE)
                .build());
    }

    /**
     * O'z izohini o'chirish.
     *
     * ⚠️ Faqat MUALLIF. Moderatorning yashirishi — boshqa yo'l (panel,
     * {@code ModerationService}); bu yerda begona izohni o'chirish
     * imkoni bo'lsa, istalgan odam istalgan izohni yo'qota olardi.
     *
     * Takroriy so'rov xato bermaydi: tarmoq javobni yo'qotib, ilova
     * qayta yuborsa, odam «topilmadi» ni ko'rmasligi kerak.
     */
    @Transactional
    public void delete(User actor, Long commentId) {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> BusinessException.notFound("Comment", commentId));

        if (comment.getAuthor() == null || !comment.getAuthor().getId().equals(actor.getId())) {
            throw BusinessException.accessDenied("Faqat o'z izohingizni o'chira olasiz");
        }
        if (comment.getStatus() != CommentStatus.DELETED) {
            comment.setStatus(CommentStatus.DELETED);
        }
    }

    /**
     * Nashr qilinmagan yoki o'chirilgan kontent umuman «yo'q» — kartochka
     * va donat reytingi bilan bir xil qoida ({@code ContentController}).
     */
    private Content visibleContent(User viewer, Long contentId) {
        Content content = contentRepo.findById(contentId)
                .orElseThrow(() -> BusinessException.notFound("Content", contentId));
        if (!accessService.isVisible(viewer, content)) {
            throw BusinessException.notFound("Content", contentId);
        }
        return content;
    }
}
