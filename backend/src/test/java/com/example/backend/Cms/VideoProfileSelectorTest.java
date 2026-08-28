package com.example.backend.Cms;

import com.example.backend.Cms.Service.Video.TranscodingProfile;
import com.example.backend.Cms.Service.Video.VideoMetadata;
import com.example.backend.Cms.Service.Video.VideoProcessingException;
import com.example.backend.Cms.Service.Video.VideoProfileSelector;
import com.example.backend.Cms.Service.Video.VideoTranscodingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sifat variantlarini tanlash.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * <ul>
 *   <li>manbadan YUQORIGA cho'zilmaydi — bu disk va protsessor
 *       isrofi, sifat esa oshmaydi;</li>
 *   <li>vertikal video to'g'ri tushuniladi — 1080×1920 bu «1080p
 *       vertikal», «1920p» emas;</li>
 *   <li>o'lchamlar JUFT — H.264 toq o'lchamni qabul qilmaydi;</li>
 *   <li>har doim kamida BITTA variant — aks holda video HLS'siz
 *       qolib, pleyerda ochilmasdi.</li>
 * </ul>
 */
class VideoProfileSelectorTest {

    private VideoProfileSelector selector;

    @BeforeEach
    void setUp() {
        selector = new VideoProfileSelector(new VideoTranscodingProperties());
    }

    private VideoMetadata source(int width, int height) {
        return new VideoMetadata(width, height, 60, 25.0, "h264", "aac", 5_000_000L);
    }

    private List<String> labels(int width, int height) {
        return selector.select(source(width, height)).stream()
                .map(VideoProfileSelector.SelectedProfile::label)
                .toList();
    }

    @Nested
    @DisplayName("Gorizontal video")
    class Landscape {

        @Test
        @DisplayName("4K manba uchta variant beradi, 4K NING O'ZI yo'q")
        void ultraHdProducesLadder() {
            // Zinapoyada 2160p yo'q — u sozlamada ham yo'q, ya'ni
            // eng yuqorisi 1080p.
            assertThat(labels(3840, 2160)).containsExactly("1080p", "720p", "480p");
        }

        @Test
        @DisplayName("1080p manba uchta variant beradi")
        void fullHdProducesThree() {
            assertThat(labels(1920, 1080)).containsExactly("1080p", "720p", "480p");
        }

        @Test
        @DisplayName("720p manba 1080p GA CHO'ZILMAYDI")
        void hdDoesNotUpscale() {
            // ⚠️ Eng muhim qoida. Cho'zish yo'q piksellarni o'ylab
            // topishga urinish: fayl kattalashadi, sifat esa oshmaydi.
            assertThat(labels(1280, 720)).containsExactly("720p", "480p");
        }

        @Test
        @DisplayName("480p manba faqat bitta variant beradi")
        void sdProducesOne() {
            assertThat(labels(854, 480)).containsExactly("480p");
        }

        @Test
        @DisplayName("Profillar orasidagi manba yuqorigisini OLMAYDI")
        void inBetweenSourcePicksLower() {
            // 900p: 1080p dan kichik, 720p dan katta.
            assertThat(labels(1600, 900)).containsExactly("720p", "480p");
        }
    }

    @Nested
    @DisplayName("Vertikal video (ТЗ §19 — Reels)")
    class Portrait {

        /**
         * ⚠️ 1080×1920 — bu «1080p vertikal», «1920p» emas.
         *
         * Balandlik bo'yicha taqqoslasak, 1920 ≥ 1080 bo'lib chiqardi
         * va 1080p variant yaratilardi — u esa 607×1080, ya'ni
         * manbadan PAST sifat. Aslida manba allaqachon 1080p.
         */
        @Test
        @DisplayName("Sifat KICHIK tomon bo'yicha aniqlanadi")
        void qualityComesFromShorterSide() {
            assertThat(labels(1080, 1920)).containsExactly("1080p", "720p", "480p");
        }

        @Test
        @DisplayName("Vertikal 720p 1080p ga cho'zilmaydi")
        void portraitDoesNotUpscale() {
            assertThat(labels(720, 1280)).containsExactly("720p", "480p");
        }

