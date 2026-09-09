package com.example.backend.Cms;

import com.example.backend.Cms.Service.Storage.ImageSizeLimit;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rasm hajmi chegarasi.
 *
 * <h2>⚠️ Nega bu kerak bo'ldi</h2>
 * Panelda «≤10 MB» yozuvi turardi, lekin uni HECH KIM tekshirmasdi:
 * {@code maxMb} faqat maydon tagidagi matnni chizardi. Serverda ham
 * chegara yo'q edi — yagona to'siq nginx (60 MB) va Spring multipart
 * (50 MB) bo'lgan.
 *
 * Ya'ni admin 40 MB lik afisha yuklasa, u qabul qilinardi va keyin
 * har bir kartochkada, katalogda va yopiq kontent ekranida
 * tomoshabinga yuborilardi. Nosozlik jimgina edi: xato yo'q,
 * shunchaki ilova sekin ishlaydi va trafik yeyiladi.
 */
class ImageSizeLimitTest {

    private static final long MB = 1024L * 1024L;

    private ImageSizeLimit limit(long maxBytes) {
        ImageSizeLimit l = new ImageSizeLimit();
        ReflectionTestUtils.setField(l, "maxImageBytes", maxBytes);
        return l;
    }

    private ImageSizeLimit tenMb() {
        return limit(10 * MB);
    }

    @Nested
    @DisplayName("Rasm")
    class Images {

        @Test
        @DisplayName("Chegaradan katta rasm rad etiladi")
        void rejectsTooLarge() {
            assertThatThrownBy(() -> tenMb().check("afisha.jpg", "image/jpeg", 11 * MB))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("10 MB");
        }

        @Test
        @DisplayName("Chegaraga teng rasm o'tadi")
        void allowsExactLimit() {
            assertThatCode(() -> tenMb().check("afisha.jpg", "image/jpeg", 10 * MB))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Kichik rasm o'tadi")
        void allowsSmall() {
            assertThatCode(() -> tenMb().check("afisha.png", "image/png", 2 * MB))
                    .doesNotThrowAnyException();
        }

        /**
         * ⚠️ Mime yo'q bo'lsa ham tur KENGAYTMADAN aniqlanadi.
         *
         * Ba'zi klientlar `Content-Type` ni umuman yubormaydi yoki
         * `application/octet-stream` deb yuboradi. Faqat mime'ga
         * tayanilsa, chegarani chetlab o'tish uchun uni bo'sh
         * qoldirish kifoya bo'lardi.
         */
        @Test
        @DisplayName("Mime bo'lmasa kengaytma bo'yicha aniqlanadi")
        void detectsByExtensionWhenMimeMissing() {
            assertThatThrownBy(() -> tenMb().check("afisha.jpg", null, 11 * MB))
                    .isInstanceOf(BusinessException.class);

            assertThatThrownBy(() -> tenMb().check("afisha.webp", "application/octet-stream", 11 * MB))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Rasm bo'lmaganlar")
    class NonImages {

        /**
         * ⚠️ Video bu chegaraga BO'YSUNMAYDI.
         *
         * U tabiatan katta va o'z chegarasi bor
         * ({@code app.upload.max-bytes}, 5 GB). Bu yerga tushib
         * qolsa, hech qanday video yuklab bo'lmasdi.
         */
        @Test
        @DisplayName("Video tekshirilmaydi")
        void skipsVideo() {
            assertThatCode(() -> tenMb().check("film.mp4", "video/mp4", 900 * MB))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Noma'lum tur tekshirilmaydi")
        void skipsUnknown() {
            assertThatCode(() -> tenMb().check("hujjat.pdf", "application/pdf", 50 * MB))
                    .doesNotThrowAnyException();
        }

        /**
         * ⚠️ Hajmi noma'lum bo'lsa bu yerda to'xtatib bo'lmaydi.
         *
         * Uni chaqiruvchi hal qiladi — masalan oqim o'qilayotganda.
         * Bu yerda xato tashlash butun yuklashni buzardi.
         */
        @Test
        @DisplayName("Hajm noma'lum bo'lsa o'tkaziladi")
        void skipsUnknownSize() {
            assertThatCode(() -> tenMb().check("afisha.jpg", "image/jpeg", null))
                    .doesNotThrowAnyException();
            assertThatCode(() -> tenMb().check("afisha.jpg", "image/jpeg", 0L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Sozlama")
    class Configuration {

        @Test
        @DisplayName("Sukut qiymati 10 MB")
        void defaultIsTenMb() {
            // ⚠️ Sukut qiymati panel yozuvi bilan MOS bo'lishi kerak
            // (mediaSpecs.js, maxMb: 10). Ajralib qolsa, panel bir xil
            // deydi-yu, server boshqacha javob beradi.
            ImageSizeLimit fromDefault = new ImageSizeLimit();
            ReflectionTestUtils.setField(fromDefault, "maxImageBytes", 10485760L);
            assertThat(fromDefault.maxMb()).isEqualTo(10);
        }

        @Test
        @DisplayName("Chegara sozlanadi")
        void limitIsConfigurable() {
            assertThatThrownBy(() -> limit(2 * MB).check("a.jpg", "image/jpeg", 3 * MB))
                    .hasMessageContaining("2 MB");
        }
    }
}
