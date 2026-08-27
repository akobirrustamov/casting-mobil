package com.example.backend.Cms;

import com.example.backend.Cms.Service.Video.TranscodingProfile;
import com.example.backend.Cms.Service.Video.VideoMetadata;
import com.example.backend.Cms.Service.Video.VideoProcessingException;
import com.example.backend.Cms.Service.Video.VideoProfileSelector;
import com.example.backend.Cms.Service.Video.VideoTranscodingProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FFmpeg buyrug'ini qurish.
 *
 * <h2>⚠️ Bu yerda FFmpeg ISHGA TUSHIRILMAYDI</h2>
 * U ishlab chiqish mashinasida o'rnatilmagan bo'lishi mumkin. Mantiq
 * esa aynan buyruqda: qaysi bayroqlar, qanday tartibda, qaysi
 * qiymatlar bilan.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * Eng muhimi — <b>segment chegaralarining moslashuvi</b>. Ularsiz ABR
 * ishlamaydi va buni faqat qurilmada, sifat almashgan paytda bilinadi.
 */
class FfmpegCommandTest {

    private static final Path SOURCE = Path.of("/tmp/video/source.mp4");
    private static final Path OUTPUT = Path.of("/tmp/video/out");

    private List<VideoProfileSelector.SelectedProfile> profiles(int width, int height) {
        return new VideoProfileSelector(new VideoTranscodingProperties())
                .select(new VideoMetadata(width, height, 3600, 25.0, "h264", "aac", 5_000_000L));
    }

