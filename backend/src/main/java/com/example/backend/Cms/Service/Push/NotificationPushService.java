package com.example.backend.Cms.Service.Push;

import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Entity.NotificationTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.NotificationAudience;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Cms.Repository.NotificationRepo;
import com.example.backend.Cms.Repository.UserDeviceRepo;
import com.example.backend.Cms.Repository.UserDeviceRepo.PushTarget;
import com.example.backend.Cms.Service.TranslationPicker;
import com.example.backend.Cms.Service.Push.PushGateway.PushMessage;
import com.example.backend.Cms.Service.Push.PushGateway.PushResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Yuborilgan bildirishnomani telefonlarga push qilib chiqaradi.
 *
 * <h2>Qachon ishlaydi</h2>
 * {@code NotificationAdminService.send} xabarni SENT qiladi va
 * {@link NotificationSentEvent} chiqaradi. Push TRANZAKSIYA YOPILGANDAN
 * KEYIN va alohida oqimda ketadi:
 * <ul>
 *   <li>yopilishdan oldin yuborilsa va tranzaksiya qaytarilsa, telefonga
 *       bazada yo'q xabar chiqib qolardi;</li>
 *   <li>minglab tokenga HTTP so'rov admin tugmasini ushlab turmasin.</li>
 * </ul>
 *
 * <h2>Nima uchun bitta oqim</h2>
 * Xabarlar kam va navbat bilan ketsa bo'ladi. Umumiy Spring executori
 * ataylab ishlatilmaydi — {@code @EnableAsync} butun ilovaga ta'sir qilardi.
 *
 * <h2>Natija</h2>
 * Qabul qilingan / rad etilgan son xabarga yoziladi (hisobot, ТЗ §33).
 * Expo «DeviceNotRegistered» degan tokenlar tozalanadi — ilova o'chirilgan.
 */
@Slf4j
@Service
public class NotificationPushService {

    /** Push matni chegarasi: Expo xabari 4 KB dan oshmasligi kerak. */
    static final int MAX_BODY = 1000;

    private final NotificationRepo notificationRepo;
    private final UserDeviceRepo deviceRepo;
    private final PushGateway gateway;
    private final TransactionTemplate tx;

    /**
     * Push rasmi uchun ochiq manzil — telefon rasmni tokensiz yuklaydi.
     *
     * <h2>⚠️ Nega logo (25.09.2026)</h2>
     * Buyurtmachi: telefonga kelgan xabarda UzCasting logosi ko'rinsin.
     * Android'ning kichik ikonkasi faqat oq siluet (tizim uni bir rangga
     * bo'yaydi), rangli logo esa faqat rasm sifatida chiqadi. Admin
     * xabarga rasm biriktirgan bo'lsa — o'sha rasm, bo'lmasa — logo.
     */
    @Value("${app.push.public-base-url:https://uzcasting.com}")
    private String publicBaseUrl;

