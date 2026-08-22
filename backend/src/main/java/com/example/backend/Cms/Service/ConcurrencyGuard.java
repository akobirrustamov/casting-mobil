package com.example.backend.Cms.Service;

import com.example.backend.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Optimistik qulf chegarasi (§60).
 *
 * <h2>Nega {@code @Version} o'zi yetarli emas</h2>
 * {@code @Version} bitta tranzaksiya ichida ustma-ust tushgan yozuvlarni
 * ushlaydi. Panel tahriri esa boshqacha: admin A formani 09:55 da
 * ochadi, admin B 10:00 da saqlaydi, A 10:01 da saqlaydi. A ning
 * so'rovi bazadan qatorni YANGI holida o'qiydi (B ning versiyasi bilan),
 * ustiga eskirgan forma ma'lumotini yozadi va muvaffaqiyatli saqlaydi.
 * Hibernate uchun hech qanday to'qnashuv yo'q — B ning ishi shunchaki
 * yo'qoladi.
 *
 * Shuning uchun klient formani OCHGANDAGI versiyani qaytarib yuborishi
 * va u joriy versiya bilan solishtirilishi kerak.
 *
 * <h2>Nega versiya majburiy</h2>
 * Tekshiruvni «versiya kelgan bo'lsa» shartiga bog'lash himoyani
 * ixtiyoriy qiladi: versiyani yubormagan klient hech qanday ogohlantirish
 * olmay boshqaning ishini bosib ketaveradi. ТЗ esa aynan «indamay
 * overwrite qilmasin» deydi — jim qolish taqiqlanadi.
 */
public final class ConcurrencyGuard {

    private ConcurrencyGuard() {
    }

    /**
     * Klient yuborgan versiyani joriy versiya bilan solishtiradi.
     *
     * @param clientVersion  klient formani ochganda olgan versiya
     * @param currentVersion bazadagi joriy versiya
     * @param what           xabarda ko'rinadigan nom («Kontent», «Qism»)
     */
    public static void check(Long clientVersion, Long currentVersion, String what) {
        if (clientVersion == null) {
            throw BusinessException.validation(
                    what + " versiyasi yuborilmadi. Sahifani yangilab, qaytadan urinib ko'ring.");
        }
        if (!clientVersion.equals(currentVersion)) {
            throw new BusinessException("CONCURRENT_MODIFICATION",
                    what + "ni boshqa foydalanuvchi o'zgartirdi. Sahifani yangilang — "
                            + "aks holda uning o'zgarishlari yo'qoladi.",
                    HttpStatus.CONFLICT);
        }
    }
}
