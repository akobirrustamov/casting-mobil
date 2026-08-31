package com.example.backend.Cms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Statik fayllar va SPA marshrutlari.
 *
 * <h2>⚠️ Qanday nosozlikni qo'riqlaydi</h2>
 * {@code WebMvcConfig} da {@code orElseGet(null)} yozilgan edi.
 * {@code orElseGet} argument sifatida FUNKSIYA kutadi va bo'sh
 * natijada uni chaqiradi — funksiya esa {@code null} edi.
 *
 * Ya'ni fayl topilmagan zahoti {@code NullPointerException} chiqardi
 * va mavjud bo'lmagan har qanday statik fayl **404 emas, 500**
 * qaytarardi.
 *
 * Bu nazariy holat emas: CRA yasaydigan {@code manifest.json} ichida
 * {@code logo192.png} yozilgan va uni HAR BIR brauzer so'raydi.
 * Fayl esa build'da yo'q — demak har bir tashrif logga stack trace
 * yozardi.
 *
 * Xato jimgina edi: sayt ishlayotgandek ko'rinardi, chunki asosiy
 * fayllar joyida va faqat yo'qlari yiqilardi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaticFileDeliveryTest {

    @Autowired private MockMvc mockMvc;

    /**
     * ⚠️ ENG MUHIM TEKSHIRUV.
     *
     * Kengaytmasi «fayl» deb tanilgan, lekin mavjud bo'lmagan yo'l.
     * Bu yerda 500 chiqishi kod xatosini bildiradi — server o'zi
     * yiqilyapti degani.
     */
    @Test
    @DisplayName("Mavjud bo'lmagan statik fayl 500 BERMAYDI")
    void missingAssetDoesNotCrash() throws Exception {
        for (String path : new String[]{
                "/logo192.png", "/yoq.js", "/static/js/yoq.js", "/yoq.css"}) {

            mockMvc.perform(get(path))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        if (code >= 500) {
                            throw new AssertionError(
                                    path + " → " + code + ". Mavjud bo'lmagan fayl "
                                            + "server xatosi emas, 404 bo'lishi kerak.");
                        }
                    });
        }
    }

    /**
     * SPA marshruti — kengaytmasi yo'q yo'l {@code index.html} ga
     * tushadi, aks holda panelni to'g'ridan-to'g'ri havola bilan
     * ochib bo'lmasdi.
     */
    @Test
    @DisplayName("Panel marshruti index.html ga tushadi")
    void spaRouteFallsBackToIndex() throws Exception {
        mockMvc.perform(get("/app/panel/subscriptions"))
                .andExpect(status().isOk());
    }

    /**
     * ⚠️ {@code /api} SPA'ga TUSHMAYDI.
     *
     * Tushsa, mavjud bo'lmagan endpoint 404 o'rniga HTML sahifa
     * qaytarardi — va klient uni JSON deb o'qishga urinib,
     * tushunarsiz xato berardi.
     */
    @Test
    @DisplayName("Mavjud bo'lmagan API yo'li HTML qaytarmaydi")
    void apiPathIsNotSwallowedBySpa() throws Exception {
        mockMvc.perform(get("/api/v1/bunday/yol/yoq"))
                .andExpect(result -> {
                    String type = result.getResponse().getContentType();
                    if (type != null && type.contains("text/html")) {
                        throw new AssertionError(
                                "API yo'li index.html qaytardi: " + type);
                    }
                });
    }
}