    @Value("${app.push.logo-path:/logo.png}")
    private String logoPath;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "notification-push");
        t.setDaemon(true);
        return t;
    });

    public NotificationPushService(NotificationRepo notificationRepo, UserDeviceRepo deviceRepo,
                                   PushGateway gateway, PlatformTransactionManager txManager) {
        this.notificationRepo = notificationRepo;
        this.deviceRepo = deviceRepo;
        this.gateway = gateway;
        this.tx = new TransactionTemplate(txManager);
    }

    /** Xabar SENT bo'ldi — tranzaksiya yopilgach push navbatga qo'yiladi. */
    public record NotificationSentEvent(Long notificationId) {
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSent(NotificationSentEvent event) {
        executor.submit(() -> {
            try {
                deliver(event.notificationId());
            } catch (RuntimeException e) {
                log.warn("Bildirishnoma #{} push qilinmadi: {}", event.notificationId(), e.getMessage(), e);
            }
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    /**
     * Push'ni SHU oqimda yuboradi va natijani yozadi.
     *
     * Ochiq, chunki testlar uni to'g'ridan-to'g'ri chaqiradi.
     *
     * @return yozilgan natija; push o'chirilgan bo'lsa {@code null}
     */
    public Result deliver(Long notificationId) {
        if (!gateway.isEnabled()) {
            log.info("Push o'chirilgan (app.push.enabled=false) — #{} faqat ilova ichida", notificationId);
            return null;
        }

        // 1. O'qish — qisqa tranzaksiya. HTTP paytida baza band turmasin.
        List<PushMessage> messages = tx.execute(s -> buildMessages(notificationId));
        if (messages == null) {
            return null;
        }

        // 2. Yuborish — tranzaksiyasiz.
        List<PushResult> results = messages.isEmpty() ? List.of() : gateway.send(messages);

        int accepted = 0;
        Set<String> gone = new LinkedHashSet<>();
        String firstError = null;
        for (int i = 0; i < results.size(); i++) {
            PushResult r = results.get(i);
            if (r.ok()) {
                accepted++;
                continue;
            }
            if (r.deviceGone()) {
                gone.add(messages.get(i).token());
            }
            if (firstError == null) {
                firstError = r.error();
            }
        }
        Result result = new Result(messages.size(), accepted, messages.size() - accepted, firstError);

        // 3. Natijani yozish.
        tx.executeWithoutResult(s -> {
            if (!gone.isEmpty()) {
                deviceRepo.clearPushTokens(gone);
            }
            notificationRepo.findById(notificationId).ifPresent(n -> {
                n.setPushRecipients(result.recipients());
                n.setPushAccepted(result.accepted());
                n.setPushFailed(result.failed());
                // Xabar ilovada BOR (SENT), faqat push qismi yiqilgan
                // bo'lsa — admin sababni ko'rsin.
                n.setFailureReason(result.accepted() == 0 && result.failed() > 0
                        ? truncate("Push yuborilmadi: " + result.firstError(), 500)
                        : null);
                notificationRepo.save(n);
            });
        });

        log.info("Bildirishnoma #{} push: {} qurilma, {} qabul, {} xato",
                notificationId, result.recipients(), result.accepted(), result.failed());
        return result;
    }

    public record Result(int recipients, int accepted, int failed, String firstError) {
    }

    // ------------------------------------------------------------ ichki

    private List<PushMessage> buildMessages(Long notificationId) {
        List<Notification> found = notificationRepo.findAllByIdIn(List.of(notificationId));
        if (found.isEmpty()) {
            return null;
        }
        Notification n = found.get(0);
        LocalDateTime now = LocalDateTime.now();

        Map<Locale, NotificationTranslation> textCache = new HashMap<>();
        Map<String, Object> data = payload(n);
        String image = image(n);

        List<PushMessage> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (PushTarget t : deviceRepo.findPushTargets()) {
            if (!seen.add(t.getPushToken())) {
                continue;
            }
            if (t.getStatus() == UserStatus.DELETED || t.getStatus() == UserStatus.BLOCKED) {
                continue;
            }
            boolean premium = t.getPremiumUntil() != null && t.getPremiumUntil().isAfter(now);
            if (!matches(n.getAudience(), premium)) {
                continue;
            }
            Locale lang = t.getLanguage() == null ? Locale.DEFAULT : t.getLanguage();
            NotificationTranslation text = textCache.computeIfAbsent(lang, l ->
                    TranslationPicker.pick(n.getTranslations(), l, NotificationTranslation::getLocale));
            if (text == null) {
                continue;
            }
            out.add(new PushMessage(t.getPushToken(), text.getTitle(),
                    truncate(text.getBody(), MAX_BODY), data, image));
        }
        return out;
    }

    /** Admin rasmi — {@code /api/v1/app/media/{id}/raw} ochiq; yo'q bo'lsa logo. */
    String image(Notification n) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return null;
        }
        String root = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        if (n.getImage() != null) {
            return root + "/api/v1/app/media/" + n.getImage().getId() + "/raw";
        }
        if (logoPath == null || logoPath.isBlank()) {
            return null;
        }
        return root + (logoPath.startsWith("/") ? logoPath : "/" + logoPath);
    }

    /** Ilova bosilganda qayerga ochishini shu ma'lumotdan biladi. */
    private static Map<String, Object> payload(Notification n) {
        Map<String, Object> data = new HashMap<>();
        data.put("notificationId", n.getId());
        if (n.getLink() != null) {
            if (n.getLink().getLinkType() != null) {
                data.put("linkType", String.valueOf(n.getLink().getLinkType()));
            }
            if (n.getLink().getLinkUrl() != null) {
                data.put("linkUrl", n.getLink().getLinkUrl());
            }
            if (n.getLink().getInternalTargetType() != null) {
                data.put("targetType", n.getLink().getInternalTargetType().name());
            }
            if (n.getLink().getInternalTargetId() != null) {
                data.put("targetId", n.getLink().getInternalTargetId());
            }
        }
        return data;
    }

    /** {@code AppNotificationController.matches} bilan bir xil qoida. */
    static boolean matches(NotificationAudience audience, boolean premium) {
        if (audience == null || audience == NotificationAudience.ALL) {
            return true;
        }
        return audience == NotificationAudience.PREMIUM_ONLY ? premium : !premium;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }
}
