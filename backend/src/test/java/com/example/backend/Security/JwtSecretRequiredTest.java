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
         * Har bir yo'l xabarda bo'lishi kerak: foydalanuvchi qaysi
         * holatda ekanini bilmasligi mumkin.
         */
        @Test
        @DisplayName("Uchala yo'l ham ko'rsatiladi")
        void everyOptionIsListed() {
            assertThat(message())
                    .contains("openssl rand -hex 32")
                    .contains("APP_JWT_SECRET=")
                    .contains("deploy/run.sh")
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
