package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.Tariff;
import com.example.backend.Cms.Entity.TariffTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.TariffRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Tariflar — ILOVA uchun ko'rinish.
 *
 * <h2>Nima uchun admin endpointi yaramaydi</h2>
 * {@code /api/v1/app/admin/tariffs} allaqachon bor, lekin u boshqa
 * savolga javob beradi:
 *
 * <ul>
 *   <li>u nofaol tariflarni ham qaytaradi — ilovada ular sotib
 *       olinmaydigan qator bo'lib turardi;</li>
 *   <li>u UCHALA tilni birdan beradi ({@code Map<Locale, ...>}) — mobil
 *       ilova keraksiz ikki tilni yuklab, tanlovni o'zi qilardi;</li>
 *   <li>u xodim ruxsatini talab qiladi ({@code TARIFF_VIEW}).</li>
 * </ul>
 *
 * <h2>Narxlar</h2>
 * Bu yerda hech qanday raqam yo'q. Ular {@code cms_tariff} da va admin
 * paneldan o'zgaradi (ТЗ §36) — boshlang'ich qiymatlar faqat
 * migratsiyadagi seed.
 */
@Service
@RequiredArgsConstructor
public class TariffCatalogService {

    private final TariffRepo tariffRepo;

    /**
     * Sotib olish mumkin bo'lgan tariflar, tanlangan tilda.
     *
     * ⚠️ Faqat {@code active} bo'lganlari. Admin tarifni o'chirmaydi —
     * uni nofaol qiladi, chunki unga bog'langan obunalar tarixi qoladi
     * ({@code Subscription.tariff}). Nofaolini ko'rsatish esa narxi
     * bekor qilingan tarifni sotishga urinish bo'lardi.
     */
    @Transactional(readOnly = true)
    public List<TariffView> active(Locale lang) {
        return tariffRepo.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActive()))
                .map(t -> view(t, lang))
                .toList();
    }

    /** Bitta tarif — sotib olish oqimi uchun. */
    @Transactional(readOnly = true)
    public TariffView view(Tariff tariff, Locale lang) {
        TariffTranslation text = TranslationPicker.pick(
                tariff.getTranslations(), lang, TariffTranslation::getLocale);

        return new TariffView(
                tariff.getId(),
                tariff.getCode(),
                tariff.getDurationMonths(),
                tariff.getPrice(),
                tariff.monthlyPrice(),
                tariff.getCurrency(),
                Boolean.TRUE.equals(tariff.getHighlighted()),
                text == null ? null : text.getName(),
                text == null ? null : text.getBadge(),
                text == null ? null : text.getDescription(),
                features(text));
    }

    /**
     * Imkoniyatlar ro'yxati.
     *
     * <h2>⚠️ Nima uchun ajratish SERVERDA</h2>
     * Ustunda bu bitta matn: qatorlar yangi qator belgisi bilan
     * ajratilgan ({@code TariffTranslation.features} izohida shunday
     * yozilgan). Ajratishni klientga qoldirish uchta klientda uchta
     * ajratish qoidasi degani edi — va ular albatta farq qilardi:
     * biri bo'sh qatorni tashlab yuborardi, ikkinchisi undan bo'sh
     * nuqta yasardi.
     *
     * Bo'sh qatorlar tashlanadi: admin matn oxirida tasodifan enter
     * bosishi — juda oddiy hol, va undan ro'yxatda osilgan bo'sh
     * element paydo bo'lardi.
     */
    private static List<String> features(TariffTranslation text) {
        if (text == null || text.getFeatures() == null || text.getFeatures().isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.getFeatures().split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    /**
     * Ilova ko'radigan tarif.
     *
     * @param name        tanlangan tildagi nom: «1 oy», «Yillik»
     * @param badge       «ENG FOYDALI TARIF» kabi yozuv yoki {@code null}
     * @param description nasriy izoh
     * @param features    nima kirishi — tayyor ro'yxat
     */
    public record TariffView(
            Long id,
            String code,
            Integer durationMonths,
            BigDecimal price,
            /** Oyiga qancha tushishi — «oyiga atigi 13 325 so'm» yozuvi uchun. */
            BigDecimal monthlyPrice,
            String currency,
            boolean highlighted,
            String name,
            String badge,
            String description,
            List<String> features) {
    }
}
