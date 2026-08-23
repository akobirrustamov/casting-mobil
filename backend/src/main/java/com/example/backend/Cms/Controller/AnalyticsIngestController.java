package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Enums.AnalyticsEventType;
import com.example.backend.Cms.Service.AnalyticsService;
import com.example.backend.exceptions.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Klientdan analitika hodisalarini qabul qilish.
 *
 * <b>Bu endpoint OCHIQ.</b> Sabab: reklama ko'rsatilishi tizimga kirmagan
 * foydalanuvchida ham qayd etilishi kerak. Token bo'lsa — foydalanuvchi
 * aniqlanadi, bo'lmasa {@code deviceKey} bo'yicha anonim hisoblanadi.
 *
 * ⚠️ <b>Suiiste'mol xavfi.</b> Ochiq yozish endpointi soxta ko'rsatkich
 * yuborish uchun ishlatilishi mumkin. Hozircha himoya — partiya hajmi
 * cheklovi. Prod uchun rate limiting yoki klient imzosi kerak; bu
 * roadmap.md → PHASE 9 da qayd etilgan.
 */
@RestController
@RequestMapping("/api/v1/app/analytics")
@RequiredArgsConstructor
public class AnalyticsIngestController {

    /** Bitta so'rovda maksimum hodisa. Klient buferlab yuborishi mumkin. */
    private static final int MAX_BATCH = 50;

    private final AnalyticsService analyticsService;

    @PostMapping("/events")
    public ResponseEntity<Void> ingest(@Valid @RequestBody EventBatch batch) {
        if (batch.getEvents().size() > MAX_BATCH) {
            throw BusinessException.validation(
                    "Bitta so'rovda " + MAX_BATCH + " tadan ortiq hodisa yuborib bo'lmaydi");
        }

        // Token bo'lsa foydalanuvchi aniqlanadi; bo'lmasa anonim — bu normal holat
        UUID userId = CurrentUser.getOrNull() == null ? null : CurrentUser.get().getId();

        for (EventDto e : batch.getEvents()) {
            analyticsService.record(e.getType(), e.getTargetId(), e.getEpisodeId(),
                    userId, batch.getDeviceKey());
        }
        return ResponseEntity.accepted().build();
    }

    @Data
    public static class EventBatch {
        /** Anonim foydalanuvchini unikal sanash uchun. Shaxsni aniqlamaydi. */
        @Size(max = 128)
        private String deviceKey;

        @NotNull(message = "Hodisalar ro'yxati bo'sh")
        @Size(min = 1, message = "Kamida bitta hodisa bo'lishi kerak")
        private List<EventDto> events;
    }

    @Data
    public static class EventDto {
        @NotNull(message = "Hodisa turi kiritilmagan")
        private AnalyticsEventType type;
        private Long targetId;
        private Long episodeId;
    }
}
