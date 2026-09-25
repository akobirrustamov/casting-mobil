package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Entity.NotificationRead;
import com.example.backend.Cms.Entity.NotificationTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.NotificationAudience;
import com.example.backend.Cms.Enums.NotificationStatus;
import com.example.backend.Cms.Enums.NotificationType;
import com.example.backend.Cms.Repository.NotificationReadRepo;
import com.example.backend.Cms.Repository.NotificationRepo;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.HomeFeedService;
import com.example.backend.Cms.Service.TranslationPicker;
import com.example.backend.Entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bildirishnomalar — ilova ichida o'qish.
 *
 * <h2>⚠️ Qaysi bo'shliq yopilyapti</h2>
 * Modul backendda TO'LIQ edi: {@code cms_notification}, uch tildagi
 * tarjimalar, rejalashtirish, admin sahifasi. Ilovada esa
 * {@code app/messages.tsx} bo'sh ekran bo'lib turardi — ya'ni admin
 * yozgan xabarni hech kim ko'rmasdi.
 *
 * <h2>FCM ulanmagan — bu to'siq emas</h2>
 * Push yuborilmaydi ({@code sentAt} qo'yiladi, xabarning o'zi
 * ketmaydi). Lekin ilova ichida ro'yxatni ko'rsatish uchun push kerak
 * emas: xabar bazada turibdi va uni o'qish mumkin.
 *
 * <h2>«O'qilgan» belgisi (25.09.2026)</h2>
 * Buyurtmachi: qo'ng'iroqchada «yangi xabar bor» qizil belgisi chiqsin,
 * «Xabarlar» ochilganda esa xabarlar o'qilgan bo'lsin. Belgi
 * {@code cms_notification_read} da (V43) — odamga tegishli, qurilmaga
 * emas.
 * <ul>
 *   <li>{@code GET /unread-count} — qo'ng'iroqchadagi son;</li>
 *   <li>{@code POST /read} — ekran ochildi, ko'rinayotganlarning hammasi
 *       o'qildi;</li>
 *   <li>{@code POST /{id}/read} — push bosildi va xabar ekrani
 *       ochilmasdan to'g'ridan-to'g'ri havolaga o'tildi.</li>
 * </ul>
 *
 * <h2>Ikki tur (25.09.2026)</h2>
 * Admin xabar turini tanlaydi: {@code APP_NOTIFICATION} — bosh sahifadagi
 * qo'ng'iroqcha, {@code CASTING_NOTIFICATION} — «Casting» bo'limidagi
 * qo'ng'iroqcha. Har bir endpoint ixtiyoriy {@code type} oladi; berilmasa
 * hammasi (eski ilova versiyalari uchun).
 */
@RestController
@RequestMapping("/api/v1/app/notifications")
@RequiredArgsConstructor
public class AppNotificationController {

    /**
     * Bitta so'rovda nechta xabar.
     *
     * ⚠️ Sahifalash yo'q: bu ro'yxat admin qo'lda yozadigan e'lonlardan
     * iborat va u yuzlab qatorga o'smaydi. Chegara baribir qo'yilgan —
     * jadval kutilmaganda o'sib ketsa, ilova butun tarixni tortmasin.
     */
    private static final int LIMIT = 50;

    private final NotificationRepo notificationRepo;
    private final NotificationReadRepo readRepo;
    private final AccessService accessService;
    private final HomeFeedService homeFeedService;

    /**
     * Menga tegishli xabarlar — yangi birinchi.
     *
     * ⚠️ Token talab qilinadi: auditoriya Premium holatiga qarab
     * ajratiladi, ya'ni «kimga» degan savolga javob bo'lmasa ro'yxatni
     * yig'ib bo'lmaydi.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<NotificationDto>> list(
            @RequestParam(required = false) Locale locale,
            @RequestParam(required = false) NotificationType type) {

        User user = CurrentUser.get();
        Locale resolved = homeFeedService.resolveLanguage(user, locale);
        List<Notification> sent = visible(user, type);
        Set<Long> read = readIds(user, sent);

        // Tarjimalar alohida so'rov bilan — sahifalash bilan fetch join
        // birga ishlamaydi (`NotificationRepo` izohida yozilgan).
        List<Notification> withText = sent.isEmpty()
                ? List.of()
                : notificationRepo.findAllByIdIn(sent.stream().map(Notification::getId).toList());

        // `findAllByIdIn` tartibni kafolatlamaydi — asl tartibni saqlaymiz.
        List<NotificationDto> result = sent.stream()
                .map(n -> withText.stream()
                        .filter(x -> x.getId().equals(n.getId()))
                        .findFirst()
                        .orElse(n))
                .map(n -> map(n, resolved, read.contains(n.getId())))
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * O'qilmaganlar soni — qo'ng'iroqchadagi qizil belgi.
     *
     * ⚠️ Ro'yxat bilan BIR XIL to'plamdan sanaladi ({@link #visible}).
     * Aks holda belgi odamga ko'rsatilmaydigan (boshqa auditoriya)
     * xabarlarni ham sanardi va ekran ochilgandan keyin ham o'chmasdi.
     */
    @GetMapping("/unread-count")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Long>> unreadCount(
            @RequestParam(required = false) NotificationType type) {
        User user = CurrentUser.get();
        List<Notification> sent = visible(user, type);
        Set<Long> read = readIds(user, sent);
        long unread = sent.stream().filter(n -> !read.contains(n.getId())).count();
        return ResponseEntity.ok(Map.of("count", unread));
    }

