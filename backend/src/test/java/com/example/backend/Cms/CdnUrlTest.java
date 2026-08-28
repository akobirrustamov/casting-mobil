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

        private static final Path PLAYBACK_URL_SERVICE = Path.of(
                "src/main/java/com/example/backend/Cms/Service/Video/PlaybackUrlService.java");

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

        /**
         * ⚠️ Manzil TANLASH mantig'i {@code PlaybackUrlService} ga
         * ko'chdi (§4.10): endi u yerda ikkita yo'l bor — himoyalangan
         * proksi va to'g'ridan CDN.
         *
         * Qoida esa o'zgarmadi: qaysi yo'l tanlansa ham natija AYNAN
         * {@code hlsUrl} maydoniga tushadi, {@code url} ga hech qachon.
         */
        @Test
        @DisplayName("Manzil AYRIM `hlsUrl` maydoniga yoziladi")
        void cdnGoesToItsOwnField() throws Exception {
            String source = Files.readString(WATCH_CONTROLLER);

            assertThat(source).contains(".hlsUrl(playbackUrlService.hlsUrlFor(");

            // Kontroller CDN'ni endi o'zi bilmaydi — bilsa, qaror ikki
            // joyga bo'linib, ular vaqt o'tib ajralib ketardi.
            assertThat(source)
                    .as("kontroller CDN manzilini yana o'zi yasamoqda")
                    .doesNotContain("cdnUrlService");

            assertThat(Files.readString(PLAYBACK_URL_SERVICE))
                    .as("CDN yo'li yo'qolgan — S3'siz muhitda HLS umuman berilmasdi")
                    .contains("cdnUrlService.masterUrl(");
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
                    .as("mobil `hlsUrl` ni o'qimaydi — HLS ishlatilmaydi")
                    .contains("source.hlsUrl");

            // ⚠️ `hlsUrl` nisbiy ham bo'lishi mumkin (himoyalangan
            // proksi, §4.10). Farq birinchi belgidan aniqlanadi.
            assertThat(player)
                    .as("nisbiy `hlsUrl` ga BASE_URL qo'shilmayapti — proksi ochilmasdi")
                    .contains("source.hlsUrl.startsWith('/')");
        }

        /**
         * ⚠️ HLS yo'liga {@code Authorization} sarlavhasi YUBORILMAYDI.
         *
         * Bu keshlash haqida emas. AVPlayer va ExoPlayer sarlavhalarni
         * BUTUN oqim uchun beradi — ular segment so'roviga ham
         * qo'shilardi. Segment esa imzolangan havola bilan
         * to'g'ridan-to'g'ri ombordan keladi, S3 esa ikkita
         * avtorizatsiyani birga qabul qilmaydi va 400 qaytaradi.
         *
         * Ya'ni sarlavha qo'shilsa video UMUMAN ochilmasdi.
         *
         * Eski {@code url} yo'lida esa u MAJBURIY — server ruxsatni
         * o'sha sarlavha orqali tekshiradi.
         */
        @Test
        @DisplayName("Token faqat ESKI yo'lga yuboriladi, HLS'ga emas")
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

            int legacyBranch = body.indexOf("${BASE_URL}${source.url}");
            assertThat(legacyBranch).as("eski shox yo'q").isGreaterThan(0);

            // HLS shoxi eski shoxdan OLDIN tugaydi, ya'ni undan
            // oldingi qismda sarlavha bo'lmasligi kerak.
            assertThat(body.substring(0, legacyBranch))
                    .as("`authHeaders()` HLS shoxida — segment so'rovini buzardi")
                    .doesNotContain("authHeaders()");

            assertThat(body).as("eski yo'lda sarlavha yo'q — pullik video 404 berardi")
                    .contains("authHeaders()");
        }
    }
}
