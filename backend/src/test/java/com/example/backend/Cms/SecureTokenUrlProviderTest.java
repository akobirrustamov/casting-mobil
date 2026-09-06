package com.example.backend.Cms;

import com.example.backend.Cms.Service.Video.CdnUrlService;
import com.example.backend.Cms.Service.Video.SecureTokenUrlProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Timeweb CDN «Secure token» havolalari.
 *
 * <h2>⚠️ Nega bu test etalon qiymatlarga tayanadi</h2>
 * Imzoni faqat CDN tekshiradi, biz emas. Ya'ni xato qilsak
 * kompilyator ham, Spring ham, hech qanday test ham gapirmasdi —
 * nosozlik faqat SERVERDA, barcha video 403 bo'lganda ko'rinardi.
 *
 * Shuning uchun quyidagi kutilgan satrlar Timeweb hujjatidagi Python
 * namunasi bilan hisoblangan va shu yerga YOZIB qo'yilgan. Java kodi
 * o'sha natijani takrorlamasa, test yiqiladi.
 *
 * <pre>
 *   raw = SECRET + PATH + IP + EXPIRES
 *   md5 → xom baytlar → base64 → (+ → -, / → _, = olib tashlanadi)
 * </pre>
 *
 * <h2>Etalonni qayta hisoblash</h2>
 * <pre>
 *   python3 -c "import hashlib,base64
 *   d=hashlib.md5(b's3cr3t/videos/146/hls/master.m3u81767225600').digest()
 *   print(base64.b64encode(d).decode().replace('+','-').replace('/','_').replace('=',''))"
 * </pre>
 */
class SecureTokenUrlProviderTest {

    private static final String SECRET = "s3cr3t";
    private static final String CDN = "https://cdn.uzcasting.com";

    /** Oyna chegarasiga to'g'ri keladigan vaqt — muddat oldindan ma'lum. */
    private static final long EXPIRES = 1767225600L;

    private SecureTokenUrlProvider provider(String cdnBase, String secret) {
        CdnUrlService cdn = new CdnUrlService();
        ReflectionTestUtils.setField(cdn, "cdnBaseUrl", cdnBase);

        SecureTokenUrlProvider p = new SecureTokenUrlProvider(cdn);
        ReflectionTestUtils.setField(p, "secret", secret);
        ReflectionTestUtils.setField(p, "ttl", Duration.ofHours(4));
        ReflectionTestUtils.setField(p, "window", Duration.ofHours(1));
        return p;
    }

    /** Manzildan token va muddatni ajratib oladi. */
    private static Matcher parse(String url) {
        Matcher m = Pattern.compile("^(.*)/md5\\(([^,]+),(\\d+)\\)(/.*)$").matcher(url);
        assertThat(m.matches())
                .as("manzil shakli: {CDN}/md5(token,expires)/yo'l — aslida: " + url)
                .isTrue();
        return m;
    }

    // ------------------------------------------------------------- algoritm

    @Nested
    @DisplayName("Algoritm")
    class Algorithm {

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV.
         *
         * Token Timeweb hujjatidagi Python namunasi bilan bir xil
         * chiqishi shart. Bir belgi farq qilsa CDN 403 beradi.
         */
        @Test
        @DisplayName("Token hujjatdagi namuna bilan bir xil")
        void tokenMatchesReference() {
            var p = provider(CDN, SECRET);

            record Case(String path, String expected) {}
            var cases = new Case[]{
                    new Case("/videos/146/hls/master.m3u8", "CUDh1k_QtfzDbLXgEshePg"),
                    new Case("/videos/146/hls/480p/segment_00000.m4s", "TQTSp_lvgE3oj3wwaXTmNQ"),
                    // ⚠️ Bu yo'lda base64 natijasida HAM `-`, HAM `_`
                    // uchraydi — ikkala almashtirish ham sinaladi.
                    new Case("/videos/22/hls/master.m3u8", "oP2dY-x1EcTV2_DtP6M3vg"),
            };

            for (Case c : cases) {
                String token = (String) ReflectionTestUtils.invokeMethod(
                        p, "token", c.path(), EXPIRES);
                assertThat(token)
                        .as("yo'l: " + c.path())
                        .isEqualTo(c.expected());
            }
        }

        /**
         * ⚠️ Base64 to'ldiruvchisi tushirilishi shart.
         *
         * MD5 har doim 16 bayt, base64 esa 24 belgi beradi va oxirgi
         * ikkitasi `==` bo'ladi. Ular qolsa manzilda `=` paydo
         * bo'lardi va CDN tokenni tanimasdi.
         */
        @Test
        @DisplayName("Tokenda to'ldiruvchi va xavfli belgi yo'q")
        void tokenIsUrlSafe() {
            var p = provider(CDN, SECRET);

            String token = (String) ReflectionTestUtils.invokeMethod(
                    p, "token", "/videos/146/hls/master.m3u8", EXPIRES);

            assertThat(token).doesNotContain("=", "+", "/");
            assertThat(token).hasSize(22);
        }