        @Test
        @DisplayName("Chiqish o'lchamlari VERTIKAL qoladi")
        void outputStaysPortrait() {
            var selected = selector.select(source(1080, 1920));

            var full = selected.get(0);
            assertThat(full.width()).isLessThan(full.height());
            assertThat(full.resolution()).isEqualTo("1080x1920");

            var medium = selected.get(1);
            // 720 × (1920/1080) = 1280
            assertThat(medium.resolution()).isEqualTo("720x1280");
        }
    }

    @Nested
    @DisplayName("O'lchamlar")
    class Dimensions {

        /**
         * ⚠️ H.264 ning {@code yuv420p} formati TOQ o'lchamni qabul
         * qilmaydi: xromatik kanallar ikki barobar kichik va ular
         * butun songa bo'linishi kerak.
         *
         * Yaxlitlanmasa FFmpeg xato berardi — va bu faqat transcoding
         * paytida, allaqachon yuklangan videoda bilinardi.
         */
        @Test
        @DisplayName("Nostandart nisbatda ham o'lchamlar JUFT chiqadi")
        void dimensionsAreAlwaysEven() {
            // 1919×1079 — ataylab toq.
            for (var selected : selector.select(source(1919, 1079))) {
                assertThat(selected.width() % 2)
                        .as("%s kengligi %d", selected.label(), selected.width())
                        .isZero();
                assertThat(selected.height() % 2)
                        .as("%s balandligi %d", selected.label(), selected.height())
                        .isZero();
            }
        }

        @Test
        @DisplayName("Nisbat SAQLANADI")
        void aspectRatioIsPreserved() {
            // 21:9 keng ekran.
            var selected = selector.select(source(2560, 1080));

            var hd = selected.stream().filter(s -> s.label().equals("720p")).findFirst().orElseThrow();
            // 720 × (2560/1080) = 1706.67 → 1706 (juft)
            assertThat(hd.height()).isEqualTo(720);
            assertThat(hd.width()).isEqualTo(1706);
        }

        @Test
        @DisplayName("Kvadrat video ham ishlaydi")
        void squareVideo() {
            var selected = selector.select(source(1080, 1080));

            assertThat(selected.get(0).resolution()).isEqualTo("1080x1080");
        }
    }

    @Nested
    @DisplayName("Chekka holatlar")
    class EdgeCases {

        /**
         * ⚠️ Bo'sh ro'yxat «transcoding qilinmasin» degani bo'lardi va
         * video HLS'siz qolib, pleyerda umuman ochilmasdi.
         */
        @Test
        @DisplayName("Eng past profildan KICHIK manba ham variant oladi")
        void tinySourceStillGetsOneVariant() {
            var selected = selector.select(source(640, 360));

            assertThat(selected).hasSize(1);
            // ⚠️ O'lchamlar manbaning O'ZIDA qoladi — 480p ga
            // cho'zilmaydi.
            assertThat(selected.get(0).resolution()).isEqualTo("640x360");
        }

        @Test
        @DisplayName("Juda kichik video ham yiqilmaydi")
        void verySmallVideo() {
            var selected = selector.select(source(100, 100));

            assertThat(selected).hasSize(1);
            assertThat(selected.get(0).width()).isEqualTo(100);
        }

        @Test
        @DisplayName("O'lchamsiz manba ANIQ xato beradi")
        void unusableSourceIsRejected() {
            VideoMetadata audioOnly =
                    new VideoMetadata(null, null, 180, null, null, "mp3", null);

            assertThatThrownBy(() -> selector.select(audioOnly))
                    .isInstanceOf(VideoProcessingException.class)
                    .hasMessageContaining("o'lchamlari");
        }

        @Test
        @DisplayName("Profillar sozlanmagan bo'lsa ANIQ xato beradi")
        void emptyLadderIsRejected() {
            VideoTranscodingProperties empty = new VideoTranscodingProperties();
            empty.setProfiles(List.of());

            assertThatThrownBy(() -> new VideoProfileSelector(empty).select(source(1920, 1080)))
                    .isInstanceOf(VideoProcessingException.class)
                    .hasMessageContaining("profillari");
        }
    }

