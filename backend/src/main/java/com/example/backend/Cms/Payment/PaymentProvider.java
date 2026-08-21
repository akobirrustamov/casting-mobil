package com.example.backend.Cms.Payment;

import java.math.BigDecimal;

/**
 * To'lov provayderi — ABSTRAKSIYA (ТЗ §44).
 *
 * <h2>Nima uchun interfeys, tayyor integratsiya emas</h2>
 * ТЗ: «Hozir mavjud payment integration bo'lsa audit qilib reuse qil.
 * Mavjud bo'lmasa <b>fake payment gateway yaratib production-ready deb
 * ko'rsatma</b>. Payment provider uchun abstraction/interface tayyorlash
 * mumkin.»
 *
 * Loyihada hech qanday to'lov integratsiyasi yo'q — Payme, Click va Uzum
 * uchun merchant ma'lumotlari ham berilmagan. Shuning uchun bu yerda
 * faqat CHEGARA belgilanadi: qaysi provayder ulansa ham, qolgan kod
 * o'zgarmaydi.
 *
 * <h2>Nima uchun provayder nomi kodga yozilmaydi</h2>
 * Qaysi provayder ishlatilishi konfiguratsiyadan aniqlanadi
 * ({@code app.payment.provider}). Business logic ichida «agar Payme
 * bo'lsa» degan shartlar paydo bo'lsa, provayderni almashtirish butun
 * kodni titishni talab qilardi.
 */
public interface PaymentProvider {

    /** Konfiguratsiyadagi nom bilan taqqoslash uchun. */
    String name();

    /**
     * Provayder ishlashga tayyormi.
     *
     * Kalitlar berilmagan bo'lsa {@code false}. Chaqiruvchi shu holatda
     * to'lov oqimini umuman boshlamasligi kerak.
     */
    boolean isConfigured();

    /**
     * To'lov yaratadi va foydalanuvchini yo'naltirish uchun ma'lumot qaytaradi.
     *
     * @param orderId  bizning tomondagi buyurtma identifikatori
     * @param amount   summa — {@code BigDecimal}, floating point pul uchun yaramaydi
     * @param currency ISO kodi, masalan {@code UZS}
     * @throws PaymentNotConfiguredException provayder sozlanmagan bo'lsa
     */
    PaymentInitResult init(String orderId, BigDecimal amount, String currency);

    /** To'lov natijasi — klientni qayerga yuborish kerakligi. */
    record PaymentInitResult(String providerPaymentId, String redirectUrl) {
    }
}
