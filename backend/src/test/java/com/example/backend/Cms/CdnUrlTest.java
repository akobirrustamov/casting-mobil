package com.example.backend.Cms;

import com.example.backend.Cms.Service.Video.CdnUrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CDN manzillari va mobil bilan SHARTNOMA.
 *
 * <h2>⚠️ Nima uchun bu test eng muhimlaridan biri</h2>
 * Mobil ilova {@code url} maydonini NISBIY deb hisoblaydi va uning
 * oldiga o'z {@code BASE_URL} ini qo'yadi:
 *
 * <pre>
 *   useVideoPlayer({ uri: `${BASE_URL}${source.url}` })
 * </pre>
 *
 * Agar u maydonga mutlaq CDN manzili yozilsa, natija
 * {@code https://uzcasting.sitehttps://cdn…} bo'lardi — jimgina
 * buzilish, hech qanday xato ko'rsatmasdan. Video shunchaki
 * ochilmasdi.
 *
 * Shuning uchun {@code url} TEGILMAYDI, CDN manzili esa YANGI
 * {@code hlsUrl} maydoniga yoziladi.
 */
class CdnUrlTest {

    private CdnUrlService service(String baseUrl) {
        CdnUrlService service = new CdnUrlService();
        ReflectionTestUtils.setField(service, "cdnBaseUrl", baseUrl);
        return service;
    }

    @Nested
    @DisplayName("Manzil yasash")
    class Building {

        @Test
        @DisplayName("Kalit CDN ildiziga ulanadi")
        void keyIsJoinedToBase() {
            assertThat(service("https://video.uzcasting.site")
                    .masterUrl("/videos/7/hls/master.m3u8"))
                    .isEqualTo("https://video.uzcasting.site/videos/7/hls/master.m3u8");
        }

        /**
         * ⚠️ Ikkala tomonda ham qiyshiq chiziq bo'lishi mumkin.
         * Tekshirilmasa {@code //videos/…} chiqardi — ba'zi CDN'lar
         * buni BOSHQA yo'l deb qabul qiladi va 404 beradi.
         */
        @Test
        @DisplayName("Ikki tomonlama qiyshiq chiziq BIRLASHTIRILADI")
        void doubleSlashIsAvoided() {
            assertThat(service("https://cdn.example.com/")
                    .masterUrl("/videos/7/hls/master.m3u8"))
                    .isEqualTo("https://cdn.example.com/videos/7/hls/master.m3u8");
        }

        @Test
        @DisplayName("Qiyshiq chiziqsiz kalit ham ishlaydi")
        void missingSlashIsAdded() {
            assertThat(service("https://cdn.example.com")
                    .masterUrl("videos/7/hls/master.m3u8"))
                    .isEqualTo("https://cdn.example.com/videos/7/hls/master.m3u8");
        }
    }

    @Nested
    @DisplayName("Sozlanmagan yoki tayyor bo'lmagan holat")
    class NotReady {

        /**
         * ⚠️ O'ylab topilgan manzil qaytarish pleyerni ishlamaydigan
         * havolaga yuborardi va nosozlik «video buzuq» bo'lib
         * ko'rinardi — sabab esa oddiy sozlama yetishmasligi edi.
         */
        @Test
        @DisplayName("CDN sozlanmagan bo'lsa null — o'ylab topilgan manzil EMAS")
        void unconfiguredCdnGivesNull() {
            assertThat(service("").masterUrl("/videos/7/hls/master.m3u8")).isNull();
            assertThat(service(null).masterUrl("/videos/7/hls/master.m3u8")).isNull();
            assertThat(service("   ").masterUrl("/videos/7/hls/master.m3u8")).isNull();
        }

        @Test
        @DisplayName("Transcoding tugamagan bo'lsa null")
        void missingKeyGivesNull() {
            CdnUrlService cdn = service("https://cdn.example.com");

            // `hlsMasterKey` hali yozilmagan — video navbatda yoki
            // qayta ishlanmoqda.
            assertThat(cdn.masterUrl(null)).isNull();
            assertThat(cdn.masterUrl("")).isNull();
        }

        @Test
        @DisplayName("isConfigured() haqiqatni aytadi")
        void configuredFlagIsHonest() {
            assertThat(service("https://cdn.example.com").isConfigured()).isTrue();
            assertThat(service("").isConfigured()).isFalse();
            assertThat(service(null).isConfigured()).isFalse();
        }
    }

    @Nested
    @DisplayName("⚠️ Mobil bilan shartnoma")
    class MobileContract {

        private static final Path WATCH_CONTROLLER = Path.of(
                "src/main/java/com/example/backend/Cms/Controller/WatchController.java");

        /**
         * ⚠️ Pleyer AJRATILGAN faylga ko'chgan.
         *
         * Ilgari bu yo'l {@code WatchDetail.tsx} ga qarardi. Hamkasb
         * pleyerni {@code Player.tsx} ga ajratgach, merge mening HLS
         * mantig'imni yo'qotdi — va aynan SHU TEST buni ushladi.
         *
         * Ya'ni manba matnini o'qiydigan test mo'rt ko'rinadi, lekin
         * u yo'qolgan o'zgarishni topdi. Uni saqlaymiz.
         */
        private static final Path MOBILE_PLAYER = Path.of(
                "../mobile/src/features/watch/Player.tsx");

        /**
         * ⚠️ `url` maydoni NISBIY qolishi SHART.
         *
         * Mobil uning oldiga `BASE_URL` qo'yadi. Mutlaq manzil
         * yozilsa `https://uzcasting.sitehttps://cdn…` chiqardi va
         * video jimgina ochilmasdi.
         */
        @Test
        @DisplayName("`url` NISBIY yo'l bo'lib qoladi")
        void urlStaysRelative() throws Exception {
            String source = Files.readString(WATCH_CONTROLLER);

            // `url` faqat `/api/v1/app/media/...` shaklida yasaladi.
            assertThat(source).contains(".url(\"/api/v1/app/media/\"");
            // Va unga hech qachon CDN manzili berilmaydi.
            assertThat(source)
                    .as("`url` ga CDN manzili berilgan — mobil uni BASE_URL bilan birlashtiradi")
                    .doesNotContain(".url(cdnUrlService");
        }

        @Test
        @DisplayName("CDN manzili AYRIM `hlsUrl` maydoniga yoziladi")
        void cdnGoesToItsOwnField() throws Exception {
            String source = Files.readString(WATCH_CONTROLLER);

            assertThat(source).contains(".hlsUrl(cdnUrlService.masterUrl(");
        }

        /**
         * ⚠️ Mobil IKKALA yo'lni ham qo'llab-quvvatlashi shart.
         *
         * `hlsUrl` bo'lsa — CDN, bo'lmasa — eski nisbiy yo'l. Faqat
         * bittasini qoldirish ikki tomonlama buzilish bo'lardi:
         *
         * - faqat `hlsUrl` → transcoding tugamagan va eski videolar
         *   umuman ochilmasdi;
         * - faqat `url` → CDN'dan foyda yo'q, butun ish behuda.
         */
        @Test
        @DisplayName("Mobil `hlsUrl` ni oladi, bo'lmasa `url` ga QAYTADI")
        void mobileSupportsBothPaths() throws Exception {
            if (!Files.isRegularFile(MOBILE_PLAYER)) {
                // Mobil papkasi mavjud bo'lmasa test o'tkazib
                // yuboriladi — backend uni talab qilmaydi.
                return;
            }
            String player = Files.readString(MOBILE_PLAYER);

            assertThat(player)
                    .as("mobil `url` ni BASE_URL bilan birlashtirishni to'xtatdi")
                    .contains("${BASE_URL}${source.url}");

            assertThat(player)
                    .as("mobil `hlsUrl` ni o'qimaydi — CDN ishlatilmaydi")
                    .contains("source.hlsUrl");
        }

        /**
         * ⚠️ CDN'ga {@code Authorization} sarlavhasi YUBORILMAYDI.
         *
         * Bitta epizodda yuzlab segment bor va har biriga ortiqcha
         * sarlavha keshlashga xalaqit beradi. Ba'zi CDN'lar esa
         * kutilmagan avtorizatsiyali so'rovni umuman rad etadi.
         *
         * Eski yo'lda esa u MAJBURIY — server ruxsatni tekshiradi.
         */
        @Test
        @DisplayName("Token faqat ESKI yo'lga yuboriladi, CDN'ga emas")
        void tokenGoesOnlyToLegacyPath() throws Exception {
            if (!Files.isRegularFile(MOBILE_PLAYER)) {
                return;
            }
            String player = Files.readString(MOBILE_PLAYER);

            // Qaror `playbackSource` da — u sof funksiya va aynan shu
            // sababdan ajratilgan.
            int start = player.indexOf("export function playbackSource");
            assertThat(start).as("`playbackSource` topilmadi").isGreaterThan(0);

            String body = player.substring(start, player.indexOf("\n}", start));

            int hlsBranch = body.indexOf("uri: source.hlsUrl");
            int legacyBranch = body.indexOf("${BASE_URL}${source.url}");
            int headers = body.indexOf("authHeaders()");

            assertThat(hlsBranch).as("HLS shoxi yo'q").isGreaterThan(0);
            assertThat(legacyBranch).as("eski shox HLS dan oldin").isGreaterThan(hlsBranch);
            assertThat(headers)
                    .as("`authHeaders()` HLS shoxida — u CDN'ga ketardi")
                    .isGreaterThan(legacyBranch);
        }
    }
}
