package com.example.backend.Security.Bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ishga tushirishda yaratiladigan master hisoblar xavfsizligi.
 *
 * <h2>Qanday zaiflik topilgan edi</h2>
 * {@code AutoRun} har ishga tushishda hisoblar yaratardi va parolning
 * STANDART qiymati kodda turardi:
 *
 * <pre>{@code @Value("${app.gipersuperadmin.password:00000000}")}</pre>
 *
 * Ya'ni har bir yangi o'rnatishda `gipersuperadmin / 00000000` hisobi
 * paydo bo'lardi — platformadagi ENG YUQORI rol. Parol manba kodda
 * bo'lgani uchun uni har kim bilardi.
 *
 * Amalda tekshirilgan edi: shu hisob bilan token olinardi, `/staff`,
 * `/audit-logs`, `/settings` ochilardi va SUPER_ADMIN yaratish mumkin edi.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * Ikki qavat: parol qoidasining o'zi, va {@code AutoRun} manbasida
 * standart parol QAYTIB KELMASLIGI.
 */
class BootstrapAccountSecurityTest {

    private static final Path AUTO_RUN =
            Paths.get("src/main/java/com/example/backend/Config/AutoRun.java");

    @Nested
    @DisplayName("Parol qoidasi")
    class Policy {

        @Test
        @DisplayName("Ma'lum zaif parollar rad etiladi")
        void knownWeakPasswordsAreRejected() {
            // Aynan shu parol bilan HYPER_ADMIN hisobi yaratilardi.
            assertThat(BootstrapPasswordPolicy.isAcceptable("00000000")).isFalse();
            assertThat(BootstrapPasswordPolicy.isAcceptable("12345678")).isFalse();
            assertThat(BootstrapPasswordPolicy.isAcceptable("password")).isFalse();
            assertThat(BootstrapPasswordPolicy.isAcceptable("admin123")).isFalse();
            // Katta-kichik harf farqi yordam bermasin.
            assertThat(BootstrapPasswordPolicy.isAcceptable("PassWord")).isFalse();
        }

        @Test
        @DisplayName("Bo'sh yoki qisqa parol rad etiladi")
        void emptyOrShortIsRejected() {
            assertThat(BootstrapPasswordPolicy.isAcceptable(null)).isFalse();
            assertThat(BootstrapPasswordPolicy.isAcceptable("")).isFalse();
            assertThat(BootstrapPasswordPolicy.isAcceptable("Ab1")).isFalse();
            assertThat(BootstrapPasswordPolicy.isAcceptable("Abc123")).isFalse();
        }

        @Test
        @DisplayName("Faqat harf yoki faqat raqam yetarli emas")
        void needsBothLetterAndDigit() {
            assertThat(BootstrapPasswordPolicy.isAcceptable("abcdefghij")).isFalse();
            assertThat(BootstrapPasswordPolicy.isAcceptable("9876543210")).isFalse();
        }

        @Test
        @DisplayName("Kuchli parol qabul qilinadi")
        void strongPasswordIsAccepted() {
            assertThat(BootstrapPasswordPolicy.isAcceptable("Xk7mQp2wLz")).isTrue();
            assertThat(BootstrapPasswordPolicy.isAcceptable("uzcasting2026Master")).isTrue();
        }

        @Test
        @DisplayName("Rad sababi parolning o'zini oshkor qilmaydi")
        void reasonNeverLeaksThePassword() {
            String secret = "SirliParol";
            assertThat(BootstrapPasswordPolicy.rejectionReason(secret))
                    .doesNotContain(secret);
        }
    }

    @Nested
    @DisplayName("AutoRun manbasi")
    class Source {

        /** {@code @Value("${kalit:standart}")} — standart qiymati bor e'lonlar. */
        private static final Pattern VALUE_WITH_DEFAULT =
                Pattern.compile("@Value\\(\"\\$\\{([a-zA-Z0-9._-]+):([^}]*)}\"\\)");

        @Test
        @DisplayName("Parol xossalarida STANDART qiymat yo'q")
        void passwordPropertiesHaveNoDefault() throws IOException {
            String source = Files.readString(AUTO_RUN);
            Matcher m = VALUE_WITH_DEFAULT.matcher(source);

            List<String> offenders = new java.util.ArrayList<>();
            while (m.find()) {
                String key = m.group(1);
                String defaultValue = m.group(2);
                // Aynan SIR bo'lgan xossalar: `...password` bilan tugaydiganlar.
                // `app.bootstrap.allow-weak-password` — bayroq, sir emas,
                // uning `false` standarti to'g'ri va kerak.
                if (key.toLowerCase().endsWith(".password") && !defaultValue.isEmpty()) {
                    offenders.add(key + " = " + "(standart qiymat berilgan)");
                }
            }

            assertThat(offenders)
                    .as("AutoRun'da parol uchun standart qiymat qaytib kelgan. "
                            + "Bu har bir yangi o'rnatishda hammaga ma'lum parolli "
                            + "hisob yaratadi — jumladan HYPER_ADMIN.")
                    .isEmpty();
        }

        @Test
        @DisplayName("Parol kodga qotirib yozilmagan")
        void noHardcodedPasswordInEncoder() throws IOException {
            String source = Files.readString(AUTO_RUN);

            // passwordEncoder.encode("...") — literal bilan chaqirilmasin.
            Matcher m = Pattern.compile("passwordEncoder\\.encode\\(\"([^\"]*)\"\\)")
                    .matcher(source);

            assertThat(m.find())
                    .as("AutoRun'da parol to'g'ridan-to'g'ri kodga yozilgan. "
                            + "U konfiguratsiyadan olinishi kerak.")
                    .isFalse();
        }
    }
}
