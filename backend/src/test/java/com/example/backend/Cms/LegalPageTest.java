package com.example.backend.Cms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Huquqiy sahifalar haqiqatan ham hujjat qaytaradimi.
 *
 * <h2>Nima uchun «200 qaytdi» yetarli emas</h2>
 * Bu yerdagi nosozlik aynan shunday ko'ringan edi: {@code /kelishuv} 200
 * qaytarardi — lekin ichida React ilovasining qobig'i turardi. Status
 * bo'yicha hammasi joyida, odam esa hujjat o'rniga ilovani ko'rardi.
 *
 * Shuning uchun har bir tekshiruvda ikkita shart bor: hujjat KELDI va
 * bu SPA qobig'i EMAS. Birinchisi yolg'iz o'zi hech narsani isbotlamaydi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegalPageTest {

    @Autowired
    private MockMvc mockMvc;

    /** SPA qobig'ining belgisi — React shu yerga o'rnashadi. */
    private static final String SPA_MARKER = "id=\"root\"";

    @Nested
    @DisplayName("Sahifalar")
    class Pages {

        @Test
        @DisplayName("/kelishuv — foydalanuvchi kelishuvi, SPA emas")
        void termsAreServed() throws Exception {
            String body = mockMvc.perform(get("/kelishuv"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/html"))
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).contains("Foydalanuvchi kelishuvi");
            assertThat(body).doesNotContain(SPA_MARKER);
        }

        @Test
        @DisplayName("/maxfiylik — maxfiylik siyosati, SPA emas")
        void privacyIsServed() throws Exception {
            String body = mockMvc.perform(get("/maxfiylik"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).contains("Maxfiylik siyosati");
            assertThat(body).doesNotContain(SPA_MARKER);
        }

        /**
         * Kodlash — bu tafsilot emas: sahifalar o'zbekcha, ularda apostrof
         * va «o'» bor. Charset yo'qolsa matn brauzerda buziladi.
         */
        @Test
        @DisplayName("Kodlash UTF-8 deb aytiladi")
        void charsetIsDeclared() throws Exception {
            mockMvc.perform(get("/kelishuv"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"));
        }

        /**
         * ⚠️ Spring Boot 3 da qiya chiziq avtomatik qo'shilmaydi. Qo'lda
         * yozilgan mapping bo'lmasa, {@code /kelishuv/} yana SPA ga tushadi.
         */
        @Test
        @DisplayName("Oxiridagi qiya chiziq ham ishlaydi")
        void trailingSlashIsHandled() throws Exception {
            String body = mockMvc.perform(get("/kelishuv/"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain(SPA_MARKER);
        }

        /**
         * Ikki hujjat bir-biriga havola qiladi. Havola manzili o'zgarsa —
         * masalan sahifa {@code /legal/kelishuv.html} ga ko'chirilsa —
         * o'quvchi ikkinchi hujjatga o'ta olmay qoladi.
         */
        @Test
        @DisplayName("Hujjatlar bir-biriga havola qiladi")
        void documentsLinkToEachOther() throws Exception {
            String terms = mockMvc.perform(get("/kelishuv"))
                    .andReturn().getResponse().getContentAsString();
            String privacy = mockMvc.perform(get("/maxfiylik"))
                    .andReturn().getResponse().getContentAsString();

            assertThat(terms).contains("href=\"/maxfiylik\"");
            assertThat(privacy).contains("href=\"/kelishuv\"");
        }
    }

    /**
     * Manzilni ilova ham biladi.
     *
     * ⚠️ Xuddi shu ikki manzil Google Cloud Console → Branding da yozilgan.
     * Server tomonda yo'lni o'zgartirish — bu ilovadagi havolani va Google
     * sozlamasini ham o'zgartirish demak. Jimgina qilib bo'lmaydi: kirish
     * ekranidagi havola 404 ga aylanadi, rozilik ekrani esa tekshiruvdan
     * o'tmaydi.
     */
    @Nested
    @DisplayName("Mobil ilova bilan shartnoma")
    class MobileContract {

        private static final Path MOBILE_LINKS = Path.of(
                "../mobile/src/features/legal/links.ts");

        @Test
        @DisplayName("Ilovadagi havolalar server yo'llari bilan bir xil")
        void mobileUsesTheSamePaths() throws Exception {
            String source = Files.readString(MOBILE_LINKS);

            assertThat(source).contains("${DOMAIN}/kelishuv");
            assertThat(source).contains("${DOMAIN}/maxfiylik");
            assertThat(source).contains("https://uzcasting.com");
        }
    }
}
