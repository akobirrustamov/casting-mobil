package com.example.backend.Cms;

import com.example.backend.Cms.Service.Storage.StorageStatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ombor hisobotining KALIT MANTIQI.
 *
 * <h2>⚠️ Nima uchun bu testga arziydi</h2>
 * Hisobot adminni HARAKATGA undaydi: «bu fayl hech qayerda
 * ishlatilmayapti» degan qator o'chirish tugmasi yonida turadi.
 *
 * Ya'ni noto'g'ri hisob — yo'qolgan kontent. Bu «raqam biroz
 * noto'g'ri» turidagi xato emas.
 *
 * <h2>⚠️ ENG NOZIK JOY — HLS papkasi</h2>
 * Transkodlangan video omborda BITTA kalit emas, yuzlab fayl:
 *
 * <pre>
 *   videos/146/hls/master.m3u8
 *   videos/146/hls/480p/segment_00001.m4s
 *   ...
 * </pre>
 *
 * Bazada esa faqat `hlsMasterKey` bor. Agar solishtirish oddiy
 * «kalit bazada bormi» bo'lsa, HAR BIR SEGMENT yetim deb
 * ko'rsatilardi — 192 ta fayl «o'chirsa bo'ladi» ro'yxatiga
 * tushardi va admin ishlab turgan videoni yo'q qilardi.
 *
 * ⚠️ Bu test Spring kontekstini KO'TARMAYDI: mantiq sof funksiya
 * va uni tekshirish uchun baza ham, S3 ham kerak emas.
 */
class StorageStatsTest {

    /** Servis bin sifatida emas, oddiy obyekt sifatida sinaladi. */
    private final StorageStatsService service =
            new StorageStatsService(null, null, null, null);

    private Long hlsMediaId(String key) {
        return ReflectionTestUtils.invokeMethod(service, "mediaIdOfHls", key);
    }

    private String normalize(String key) {
        return ReflectionTestUtils.invokeMethod(service, "normalize", key);
    }

    @Nested
    @DisplayName("⚠️ HLS papkasi media'ga bog'lanadi")
    class HlsFolder {

        /**
         * ENG MUHIM TEKSHIRUV — segment o'z media'siga tegishli
         * deb tanilsin.
         */
        @Test
        @DisplayName("Segment media raqamini beradi")
        void segmentResolvesToMedia() {
            assertThat(hlsMediaId("videos/146/hls/480p/segment_00001.m4s"))
                    .isEqualTo(146L);
            assertThat(hlsMediaId("videos/7/hls/master.m3u8"))
                    .isEqualTo(7L);
        }

        /**
         * ⚠️ Raqam bo'lmagan papka HECH QAYSI media'ga tegishli emas.
         *
         * Testlardan qolgan `videos/hls-test-8/...` kabi papkalar
         * aynan shunday ko'rinadi va ular haqiqatan yetim.
         *
         * Busiz `Long.parseLong` istisno tashlab, butun skanerlashni
         * yiqitardi.
         */
        @Test
        @DisplayName("Raqamsiz papka — media emas")
        void nonNumericFolderIsNotMedia() {
            assertThat(hlsMediaId("videos/hls-test-8/hls/master.m3u8")).isNull();
            assertThat(hlsMediaId("videos//hls/master.m3u8")).isNull();
        }

        /** `videos/` dan tashqarisi bu qoidaga tushmaydi. */
        @Test
        @DisplayName("Boshqa papka — null")
        void otherFolderIsNull() {
            assertThat(hlsMediaId("content/abc.mp4")).isNull();
            assertThat(hlsMediaId("videos")).isNull();
        }
    }

    @Nested
    @DisplayName("⚠️ Kalit shakli")
    class KeyShape {

        /**
         * ⚠️ Baza kalitni `/content/x.mp4` deb saqlaydi, S3 esa
         * `content/x.mp4` deb qaytaradi.
         *
         * Solishtirishdan oldin tenglashtirilmasa, BITTA fayl ham
         * mos kelmasdi va butun ombor «yetim» bo'lib ko'rinardi.
         */
        @Test
        @DisplayName("Boshidagi qiyshiq chiziq tenglashtiriladi")
        void leadingSlashNormalised() {
            assertThat(normalize("/content/a.mp4")).isEqualTo("content/a.mp4");
            assertThat(normalize("content/a.mp4")).isEqualTo("content/a.mp4");
        }

        @Test
        @DisplayName("null xavfsiz")
        void nullIsSafe() {
            assertThat(normalize(null)).isEmpty();
        }
    }
}
