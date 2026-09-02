package com.example.backend.Sms;

/**
 * SMS shlyuzi.
 *
 * <h2>Nega interfeys kerak bo'ldi</h2>
 * {@link OtpService} ilgari to'g'ridan-to'g'ri {@link EskizSmsClient} ga
 * bog'langan edi. Natijada lokal stendda kirishni umuman sinab
 * bo'lmasdi: Eskiz kabineti yo'q, demak {@code /otp/send} har safar
 * {@code SMS_NOT_CONFIGURED} qaytarardi va ilovadagi kirish ekrani
 * o'lik edi.
 *
 * Endi shlyuz almashtiriladi: prod'da {@link EskizSmsClient}, lokal
 * profilda {@link LoggingSmsClient} (kodni logga yozadi).
 *
 * <h2>⚠️ Soxta muvaffaqiyat qoidasi buzilmaydi</h2>
 * Sozlanmagan Eskiz baribir XATO qaytaradi (§44) — u hech qachon
 * "yuborildi" deb yolg'on aytmaydi. Logga yozadigan nusxa esa alohida
 * bean va u FAQAT {@code local} profilida mavjud, ya'ni serverda
 * yaratilmaydi.
 */
public interface SmsClient {

    /**
     * @throws IllegalStateException shlyuz sozlanmagan bo'lsa
     * @throws org.springframework.web.client.RestClientException shlyuz javob
     *                                        bermasa yoki xato qaytarsa
     */
    void send(String phone, String message);
}