    @Nested
    @DisplayName("Sozlama")
    class Configuration {

        @Test
        @DisplayName("Tartibi buzilgan sozlama TUZATILADI")
        void ladderIsSortedRegardlessOfConfigOrder() {
            // ⚠️ master.m3u8 da variantlar sifat bo'yicha tartiblangan
            // bo'lishi kerak: ba'zi pleyerlar birinchisini sukut deb
            // oladi va u eng past sifat bo'lib qolardi.
            VideoTranscodingProperties shuffled = new VideoTranscodingProperties();
            shuffled.setProfiles(List.of(
                    new TranscodingProfile(480, "1200k", "96k"),
                    new TranscodingProfile(1080, "5000k", "128k"),
                    new TranscodingProfile(720, "2800k", "128k")));

            var selected = new VideoProfileSelector(shuffled).select(source(1920, 1080));

            assertThat(selected.stream().map(VideoProfileSelector.SelectedProfile::label))
                    .containsExactly("1080p", "720p", "480p");
        }

        @Test
        @DisplayName("Bitrate sozlamadan o'zgarishsiz o'tadi")
        void bitrateComesFromConfiguration() {
            VideoTranscodingProperties custom = new VideoTranscodingProperties();
            custom.setProfiles(List.of(new TranscodingProfile(720, "9999k", "192k")));

            var selected = new VideoProfileSelector(custom).select(source(1280, 720));

            assertThat(selected.get(0).profile().getVideoBitrate()).isEqualTo("9999k");
            assertThat(selected.get(0).profile().getAudioBitrate()).isEqualTo("192k");
        }
    }

    @Nested
    @DisplayName("⚠️ Panel bilan shartnoma (§4.13)")
    class PanelContract {

        private static final java.nio.file.Path PROBE = java.nio.file.Path.of(
                "../frontend/src/adminpanel/utils/videoProbe.js");

        /**
         * ⚠️ Panel yuklashdan OLDIN ogohlantiradi, backend esa keyin
         * profil tanlaydi. Ikkalasi BIR XIL qoidani ishlatishi shart.
         *
         * Panel balandlik bo'yicha hisoblasa, har bir oddiy vertikal
         * rolik (1080×1920) «juda katta» deb ogohlantirilardi —
         * backend esa uni tushirmasdi. Loyihada vertikal kontent
         * birinchi darajali (§19), ya'ni bu har kuni takrorlanardi va
         * ogohlantirish tezda o'qilmaydigan bo'lib qolardi.
         *
         * Bu test manba matnini o'qiydi — mo'rt ko'rinadi, lekin
         * boshqa yo'l bilan ikki tildagi bu bog'liqlikni ushlab
         * bo'lmaydi.
         */
        @Test
        @DisplayName("Panel ham QISQA tomon bo'yicha hisoblaydi")
        void panelUsesTheShortSideToo() throws java.io.IOException {
            if (!java.nio.file.Files.isRegularFile(PROBE)) {
                // Frontend papkasi bo'lmasa test o'tkazib yuboriladi —
                // backend uni talab qilmaydi.
                return;
            }
            String source = java.nio.file.Files.readString(PROBE);

            assertThat(source)
                    .as("panel qisqa tomonni olmayapti — har bir vertikal "
                            + "rolik bekorga ogohlantirilardi")
                    .contains("Math.min(size.width, size.height)");

            assertThat(source)
                    .as("chegara backend'dagi eng yuqori profildan ajralib ketgan")
                    .contains("MAX_RECOMMENDED_HEIGHT = 1080");
        }

        /**
         * Backend tomonidagi qoida — panel unga tayanadi.
         *
         * ⚠️ Bu o'zgarsa yuqoridagi test ham, panel ham yangilanishi
         * kerak.
         */
        @Test
        @DisplayName("Tik 1080×1920 TUSHIRILMAYDI")
        void verticalFullHdIsNotDownscaled() {
            var selected = selector.select(source(1080, 1920));

            assertThat(selected.get(0).resolution())
                    .as("vertikal 1080p tushirilyapti — panel ogohlantirishi ham "
                            + "noto'g'ri bo'lib qolardi")
                    .isEqualTo("1080x1920");
        }
    }
}