    /** «Xabarlar» ochildi — ko'rinayotgan hamma xabar o'qildi. */
    @PostMapping("/read")
    @Transactional
    public ResponseEntity<Void> readAll(
            @RequestParam(required = false) NotificationType type) {
        User user = CurrentUser.get();
        List<Notification> sent = visible(user, type);
        Set<Long> read = readIds(user, sent);
        markRead(user, sent.stream().map(Notification::getId).filter(id -> !read.contains(id)).toList());
        return ResponseEntity.noContent().build();
    }

    /**
     * Bitta xabar o'qildi — push bosilib, havolaga to'g'ridan-to'g'ri
     * o'tilganda.
     *
     * ⚠️ Faqat shu odamga KO'RINADIGAN xabar belgilanadi: mavjud bo'lmagan
     * id chet el kalitiga urilib 500 berardi, boshqa auditoriyaning
     * xabari esa jadvalda ma'nosiz qator bo'lib qolardi.
     */
    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Void> readOne(@PathVariable Long id) {
        User user = CurrentUser.get();
        boolean visible = visible(user, null).stream().anyMatch(n -> n.getId().equals(id));
        if (visible && readRepo.findReadIds(user.getId(), List.of(id)).isEmpty()) {
            markRead(user, List.of(id));
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Shu odamga ko'rsatiladigan xabarlar — yangi birinchi.
     *
     * ⚠️ Faqat YUBORILGANLARI. Qoralama va rejalashtirilgani hali
     * xabar emas: birinchisi tayyor emas, ikkinchisining vaqti
     * kelmagan — ikkalasi ham ilovada ko'rinmasligi kerak.
     */
    private List<Notification> visible(User user, NotificationType type) {
        boolean premium = accessService.premiumStatus(user).active();
        return notificationRepo
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, LIMIT * 2))
                .getContent().stream()
                .filter(n -> n.getStatus() == NotificationStatus.SENT)
                .filter(n -> type == null || typeOf(n) == type)
                .filter(n -> matches(n.getAudience(), premium))
                .limit(LIMIT)
                .toList();
    }

    /** Eski qatorlarda tur bo'lmasa — umumiy ilova xabari. */
    private static NotificationType typeOf(Notification n) {
        return n.getType() == null ? NotificationType.APP_NOTIFICATION : n.getType();
    }

    private Set<Long> readIds(User user, List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(readRepo.findReadIds(user.getId(),
                notifications.stream().map(Notification::getId).toList()));
    }

    private void markRead(User user, List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        readRepo.saveAll(ids.stream()
                .map(id -> NotificationRead.builder()
                        .userId(user.getId())
                        .notificationId(id)
                        .readAt(now)
                        .build())
                .toList());
    }

    /**
     * Auditoriya mos keladimi.
     *
     * ⚠️ Qaror shu yerda, chunki u faqat shu ro'yxat uchun kerak va
     * kirish huquqiga aloqasi yo'q: bu «kimga ko'rsatamiz», «nimani
     * ochamiz» emas. Premium holatining O'ZI esa
     * {@code AccessService} dan olinadi — u bitta joyda (ТЗ §37).
     */
    private static boolean matches(NotificationAudience audience, boolean premium) {
        if (audience == null || audience == NotificationAudience.ALL) {
            return true;
        }
        return audience == NotificationAudience.PREMIUM_ONLY ? premium : !premium;
    }

    private NotificationDto map(Notification n, Locale locale, boolean read) {
        NotificationTranslation text = TranslationPicker.pick(
                n.getTranslations(), locale, NotificationTranslation::getLocale);

        return NotificationDto.builder()
                .id(n.getId())
                .type(typeOf(n).name())
                .title(text == null ? null : text.getTitle())
                .body(text == null ? null : text.getBody())
                .imageId(n.getImage() == null ? null : n.getImage().getId())
                .sentAt(n.getSentAt())
                .linkType(n.getLink() == null ? null : String.valueOf(n.getLink().getLinkType()))
                .linkUrl(n.getLink() == null ? null : n.getLink().getLinkUrl())
                .targetType(n.getLink() == null || n.getLink().getInternalTargetType() == null
                        ? null : n.getLink().getInternalTargetType().name())
                .targetId(n.getLink() == null ? null : n.getLink().getInternalTargetId())
                .read(read)
                .build();
    }

    // ------------------------------------------------------------------ DTO

    @Data
    @Builder
    public static class NotificationDto {
        private Long id;

        /** {@code APP_NOTIFICATION} / {@code CASTING_NOTIFICATION}. */
        private String type;

        /** Tanlangan tildagi sarlavha. */
        private String title;
        private String body;

        /** Rasm — {@code /api/v1/app/media/{id}/raw}. */
        private Long imageId;

        /**
         * Qachon yuborilgan.
         *
         * ⚠️ {@code createdAt} emas: admin xabarni bir hafta oldin
         * yozib, keyin yuborishi mumkin. Odam uchun sana — u xabarni
         * olgan kun.
         */
        private LocalDateTime sentAt;

        /** {@code NONE} / {@code INTERNAL} / {@code EXTERNAL}. */
        private String linkType;
        private String linkUrl;

        /** Ichki havola: nimaga va qaysi id ga. */
        private String targetType;
        private Long targetId;

        /** Shu odam o'qiganmi. */
        private boolean read;
    }
}
