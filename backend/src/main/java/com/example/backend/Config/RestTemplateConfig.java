package com.example.backend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;



@Configuration
public class RestTemplateConfig {

    /**
     * Tashqi xizmatlar (Eskiz SMS, Expo push) uchun klient.
     *
     * <h2>⚠️ Nega timeout SHART</h2>
     * Sukut bo'yicha {@code RestTemplate} CHEKSIZ kutadi. Eskiz javob
     * bermay qolsa, OTP so'ragan har bir so'rov Tomcat oqimini abadiy
     * ushlab turardi ({@code EskizSmsClient.login()} esa
     * {@code synchronized} — qolganlari navbatda turadi). Oqimlar
     * tugagach butun server «qotib» qolardi, garchi xato faqat SMS
     * xizmatida bo'lsa ham.
     *
     * Endi sekin xizmat xato bilan tugaydi va faqat o'sha so'rov
     * muvaffaqiyatsiz bo'ladi.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        return new RestTemplate(factory);
    }
}
