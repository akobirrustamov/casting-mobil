package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.PlatformSetting;
import com.example.backend.Cms.Repository.PlatformSettingRepo;
import com.example.backend.Entity.User;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Platforma sozlamalari.
 *
 * Narxlar va kurslar KODDA QOTIRILMAYDI — admin panel orqali o'zgartiriladi
 * va o'zgarish darhol kuchga kiradi (§23, §36, §40, §41).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final PlatformSettingRepo repo;
    private final AuditService auditService;

    /** Barcha sozlamalar. Yetishmayotganlari default bilan to'ldiriladi. */
    @Transactional
    public List<PlatformSetting> all() {
        for (String[] d : SettingKeys.defaults()) {
            if (!repo.existsById(d[0])) {
                repo.save(PlatformSetting.builder()
                        .key(d[0]).value(d[1]).description(d[2]).build());
            }
        }
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public String get(String key, String fallback) {
        return repo.findById(key).map(PlatformSetting::getValue).orElse(fallback);
    }

    /**
     * Qiymat: bazadan, bo'lmasa KODDA E'LON QILINGAN standartdan.
     *
     * ⚠️ Ilgari chaqiruvchilar {@code get(key, "0")} yozardi va bazada satr
     * bo'lmasa 0 qaytardi. Sozlamalar esa faqat admin sozlamalar sahifasini
     * ochganda yozilardi — natijada yangi o'rnatishda qism narxi
     * <b>0 so'm</b> bo'lardi.
     *
     * Endi zaxira {@link SettingKeys#defaultValue} dan keladi: sahifa
     * ochilmagan bo'lsa ham to'g'ri qiymat ishlaydi.
     */
    @Transactional(readOnly = true)
    public String get(String key) {
        return repo.findById(key)
                .map(PlatformSetting::getValue)
                .orElseGet(() -> SettingKeys.defaultValue(key));
    }

    @Transactional(readOnly = true)
    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            // Buzilgan qiymat butun oqimni to'xtatmasin - default bilan davom etamiz
            log.warn("Sozlama '{}' son emas, default ishlatiladi: {}", key, fallback);
            return fallback;
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getMoney(String key) {
        try {
            String value = get(key);
            if (value == null || value.isBlank()) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Sozlama '{}' pul qiymati emas", key);
            return BigDecimal.ZERO;
        }
    }

    @Transactional
    /**
     * Qiymat kalitning turiga mos keladimi.
     *
     * @throws BusinessException mos kelmasa — xabar aniq nima kutilayotganini
     *                           aytadi, «noto'g'ri qiymat» degan umumiy
     *                           xato admin nimani tuzatishni bilmasligiga
     *                           olib kelardi
     */
    void validateValue(String key, String value) {
        switch (SettingKeys.typeOf(key)) {
            case MONEY -> {
                BigDecimal parsed;
                try {
                    parsed = new BigDecimal(value);
                } catch (NumberFormatException e) {
                    throw BusinessException.validation(
                            "«" + key + "» pul qiymati bo'lishi kerak, masalan 3000 "
                                    + "yoki 1250.50. Kiritilgani: " + value);
                }
                if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                    throw BusinessException.validation(
                            "«" + key + "» manfiy bo'lishi mumkin emas");
                }
            }
            case INTEGER -> {
                try {
                    if (Integer.parseInt(value) < 0) {
                        throw BusinessException.validation(
                                "«" + key + "» manfiy bo'lishi mumkin emas");
                    }
                } catch (NumberFormatException e) {
                    throw BusinessException.validation(
                            "«" + key + "» butun son bo'lishi kerak. Kiritilgani: " + value);
                }
            }
            case ENUM -> {
                var allowed = SettingKeys.allowedValues(key);
                if (!allowed.contains(value)) {
                    throw BusinessException.validation(
                            "«" + key + "» quyidagilardan biri bo'lishi kerak: "
                                    + String.join(", ", allowed) + ". Kiritilgani: " + value);
                }
            }
            case TEXT -> {
                // Cheklov yo'q.
            }
        }
    }

    public PlatformSetting update(User actor, String key, String value) {
        if (value == null || value.isBlank()) {
            throw BusinessException.validation("Qiymat bo'sh bo'lishi mumkin emas");
        }
        // ⚠️ Tekshiruv AYNAN SHU YERDA — yozish paytida.
        //
        // Ilgari qiymat hech qanday tekshiruvsiz saqlanardi. Admin narx
        // maydoniga xato yozsa, u saqlanardi va panelda ko'rinardi —
        // lekin o'qishda 0 ga aylanardi. Ya'ni PULLIK KONTENT BEPUL
        // bo'lib qolardi va buni kimdir pulsiz tomosha qilgandagina
        // bilishardi.
        validateValue(key, value.trim());
        // Satr bo'lmasa, lekin kalit KODDA E'LON QILINGAN bo'lsa - yaratamiz.
        //
        // ⚠️ Ilgari bu yerda shartsiz 404 turardi. Natijada kodga yangi
        // sozlama qo'shilganda uni o'zgartirib bo'lmasdi: satr faqat admin
        // «Sozlamalar» sahifasini ochganda paydo bo'lardi. B24 bilan bir
        // xil sinf.
        //
        // Noma'lum kalit baribir rad etiladi - admin xato yozib, hech kim
        // o'qimaydigan satr yaratib qo'ymasin.
        PlatformSetting s = repo.findById(key).orElseGet(() -> {
            String declared = SettingKeys.defaultValue(key);
            if (declared == null) {
                throw BusinessException.notFound("Setting", key);
            }
            return PlatformSetting.builder()
                    .key(key)
                    .value(declared)
                    .description(SettingKeys.descriptionOf(key))
                    .build();
        });
        String before = s.getValue();
        s.setValue(value.trim());
        s.setUpdatedBy(actor == null ? null : actor.getId());
        PlatformSetting saved = repo.save(s);

        auditService.log(actor, AuditAction.SETTING_CHANGED, "PlatformSetting", key,
                Map.of("value", before), Map.of("value", saved.getValue()));
        return saved;
    }
}
