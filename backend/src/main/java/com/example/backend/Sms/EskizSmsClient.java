package com.example.backend.Sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Eskiz.uz SMS shlyuzi bilan integratsiya (https://notify.eskiz.uz/api).
 *
 * Token email/parol bilan olinadi va 30 kun amal qiladi (Eskiz hujjati),
 * shuning uchun xotirada saqlanadi — har xabar uchun qayta login qilinmaydi.
 * 401 kelsa token bir marta yangilanib qayta urinib ko'riladi.
 *
 * ⚠️ {@code eskiz.email}/{@code eskiz.password} berilmaguncha bu klient
 * ISHLAMAYDI va buni ATAYLAB yashirmaydi ({@link #isConfigured()}) —
 * GoogleTokenVerifier'dagi kabi, sozlanmagan tashqi xizmat soxta
 * muvaffaqiyat qaytarmasligi kerak (§44 tamoyili to'lovga o'xshash: SMS
 * ham "yuborildi" deb yolg'on aytmaydi).
 *
 * Eskiz kabineti moderatsiyadan o'tmaguncha faqat quyidagi test matnlari
 * va faqat whitelist raqamiga yuboriladi: "Bu Eskiz dan test",
 * "Это тест от Eskiz", "This is test from Eskiz". Haqiqiy OTP matni
 * moderatsiya tasdiqlanguncha yetib bormaydi — bu Eskiz tomonidagi
 * cheklov, kod bilan chetlab bo'lmaydi.
 *
 * <h2>⚠️ Nega {@code @Profile("!local")}</h2>
 * Lokal stendda Eskiz kabineti yo'q, ya'ni bu klient u yerda faqat
 * {@code SMS_NOT_CONFIGURED} qaytara olardi va telefon orqali kirishni
 * sinab bo'lmasdi. Lokal profilda uning o'rnini {@link LoggingSmsClient}
 * egallaydi — kodni SMS o'rniga logga yozadi.
 *
 * Serverda {@code local} profili yo'q, demak bu yerda hamma narsa
 * avvalgidek: haqiqiy SMS yoki halol xato.
 */
@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class EskizSmsClient implements SmsClient {

    private final RestTemplate restTemplate;

    @Value("${eskiz.base-url:https://notify.eskiz.uz/api}")
    private String baseUrl;

    @Value("${eskiz.email:}")
    private String email;

    @Value("${eskiz.password:}")
    private String password;

    /**
     * Tasdiqlangan nickname (Eskiz kabinetida). Moderatsiyadan o'tmagan
     * hisobda standart qiymat "4546" — faqat test matnlari bilan ishlaydi.
     */
    @Value("${eskiz.sms.from:4546}")
    private String from;

    private final AtomicReference<String> token = new AtomicReference<>();

    public boolean isConfigured() {
        return !email.isBlank() && !password.isBlank();
    }

    /**
     * SMS yuboradi.
     *
     * @throws IllegalStateException          email/parol sozlanmagan bo'lsa
     * @throws org.springframework.web.client.RestClientException Eskiz javob
     *                                         bermasa yoki xato qaytarsa
     */
    @Override
    public void send(String phone, String message) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "eskiz.email/eskiz.password sozlanmagan - SMS yuborilmadi");
        }

        String bearer = ensureToken();
        try {
            doSend(phone, message, bearer);
        } catch (HttpClientErrorException.Unauthorized e) {
            // Token eskirgan yoki bekor qilingan bo'lishi mumkin - bir marta
            // qayta login qilib urinib ko'ramiz.
            doSend(phone, message, login());
        }
    }

    private String ensureToken() {
        String current = token.get();
        return current != null ? current : login();
    }

    private synchronized String login() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("email", email);
        body.add("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/auth/login", org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

        Object data = response.getBody() == null ? null : response.getBody().get("data");
        if (!(data instanceof Map<?, ?> dataMap) || dataMap.get("token") == null) {
            throw new IllegalStateException("Eskiz login javobida token yo'q");
        }

        String newToken = dataMap.get("token").toString();
        token.set(newToken);
        return newToken;
    }

    private void doSend(String phone, String message, String bearer) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("mobile_phone", phone);
        body.add("message", message);
        body.add("from", from);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(bearer);

        restTemplate.postForEntity(
                baseUrl + "/message/sms/send", new HttpEntity<>(body, headers), String.class);
    }
}