    /** Buyruq quruvchi paket-xususiy — u ichki detal, API emas. */
    @SuppressWarnings("unchecked")
    private List<String> build(List<VideoProfileSelector.SelectedProfile> selected,
                               boolean hasAudio) throws Exception {
        Class<?> builder = Class.forName(
                "com.example.backend.Cms.Service.Video.FfmpegCommandBuilder");
        Method method = builder.getDeclaredMethod("build",
                String.class, Path.class, Path.class, List.class, boolean.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(null, "ffmpeg", SOURCE, OUTPUT, selected, hasAudio);
    }

    /** Bayroqdan keyingi qiymat. */
    private String valueOf(List<String> command, String flag) {
        int index = command.indexOf(flag);
        return index < 0 || index + 1 >= command.size() ? null : command.get(index + 1);
    }

    @Nested
    @DisplayName("⚠️ Segment chegaralari — ABR ning asosi")
    class KeyframeAlignment {

        /**
         * ABR ishlashi uchun BARCHA variantlarda segmentlar AYNI
         * vaqtlarda boshlanishi shart. Aks holda pleyer sifatni
         * almashtirganda kadr sakraydi yoki oqim umuman uziladi.
         *
         * ⚠️ Bu nosozlik transcoding paytida KO'RINMAYDI: fayllar
         * yaratiladi, playlist to'g'ri chiqadi, video ham ochiladi.
         * U faqat qurilmada, internet sekinlashib sifat almashgan
         * paytda bilinadi.
         */
        @Test
        @DisplayName("Kalit kadrlar QAT'IY oraliqda majburlanadi")
        void keyframesAreForcedAtFixedInterval() throws Exception {
            List<String> command = build(profiles(1920, 1080), true);

            // Kadr chastotasidan QAT'I NAZAR — `ffprobe` fps ni har
            // doim ham bermaydi, GOP ni qo'lda hisoblash esa noto'g'ri
            // fps da chegaralarni siljitardi.
            assertThat(valueOf(command, "-force_key_frames"))
                    .isEqualTo("expr:gte(t,n_forced*6)");
        }

        @Test
        @DisplayName("Sahna almashuvidagi kalit kadrlar O'CHIRILGAN")
        void sceneChangeDetectionIsDisabled() throws Exception {
            List<String> command = build(profiles(1920, 1080), true);

            // ⚠️ Yoqilgan bo'lsa FFmpeg sahna almashganda O'ZI kalit
            // kadr qo'yadi va u variantlarda turli joyda chiqadi —
            // chegaralar yana ajraladi.
            assertThat(valueOf(command, "-sc_threshold")).isEqualTo("0");
        }

        @Test
        @DisplayName("Segment uzunligi kalit kadr oralig'i bilan BIR XIL")
        void segmentDurationMatchesKeyframeInterval() throws Exception {
            List<String> command = build(profiles(1920, 1080), true);

            // Mos bo'lmasa segmentlar kalit kadrsiz joyda kesilardi.
            assertThat(valueOf(command, "-hls_time")).isEqualTo("6");
            assertThat(valueOf(command, "-force_key_frames")).contains("*6)");
        }
    }

    @Nested
    @DisplayName("Variantlar")
    class Variants {

        @Test
        @DisplayName("Manba BIR MARTA o'qiladi, har variant uchun emas")
        void sourceIsDecodedOnce() throws Exception {
            List<String> command = build(profiles(1920, 1080), true);

            // ⚠️ Uchta alohida chaqiruv bo'lsa manba UCH MARTA
            // dekodlanardi — ikki soatlik film uchun protsessor
            // vaqtining uch barobari.
            assertThat(command.stream().filter("-i"::equals)).hasSize(1);
            assertThat(valueOf(command, "-var_stream_map"))
                    .isEqualTo("v:0,a:0,name:1080p v:1,a:1,name:720p v:2,a:2,name:480p");
        }

        @Test
        @DisplayName("Papkalar SIFAT nomi bilan ataladi")
        void variantFoldersAreNamed() throws Exception {
            List<String> command = build(profiles(1920, 1080), true);

            // ⚠️ `name:` bo'lmasa papkalar 0, 1, 2 deb atalardi va S3
            // dagi kalitdan qaysi sifat ekanini bilib bo'lmasdi.
            assertThat(valueOf(command, "-var_stream_map")).contains("name:1080p");
            assertThat(valueOf(command, "-var_stream_map")).contains("name:480p");
        }

        @Test
        @DisplayName("Har variant o'z o'lchami va bitrate'ini oladi")
        void eachVariantHasItsOwnSettings() throws Exception {
            List<String> command = build(profiles(1920, 1080), true);

            assertThat(valueOf(command, "-filter:v:0")).isEqualTo("scale=1920:1080");
            assertThat(valueOf(command, "-filter:v:1")).isEqualTo("scale=1280:720");
            assertThat(valueOf(command, "-b:v:0")).isEqualTo("5000k");
            assertThat(valueOf(command, "-b:v:1")).isEqualTo("2800k");
        }

        @Test
        @DisplayName("Bitrate CHEGARALANADI, o'rtacha emas")
        void bitrateIsCapped() throws Exception {
            List<String> command = build(profiles(1920, 1080), true);

            // ⚠️ `maxrate`siz FFmpeg qiymatni o'rtacha deb qabul qiladi
            // va murakkab sahnalarda uni bir necha barobar oshirib
            // yuborishi mumkin — sekin kanalda bu uzilish demakdir.
            assertThat(valueOf(command, "-maxrate:v:0")).isEqualTo("5000k");
            assertThat(valueOf(command, "-bufsize:v:0")).isEqualTo("10000k");
        }
    }

    @Nested
    @DisplayName("Ovozsiz video")
    class WithoutAudio {

        @Test
        @DisplayName("Ovoz oqimi ULANMAYDI")
        void audioIsNotMapped() throws Exception {
            List<String> command = build(profiles(1280, 720), false);

            // ⚠️ Mavjud bo'lmagan oqimni ulashga urinish FFmpeg ni
            // "Stream map '0:a:0' matches no streams" bilan yiqitardi.
            assertThat(command).doesNotContain("0:a:0");
            assertThat(command).doesNotContain("-c:a");
        }

        @Test
        @DisplayName("Variant xaritasida ham ovoz YO'Q")
        void variantMapHasNoAudio() throws Exception {
            List<String> command = build(profiles(1280, 720), false);

            assertThat(valueOf(command, "-var_stream_map"))
                    .isEqualTo("v:0,name:720p v:1,name:480p");
        }
    }

    @Nested
    @DisplayName("HLS sozlamalari")
    class HlsSettings {

        @Test
        @DisplayName("Segmentlar SAQLANADI — o'chirilmaydi")
        void allSegmentsAreKept() throws Exception {
            List<String> command = build(profiles(1920, 1080), true);

            // ⚠️ Sukut qiymat oxirgi bir nechtasini qoldirib, qolganini
            // o'chiradi — jonli efir uchun to'g'ri, VOD uchun esa
            // fayllarning YARMI yo'qolishini bildiradi.
            assertThat(valueOf(command, "-hls_list_size")).isEqualTo("0");
            assertThat(valueOf(command, "-hls_playlist_type")).isEqualTo("vod");
        }

        @Test
        @DisplayName("fMP4 segmentlar ishlatiladi")
        void fragmentedMp4IsUsed() throws Exception {
            List<String> command = build(profiles(1920, 1080), true);

            assertThat(valueOf(command, "-hls_segment_type")).isEqualTo("fmp4");
            assertThat(valueOf(command, "-hls_fmp4_init_filename")).isEqualTo("init.mp4");
            assertThat(valueOf(command, "-hls_segment_filename")).endsWith("segment_%05d.m4s");
        }

        @Test
        @DisplayName("master.m3u8 ni FFmpeg ning O'ZI yozadi")
        void masterPlaylistIsGeneratedByFfmpeg() throws Exception {
            List<String> command = build(profiles(1920, 1080), true);

            // ⚠️ Qo'lda yozilsa `CODECS` atributini ham qo'lda
            // hisoblash kerak bo'lardi (`avc1.4d401f` kabi qatorlar).
            // Noto'g'ri qiymat pleyerni oqimni umuman ochmaslikka olib
            // boradi — va bu faqat qurilmada bilinadi.
            assertThat(valueOf(command, "-master_pl_name")).isEqualTo("master.m3u8");
        }

        @Test
        @DisplayName("Mobil uchun mos kodek va piksel formati")
        void mobileCompatibleCodecs() throws Exception {
            List<String> command = build(profiles(1920, 1080), true);

            assertThat(valueOf(command, "-c:v")).isEqualTo("libx264");
            assertThat(valueOf(command, "-c:a")).isEqualTo("aac");
            // ⚠️ Manba 10-bitli bo'lsa FFmpeg uni saqlab qolardi va
            // natija ko'p qurilmada ochilmasdi.
            assertThat(valueOf(command, "-pix_fmt")).isEqualTo("yuv420p");
        }
    }

    @Nested
    @DisplayName("Chekka holatlar")
    class EdgeCases {

        @Test
        @DisplayName("Vertikal video uchun o'lchamlar to'g'ri")
        void portraitDimensions() throws Exception {
            List<String> command = build(profiles(1080, 1920), true);

            assertThat(valueOf(command, "-filter:v:0")).isEqualTo("scale=1080:1920");
            assertThat(valueOf(command, "-filter:v:1")).isEqualTo("scale=720:1280");
        }

        @Test
        @DisplayName("Bo'sh variantlar ro'yxati ANIQ xato beradi")
        void emptyProfilesRejected() {
            assertThatThrownBy(() -> build(List.of(), true))
                    .hasRootCauseInstanceOf(VideoProcessingException.class);
        }

        @Test
        @DisplayName("Tushunarsiz bitrate ANIQ xato beradi")
        void malformedBitrateRejected() {
            VideoTranscodingProperties broken = new VideoTranscodingProperties();
            broken.setProfiles(List.of(new TranscodingProfile(720, "juda-tez", "128k")));

            var selected = new VideoProfileSelector(broken)
                    .select(new VideoMetadata(1280, 720, 60, 25.0, "h264", "aac", 1L));

            assertThatThrownBy(() -> build(selected, true))
                    .hasRootCauseInstanceOf(VideoProcessingException.class);
        }

        @Test
        @DisplayName("Bitrate birligi SAQLANADI")
        void bitrateSuffixIsPreserved() throws Exception {
            VideoTranscodingProperties custom = new VideoTranscodingProperties();
            custom.setProfiles(List.of(new TranscodingProfile(720, "3M", "128k")));

            var selected = new VideoProfileSelector(custom)
                    .select(new VideoMetadata(1280, 720, 60, 25.0, "h264", "aac", 1L));
            List<String> command = build(selected, true);

            assertThat(valueOf(command, "-bufsize:v:0")).isEqualTo("6M");
        }
    }
}
