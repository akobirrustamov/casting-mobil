package com.example.backend.Cms.Service;

import com.example.backend.Admin.Dto.NotificationSaveRequest;
import com.example.backend.Cms.Entity.InternalLink;
import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Entity.NotificationTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.NotificationStatus;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Repository.NotificationRepo;
import com.example.backend.Entity.User;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Bildirishnomalarni yaratish va rejalashtirish.
 *
 * ⚠️ HAQIQIY YUBORISH ULANMAGAN. FCM provayderi sozlanmagan, shuning uchun
 * "yuborildi" holati qo'yilmaydi va soxta statistika ko'rsatilmaydi (§32, §33).
 * Yuborishga urinilsa aniq xato qaytariladi — jim muvaffaqiyat emas.
 */
@Service
@RequiredArgsConstructor
public class NotificationAdminService {

    private final NotificationRepo notificationRepo;
    private final MediaAssetRepo mediaAssetRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<Notification> list(Pageable pageable) {
        Page<Notification> page = notificationRepo.findAllByOrderByCreatedAtDesc(pageable);
        // Sahifa toza limit bilan olindi; tarjimalar bitta so'rov bilan
        // to'ldiriladi. Batafsil: PageHydrator.
        return PageHydrator.warm(page, Notification::getId, notificationRepo::findAllByIdIn);
    }

    @Transactional
    public Notification save(User actor, Long id, NotificationSaveRequest request) {
        var uz = request.getTranslations().get(Locale.UZ);
        if (uz == null || isBlank(uz.getTitle()) || isBlank(uz.getBody())) {
            throw BusinessException.validation(
                    "O'zbekcha sarlavha va matn majburiy - u asosiy til");
        }

        Notification n = id == null ? new Notification()
                : notificationRepo.findById(id)
                        .orElseThrow(() -> BusinessException.notFound("Notification", id));

        // Yuborilgan bildirishnoma o'zgartirilmaydi - u allaqachon yetib borgan
        if (n.getStatus() == NotificationStatus.SENT) {
            throw new BusinessException("NOTIFICATION_ALREADY_SENT",
                    "Yuborilgan bildirishnomani o'zgartirib bo'lmaydi",
                    HttpStatus.CONFLICT);
        }

        n.setType(request.getType());
        n.setAudience(request.getAudience());
        n.setImage(request.getImageMediaId() == null ? null
                : mediaAssetRepo.findById(request.getImageMediaId())
                        .orElseThrow(() -> BusinessException.notFound("Media", request.getImageMediaId())));
        n.setLink(request.getLink() == null ? new InternalLink() : request.getLink().toEntity());
        n.setScheduledAt(request.getScheduledAt());
        n.setStatus(request.getScheduledAt() != null
                ? NotificationStatus.SCHEDULED : NotificationStatus.DRAFT);
        if (id == null) {
            n.setCreatedBy(actor == null ? null : actor.getId());
        }

        Map<Locale, NotificationTranslation> existing = new HashMap<>();
        n.getTranslations().forEach(t -> existing.put(t.getLocale(), t));
        Set<Locale> keep = new HashSet<>();
        request.getTranslations().forEach((locale, dto) -> {
            if (dto == null || isBlank(dto.getTitle()) || isBlank(dto.getBody())) {
                return;
            }
            keep.add(locale);
            NotificationTranslation row = existing.get(locale);
            if (row == null) {
                row = NotificationTranslation.builder().locale(locale).build();
                n.addTranslation(row);
            }
            row.setTitle(dto.getTitle().trim());
            row.setBody(dto.getBody().trim());
        });
        n.getTranslations().removeIf(t -> !keep.contains(t.getLocale()));

        Notification saved = notificationRepo.save(n);
        auditService.log(actor, id == null ? "NOTIFICATION_CREATED" : "NOTIFICATION_UPDATED",
                "Notification", saved.getId(), null,
                Map.of("type", saved.getType(), "audience", saved.getAudience(),
                        "status", saved.getStatus()));
        return saved;
    }

    /**
     * Yuborishga urinish.
     *
     * ⚠️ FCM ULANMAGAN. Provayder ulangach bu yerda haqiqiy chaqiruv bo'ladi
     * va status FAQAT tasdiqdan keyin SENT ga o'tadi. Hozircha har urinish
     * FAILED sifatida yoziladi — "yuborildi" deb belgilash foydalanuvchilar
     * xabar olgandek taassurot qoldirardi.
     *
     * ⚠️ Bu metod ISTISNO TASHLAMAYDI. Ilgari tashlardi va tranzaksiya
     * qaytarilib, urinish haqidagi yozuv ham, audit ham yo'qolardi — ya'ni
     * urinishdan hech qanday iz qolmasdi. Endi natija SAQLANADI, HTTP kodini
     * esa controller qaytaradi.
     *
     * @return saqlangan bildirishnoma; muvaffaqiyatsizlikda status FAILED
     */
    @Transactional
    public Notification send(User actor, Long id) {
        Notification n = notificationRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Notification", id));

        if (n.getStatus() == NotificationStatus.SENT) {
            throw new BusinessException("NOTIFICATION_ALREADY_SENT",
                    "Bu bildirishnoma allaqachon yuborilgan", HttpStatus.CONFLICT);
        }

        // TODO: FCM ulangach — provayderga yuborish, javobga qarab SENT yoki FAILED.
        n.setStatus(NotificationStatus.FAILED);
        n.setFailureReason(PROVIDER_NOT_CONFIGURED);
        Notification saved = notificationRepo.save(n);

        auditService.log(actor, AuditAction.NOTIFICATION_SENT, "Notification", id, null,
                Map.of("result", "provider_not_configured"));
        return saved;
    }

    /** Provayder sozlanmaganini bildiruvchi sabab — controller ham tekshiradi. */
    public static final String PROVIDER_NOT_CONFIGURED =
            "Push provayderi (FCM) sozlanmagan. APP_FCM_CREDENTIALS berilmagan.";

    @Transactional
    public void cancel(User actor, Long id) {
        Notification n = notificationRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Notification", id));
        if (n.getStatus() == NotificationStatus.SENT) {
            throw new BusinessException("NOTIFICATION_ALREADY_SENT",
                    "Yuborilgan bildirishnomani bekor qilib bo'lmaydi", HttpStatus.CONFLICT);
        }
        n.setStatus(NotificationStatus.CANCELLED);
        notificationRepo.save(n);
        auditService.log(actor, "NOTIFICATION_CANCELLED", "Notification", id);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
