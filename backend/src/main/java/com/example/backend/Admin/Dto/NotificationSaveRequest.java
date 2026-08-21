package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.NotificationAudience;
import com.example.backend.Cms.Enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class NotificationSaveRequest {

    @NotNull(message = "Tur tanlanmagan")
    private NotificationType type = NotificationType.APP_NOTIFICATION;

    @NotNull(message = "Auditoriya tanlanmagan")
    private NotificationAudience audience = NotificationAudience.ALL;

    private Long imageMediaId;
    private InternalLinkDto link = new InternalLinkDto();

    /** null - qoralama, sana berilsa rejalashtiriladi. */
    private LocalDateTime scheduledAt;

    @NotNull(message = "Matnlar kiritilmagan")
    private Map<Locale, NotificationTextDto> translations = new LinkedHashMap<>();

    @Data
    public static class NotificationTextDto {
        private String title;
        private String body;
    }
}
