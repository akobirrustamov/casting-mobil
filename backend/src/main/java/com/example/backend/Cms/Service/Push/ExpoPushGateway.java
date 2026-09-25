package com.example.backend.Cms.Service.Push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expo Push Service — {@code POST https://exp.host/--/api/v2/push/send}.
 *
 * <h2>Nima uchun FCM to'g'ridan-to'g'ri emas</h2>
 * Ilova Expo'da qurilgan va tokenni {@code expo-notifications} beradi.
 * Expo o'zi Android'da FCM'ga, iOS'da APNs'ga uzatadi — backendga
 * Firebase kaliti kerak emas. FCM kaliti EAS'ga bir marta yuklanadi
 * ({@code eas credentials}).
 *
 * <h2>Sozlamalar</h2>
 * <ul>
 *   <li>{@code app.push.enabled} — o'chirilsa hech narsa yuborilmaydi
 *       (testlar);</li>
 *   <li>{@code app.push.expo.access-token} — ixtiyoriy. Expo loyihasida
 *       «Enhanced push security» yoqilgan bo'lsa SHART.</li>
 * </ul>
 *
 * Bitta so'rovda ko'pi bilan 100 ta xabar (Expo cheklovi).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpoPushGateway implements PushGateway {

    static final int CHUNK = 100;

    private final RestTemplate restTemplate;

    @Value("${app.push.enabled:true}")
    private boolean enabled;

    @Value("${app.push.expo.url:https://exp.host/--/api/v2/push/send}")
    private String url;

    @Value("${app.push.expo.access-token:}")
    private String accessToken;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<PushResult> send(List<PushMessage> messages) {
        List<PushResult> out = new ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i += CHUNK) {
            out.addAll(sendChunk(messages.subList(i, Math.min(i + CHUNK, messages.size()))));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<PushResult> sendChunk(List<PushMessage> chunk) {
        List<Map<String, Object>> body = chunk.stream().map(ExpoPushGateway::toJson).toList();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken.trim());
        }

        Map<String, Object> response;
        try {
            response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
        } catch (RestClientException e) {
            // Butun to'plam yiqildi (tarmoq, 401, 5xx) — har biri xato.
            log.warn("Expo push so'rovi yiqildi: {}", e.getMessage());
            return failAll(chunk.size(), "Expo: " + e.getMessage());
        }

        Object data = response == null ? null : response.get("data");
        if (!(data instanceof List<?> tickets) || tickets.size() != chunk.size()) {
            log.warn("Expo push javobi kutilmagan: {}", response);
            return failAll(chunk.size(), "Expo javobi noto'g'ri: " + response);
        }

        List<PushResult> out = new ArrayList<>(chunk.size());
        for (Object t : tickets) {
            Map<String, Object> ticket = t instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
            if ("ok".equals(ticket.get("status"))) {
                out.add(PushResult.accepted());
                continue;
            }
            Object details = ticket.get("details");
            Object code = details instanceof Map<?, ?> d ? d.get("error") : null;
            String message = String.valueOf(ticket.getOrDefault("message", code));
            out.add(new PushResult(false, "DeviceNotRegistered".equals(code), message));
        }
        return out;
    }

    private static Map<String, Object> toJson(PushMessage m) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("to", m.token());
        json.put("title", m.title());
        json.put("body", m.body());
        json.put("data", m.data());
        json.put("sound", "default");
        json.put("priority", "high");
        // Ilova yaratadigan Android kanali (`features/notifications/push.ts`).
        json.put("channelId", "default");
        if (m.image() != null) {
            // Android buni o'zi ko'rsatadi; iOS'da Notification Service
            // Extension kerak — u yo'q, iPhone rasmsiz ko'rsatadi.
            json.put("richContent", Map.of("image", m.image()));
        }
        return json;
    }

    private static List<PushResult> failAll(int n, String error) {
        List<PushResult> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(PushResult.failed(error));
        }
        return out;
    }
}
