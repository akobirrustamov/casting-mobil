package com.example.backend.Security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JWT kaliti bo'lmasa ilova ko'tarilmaydi — va SABABINI aytadi.
 *
 * <h2>⚠️ Nima uchun bu testga arziydi</h2>
 * Ilgari kalit yo'q bo'lsa Spring shunday derdi:
 *
 *   Could not resolve placeholder 'app.jwt.secret'
 *
 * Xato TO'G'RI, lekin nima qilish kerakligini aytmaydi. Foydalanuvchi
 * uni uch marta oldi va har safar sabab boshqacha edi: goh muhit
 * o'zgaruvchisi yo'q, goh env fayl o'qilmagan. Stack trace ikkalasida
 * ham bir xil ko'rinardi.
 *
 * Shuning uchun endi xabar buyruqlarni o'z ichiga oladi.
 */
class JwtSecretRequiredTest {

    private JwtService serviceWith(String secret) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        return service;
    }

    @Nested
    @DisplayName("Kalit talab qilinadi")
    class Required {

        @Test
        @DisplayName("Kalitsiz — ishga tushmaydi")
        void missingSecretStopsStartup() {
            assertThatThrownBy(() -> serviceWith("").verifySecret())
                    .isInstanceOf(IllegalStateException.class);

            assertThatThrownBy(() -> serviceWith(null).verifySecret())
                    .isInstanceOf(IllegalStateException.class);
        }

        /**
         * ⚠️ Qisqa kalit ilgari ishga tushishda emas, BIRINCHI
         * KIRISHDA yiqilardi: `Keys.hmacShaKeyFor` uni o'shanda rad
         * etadi.
         *
         * Ya'ni server ko'tarilib, ishlayotgandek ko'rinardi va
         * nosozlik birinchi foydalanuvchida chiqardi — deploy'dan
         * ancha keyin.
         */
        @Test
        @DisplayName("Qisqa kalit ham ishga tushirishda ushlanadi")
        void shortSecretIsCaughtAtStartup() {
            assertThatThrownBy(() -> serviceWith("qisqa").verifySecret())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("32");
        }

        @Test
        @DisplayName("Yetarli kalit — o'tadi")
        void validSecretPasses() {
            assertThatCode(() -> serviceWith("a".repeat(32)).verifySecret())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("⚠️ Xabar NIMA QILISH KERAKLIGINI aytadi")
    class ActionableMessage {

        private String message() {
            try {
                serviceWith("").verifySecret();
            } catch (IllegalStateException e) {
                return e.getMessage();
            }
            throw new AssertionError("xato tashlanmadi");
        }

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV — va u haqiqiy nosozlikdan tug'ilgan.
         *
         * Xabar ilgari «sh deploy/run.sh» deb maslahat berardi. Ikki
         * jihatdan noto'g'ri edi:
         *
         *   1. `run.sh` keyinchalik o'chirildi va xabar mavjud
         *      bo'lmagan faylga yuborardi;
         *   2. undan ham muhimi — bu xabar SERVERDA o'qiladi, u yerda
         *      esa loyiha kodi UMUMAN yo'q. Serverda faqat ikkita
         *      fayl bor: `backend.jar` va `application.properties`.
         *
         * Ya'ni manba daraxtidagi har qanday yo'l bu yerda befoyda.
         * Birinchi xatoni oddiy `contains(...)` ushlay olmadi: fayl
         * o'chirilganda ham xabarda o'sha satr turaverdi va test
         * yashil qolaverdi.
         *
         * Shuning uchun endi teskarisini tekshiramiz.
         */
        @Test
        @DisplayName("Loyiha ichidagi fayllarga YUBORMAYDI")
        void doesNotPointAtRepositoryFiles() {
            assertThat(message())
                    .as("serverda loyiha kodi yo'q — u yerdagi yo'l befoyda")
                    .doesNotContain("deploy/")
                    .doesNotContain(".sh");
        }

        /**
         * ⚠️ Haqiqiy sabab deyarli har doim bitta: Spring Boot
         * `application.properties` ni JAR YONIDAN emas, JORIY
         * PAPKADAN o'qiydi.
         *
         * Foydalanuvchi `/root` da turib `java -jar /opt/uzcasting/
         * backend.jar` deb yozdi va fayl o'qilmadi. Stack trace esa
         * «kalit yo'q» derdi — bu to'g'ri, lekin sababni yashirardi.
         */
        @Test
        @DisplayName("Joriy papkani KO'RSATADI")
        void namesTheWorkingDirectory() {
            assertThat(message())
                    .as("sabab papkada — demak papka xabarda bo'lsin")
                    .contains(System.getProperty("user.dir"));
        }

        @Test
        @DisplayName("Ikkala tuzatish yo'li ham beriladi")
        void everyOptionIsListed() {
            assertThat(message())
                    .contains("cd /opt/uzcasting")
                    .contains("--spring.config.additional-location")
                    .contains("openssl rand -hex 32")
                    .contains("--spring.profiles.active=dev");
        }

        /**
         * ⚠️ Kalitni almashtirish oqibati ham aytiladi — aks holda
         * odam uni har deployda qaytadan yasab, foydalanuvchilarni
         * har safar tizimdan chiqarib yuborardi.
         */
        @Test
        @DisplayName("Almashtirish oqibati ogohlantiriladi")
        void warnsAboutRotation() {
            assertThat(message()).contains("chiqib ketadi");
        }
    }
}
