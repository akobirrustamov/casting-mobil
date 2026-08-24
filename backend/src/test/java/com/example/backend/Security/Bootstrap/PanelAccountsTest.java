package com.example.backend.Security.Bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Panel hisoblari AutoRun orqali yaratiladi.
 *
 * <h2>Nega kerak edi</h2>
 * AutoRun faqat ikkita MASTER hisob yaratardi
 * ({@code gipersuperadmin}, {@code superadmin}) va eski casting
 * adminlarini. Panel uchun ADMIN va WORKER yo'q edi — ya'ni bu
 * rollarni sinash uchun ularni qo'lda, baza orqali qo'shishga to'g'ri
 * kelardi.
 */
class PanelAccountsTest {

    private static final Path AUTORUN =
            Path.of("src/main/java/com/example/backend/Config/AutoRun.java");

    @Nested
    @DisplayName("To'rtta rol ham qamrab olingan")
    class AllRoles {

        @Test
        @DisplayName("Har bir rol uchun hisob yaratiladi")
        void everyRoleHasAnAccount() throws IOException {
            String src = Files.readString(AUTORUN);

            assertThat(src).contains("UserRoles.ROLE_GIPERSUPERADMIN");
            assertThat(src).contains("UserRoles.ROLE_SUPERADMIN");
            assertThat(src)
                    .as("panel ADMIN hisobi - ilgari yo'q edi")
                    .contains("ensurePanelUser(adminPhone, adminPassword, UserRoles.ROLE_ADMIN)");
            assertThat(src)
                    .as("panel WORKER hisobi - ilgari yo'q edi")
                    .contains("ensurePanelUser(workerPhone, workerPassword, UserRoles.ROLE_WORKER)");
        }

        /**
         * ⚠️ Bu xato haqiqatda yuz berdi.
         *
         * {@code run()} ichida {@code String adminPhone = "admin1234"}
         * degan lokal o'zgaruvchi bor edi va u shu nomdagi MAYDONNI
         * soya qilardi. Natijada {@code ensurePanelUser} sozlamadagi
         * telefonni emas, {@code admin1234} ni olardi — u esa bir necha
         * qator yuqorida allaqachon yaratilgan.
         *
         * Hisob jimgina yaratilmasdi: «allaqachon mavjud» deb
         * hisoblanardi va logga hech narsa yozilmasdi. Buni faqat
         * kirishga urinib ko'rgandagina sezish mumkin edi.
         */
        @Test
        @DisplayName("Maydon lokal o'zgaruvchi bilan soya qilinmagan")
        void configuredPhoneIsNotShadowed() throws IOException {
            String src = Files.readString(AUTORUN);

            for (String field : List.of("adminPhone", "workerPhone")) {
                assertThat(src)
                        .as(field + " lokal o'zgaruvchi sifatida qayta e'lon qilinmasin")
                        .doesNotContain("String " + field + " =");
            }
        }

        @Test
        @DisplayName("Login va parol sozlamadan olinadi")
        void credentialsComeFromConfiguration() throws IOException {
            String src = Files.readString(AUTORUN);

            for (String key : List.of(
                    "app.admin.phone", "app.admin.password",
                    "app.worker.phone", "app.worker.password")) {
                assertThat(src).as(key).contains(key);
            }
        }
    }

    @Nested
    @DisplayName("Xavfsizlik")
    class Safety {

        /**
         * ⚠️ Parol qotirilmasin.
         *
         * Standart parolli admin hisobi ochiq eshikdan YOMONROQ:
         * uni hech kim ko'rmaydi va u yillab qolib ketadi.
         */
        @Test
        @DisplayName("Parolning standart qiymati bo'sh")
        void passwordHasNoDefault() throws IOException {
            String src = Files.readString(AUTORUN);

            // `${app.admin.password:}` — ikki nuqtadan keyin hech narsa.
            assertThat(src).contains("${app.admin.password:}");
            assertThat(src).contains("${app.worker.password:}");
        }

        /**
         * ⚠️ Faqat REPOZITORIYGA TUSHADIGAN fayl tekshiriladi.
         *
         * Lokal {@code application.properties} {@code .gitignore} da:
         * u dasturchining shaxsiy sozlamasi va u yerdagi parol hech
         * qachon repozitoriyga tushmaydi. Uni tekshirish dasturchini
         * o'z mashinasida ishlaydigan sozlama yozishdan to'sardi.
         *
         * Agar kimdir faylni {@code .gitignore} dan chiqarsa,
         * tekshiruv yana ishlaydi.
         */
        @Test
        @DisplayName("Repozitoriyga tushadigan sozlamada parol yozilmagan")
        void trackedConfigHasNoPassword() throws IOException {
            Path prod = Path.of("src/main/resources/application.properties");
            if (!Files.exists(prod) || isGitIgnored("application.properties")) {
                return;
            }

            for (String line : Files.readString(prod).split("\n")) {
                String t = line.trim();
                if (t.startsWith("#") || !t.contains("password=")) {
                    continue;
                }
                String value = t.substring(t.indexOf('=') + 1).trim();
                assertThat(value)
                        .as("qiymat bo'sh yoki environmentdan bo'lsin: " + t)
                        .matches("^$|^\\$\\{.*}$");
            }
        }

        private static boolean isGitIgnored(String fileName) throws IOException {
            Path ignore = Path.of("../.gitignore");
            if (!Files.exists(ignore)) {
                return false;
            }
            return Files.readAllLines(ignore).stream()
                    .map(String::trim)
                    .anyMatch(l -> !l.startsWith("#") && l.endsWith(fileName));
        }

        /**
         * WORKER hech qanday ruxsatsiz yaratiladi.
         *
         * ⚠️ Bu ataylab: ТЗ §12 bo'yicha ruxsatni Admin yoki
         * SuperAdmin beradi va bu amal auditga tushadi. Avtomatik
         * berish o'sha izni hech kim bermagan holga keltirardi.
         */
        @Test
        @DisplayName("Worker avtomatik ruxsat olmaydi")
        void workerGetsNoAutomaticPermissions() throws IOException {
            String src = Files.readString(AUTORUN);

            assertThat(src)
                    .as("AutoRun ruxsat bermasligi kerak")
                    .doesNotContain("replacePermissions")
                    .doesNotContain("UserPermission");
        }
    }

    @Test
    @DisplayName("Namuna faylda haqiqiy parol yo'q")
    void exampleFileHasNoRealPassword() throws IOException {
        String example = Files.readString(
                Path.of("src/main/resources/application.properties.example"));

        for (String key : List.of("APP_ADMIN_PASSWORD", "APP_WORKER_PASSWORD")) {
            assertThat(example).contains(key + "=");
            assertThat(example)
                    .as(key + " bo'sh qoldirilsin")
                    .doesNotContain(key + "=00000000");
        }
    }
}
