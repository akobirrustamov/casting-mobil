package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Enums.CommentStatus;
import com.example.backend.Cms.Service.AppCommentService;
import com.example.backend.Entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ilovadagi izohlar (10.09.2026).
 *
 * <pre>
 *   GET    /api/v1/app/content/{id}/comments   ro'yxat — mehmonga ham ochiq
 *   POST   /api/v1/app/content/{id}/comments   yozish — kirish talab qilinadi
 *   DELETE /api/v1/app/comments/{id}           o'z izohini o'chirish
 * </pre>
 *
 * <h2>Nima uchun ro'yxat ochiq</h2>
 * Kartochkadagi «Izohlar» plitkasi mehmonga ham ko'rinadi. Sonini ko'rsatib,
 * mazmunini yopish — «12 ta izoh bor, lekin kirmaguningizcha aytmaymiz»
 * bo'lardi. Yozish esa kim yozgani bilan bog'liq, shuning uchun tokensiz
 * 401 ({@code /api/**} qoidasi).
 *
 * Qoidalar {@link AppCommentService} da.
 */
@RestController
@RequiredArgsConstructor
public class AppCommentController {

    private final AppCommentService commentService;

    @GetMapping("/api/v1/app/content/{contentId}/comments")
    public ResponseEntity<CommentPage> list(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User viewer = CurrentUser.getOrNull();
        Page<Comment> result = commentService.list(viewer, contentId, page, size);

        return ResponseEntity.ok(CommentPage.builder()
                .items(result.getContent().stream().map(c -> CommentDto.of(c, viewer)).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .hasMore(result.hasNext())
                .build());
    }

    @PostMapping("/api/v1/app/content/{contentId}/comments")
    public ResponseEntity<CommentDto> post(
            @PathVariable Long contentId,
            @RequestBody CommentRequest body) {

        User author = CurrentUser.get();
        Comment saved = commentService.post(author, contentId, body == null ? null : body.getText());
        return ResponseEntity.ok(CommentDto.of(saved, author));
    }

    @DeleteMapping("/api/v1/app/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId) {
        commentService.delete(CurrentUser.get(), commentId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ DTO

    @Data
    public static class CommentRequest {
        private String text;
    }

    @Data
    @Builder
    public static class CommentPage {
        private List<CommentDto> items;
        private int page;
        private int size;
        private long totalItems;
        /** Keyingi sahifa bormi — ilova shunga qarab pastga yetganda so'raydi. */
        private boolean hasMore;
    }

    @Data
    @Builder
    public static class CommentDto {
        private Long id;
        private String text;
        private LocalDateTime createdAt;

        /** Ism va rasm — {@code Donor} bilan bir xil manba ({@code User}). */
        private String authorName;
        private String authorAvatarUrl;

        /** So'rayotgan odamning o'z izohi — ilova faqat unga «o'chirish» ni ko'rsatadi. */
        private boolean mine;

        /**
         * Moderator yashirgan. Bunday izoh faqat MUALLIFGA keladi — u
         * izohi yo'qolmaganini, balki yashirilganini ko'rishi uchun.
         */
        private boolean hidden;

        static CommentDto of(Comment c, User viewer) {
            User author = c.getAuthor();
            return CommentDto.builder()
                    .id(c.getId())
                    .text(c.getText())
                    .createdAt(c.getCreatedAt())
                    .authorName(author == null ? null : author.getName())
                    .authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                    .mine(viewer != null && author != null && author.getId().equals(viewer.getId()))
                    .hidden(c.getStatus() == CommentStatus.HIDDEN)
                    .build();
        }
    }
}
