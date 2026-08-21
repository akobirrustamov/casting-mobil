package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Entity.NotificationTranslation;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class NotificationDto {

    private Long id;
    private NotificationType type;
    private NotificationAudience audience;
    private Long imageMediaId;
    private InternalLinkDto link;
    private NotificationStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private String failureReason;
    private LocalDateTime createdAt;
    private Map<Locale, NotificationSaveRequest.NotificationTextDto> translations;

    public static NotificationDto from(Notification n) {
        Map<Locale, NotificationSaveRequest.NotificationTextDto> tr = new LinkedHashMap<>();
        for (NotificationTranslation t : n.getTranslations()) {
            var dto = new NotificationSaveRequest.NotificationTextDto();
            dto.setTitle(t.getTitle());
            dto.setBody(t.getBody());
            tr.put(t.getLocale(), dto);
        }
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .audience(n.getAudience())
                .imageMediaId(n.getImage() == null ? null : n.getImage().getId())
                .link(InternalLinkDto.from(n.getLink()))
                .status(n.getStatus())
                .scheduledAt(n.getScheduledAt())
                .sentAt(n.getSentAt())
                .failureReason(n.getFailureReason())
                .createdAt(n.getCreatedAt())
                .translations(tr)
                .build();
    }
}
