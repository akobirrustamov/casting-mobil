package com.example.backend.Sms;

import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sof unit test — Spring konteksti kerak emas. {@link EskizSmsClient} o'rniga
 * qo'lda yozilgan {@link FakeEskizSmsClient} ishlatiladi: repo'da Mockito
 * hech qayerda ishlatilmaydi va bu muhitda (JDK 25) Byte Buddy'ning eski
 * versiyasi bilan mos kelmaydi ("Java 25 is not supported").
 *
 * Muhim xatti-harakatlar: 2 daqiqa qayta yuborish cheklovi, urinishlar
 * chegarasi, muddat tugashi va sozlanmagan/ishlamayotgan SMS provayderida
 * soxta muvaffaqiyat qaytmasligi (§44 tamoyili).
 */
class OtpServiceTest {

    private FakeEskizSmsClient smsClient;
    private OtpService service;

    @BeforeEach
    void setUp() {
        smsClient = new FakeEskizSmsClient();
        service = new OtpService(smsClient, new BCryptPasswordEncoder());
        ReflectionTestUtils.setField(service, "ttlSeconds", 180);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 120);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "messageTemplate",
                "UzCasting platformasida ro'yxatdan o'tish uchun tasdiqlash kod: %s");
    }

    @Test
    @DisplayName("To'g'ri kod bir marta tasdiqlanadi")
    void correctCodeVerifiesOnce() {
        String phone = "+998 90 111 22 33";
        service.send(phone);
        String sentCode = smsClient.lastCode();

        service.verify(phone, sentCode);

        // Ishlatilgan kod ikkinchi marta o'tmaydi.
        assertThatThrownBy(() -> service.verify(phone, sentCode))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "OTP_EXPIRED");
    }

    @Test
    @DisplayName("Noto'g'ri kod xato qaytaradi, muvaffaqiyat emas")
    void wrongCodeFails() {
        String phone = "+998901112244";
        service.send(phone);

        assertThatThrownBy(() -> service.verify(phone, "0000"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "OTP_INVALID")
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("Ko'p noto'g'ri urinishdan keyin kod bloklanadi")
    void locksAfterMaxAttempts() {
        String phone = "+998901112255";
        service.send(phone);
        String sentCode = smsClient.lastCode();

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.verify(phone, "0000"))
                    .isInstanceOf(BusinessException.class);
        }

        // Kod baribir to'g'ri bo'lsa ham - urinishlar tugagan.
        assertThatThrownBy(() -> service.verify(phone, sentCode))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "OTP_LOCKED");
    }

    @Test
    @DisplayName("2 daqiqadan oldin qayta yuborish rad etiladi")
    void resendCooldownBlocksImmediateRetry() {
        String phone = "+998901112266";
        service.send(phone);

        assertThatThrownBy(() -> service.send(phone))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "OTP_COOLDOWN")
                .hasFieldOrPropertyWithValue("status", HttpStatus.TOO_MANY_REQUESTS);

        assertThat(smsClient.sentMessages).hasSize(1);
    }

    @Test
    @DisplayName("Eskiz sozlanmagan bo'lsa - aniq xato, soxta muvaffaqiyat emas")
    void unconfiguredClientFailsLoudly() {
        smsClient.failWith(new IllegalStateException("eskiz.email/eskiz.password sozlanmagan"));

        assertThatThrownBy(() -> service.send("+998901112277"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "SMS_NOT_CONFIGURED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Telefon +998 formatiga normallashtiriladi")
    void normalizesPhone() {
        assertThat(OtpService.normalize("+998 90 111 22 88")).isEqualTo("998901112288");
        assertThat(OtpService.normalize("901112288")).isEqualTo("998901112288");
        assertThatThrownBy(() -> OtpService.normalize("123")).isInstanceOf(BusinessException.class);
    }

    /** Real Eskiz'ga hech qachon murojaat qilmaydi - xabarni xotirada ushlab qoladi. */
    private static final class FakeEskizSmsClient extends EskizSmsClient {
        private final List<String> sentMessages = new ArrayList<>();
        private RuntimeException failure;

        FakeEskizSmsClient() {
            super(new RestTemplate());
        }

        void failWith(RuntimeException e) {
            this.failure = e;
        }

        String lastCode() {
            String message = sentMessages.get(sentMessages.size() - 1);
            return message.substring(message.length() - 4);
        }

        @Override
        public void send(String phone, String message) {
            if (failure != null) {
                throw failure;
            }
            sentMessages.add(message);
        }
    }
}
