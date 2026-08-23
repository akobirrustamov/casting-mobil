package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Enums.NotificationStatus;
import com.example.backend.Cms.Repository.NotificationRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Rejalashtirilgan bildirishnomalarni vaqti kelganda yuboradi (ТЗ §32).
 *
 * <h2>Nima uchun kerak</h2>
 * ТЗ: «Admin/SuperAdmin notification yaratib schedule qila olsin.»
 * {@code scheduledAt} maydoni ham, {@code SCHEDULED} holati ham bor edi —
 * lekin ularni O'QIYDIGAN hech narsa yo'q edi. Ya'ni ertaga soat 9 ga
 * rejalashtirilgan xabar abadiy {@code SCHEDULED} bo'lib qolardi va admin
 * buni faqat foydalanuvchilar shikoyat qilganda bilardi.
 *
 * <h2>Provayder sozlanmagan bo'lsa</h2>
 * Yuborish {@code FAILED} bo'ladi va sabab yoziladi. «Yuborildi» deb
 * belgilanmaydi — bu foydalanuvchilar xabar olgandek soxta taassurot
 * qoldirardi va admin muammoni umuman ko'rmasdi.
 *
 * <h2>Nima uchun chegaralangan to'plam</h2>
 * Bir vaqtning o'zida yuzlab xabar vaqti kelsa, hammasini bitta
 * tranzaksiyada ishlash bazani uzoq band qilardi. Har yugurishda
 * {@link #BATCH} tasi olinadi, qolganlari keyingisiga qoladi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    /** Bir yugurishda nechta xabar ishlanadi. */
    static final int BATCH = 50;

    private final NotificationRepo notificationRepo;
    private final NotificationAdminService notificationAdminService;

    @Scheduled(fixedDelayString = "${app.notifications.dispatch-delay-ms:60000}")
    public void dispatchDue() {
        List<Notification> due = findDue();
        if (due.isEmpty()) {
            return;
        }
        log.info("Vaqti kelgan bildirishnomalar: {}", due.size());
        for (Notification n : due) {
            try {
                // actor = null: bu tizim harakati, odam emas. Audit jurnali
                // ham shuni ko'rsatadi.
                notificationAdminService.send(null, n.getId());
            } catch (RuntimeException e) {
                // Bitta xabarning xatosi qolganlarini to'xtatmasin.
                log.warn("Bildirishnoma #{} yuborilmadi: {}", n.getId(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> findDue() {
        return notificationRepo
                .findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        NotificationStatus.SCHEDULED, LocalDateTime.now(),
                        PageRequest.of(0, BATCH));
    }
}
