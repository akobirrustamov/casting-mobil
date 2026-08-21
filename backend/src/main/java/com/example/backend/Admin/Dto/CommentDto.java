package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Enums.CommentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommentDto {

    private Long id;
    private UUID authorId;
    private String authorName;
    private String authorPhone;
    private Long contentId;
    private String contentSlug;
    private Long episodeId;
    private String text;
    private CommentStatus status;
    private Integer reportsCount;
    private LocalDateTime createdAt;
    private LocalDateTime moderatedAt;

    public static CommentDto from(Comment c) {
        return CommentDto.builder()
                .id(c.getId())
                .authorId(c.getAuthor() == null ? null : c.getAuthor().getId())
                .authorName(c.getAuthor() == null ? null : c.getAuthor().getName())
                .authorPhone(c.getAuthor() == null ? null : c.getAuthor().getPhone())
                .contentId(c.getContent() == null ? null : c.getContent().getId())
                .contentSlug(c.getContent() == null ? null : c.getContent().getSlug())
                .episodeId(c.getEpisode() == null ? null : c.getEpisode().getId())
                .text(c.getText())
                .status(c.getStatus())
                .reportsCount(c.getReportsCount())
                .createdAt(c.getCreatedAt())
                .moderatedAt(c.getModeratedAt())
                .build();
    }
}
