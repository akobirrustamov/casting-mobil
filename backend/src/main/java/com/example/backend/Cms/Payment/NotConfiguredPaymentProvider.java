package com.example.backend.Cms.Payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Standart provayder — HECH NARSA QILMAYDI (ТЗ §44).
 *
 * <h2>Nima uchun bunday sinf bor</h2>
 * Chaqiruvchi kod {@code null} tekshirmasin: interfeys doim mavjud, lekin
 * u halol javob beradi — «sozlanmagan».
 *
 * <h2>Nima uchun soxta muvaffaqiyat qaytarmaydi</h2>
 * Buyurtmachi talabi: «Agar provider configure qilinmagan bo'lsa fake
 * successful response qaytarma.» Soxta «to'landi» javobi eng xavfli
 * variant: foydalanuvchi premium olardi, pul esa hech qayerdan
 * kelmasdi — va buni faqat oy oxirida hisob-kitobda payqashardi.
 */
@Slf4j
@Component
public class NotConfiguredPaymentProvider implements PaymentProvider {

    /**
     * Qaysi provayder kutilyapti — faqat XABAR uchun.
     *
     * Nom kodga yozilmaydi: business logic «agar Payme bo'lsa» degan
     * shartlarga to'lib ketmasin.
     */
    @Value("${app.payment.provider:none}")
    private String configuredName;

    @Override
    public String name() {
        return configuredName;
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public PaymentInitResult init(String orderId, BigDecimal amount, String currency) {
        log.warn("To'lov so'raldi, lekin provayder sozlanmagan: order={} summa={} {}",
                orderId, amount, currency);
        throw new PaymentNotConfiguredException(configuredName);
    }
}
