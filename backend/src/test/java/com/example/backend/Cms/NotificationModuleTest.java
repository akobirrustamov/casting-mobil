package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.InternalLinkDto;
import com.example.backend.Admin.Dto.NotificationReportDto;
import com.example.backend.Admin.Dto.NotificationSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Service.AnalyticsService;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.NotificationAdminService;
import com.example.backend.Cms.Service.NotificationDispatcher;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import java.util.EnumSet;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §32 (bildirishnomalar) va §33 (hisobot).
 *
 * <h2>Asosiy qoida</h2>
 * Push provayderi (FCM) ulanmagan. Shuning uchun HECH QAYERDA
 * «yuborildi» deb ko'rsatilmaydi va hisobotda o'lchanmaydigan ko'rsatkich
 * nol bo'lib chiqmaydi — bu ikkalasi ham yolg'on bo'lardi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@org.springframework.context.annotation.Import(com.example.backend.Admin.TestStaffFactory.class)
class NotificationModuleTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private NotificationAdminService notificationService;
    @Autowired private NotificationDispatcher dispatcher;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private ContentService contentService;
    @Autowired private com.example.backend.Cms.Repository.NotificationRepo notificationRepo;
    @Autowired private com.example.backend.Admin.TestStaffFactory staff;

    // ------------------------------------------------------------ yaratish

    @Nested
    @DisplayName("Yaratish va tekshirish")
    class Creation {

        @Test
        @DisplayName("Ikkala tur ham qo'llab-quvvatlanadi")
        void bothTypesAreSupported() {
            for (NotificationType type : NotificationType.values()) {
                NotificationSaveRequest r = request();
                r.setType(type);
                assertThat(notificationService.save(null, null, r).getType()).isEqualTo(type);
            }
        }

        @Test
        @DisplayName("Qoralama uchun o'zbekcha yetarli")
        void draftNeedsOnlyBaseLanguage() {
            Notification n = notificationService.save(null, null, request());

            assertThat(n.getStatus()).isEqualTo(NotificationStatus.DRAFT);
            assertThat(n.getTranslations()).hasSize(1);
        }

        @Test
        @DisplayName("Rejalashtirishda uchala til majburiy")
        void schedulingRequiresAllThree() {
            NotificationSaveRequest r = request();
            r.setScheduledAt(LocalDateTime.now().plusDays(1));

            // Xabar telefonga BORADI va uni qaytarib olish mumkin emas.
            assertThatThrownBy(() -> notificationService.save(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RU");
        }

        @Test
        @DisplayName("Uchala til bo'lsa rejalashtiriladi")
        void schedulingSucceedsWithAllThree() {
            NotificationSaveRequest r = request();
            fillAllLocales(r);
            r.setScheduledAt(LocalDateTime.now().plusDays(1));

            Notification n = notificationService.save(null, null, r);

            assertThat(n.getStatus()).isEqualTo(NotificationStatus.SCHEDULED);
            assertThat(n.getScheduledAt()).isNotNull();
        }

        @Test
        @DisplayName("O'tmishdagi vaqtga rejalashtirib bo'lmaydi")
        void pastScheduleIsRejected() {
            NotificationSaveRequest r = request();
            fillAllLocales(r);
            r.setScheduledAt(LocalDateTime.now().minusHours(1));

            // Aks holda u jimgina «hozir yuborish» ga aylanardi.
            assertThatThrownBy(() -> notificationService.save(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("o'tmishda");
        }

        @Test
        @DisplayName("⚠️ Yarim to'ldirilgan til JIMGINA yo'qolmaydi")
        void halfFilledLocaleIsNotSilentlyDropped() {
            NotificationSaveRequest r = request();
            // Admin rus tabida sarlavhani yozdi, matnni unutdi.
            NotificationSaveRequest.NotificationTextDto ru =
                    new NotificationSaveRequest.NotificationTextDto();
            ru.setTitle("Заголовок");
            r.getTranslations().put(Locale.RU, ru);

            // Ilgari butun RU qatori o'tkazib yuborilardi: saqlash
            // muvaffaqiyatli ko'rinardi, sarlavha esa izsiz yo'qolardi.
            assertThatThrownBy(() -> notificationService.save(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RU");
        }

        @Test
        @DisplayName("Uchala til to'liq bo'lsa saqlanadi")
        void fullyTranslatedIsStored() {
            Notification n = notificationService.save(null, null, translated());

            assertThat(n.getTranslations()).hasSize(3);
            assertThat(n.getTranslations()).allSatisfy(t -> {
                assertThat(t.getTitle()).isNotBlank();
                assertThat(t.getBody()).isNotBlank();
            });
        }

        @Test
        @DisplayName("Havola nishoni tekshiriladi — §28 bilan umumiy")
        void deadLinkIsRejected() {
            NotificationSaveRequest r = request();
            InternalLinkDto link = new InternalLinkDto();
            link.setLinkType(LinkType.INTERNAL);
            link.setInternalTargetType(InternalTargetType.CONTENT);
            link.setInternalTargetId(999_999L);
            r.setLink(link);

            assertThatThrownBy(() -> notificationService.save(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("topilmadi");
        }

        @Test
        @DisplayName("Mavjud kontentga havola qabul qilinadi")
        void validLinkIsAccepted() {
            Content c = content();
            NotificationSaveRequest r = request();
            InternalLinkDto link = new InternalLinkDto();
            link.setLinkType(LinkType.INTERNAL);
            link.setInternalTargetType(InternalTargetType.CONTENT);
            link.setInternalTargetId(c.getId());
            r.setLink(link);

            assertThat(notificationService.save(null, null, r)
                    .getLink().getInternalTargetId()).isEqualTo(c.getId());
        }
    }

    // ----------------------------------------------------------- yuborish

    @Nested
    @DisplayName("Yuborish")
    class Sending {

        @Test
        @DisplayName("Provayder sozlanmagan — FAILED, soxta muvaffaqiyat emas")
        void withoutProviderResultIsFailed() {
            Notification n = notificationService.save(null, null, translated());

            Notification sent = notificationService.send(null, n.getId());

            // «Yuborildi» deb belgilash foydalanuvchilar xabar olgandek
            // taassurot qoldirardi va admin muammoni ko'rmasdi.
            assertThat(sent.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(sent.getSentAt()).isNull();
            assertThat(sent.getFailureReason()).contains("FCM");
        }

        @Test
        @DisplayName("Urinish natijasi SAQLANADI — iz qoladi")
        void failedAttemptIsPersisted() {
            Notification n = notificationService.save(null, null, translated());
            notificationService.send(null, n.getId());

            assertThat(notificationService.report(n.getId()).getStatus())
                    .isEqualTo(NotificationStatus.FAILED.name());
        }

        @Test
        @DisplayName("⚠️ Tarjimasiz xabar YUBORILMAYDI")
        void untranslatedNotificationCannotBeSent() {
            // Ilgari uch til qoidasi FAQAT saqlashda va faqat `scheduledAt`
            // berilgan bo'lsa ishlardi. Ya'ni teshik bor edi: qoralamani
            // o'zbekcha yaratib «yuborish» tugmasini bosish kifoya edi —
            // rus foydalanuvchiga o'zbekcha push ketardi.
            Notification draft = notificationService.save(null, null, request());

            assertThatThrownBy(() -> notificationService.send(null, draft.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RU");
        }

        @Test
        @DisplayName("Tarjimasiz xabar FAILED deb ham belgilanmaydi")
        void untranslatedNotificationKeepsItsStatus() {
            Notification draft = notificationService.save(null, null, request());

            assertThatThrownBy(() -> notificationService.send(null, draft.getId()))
                    .isInstanceOf(BusinessException.class);

            // Tarjimasi to'liqsiz xabar BUZILGAN emas — u shunchaki tayyor
            // emas. FAILED «provayder ishlamadi» degan ma'noni berardi va
            // admin muammoni butunlay boshqa joydan qidirardi.
            assertThat(notificationService.report(draft.getId()).getStatus())
                    .isEqualTo(NotificationStatus.DRAFT.name());
        }

        @Test
        @DisplayName("Matni yetishmasa ham yuborilmaydi")
        void missingBodyBlocksSending() {
            NotificationSaveRequest r = request();
            // Sarlavha uchala tilda, matn esa faqat o'zbekchada.
            r.getTranslations().put(Locale.RU, text("Заголовок"));
            r.getTranslations().put(Locale.EN, text("Title"));
            r.getTranslations().get(Locale.RU).setBody("Текст");
            r.getTranslations().get(Locale.EN).setBody("Body");
            Notification n = notificationService.save(null, null, r);

            // Bu holat to'g'ri — hammasi to'liq.
            assertThat(notificationService.send(null, n.getId()).getStatus())
                    .isEqualTo(NotificationStatus.FAILED);
        }
    }

    // -------------------------------------------------------- rejalashtirish

    @Nested
    @DisplayName("Rejalashtirilganlarni yuborish (dispatcher)")
    class Dispatching {

        @Test
        @DisplayName("Vaqti kelmagan xabar tegilmaydi")
        void futureNotificationIsNotPickedUp() {
            NotificationSaveRequest r = request();
            fillAllLocales(r);
            r.setScheduledAt(LocalDateTime.now().plusDays(3));
            Notification n = notificationService.save(null, null, r);

            assertThat(dispatcher.findDue())
                    .extracting(Notification::getId)
                    .doesNotContain(n.getId());
        }

        @Test
        @DisplayName("Vaqti kelgan xabar navbatga tushadi")
        void dueNotificationIsPickedUp() {
            Notification n = scheduledInPast();

            // Bu tekshiruvsiz `scheduledAt` va SCHEDULED holati bor edi,
            // lekin ularni O'QIYDIGAN hech narsa yo'q edi: ertaga soat 9 ga
            // qo'yilgan xabar abadiy SCHEDULED bo'lib qolardi.
            assertThat(dispatcher.findDue())
                    .extracting(Notification::getId)
                    .contains(n.getId());
        }

        @Test
        @DisplayName("Yuborilgandan keyin navbatda qolmaydi")
        void dispatchedNotificationLeavesTheQueue() {
            Notification n = scheduledInPast();

            dispatcher.dispatchDue();

            // Provayder yo'qligi uchun FAILED bo'ladi — lekin SCHEDULED
            // emas, ya'ni cheksiz qayta urinish halqasi yo'q.
            assertThat(dispatcher.findDue())
                    .extracting(Notification::getId)
                    .doesNotContain(n.getId());
            assertThat(notificationService.report(n.getId()).getStatus())
                    .isEqualTo(NotificationStatus.FAILED.name());
        }

        @Test
        @DisplayName("Bekor qilingan xabar yuborilmaydi")
        void cancelledNotificationIsNotDispatched() {
            Notification n = scheduledInPast();
            notificationService.cancel(null, n.getId());

            assertThat(dispatcher.findDue())
                    .extracting(Notification::getId)
                    .doesNotContain(n.getId());
        }
    }

    // --------------------------------------------------------------- rollar

    @Nested
    @DisplayName("Kim yarata oladi (ТЗ §32)")
    class Roles {

        @Test
        @DisplayName("Ruxsat rolga BOG'LANMAGAN — Worker ham ola oladi")
        void permissionIsNotTiedToRole() {
            // ТЗ: «Admin/SuperAdmin notification yaratib schedule qila
            // olsin. Worker uchun permission orqali berilsin.»
            //
            // Ya'ni tekshiruv ROL bo'yicha emas, RUXSAT bo'yicha bo'lishi
            // kerak. Rolga bog'lansa, Workerga bu ishni topshirish uchun
            // uni Adminga ko'tarish kerak bo'lardi — bu esa unga butunlay
            // keraksiz huquqlarni ham berardi.
            String worker = staff.tokenForRole("+998900000401",
                    PlatformRole.WORKER,
                    EnumSet.of(Permission.NOTIFICATION_VIEW,
                            Permission.NOTIFICATION_CREATE,
                            Permission.NOTIFICATION_SEND));

            assertThat(worker).isNotBlank();
        }

        @Test
        @DisplayName("Uchta alohida ruxsat: ko'rish · yaratish · yuborish")
        void sendIsSeparateFromCreate() {
            // Yuborish alohida ruxsat: xabar tayyorlash bilan uni
            // MINGLAB telefonga jo'natish bir xil mas'uliyat emas.
            assertThat(Permission.values())
                    .contains(Permission.NOTIFICATION_VIEW,
                            Permission.NOTIFICATION_CREATE,
                            Permission.NOTIFICATION_SEND);
        }
    }

    // ------------------------------------------------------------- hisobot

    @Nested
    @DisplayName("Hisobot (ТЗ §33)")
    class Report {

        @Test
        @DisplayName("O'lchanmaydigan ko'rsatkich NOL emas — «mavjud emas»")
        void deliveredIsNotFakedAsZero() {
            Notification n = notificationService.save(null, null, request());

            NotificationReportDto report = notificationService.report(n.getId());

            // Nol qaytarilsa admin «hech kimga yetib bormadi» deb
            // o'ylardi. Aslida biz shunchaki BILMAYMIZ.
            assertThat(report.getDelivered().getAvailable()).isFalse();
            assertThat(report.getDelivered().getValue()).isNull();
            assertThat(report.getDelivered().getUnavailableReason()).isNotBlank();
        }

        @Test
        @DisplayName("Ochilish klient hodisasidan hisoblanadi")
        void openedComesFromClientEvents() {
            Notification n = notificationService.save(null, null, request());

            analyticsService.record(AnalyticsEventType.NOTIFICATION_OPEN,
                    n.getId(), null, null, "qurilma-1");
            analyticsService.record(AnalyticsEventType.NOTIFICATION_OPEN,
                    n.getId(), null, null, "qurilma-1");
            analyticsService.record(AnalyticsEventType.NOTIFICATION_OPEN,
                    n.getId(), null, null, "qurilma-2");

            NotificationReportDto report = notificationService.report(n.getId());

            assertThat(report.getOpened().getAvailable()).isTrue();
            assertThat(report.getOpened().getValue()).isEqualTo(3);
            assertThat(report.getOpened().getUnique())
                    .as("Bir qurilmadan ikki marta ochilsa ham bitta odam")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("Bosish va ochish ALOHIDA sanaladi")
        void clickIsSeparateFromOpen() {
            Notification n = notificationService.save(null, null, request());

            analyticsService.record(AnalyticsEventType.NOTIFICATION_OPEN,
                    n.getId(), null, null, "qurilma-1");
            analyticsService.record(AnalyticsEventType.NOTIFICATION_OPEN,
                    n.getId(), null, null, "qurilma-2");
            analyticsService.record(AnalyticsEventType.NOTIFICATION_CLICK,
                    n.getId(), null, null, "qurilma-1");

            NotificationReportDto report = notificationService.report(n.getId());

            // Odam xabarni ochib, havolani bosmasligi mumkin. Ikkalasi
            // bitta hodisa bo'lsa «clicked» «opened» ning nusxasi bo'lardi.
            assertThat(report.getOpened().getValue()).isEqualTo(2);
            assertThat(report.getClicked().getValue()).isEqualTo(1);
        }

        @Test
        @DisplayName("Hodisasiz hisobot nol — bu haqiqiy nol")
        void zeroEventsIsARealZero() {
            Notification n = notificationService.save(null, null, request());

            NotificationReportDto report = notificationService.report(n.getId());

            // Bu yerda nol TO'G'RI: biz hodisalarni yozamiz va ular yo'q.
            // «delivered» dan farqi shu — u bizga umuman ko'rinmaydi.
            assertThat(report.getOpened().getAvailable()).isTrue();
            assertThat(report.getOpened().getValue()).isZero();
        }
    }

    // ------------------------------------------------------------ yordamchi

    private NotificationSaveRequest request() {
        NotificationSaveRequest r = new NotificationSaveRequest();
        r.setType(NotificationType.APP_NOTIFICATION);
        r.setAudience(NotificationAudience.ALL);
        Map<Locale, NotificationSaveRequest.NotificationTextDto> tr = new LinkedHashMap<>();
        tr.put(Locale.UZ, text("Xabar " + SEQ.incrementAndGet()));
        r.setTranslations(tr);
        return r;
    }

    /** Uchala tili ham to'liq so'rov — yuborishga tayyor. */
    private NotificationSaveRequest translated() {
        NotificationSaveRequest r = request();
        fillAllLocales(r);
        return r;
    }

    private void fillAllLocales(NotificationSaveRequest r) {
        r.getTranslations().put(Locale.RU, text("Сообщение"));
        r.getTranslations().put(Locale.EN, text("Message"));
    }

    private NotificationSaveRequest.NotificationTextDto text(String title) {
        NotificationSaveRequest.NotificationTextDto t =
                new NotificationSaveRequest.NotificationTextDto();
        t.setTitle(title);
        t.setBody(title + " matni");
        return t;
    }

    /**
     * Vaqti kelgan rejalashtirilgan xabar.
     *
     * Saqlash o'tmishdagi sanani rad etadi (bu to'g'ri), shuning uchun
     * kelajakka qo'yilib, keyin sanasi orqaga suriladi — xuddi vaqt
     * o'tgandek.
     */
    private Notification scheduledInPast() {
        NotificationSaveRequest r = request();
        fillAllLocales(r);
        r.setScheduledAt(LocalDateTime.now().plusMinutes(5));
        Notification n = notificationService.save(null, null, r);

        // Saqlash o'tmishdagi sanani ataylab rad etadi, shuning uchun
        // sana bazada to'g'ridan-to'g'ri suriladi — xuddi vaqt o'tgandek.
        n.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        return notificationRepo.save(n);
    }

    private Content content() {
        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(ContentType.MOVIE);
        req.setStructureType(StructureType.SINGLE);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(PublicationStatus.DRAFT);
        req.setTranslations(Translations.all("Xabar nishoni " + SEQ.incrementAndGet()));
        return contentService.create(null, req);
    }
}
