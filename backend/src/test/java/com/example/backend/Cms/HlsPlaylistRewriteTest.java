package com.example.backend.Cms;

import com.example.backend.Cms.Service.StorageService;
import com.example.backend.Cms.Service.Video.HlsPlaylistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Playlist qayta yozilishi — sof mantiq.
 *
 * <h2>Nega alohida test</h2>
 * Bu yerda XATO jimgina bo'ladi: playlist to'g'ri ko'rinadi, HTTP 200
 * qaytadi, lekin pleyer bitta yo'lni topa olmay video ochilmaydi.
 * Ayniqsa {@code #EXT-X-MAP} — u oddiy qatorga o'xshamaydi va uni
 * unutish oson.
 */
class HlsPlaylistRewriteTest {

    private final StorageService storage = mock(StorageService.class);
    private final HlsPlaylistService service = new HlsPlaylistService(storage);

    private void given(String body) {
        Resource resource = new ByteArrayResource(body.getBytes(StandardCharsets.UTF_8));
        when(storage.load(anyString())).thenReturn(resource);
    }

    /** Kalitni ko'rinadigan belgi bilan o'raydi — nima almashganini aniq ko'rsatadi. */
    private String rewrite(String key) {
        return service.rewrite(key, k -> "[" + k + "]");
    }

    @Nested
    @DisplayName("Master playlist")
    class Master {

        @Test
        @DisplayName("Variant yo'llari to'liq kalitga aylanadi")
        void variantsBecomeKeys() {
            given("""
                    #EXTM3U
                    #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=854x480
                    480p/index.m3u8
                    #EXT-X-STREAM-INF:BANDWIDTH=2400000,RESOLUTION=1280x720
                    720p/index.m3u8
                    """);

            assertThat(rewrite("/videos/7/hls/master.m3u8"))
                    .contains("[/videos/7/hls/480p/index.m3u8]")
                    .contains("[/videos/7/hls/720p/index.m3u8]");
        }

        /**
         * ⚠️ {@code #EXT-X-STREAM-INF} qatorida {@code RESOLUTION=1280x720}
         * bor va u yo'lga o'xshab tuyuladi.
         *
         * Metama'lumot almashtirilsa pleyer playlistni umuman o'qiy
         * olmasdi — sintaksis buzilardi.
         */
        @Test
        @DisplayName("Metama'lumot qatorlari TEGILMAYDI")
        void metadataUntouched() {
            given("""
                    #EXTM3U
                    #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=854x480
                    480p/index.m3u8
                    """);

            assertThat(rewrite("/videos/7/hls/master.m3u8"))
                    .contains("#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=854x480");
        }
    }

    @Nested
    @DisplayName("Variant playlist")
    class Variant {

        /**
         * ⚠️ ENG OSON UNUTILADIGAN QATOR.
         *
         * {@code init.mp4} fMP4 segmentlarini dekodlash uchun kerak.
         * U almashtirilmasa pleyer uni CDN ildizidan izlaydi, topa
         * olmaydi va video UMUMAN ochilmaydi — playlistning qolgan
         * qismi butunlay to'g'ri bo'lsa ham.
         */
        @Test
        @DisplayName("`#EXT-X-MAP:URI` ham almashtiriladi")
        void extXMapRewritten() {
            given("""
                    #EXTM3U
                    #EXT-X-MAP:URI="init.mp4"
                    #EXTINF:6.000,
                    segment_00001.m4s
                    """);

            String out = rewrite("/videos/7/hls/720p/index.m3u8");

            assertThat(out).contains("#EXT-X-MAP:URI=\"[/videos/7/hls/720p/init.mp4]\"");
            assertThat(out).contains("[/videos/7/hls/720p/segment_00001.m4s]");
        }

        @Test
        @DisplayName("`#EXTINF` davomiyligi saqlanadi")
        void durationsKept() {
            given("""
                    #EXTM3U
                    #EXTINF:6.000,
                    segment_00001.m4s
                    #EXT-X-ENDLIST
                    """);

            assertThat(rewrite("/videos/7/hls/720p/index.m3u8"))
                    .contains("#EXTINF:6.000,")
                    .contains("#EXT-X-ENDLIST");
        }
    }

    @Nested
    @DisplayName("Chekka holatlar")
    class Edges {

        /**
         * ⚠️ Playlistda tashqi havola bo'lishi mumkin (reklama oqimi).
         * Uni kalit deb talqin qilish manzilni buzardi.
         */
        @Test
        @DisplayName("Mutlaq manzil kalitga AYLANTIRILMAYDI")
        void absoluteUrlPassedThrough() {
            given("""
                    #EXTM3U
                    https://ads.example.com/pre-roll.m3u8
                    """);

            assertThat(rewrite("/videos/7/hls/master.m3u8"))
                    .contains("[https://ads.example.com/pre-roll.m3u8]")
                    .doesNotContain("/videos/7/hls/https");
        }

        @Test
        @DisplayName("Bo'sh qatorlar joyida qoladi")
        void blankLinesKept() {
            given("#EXTM3U\n\n480p/index.m3u8\n");

            assertThat(rewrite("/videos/7/hls/master.m3u8").split("\n"))
                    .contains("");
        }

        /**
         * Playlist ildizda yotsa {@code parentOf} bo'sh qaytaradi va
         * qo'shilish {@code /480p/…} kabi noto'g'ri kalit yasab
         * qo'yishi mumkin edi.
         */
        @Test
        @DisplayName("Papkasiz kalit ham buzilmaydi")
        void keyWithoutDirectory() {
            given("#EXTM3U\n480p/index.m3u8\n");

            assertThat(rewrite("master.m3u8")).contains("[480p/index.m3u8]");
        }
    }
}
