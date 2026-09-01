package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Service.Storage.StorageStatsService;
import com.example.backend.Enums.Permission;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ombor holati — panel uchun.
 *
 * <h2>⚠️ Ikkita alohida amal, ATAYLAB</h2>
 * <pre>
 *   GET  /storage   — keshdagi natija, TEZ
 *   POST /storage/scan — qayta sanash, QIMMAT
 * </pre>
 *
 * Bitta endpoint qilib, «kesh eskirgan bo'lsa o'zi yangilasin» degan
 * yechim jozibali ko'rinadi, lekin u sahifani ochgan adminni
 * kutishga majbur qilardi — va u nima uchun kutayotganini bilmasdi.
 *
 * Endi sahifa darrov ochiladi, raqam yonida esa uning VAQTI turadi.
 * Yangilash — adminning ongli qarori.
 *
 * ⚠️ Faqat S3 rejimida mavjud. Lokal saqlashda ombor — oddiy papka
 * va uni operatsion tizim vositalari bilan ko'rish qulayroq.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/app/admin/storage")
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class StorageStatsController {

    private final StorageStatsService statsService;
    private final PermissionService permissionService;

    /**
     * Oxirgi skanerlash natijasi.
     *
     * ⚠️ Hali skanerlanmagan bo'lsa {@code 204} qaytadi — bo'sh
     * hisobot emas. Nol raqamli hisobot «ombor bo'sh» degan YOLG'ON
     * taassurot berardi.
     */
    @GetMapping
    public ResponseEntity<StorageStatsService.Report> current() {
        require();
        StorageStatsService.Report report = statsService.cached();
        return report == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(report);
    }

    /**
     * Qayta sanash.
     *
     * ⚠️ {@code POST}, chunki bu O'QISH emas: u S3 ga o'nlab so'rov
     * yuboradi va pul turadi. `GET` bo'lsa uni brauzer yoki proksi
     * o'zi takrorlab yuborishi mumkin edi.
     */
    @PostMapping("/scan")
    public ResponseEntity<StorageStatsService.Report> scan() {
        require();
        return ResponseEntity.ok(statsService.refresh());
    }

    /**
     * ⚠️ Ombor holati — TIZIM ma'lumoti, kontent emas.
     *
     * Fayl nomlari va hajmlari orqali platformada nima borligini
     * taxmin qilish mumkin, shuning uchun oddiy `CONTENT_VIEW`
     * yetarli emas.
     */
    private void require() {
        if (!permissionService.hasPermission(CurrentUser.get(), Permission.MEDIA_DELETE)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + Permission.MEDIA_DELETE);
        }
    }
}
