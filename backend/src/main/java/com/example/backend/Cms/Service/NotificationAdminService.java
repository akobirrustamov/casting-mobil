package com.example.backend.Cms.Service;

import com.example.backend.Admin.Dto.NotificationDto;
import com.example.backend.Admin.Dto.NotificationReportDto;
import com.example.backend.Admin.Dto.NotificationSaveRequest;
import com.example.backend.Cms.Entity.InternalLink;
import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Entity.NotificationTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.AnalyticsEventType;
import com.example.backend.Cms.Enums.NotificationAudience;
import com.example.backend.Cms.Enums.NotificationStatus;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Repository.AnalyticsEventRepo;
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

import java.time.LocalDateTime;
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
    private final InternalLinkValidator linkValidator;
    private final AnalyticsEventRepo eventRepo;
    private final com.example.backend.Repository.UserRepo userRepo;
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
        // Havola nishoni bazada bor-yo'qligi tekshiriladi — reklama va
        // premyera bilan AYNI mexanizm (§28).
        linkValidator.validate(request.getLink());

        // Rejalashtirilgan bildirishnoma foydalanuvchiga BORADI, ya'ni u
        // uchala tilda bo'lishi shart: aks holda rus tilidagi odamga
        // o'zbekcha push kelardi va uni o'chirishning iloji yo'q — xabar
        // allaqachon telefonda.
        boolean willBeSent = request.getScheduledAt() != null;
        TranslationRules.require(request.getTranslations(),
                NotificationSaveRequest.NotificationTextDto::getTitle, "Sarlavha", willBeSent);
        TranslationRules.require(request.getTranslations(),
                NotificationSaveRequest.NotificationTextDto::getBody, "Matn", willBeSent);

        if (willBeSent && request.getScheduledAt().isBefore(LocalDateTime.now())) {
            // O'tmishdagi vaqt jimgina «hozir yuborish» ga aylanib
            // qolmasin — admin sanani xato kiritgan bo'lishi mumkin.
            throw BusinessException.validation(
                    "Rejalashtirilgan vaqt o'tmishda bo'lishi mumkin emas");
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

        // ⚠️ To'liqsiz til qatori JIMGINA TASHLANARDI.
        //
        // Admin rus tabida sarlavhani yozib matnni to'ldirmasa, pastdagi
        // sikl butun qatorni o'tkazib yuborardi: saqlash muvaffaqiyatli
        // ko'rinardi, yozilgan sarlavha esa izsiz yo'qolardi.
        //
        // Ikkala ustun ham `not null`, shuning uchun yarim qatorni saqlab
        // bo'lmaydi — jimgina yo'qotish o'rniga aniq xato.
        request.getTranslations().forEach((locale, dto) -> {
            if (dto == null) {
                return;
            }
            boolean titleFilled = !isBlank(dto.getTitle());
            boolean bodyFilled = !isBlank(dto.getBody());
            if (titleFilled != bodyFilled) {
                throw BusinessException.validation(locale.name()
                        + " tilida " + (titleFilled ? "matn" : "sarlavha")
                        + " to'ldirilmagan. Ikkalasi ham kerak — aks holda"
                        + " bu til umuman saqlanmaydi");
            }
        });

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

        // ⚠️ UCH TIL — AYNAN SHU YERDA.
        //
        // Ilgari tekshiruv faqat saqlashda va faqat `scheduledAt` berilgan
        // bo'lsa ishlardi. Ya'ni teshik bor edi: qoralamani o'zbekcha
        // yaratib, «yuborish» tugmasini bosish kifoya edi — rus tilidagi
        // foydalanuvchiga o'zbekcha push ketardi.
        //
        // Push telefonga chiqqandan keyin uni qaytarib olib bo'lmaydi,
        // shuning uchun tekshiruv yuborishning O'ZIDA turadi: qaysi yo'l
        // bilan kelishidan qat'i nazar.
        requireAllLanguages(n);

        // TODO: FCM ulangach — provayderga yuborish, javobga qarab SENT yoki FAILED.
        n.setStatus(NotificationStatus.FAILED);
        n.setFailureReason(PROVIDER_NOT_CONFIGURED);
        Notification saved = notificationRepo.save(n);

        auditService.log(actor, AuditAction.NOTIFICATION_SENT, "Notification", id, null,
                Map.of("result", "provider_not_configured"));
        return saved;
    }

    /**
     * Saqlangan bildirishnomada uchala til ham bormi.
     *
     * <h2>Nima uchun so'rov emas, ENTITY tekshiriladi</h2>
     * Yuborish so'rov bilan emas, ID bilan chaqiriladi: bildirishnoma
     * ancha oldin yaratilgan bo'lishi mumkin. Shuning uchun bazadagi
     * holat tekshiriladi.
     *
     * <h2>Nima uchun FAILED deb belgilanmaydi</h2>
     * Tarjimasi to'liqsiz bildirishnoma buzilgan emas — u shunchaki
     * TAYYOR emas. FAILED deb belgilash «provayder ishlamadi» degan
     * ma'noni berardi va admin muammoni butunlay boshqa joydan qidirardi.
     */
    private void requireAllLanguages(Notification n) {
        Map<Locale, NotificationTranslation> byLocale = new HashMap<>();
        n.getTranslations().forEach(t -> byLocale.put(t.getLocale(), t));

        TranslationRules.requireAll(byLocale, NotificationTranslation::getTitle, "Sarlavha");
        TranslationRules.requireAll(byLocale, NotificationTranslation::getBody, "Matn");
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

    /**
     * Bildirishnoma hisoboti (ТЗ §33).
     *
     * <h2>Qaysi ko'rsatkich HAQIQATDAN o'lchanadi</h2>
     * <ul>
     *   <li><b>sent · failed</b> — bizning yozuvimiz, ishonchli;</li>
     *   <li><b>opened · clicked</b> — klient yuboradigan analitika
     *       hodisasi ({@code NOTIFICATION_OPEN}, {@code NOTIFICATION_CLICK});</li>
     *   <li><b>delivered</b> — FAQAT push provayderi kvitansiyasidan
     *       kelishi mumkin. Provayder ulanmagan, shuning uchun raqam emas,
     *       «o'lchanmaydi» holati qaytariladi.</li>
     * </ul>
     *
     * Nol qaytarilsa admin «hech kimga yetib bormadi» deb o'ylardi — bu
     * yolg'on bo'lardi, chunki biz shunchaki BILMAYMIZ.
     */
    @Transactional(readOnly = true)
    public NotificationReportDto report(Long id) {
        Notification n = notificationRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Notification", id));

        // Qabul qiluvchilar bo'yicha ko'rsatkichlar uchun har bir odam
        // bo'yicha yozuv kerak. Bunday jadval yo'q va u push provayderi
        // ulangandan keyin paydo bo'ladi.
        String needsProvider = "Qabul qiluvchilar bo'yicha yozuv yo'q. "
                + PROVIDER_NOT_CONFIGURED;

        return NotificationReportDto.builder()
                .notificationId(n.getId())
                // Xabarning O'Z holati — bu voronka ko'rsatkichi EMAS.
                .status(n.getStatus().name())
                .scheduledAt(n.getScheduledAt())
                .sentAt(n.getSentAt())
                .failureReason(n.getFailureReason())

                // Auditoriya hajmi — HAQIQIY son, bazadan hisoblanadi.
                .audienceSize(NotificationReportDto.Metric.of(audienceSize(n.getAudience())))

                // ⚠️ Ilgari bu yerda `sent = 1` turardi, ya'ni «holati SENT».
                // Bu voronkani ma'nosiz qilardi: 1 kishiga yuborilgan
                // xabarni 250 kishi ochgan bo'lib chiqardi.
                .sent(NotificationReportDto.Metric.unavailable(needsProvider))
                .failed(NotificationReportDto.Metric.unavailable(needsProvider))
                .delivered(NotificationReportDto.Metric.unavailable(
                        "Yetkazish kvitansiyasi push provayderidan keladi. "
                                + PROVIDER_NOT_CONFIGURED))

                // Bular HAQIQIY: klient analitika hodisasini yuboradi.
                .opened(NotificationReportDto.Metric.of(
                        eventRepo.countByTypeAndTargetId(AnalyticsEventType.NOTIFICATION_OPEN, id),
                        eventRepo.countUniquesForTarget(AnalyticsEventType.NOTIFICATION_OPEN, id)))
                .clicked(NotificationReportDto.Metric.of(
                        eventRepo.countByTypeAndTargetId(AnalyticsEventType.NOTIFICATION_CLICK, id),
                        eventRepo.countUniquesForTarget(AnalyticsEventType.NOTIFICATION_CLICK, id)))
                .build();
    }

    /**
     * Nishon auditoriyasining hozirgi hajmi.
     *
     * ⚠️ Bu «yuborildi» EMAS — hech narsa yuborilmagan bo'lsa ham son
     * chiqadi. U {@code opened} ni talqin qilish uchun kerak: 250 ta
     * ochilish ko'p ham, oz ham bo'lishi mumkin — auditoriya 300 kishimi
     * yoki 300 000 kishimi, usiz bilinmaydi.
     *
     * Xodimlar hisobga olinmaydi — ular ilova foydalanuvchisi emas va
     * push xabar olmaydi.
     */
    private long audienceSize(NotificationAudience audience) {
        long all = userRepo.countAppUsers();
        if (audience == null || audience == NotificationAudience.ALL) {
            return all;
        }
        long premium = userRepo.countPremiumAppUsers(LocalDateTime.now());
        return audience == NotificationAudience.PREMIUM_ONLY ? premium : all - premium;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
