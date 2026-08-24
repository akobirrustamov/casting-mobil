package com.example.backend.Security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * CORS sozlamasi cookie bilan ishlaydigan so'rovlarga mos.
 *
 * <h2>⚠️ Bu xato haqiqatda yuz berdi</h2>
 * §61 da panel refresh tokenni {@code httpOnly} cookie'da oladigan
 * bo'ldi va so'rovlar {@code withCredentials} bilan ketadi. CORS esa
 * eski holida qoldi: {@code allowedOrigins("*")}, credentials
 * yoqilmagan.
 *
 * Natijada <b>server 200 qaytarardi, brauzer esa javobni bloklardi</b>
 * va foydalanuvchi «Server bilan aloqa yo'q. Internetni tekshiring»
 * xatosini ko'rardi. Server loglarida hech qanday xato yo'q — aynan
 * shuning uchun buni topish qiyin.
 *
 * <h2>Nega `curl` bilan sezilmadi</h2>
 * `curl` CORS qoidalarini umuman qo'llamaydi: u javobni oladi va
 * ko'rsatadi. Tekshiruv faqat {@code Origin} sarlavhasi bilan va
 * javob sarlavhalarini o'qib qilinganda ma'noli bo'ladi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsCredentialsTest {

    private static final String ORIGIN = "http://localhost:3000";

    @Autowired private MockMvc mvc;

    @Test
    @DisplayName("⚠️ Javobda ANIQ manba qaytadi, yulduzcha emas")
    void originIsEchoedNotWildcard() throws Exception {
        var result = mvc.perform(post("/api/v1/app/admin/auth/login")
                        .header("Origin", ORIGIN)
                        .contentType("application/json")
                        .content("{\"phone\":\"yo'q\",\"password\":\"yo'q\"}"))
                .andReturn();

        String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");

        // ⚠️ `*` bilan brauzer credentials'li so'rovni RAD ETADI.
        assertThat(allowOrigin)
                .as("cookie bilan ishlaydigan so'rovda `*` qabul qilinmaydi")
                .isEqualTo(ORIGIN);
    }

    @Test
    @DisplayName("Credentials sarlavhasi yoqilgan")
    void credentialsAreAllowed() throws Exception {
        var result = mvc.perform(post("/api/v1/app/admin/auth/login")
                        .header("Origin", ORIGIN)
                        .contentType("application/json")
                        .content("{\"phone\":\"yo'q\",\"password\":\"yo'q\"}"))
                .andReturn();

        assertThat(result.getResponse().getHeader("Access-Control-Allow-Credentials"))
                .as("usiz brauzer cookie'ni yubormaydi va javobni bloklaydi")
                .isEqualTo("true");
    }

    @Test
    @DisplayName("Preflight so'rovi o'tadi")
    void preflightSucceeds() throws Exception {
        // Brauzer `application/json` bilan POST yuborishdan OLDIN
        // OPTIONS so'rovini yuboradi. U rad etilsa asosiy so'rov
        // umuman ketmaydi.
        var result = mvc.perform(options("/api/v1/app/admin/auth/login")
                        .header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Credentials"))
                .isEqualTo("true");
    }

    @Test
    @DisplayName("Sozlamada `allowedOrigins(\"*\")` ishlatilmaydi")
    void wildcardOriginsIsNotUsed() throws IOException {
        String src = Files.readString(Path.of(
                "src/main/java/com/example/backend/Config/WebMvcConfig.java"));

        // ⚠️ Faqat KOD qatorlari tekshiriladi. Bu faylning javadoc'ida
        // `allowedOrigins("*")` nima uchun yaramasligi tushuntirilgan
        // va matn bo'yicha qidiruv o'sha izohni ushlab qolardi.
        //
        // Izohlarni regex bilan kesish ham yaramadi: u kodning bir
        // qismini ham yeb qo'ydi. Qatorlar bo'yicha tekshirish
        // aniqroq va o'zini tushuntiradi.
        String code = src.lines()
                .map(String::trim)
                .filter(l -> !l.startsWith("*") && !l.startsWith("/*") && !l.startsWith("//"))
                .collect(java.util.stream.Collectors.joining("\n"));

        assertThat(code)
                .as("`allowedOrigins(\"*\")` credentials bilan ishlamaydi - "
                        + "brauzer bunday javobni bloklaydi")
                .doesNotContain("allowedOrigins(\"*\")");
        assertThat(code).contains("allowedOriginPatterns");
        assertThat(code).contains("allowCredentials(true)");
    }

    @Test
    @DisplayName("Panel `withCredentials` bilan so'rov yuboradi")
    void panelSendsCredentials() throws IOException {
        // Ikkalasi bir-biriga bog'liq: biri o'zgarsa ikkinchisi ham
        // o'zgarishi kerak. Shuning uchun shu yerda birga tekshiriladi.
        String client = Files.readString(Path.of(
                "../frontend/src/adminpanel/api/client.js"));

        assertThat(client).contains("withCredentials: true");
    }
}
