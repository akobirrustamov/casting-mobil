package com.example.backend.Cms;

import com.example.backend.Cms.Service.Storage.MediaContentTypes;
import com.example.backend.Cms.Service.Storage.S3Properties;
import com.example.backend.Cms.Service.Storage.StorageKeys;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Saqlash kalitlari, MIME jadvali va S3 sozlamalari.
 *
 * ⚠️ Bu testlarda HAQIQIY S3 hisob ma'lumotlari ishlatilmaydi va
 * tarmoqqa chiqilmaydi — tekshiriladigan mantiq sof hisoblash.
 */
class StorageKeysTest {

    @Nested
    @DisplayName("Kalit yasash")
    class KeyGeneration {

        @Test
        @DisplayName("Nom SERVER tomonida yasaladi — foydalanuvchi nomi yo'l bo'lmaydi")
        void userFilenameNeverBecomesPath() {
            String key = StorageKeys.newKey("../../etc/passwd.mp4", "content");

            assertThat(key).doesNotContain("..");
            assertThat(key).doesNotContain("passwd");
            assertThat(key).startsWith("/content/");
            assertThat(key).endsWith(".mp4");
        }

        @Test
        @DisplayName("Papka nomidan xavfli belgilar olib tashlanadi")
        void folderIsSanitised() {
            assertThat(StorageKeys.newKey("a.mp4", "../secret")).startsWith("/secret/");
            assertThat(StorageKeys.newKey("a.mp4", "con/tent")).startsWith("/content/");
            // Bo'sh papka — `misc`, ildizga tushib qolmasin.
            assertThat(StorageKeys.newKey("a.mp4", "")).startsWith("/misc/");
            assertThat(StorageKeys.newKey("a.mp4", null)).startsWith("/misc/");
        }

        @Test
        @DisplayName("Har chaqiruvda BOSHQA kalit — fayl ustiga yozilmaydi")
        void keysAreUnique() {
            assertThat(StorageKeys.newKey("a.mp4", "content"))
                    .isNotEqualTo(StorageKeys.newKey("a.mp4", "content"));
        }

        @Test
        @DisplayName("Ruxsat etilmagan kengaytma rad etiladi")
        void unknownExtensionRejected() {
            assertThatThrownBy(() -> StorageKeys.newKey("virus.exe", "content"))
                    .isInstanceOf(BusinessException.class);
            // Kengaytmasiz nom ham.
            assertThatThrownBy(() -> StorageKeys.newKey("noextension", "content"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Kengaytma registrga bog'liq emas")
        void extensionIsCaseInsensitive() {
            assertThat(StorageKeys.accepts("KINO.MP4")).isTrue();
            assertThat(StorageKeys.newKey("KINO.MP4", "content")).endsWith(".mp4");
        }
    }

    @Nested
    @DisplayName("MIME turlari")
    class ContentTypes {

        /**
         * ⚠️ Bu jimgina buziladigan yo'nalish.
         *
         * S3 ga `Content-Type` berilmasa u `binary/octet-stream` qo'yadi
         * va CDN ham shuni qaytaradi. Natijada brauzer HLS playlistini
         * tanimaydi, pleyer esa videoni ochish o'rniga YUKLAB oladi —
         * va buni hech qanday xato ko'rsatmaydi.
         */
        @Test
        @DisplayName("HLS formatlari to'g'ri turga ega")
        void hlsTypesAreCorrect() {
            assertThat(MediaContentTypes.of("master.m3u8"))
                    .isEqualTo("application/vnd.apple.mpegurl");
            assertThat(MediaContentTypes.of("segment_00001.m4s"))
                    .isEqualTo("video/iso.segment");
            assertThat(MediaContentTypes.of("segment_00001.ts"))
                    .isEqualTo("video/mp2t");
        }

        @Test
        @DisplayName("Ruxsat etilgan HAR BIR kengaytma uchun tur bor")
        void everyAllowedExtensionHasAType() {
            // ⚠️ Ro'yxat ataylab qo'lda yozilgan: `StorageKeys.ALLOWED` dan
            // o'qilsa test o'z-o'zini tasdiqlardi.
            for (String ext : List.of("jpg", "jpeg", "png", "webp", "gif", "svg",
                    "mp4", "mov", "webm", "m4v", "mkv", "avi", "pdf")) {
                assertThat(MediaContentTypes.of("fayl." + ext))
                        .as("`.%s` uchun MIME", ext)
                        .isNotEqualTo("application/octet-stream");
            }
        }

        @Test
        @DisplayName("Noma'lum kengaytma — oktet oqimi, xato emas")
        void unknownFallsBack() {
            assertThat(MediaContentTypes.of("fayl.xyz")).isEqualTo("application/octet-stream");
            assertThat(MediaContentTypes.of(null)).isEqualTo("application/octet-stream");
        }
    }

    @Nested
    @DisplayName("S3 sozlamalari")
    class Configuration {

        @Test
        @DisplayName("To'liq bo'lmagan sozlama YETISHMAYOTGAN maydonlarni aytadi")
        void missingFieldsAreNamed() {
            S3Properties properties = new S3Properties();
            properties.setEndpoint("https://s3.twcstorage.ru");
            properties.setBucket("uzcasting");

            assertThat(properties.isConfigured()).isFalse();
            assertThat(properties.missingFields())
                    .containsExactlyInAnyOrder(
                            "app.storage.s3.access-key",
                            "app.storage.s3.secret-key");
        }

        @Test
        @DisplayName("BO'SH satr ham «berilmagan» hisoblanadi")
        void blankCountsAsMissing() {
            S3Properties properties = configured();
            properties.setSecretKey("   ");

            // ⚠️ Environment o'zgaruvchisi mavjud, lekin qiymatsiz —
            // odatiy hol. Usiz bu jimgina autentifikatsiya xatosiga
            // olib borardi.
            assertThat(properties.isConfigured()).isFalse();
        }

        @Test
        @DisplayName("Yetishmayotgan maydonlar ro'yxatida QIYMAT bo'lmaydi")
        void missingFieldsNeverLeakValues() {
            S3Properties properties = new S3Properties();
            properties.setAccessKey("");
            properties.setSecretKey("");

            // Xato xabari logga tushadi — yarim yozilgan kalit u yerga
            // tushmasligi kerak.
            assertThat(properties.missingFields())
                    .allSatisfy(field -> assertThat(field).startsWith("app.storage.s3."));
        }

        @Test
        @DisplayName("To'liq sozlama qabul qilinadi")
        void completeConfigurationIsAccepted() {
            assertThat(configured().isConfigured()).isTrue();
        }

        private S3Properties configured() {
            S3Properties properties = new S3Properties();
            properties.setEndpoint("https://s3.twcstorage.ru");
            properties.setBucket("uzcasting");
            properties.setAccessKey("KEY");
            properties.setSecretKey("SECRET");
            return properties;
        }
    }
}
