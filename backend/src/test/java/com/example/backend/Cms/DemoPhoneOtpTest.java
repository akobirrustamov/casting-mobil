package com.example.backend.Cms;

import com.example.backend.Sms.SmsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Google Play tekshiruvchisi uchun demo raqam (13.09.2026).
 *
 * <h2>Nima uchun bu kerak</h2>
 * Ilovaga kirish — o'zbek raqamiga keladigan SMS kod orqali. Google
 * tekshiruvchisi boshqa mamlakatda o'tiradi: SMS kelmaydi, u kirish
 * ekranidan nariga o'tolmaydi va ilovani «ishlamaydi» deb rad etadi.
 * Shuning uchun prodda bitta raqam uchun SMS yuborilmaydi, kod esa
 * oldindan ma'lum bo'ladi (Play Console → App access).
 *
 * <h2>Nima jim buziladi</h2>
 * <ul>
 *   <li>Demo kod BOSHQA raqamlarda ham ishlaydi — bu butun ilova uchun
 *       ochiq eshik bo'lardi, va uni hech kim payqamasdi.</li>
 *   <li>Demo raqamga haqiqiy SMS ketadi — pul sarflanadi, tekshiruvchi
 *       esa kodni baribir ololmaydi.</li>
 *   <li>Noto'g'ri kod ham o'tadi.</li>
 * </ul>
 *
 * ⚠️ Sozlama sukut bo'yicha BO'SH: mexanizm faqat ikkala qiymat ham
 * berilganda yoqiladi. Shuning uchun bu testda ular ataylab beriladi —
 * oddiy holatda hech qanday demo raqam yo'q.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "app.ratelimit.enabled=false",
        "app.otp.demo.phone=+998900000000",
        "app.otp.demo.code=0000"
})
class DemoPhoneOtpTest {

    private static final String SEND = "/api/v1/app/auth/otp/send";
    private static final String VERIFY = "/api/v1/app/auth/otp/verify";
    private static final String COMPLETE = "/api/v1/app/auth/otp/complete";

    private static final String DEMO_PHONE = "+998900000000";
    private static final String DEMO_CODE = "0000";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    /** SMS yuborilmasligini isbotlash uchun — haqiqiy shlyuz kerak emas. */
    @MockBean private SmsClient smsClient;

    private org.springframework.test.web.servlet.ResultActions call(String url, Map<String, ?> body)
            throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Nested
    @DisplayName("Demo raqam")
    class Demo {

        @Test
        @DisplayName("Kod so'ralganda SMS YUBORILMAYDI")
        void noSmsIsSent() throws Exception {
            call(SEND, Map.of("phone", DEMO_PHONE))
                    .andExpect(status().isOk());

            verify(smsClient, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("Oldindan ma'lum kod o'tadi")
        void fixedCodeWorks() throws Exception {
            call(SEND, Map.of("phone", DEMO_PHONE)).andExpect(status().isOk());

            call(VERIFY, Map.of("phone", DEMO_PHONE, "code", DEMO_CODE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Noto'g'ri kod o'tmaydi")
        void wrongCodeFails() throws Exception {
            call(SEND, Map.of("phone", DEMO_PHONE)).andExpect(status().isOk());

            call(VERIFY, Map.of("phone", DEMO_PHONE, "code", "9999"))
                    .andExpect(status().isUnprocessableEntity());
        }

        /**
         * ⚠️ Kod 4 XONALI. Ilovadagi maydon ham 4 katakli
         * ({@code mobile/app/(auth)/otp.tsx}, {@code CODE_LENGTH = 4}) —
         * olti xonali kod sozlansa, tekshiruvchi uni terib ham ko'ra
         * olmasdi.
         */
        @Test
        @DisplayName("Kod 4 xonali — ilovadagi maydonga sig'adi")
        void codeFitsTheInput() {
            assertThat(DEMO_CODE).hasSize(4);
        }

        /**
         * Tekshiruvchi katalogning yopiq qismini ham ko'rishi kerak, aks
         * holda ilovaning yarmini baholay olmaydi — shuning uchun demo
         * hisobga Premium beriladi.
         */
        @Test
        @DisplayName("Demo hisob Premium bilan kiradi")
        void demoAccountGetsPremium() throws Exception {
            call(SEND, Map.of("phone", DEMO_PHONE)).andExpect(status().isOk());
            call(VERIFY, Map.of("phone", DEMO_PHONE, "code", DEMO_CODE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name_required").value(true));

            String body = call(COMPLETE, Map.of("phone", DEMO_PHONE, "name", "Play Reviewer"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode json = objectMapper.readTree(body);
            String token = json.get("access_token").asText();

            mockMvc.perform(get("/api/v1/app/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.premium.active").value(true));
        }

        /**
         * ⚠️ Eng muhim tekshiruv: demo kod faqat BITTA raqamga tegishli.
         * Aks holda «000000» butun bazaga kalit bo'lardi.
         */
        @Test
        @DisplayName("Demo kod boshqa raqamda ishlamaydi")
        void demoCodeDoesNotOpenOtherNumbers() throws Exception {
            call(VERIFY, Map.of("phone", "+998901234567", "code", DEMO_CODE))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
