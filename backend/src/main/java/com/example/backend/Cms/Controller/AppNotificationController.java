package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Entity.NotificationTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.NotificationAudience;
import com.example.backend.Cms.Enums.NotificationStatus;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

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
 * <h2>«O'qilgan» belgisi YO'Q — ataylab</h2>
 * U alohida jadval talab qiladi (kim nimani o'qigan) va har ochilishda
 * yozuv. Buyurtmachiga birinchi versiyada bu taklif qilinmadi: xabarlar
 * kam, va o'qilmaganlar soni uchun butun jadval saqlash erta. Kerak
 * bo'lsa keyin qo'shiladi — hozirgi shakl unga xalaqit bermaydi.
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
            @RequestParam(required = false) Locale locale) {

        User user = CurrentUser.get();
        Locale resolved = homeFeedService.resolveLanguage(user, locale);
        boolean premium = accessService.premiumStatus(user).active();

        // ⚠️ Faqat YUBORILGANLARI. Qoralama va rejalashtirilgani hali
        // xabar emas: birinchisi tayyor emas, ikkinchisining vaqti
        // kelmagan — ikkalasi ham ilovada ko'rinmasligi kerak.
        List<Notification> sent = notificationRepo
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, LIMIT * 2))
                .getContent().stream()
                .filter(n -> n.getStatus() == NotificationStatus.SENT)
                .filter(n -> matches(n.getAudience(), premium))
                .limit(LIMIT)
                .toList();

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
                .map(n -> map(n, resolved))
                .toList();

        return ResponseEntity.ok(result);
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

    private NotificationDto map(Notification n, Locale locale) {
        NotificationTranslation text = TranslationPicker.pick(
                n.getTranslations(), locale, NotificationTranslation::getLocale);

        return NotificationDto.builder()
                .id(n.getId())
                .title(text == null ? null : text.getTitle())
                .body(text == null ? null : text.getBody())
                .imageId(n.getImage() == null ? null : n.getImage().getId())
                .sentAt(n.getSentAt())
                .linkType(n.getLink() == null ? null : String.valueOf(n.getLink().getLinkType()))
                .linkUrl(n.getLink() == null ? null : n.getLink().getLinkUrl())
                .targetType(n.getLink() == null || n.getLink().getInternalTargetType() == null
                        ? null : n.getLink().getInternalTargetType().name())
                .targetId(n.getLink() == null ? null : n.getLink().getInternalTargetId())
                .build();
    }

    // ------------------------------------------------------------------ DTO

    @Data
    @Builder
    public static class NotificationDto {
        private Long id;

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
    }
}
