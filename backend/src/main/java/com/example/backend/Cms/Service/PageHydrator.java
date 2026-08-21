package com.example.backend.Cms.Service;

import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Sahifadagi elementlarning to'plamlarini bitta qo'shimcha so'rov bilan to'ldiradi.
 *
 * <h2>Muammo</h2>
 * To-many to'plamni {@code @EntityGraph} bilan olib, ustiga sahifalash
 * so'ralsa, Hibernate SQL'da {@code limit} ni ISHLATA OLMAYDI: fetch join
 * natijasida bitta entity bir necha satrga yoyiladi va {@code limit} noto'g'ri
 * kesardi. Shuning uchun u BARCHA satrlarni tortib, sahifani xotirada kesadi
 * va {@code HHH90003004} deb ogohlantiradi.
 *
 * Grafsiz qoldirilsa esa teskari muammo: har bir satr uchun alohida so'rov
 * (N+1).
 *
 * <h2>Yechim</h2>
 * Ikki so'rov, ikkalasi ham qat'iy chegaralangan:
 * <ol>
 *   <li>toza sahifa — haqiqiy {@code limit/offset};</li>
 *   <li>o'sha id'lar uchun bitta {@code in (...)} so'rovi grafi bilan.</li>
 * </ol>
 *
 * Ikkinchi so'rov natijasi ishlatilmaydi — u persistence context'ni
 * "isitadi". Sahifadagi obyektlar aynan o'sha nusxalar, ya'ni ular
 * initsializatsiya qilingan holda qaytadi.
 *
 * ⚠️ Ikkala so'rov BITTA tranzaksiyada bo'lishi shart, aks holda ikkinchi
 * so'rov boshqa kontekstga tushadi va isitish ishlamaydi.
 */
public final class PageHydrator {

    private PageHydrator() {
    }

    /**
     * @param page   to'ldiriladigan sahifa
     * @param idOf   elementdan id oluvchi
     * @param loader id'lar bo'yicha graf bilan yuklovchi (natijasi kerak emas)
     */
    public static <T> Page<T> warm(Page<T> page,
                                   Function<T, Long> idOf,
                                   Consumer<Collection<Long>> loader) {
        if (page == null || page.isEmpty()) {
            return page;
        }
        List<Long> ids = page.getContent().stream().map(idOf).toList();
        loader.accept(ids);
        return page;
    }
}
