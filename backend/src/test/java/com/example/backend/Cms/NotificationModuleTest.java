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
class NotificationModuleTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private NotificationAdminService notificationService;
    @Autowired private NotificationDispatcher dispatcher;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private ContentService contentService;
    @Autowired private com.example.backend.Cms.Repository.NotificationRepo notificationRepo;

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
            Notification n = notificationService.save(null, null, request());

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
            Notification n = notificationService.save(null, null, request());
            notificationService.send(null, n.getId());

            assertThat(notificationService.report(n.getId()).getStatus())
                    .isEqualTo(NotificationStatus.FAILED.name());
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
