package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Enums.DonationTargetType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Repository.CreatorRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Donat nishonining nomi (ТЗ §42).
 *
 * <h2>Nega kerak</h2>
 * Donat yozuvida faqat {@code targetType} va {@code targetId} bor edi,
 * ya'ni panelda «CREATOR #5» ko'rinardi. Admin donat KIMGA
 * berilganini bila olmasdi.
 *
 * Asimmetriya buni e'tibordan chetda qolgan deb ko'rsatadi:
 * YUBORUVCHINING ismi qaytariladi ({@code senderName}), oluvchiniki
 * esa yo'q.
 *
 * <h2>Nima uchun alohida xizmat</h2>
 * Nom ikki joyda kerak: tranzaksiyalar ro'yxatida va «eng ko'p donat
 * olganlar» jadvalida. Ikkalasida ham qatorlar ko'p, shuning uchun nom
 * BITTALAB emas, TO'PLAM bo'lib yuklanadi — aks holda har bir qator
 * uchun alohida so'rov ketardi (§66).
 */
@Service
@RequiredArgsConstructor
public class DonationTargetNames {

    private final CreatorRepo creatorRepo;
    private final ContentRepo contentRepo;

    /**
     * Nishon nomlarini bir yo'la yuklaydi.
     *
     * @return {@code "CREATOR:5"} → nom xaritasi. Topilmagan nishon
     *         xaritaga KIRMAYDI — chaqiruvchi uni {@code null} deb
     *         ko'radi va «#5» ko'rsatadi. O'chirilgan ijodkorga
     *         berilgan eski donat shu holatda bo'ladi.
     */
    @Transactional(readOnly = true)
    public Map<String, String> resolve(Collection<Ref> refs) {
        Map<String, String> names = new HashMap<>();

        Set<Long> creatorIds = new LinkedHashSet<>();
        Set<Long> contentIds = new LinkedHashSet<>();
        for (Ref r : refs) {
            if (r == null || r.id() == null) {
                continue;
            }
            if (r.type() == DonationTargetType.CREATOR) {
                creatorIds.add(r.id());
            } else if (r.type() == DonationTargetType.CONTENT) {
                contentIds.add(r.id());
            }
        }

        if (!creatorIds.isEmpty()) {
            for (Creator c : creatorRepo.findAllById(creatorIds)) {
                String name = firstNonBlank(c.getTranslations().stream()
                        .sorted(byUzFirst(t -> t.getLocale()))
                        .map(t -> t.getDisplayName()));
                if (name != null) {
                    names.put(key(DonationTargetType.CREATOR, c.getId()), name);
                }
            }
        }

        if (!contentIds.isEmpty()) {
            for (Content c : contentRepo.findAllById(contentIds)) {
                String name = firstNonBlank(c.getTranslations().stream()
                        .sorted(byUzFirst(t -> t.getLocale()))
                        .map(t -> t.getTitle()));
                if (name != null) {
                    names.put(key(DonationTargetType.CONTENT, c.getId()), name);
                }
            }
        }

        return names;
    }

    public static String key(DonationTargetType type, Long id) {
        return type + ":" + id;
    }

    /** Bitta nishonga havola. */
    public record Ref(DonationTargetType type, Long id) {
    }

    /**
     * O'zbekcha nom afzal ko'riladi.
     *
     * Panel tili har xil bo'lishi mumkin, lekin bu ADMIN ro'yxati va
     * unda nishonni tanib olish muhim. UZ — asosiy til, u deyarli
     * har doim to'ldirilgan (nashr qoidasi shuni talab qiladi).
     */
    private static <T> java.util.Comparator<T> byUzFirst(
            java.util.function.Function<T, Locale> locale) {
        return java.util.Comparator.comparingInt(t -> locale.apply(t) == Locale.UZ ? 0 : 1);
    }

    private static String firstNonBlank(java.util.stream.Stream<String> values) {
        return values.filter(v -> v != null && !v.isBlank()).findFirst().orElse(null);
    }
}
