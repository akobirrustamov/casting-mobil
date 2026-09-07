package com.example.backend.Cms.Controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Huquqiy sahifalar: foydalanuvchi kelishuvi va maxfiylik siyosati.
 *
 * <h2>Nima uchun bu kontroller kerak, sahifa oddiy HTML bo'lsa ham</h2>
 * {@code WebMvcConfig.PushStateResourceResolver} kengaytmasi yo'q har qanday
 * yo'lni {@code static/index.html} ga — ya'ni React ilovasiga — yuboradi.
 * Shuning uchun {@code /kelishuv} 404 EMAS, balki 200 va SPA qobig'ini
 * qaytarardi: odam hujjat o'rniga ilovani ko'rardi.
 *
 * ⚠️ Bu Google uchun 404 dan ham yomon. Rozilik ekranini tekshiruvchi
 * havolani ochadi va hujjat o'rniga ilovani ko'radi — bu rad javobi.
 *
 * Fayllarni {@code static/} ga qo'yish yordam bermaydi: resolver yo'lda
 * nuqta bo'lmasa faylni umuman qidirmaydi, ya'ni {@code /kelishuv} baribir
 * index.html ga tushardi. Manzil esa aynan shunday bo'lishi kerak — u
 * ilovaga ({@code mobile/src/features/legal/links.ts}) va Google Cloud
 * Console → Branding ga yozilgan.
 *
 * <h2>Nima uchun fayllar {@code static/} da emas</h2>
 * {@code resources/legal/} — resolver ko'rmaydigan joy. Shunday qilib har
 * bir hujjatning manzili BITTA bo'ladi: {@code /static/legal/kelishuv.html}
 * kabi ikkinchi, tasodifiy manzil paydo bo'lmaydi. Qidiruv tizimi ikki
 * nusxani ko'rmaydi, havola esa hamma joyda bir xil.
 *
 * <h2>Yo'l oxiridagi qiya chiziq</h2>
 * Spring Boot 3 da {@code /kelishuv/} avtomatik ravishda {@code /kelishuv}
 * bilan mos kelmaydi. Qo'lda yozilmasa, qiya chiziq bilan tergan odam yana
 * SPA qobig'iga tushardi.
 *
 * <h2>Nima uchun {@code Cms/Controller} da, garchi bu CMS bo'lmasa ham</h2>
 * Yo'l {@code /api/v1/app/**} makonidan tashqarida, shuning uchun eski
 * {@code Controller/} papkasi mantiqiyroq ko'rinardi. Lekin u MUZLATILGAN:
 * {@code OldCastingFrozenTest.noNewCodeLandsInTheOldPackage} u yerga yangi
 * fayl qo'shilishini taqiqlaydi, toki eski Telegram bot va yangi kod
 * aralashib ketmasin. Yangi kod uchun ikkita joy qolgan — {@code Cms/} va
 * {@code Admin/}.
 */
@RestController
public class LegalPageController {

    /** Foydalanuvchi kelishuvi. */
    @GetMapping({"/kelishuv", "/kelishuv/"})
    public ResponseEntity<Resource> terms() {
        return page("kelishuv");
    }

    /** Maxfiylik siyosati. */
    @GetMapping({"/maxfiylik", "/maxfiylik/"})
    public ResponseEntity<Resource> privacy() {
        return page("maxfiylik");
    }

    /**
     * Fayl nomi shu yerda yozilgan — so'rovdan olinmaydi. Aks holda
     * {@code /legal/../../application.properties} kabi yo'l bilan jar ichidagi
     * boshqa fayl so'ralishi mumkin bo'lardi.
     */
    private ResponseEntity<Resource> page(String name) {
        Resource file = new ClassPathResource("legal/" + name + ".html");
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                // ⚠️ charset shart: sahifalar o'zbekcha, apostrof va «o'» bilan.
                // Charsetsiz brauzer kodlashni o'zi taxmin qiladi.
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(file);
    }
}
