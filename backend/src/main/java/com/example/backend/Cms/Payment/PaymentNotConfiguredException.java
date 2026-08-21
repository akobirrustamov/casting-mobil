package com.example.backend.Cms.Payment;

import com.example.backend.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * To'lov provayderi sozlanmagan (ТЗ §44).
 *
 * <h2>Nima uchun istisno, {@code false} emas</h2>
 * Sozlanmagan provayder «muvaffaqiyatsiz to'lov» EMAS — u umuman
 * mavjud bo'lmagan imkoniyat. Ikkalasi bir xil qaytarilsa, chaqiruvchi
 * kod «foydalanuvchi kartasida pul yo'q» bilan «biz to'lovni umuman
 * qabul qila olmaymiz» ni farqlay olmasdi.
 *
 * <h2>503, 500 emas</h2>
 * Bu dastur xatosi emas — sozlama yetishmayapti. 503 aynan shuni
 * bildiradi: xizmat hozircha mavjud emas.
 */
public class PaymentNotConfiguredException extends BusinessException {

    public PaymentNotConfiguredException(String provider) {
        super("PAYMENT_NOT_CONFIGURED",
                "To'lov provayderi sozlanmagan: " + provider
                        + ". Merchant kalitlari environment orqali berilishi kerak.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