        /**
         * ⚠️ MD5 dan XOM BAYTLAR olinadi, o'n oltilik satr emas.
         *
         * Farqi ko'rinmaydi: ikkalasi ham o'xshash uzunlikdagi token
         * beradi. Lekin o'n oltilik bilan CDN har bir segmentga 403
         * qaytarardi — ya'ni xato faqat serverda ko'rinardi.
         */
        @Test
        @DisplayName("O'n oltilik emas, xom baytlardan")
        void hashesRawBytesNotHex() {
            var p = provider(CDN, SECRET);

            String token = (String) ReflectionTestUtils.invokeMethod(
                    p, "token", "/videos/146/hls/master.m3u8", EXPIRES);

            // O'n oltilik satr (32 belgi) base64'da 44 belgi berardi.
            assertThat(token).hasSize(22);
        }
    }

    // --------------------------------------------------------------- manzil

    @Nested
    @DisplayName("Manzil shakli")
    class UrlShape {

        @Test
        @DisplayName("Hujjatdagi ko'rinishda yig'iladi")
        void buildsDocumentedShape() {
            var p = provider(CDN, SECRET);

            String url = p.sign("/videos/146/hls/master.m3u8");

            Matcher m = parse(url);
            assertThat(m.group(1)).isEqualTo(CDN);
            assertThat(m.group(4)).isEqualTo("/videos/146/hls/master.m3u8");
        }

        /**
         * ⚠️ Imzo AYNAN manzildagi yo'l ustidan hisoblanadi.
         *
         * Kalit `/` siz kelsa ham imzo va manzil bir xil satrni
         * ishlatishi shart. Farq qilsa CDN 403 berardi.
         */
        @Test
        @DisplayName("Boshida qiyshiq chiziq bo'lmasa ham bir xil")
        void normalisesLeadingSlash() {
            var p = provider(CDN, SECRET);

            String withSlash = p.sign("/videos/146/hls/master.m3u8");
            String without = p.sign("videos/146/hls/master.m3u8");

            assertThat(without).isEqualTo(withSlash);
        }

        /** CDN manzili oxiridagi qiyshiq chiziq ikkilanmasin. */
        @Test
        @DisplayName("CDN manzili oxiridagi chiziq ikkilanmaydi")
        void doesNotDoubleSlash() {
            var p = provider(CDN + "/", SECRET);

            assertThat(p.sign("/videos/1/hls/master.m3u8"))
                    .doesNotContain("//md5")
                    .startsWith(CDN + "/md5(");
        }
    }

    // ----------------------------------------------------------- keshlanish

    @Nested
    @DisplayName("Keshlanish")
    class Caching {

        /**
         * ⚠️ Butun kesh strategiyasi shunga tayanadi.
         *
         * Har chaqiruv boshqa muddat bersa, har tomoshabin BOSHQA
         * manzil olardi. CDN uchun bu boshqa fayl degani: 3000 kishi
         * bitta filmni ko'rsa kesh umuman ishlamasdi va butun trafik
         * omborga tushardi.
         */
        @Test
        @DisplayName("Bir xil kalit bir xil manzil beradi")
        void sameKeyGivesSameUrl() {
            var p = provider(CDN, SECRET);

            assertThat(p.sign("/videos/146/hls/master.m3u8"))
                    .isEqualTo(p.sign("/videos/146/hls/master.m3u8"));
        }

        @Test
        @DisplayName("Boshqa kalit boshqa token beradi")
        void differentKeyGivesDifferentToken() {
            var p = provider(CDN, SECRET);

            String a = parse(p.sign("/videos/146/hls/master.m3u8")).group(2);
            String b = parse(p.sign("/videos/147/hls/master.m3u8")).group(2);

            assertThat(a).isNotEqualTo(b);
        }

        /**
         * ⚠️ Muddat KELAJAKDA bo'lishi shart va TTL dan qisqa emas.
         *
         * Qisqa bo'lsa uzun filmni ko'rayotgan odam o'rtasida uzilib
         * qolardi: pleylistdagi havolalar eskirardi.
         */
        @Test
        @DisplayName("Muddat kamida TTL cha kelajakda")
        void expiryIsAtLeastTtlAhead() {
            var p = provider(CDN, SECRET);
            long now = java.time.Instant.now().getEpochSecond();

            long expires = Long.parseLong(parse(p.sign("/videos/1/x.m4s")).group(3));

            assertThat(expires - now)
                    .isGreaterThanOrEqualTo(Duration.ofHours(4).toSeconds())
                    .isLessThanOrEqualTo(Duration.ofHours(5).toSeconds());
        }
    }

    // ------------------------------------------------------------ yoqilishi

    @Nested
    @DisplayName("Yoqilishi")
    class Availability {

        @Test
        @DisplayName("Kalit va CDN bo'lsa ishlaydi")
        void availableWhenConfigured() {
            assertThat(provider(CDN, SECRET).isAvailable()).isTrue();
        }

        /**
         * ⚠️ Kalit bor, CDN manzili yo'q — sozlama xatosi.
         *
         * Bunda token yasab bo'lmaydi. `false` qaytarilsa tizim
         * imzosiz ishlashda davom etadi; `true` qaytsa video umuman
         * ochilmay qolardi.
         */
        @Test
        @DisplayName("CDN manzilisiz ishlamaydi")
        void unavailableWithoutCdn() {
            assertThat(provider("", SECRET).isAvailable()).isFalse();
        }

        @Test
        @DisplayName("Kalitsiz ishlamaydi")
        void unavailableWithoutSecret() {
            assertThat(provider(CDN, "").isAvailable()).isFalse();
        }
    }
}
