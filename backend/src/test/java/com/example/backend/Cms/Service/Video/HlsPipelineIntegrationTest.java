package com.example.backend.Cms.Service.Video;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * HAQIQIY FFmpeg bilan to'liq zanjir.
 *
 * <h2>Nega bu test kerak</h2>
 * Qolgan barcha video testlari FFmpeg ni MOCK qiladi: ular buyruq
 * to'g'ri qurilganini tekshiradi, lekin FFmpeg uni qabul qilishini
 * EMAS. Bu farq muhim — noto'g'ri bayroq yoki mos kelmaydigan
 * parametr faqat haqiqiy ishga tushirishda bilinadi.
 *
 * <h2>⚠️ FFmpeg bo'lmasa test O'TKAZIB YUBORILADI</h2>
 * U ishlab chiqish mashinasida ham, CI da ham kafolatlanmagan.
 * {@code assumeTrue} testni yiqitmaydi, balki o'tkazib yuboradi —
 * aks holda butun to'plam FFmpeg'siz muhitda qizil bo'lardi.
 *
 * <h2>Manba — sintetik, lekin HAQIQIY kodlangan</h2>
 * {@code testsrc} filtri bilan yasaladi: hech narsa yuklab olinmaydi,
 * natija takrorlanadigan, lekin video haqiqiy H.264 oqim.
 */
class HlsPipelineIntegrationTest {

    private static final String FFMPEG = "/opt/homebrew/bin/ffmpeg";
    private static final String FFPROBE = "/opt/homebrew/bin/ffprobe";

    private static boolean available;

    @BeforeAll
    static void checkTools() {
        available = Files.isExecutable(Path.of(FFMPEG))
                && Files.isExecutable(Path.of(FFPROBE));
    }

    // ------------------------------------------------------- yordamchilar

    private VideoProbeService probeService() {
        VideoProbeService service = new VideoProbeService(new ObjectMapper());
        ReflectionTestUtils.setField(service, "ffprobePath", FFPROBE);
        ReflectionTestUtils.setField(service, "timeout", Duration.ofSeconds(30));
        return service;
    }

    private HlsTranscodingService transcodingService() {
        HlsTranscodingService service = new HlsTranscodingService();
        ReflectionTestUtils.setField(service, "ffmpegPath", FFMPEG);
        return service;
    }

    /**
     * Sinov uchun video yasaydi.
     *
     * ⚠️ Faqat 4 soniya va past bitrate: test to'plami sekinlashmasin.
     * Zanjirni tekshirish uchun uzunlik ahamiyatsiz.
     */
    private Path makeVideo(Path dir, int width, int height, boolean withAudio) throws Exception {
        Path file = dir.resolve("source.mp4");

        List<String> command = new java.util.ArrayList<>(List.of(
                FFMPEG, "-hide_banner", "-v", "error", "-y",
                "-f", "lavfi", "-i", "testsrc=size=" + width + "x" + height + ":rate=30:duration=4"));

        if (withAudio) {
            command.addAll(List.of("-f", "lavfi", "-i", "sine=frequency=440:duration=4"));
        }

        command.addAll(List.of("-c:v", "libx264", "-preset", "ultrafast", "-pix_fmt", "yuv420p"));
        if (withAudio) {
            command.addAll(List.of("-c:a", "aac", "-shortest"));
        }
        command.add(file.toString());

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertThat(process.waitFor(120, TimeUnit.SECONDS)).as("manba yasalmadi").isTrue();
        assertThat(process.exitValue()).as("ffmpeg: %s", output).isZero();

        return file;
    }

    private void deleteTree(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // test tozalashi
                }
            });
        } catch (IOException ignored) {
            // test tozalashi
        }
    }

    // -------------------------------------------------------------- testlar

    @Test
    @DisplayName("Gorizontal video: to'liq zanjir HLS beradi va ABR chegaralari MOSLASHADI")
    void landscapePipeline() throws Exception {
        assumeTrue(available, "FFmpeg o'rnatilmagan — test o'tkazib yuborildi");

        Path work = Files.createTempDirectory("hls-landscape");
        try {
            Path source = makeVideo(work, 1920, 1080, true);

            VideoMetadata metadata = probeService().probe(source);
            assertThat(metadata.width()).isEqualTo(1920);
            assertThat(metadata.height()).isEqualTo(1080);
            assertThat(metadata.videoCodec()).isEqualTo("h264");
            assertThat(metadata.audioCodec()).isEqualTo("aac");

            var profiles = new VideoProfileSelector(new VideoTranscodingProperties()).select(metadata);
            assertThat(profiles).hasSize(3);

            Path output = work.resolve("out");
            Files.createDirectories(output);
            Path master = transcodingService().transcode(source, output, profiles, metadata, p -> { });

            // 1. master.m3u8 yaratildi va uchala variantni ko'rsatadi.
            assertThat(master).exists();
            String masterText = Files.readString(master);
            assertThat(masterText).contains("1080p/index.m3u8", "720p/index.m3u8", "480p/index.m3u8");

            // ⚠️ CODECS — pleyer shu asosda oqimni ochadimi yoki yo'qmi
            // deb qaror qiladi. Noto'g'ri qiymat videoni umuman
            // ochilmaydigan qiladi.
            assertThat(masterText).contains("CODECS=");
            assertThat(masterText).contains("RESOLUTION=1920x1080");

            // 2. Har variantda segmentlar va init fayli bor.
            //
            // ⚠️ Init faylining NOMI qat'iy tekshirilmaydi.
            //
            // Bir variantda FFmpeg uni `init.mp4` deb ataydi, bir
            // nechtasida esa indeks qo'shadi: `init_0.mp4`,
            // `init_1.mp4`. Buni haqiqiy ishga tushirish ko'rsatdi —
            // mock testlar buni umuman ko'ra olmasdi.
            //
            // Shuning uchun kuchliroq tekshiruv: PLAYLIST havola
            // qilgan fayl haqiqatan bormi. Bu nom o'zgarsa ham
            // to'g'ri qoladi va playlist bilan fayllar mos kelishini
            // kafolatlaydi.
            for (var profile : profiles) {
                Path dir = output.resolve(profile.label());
                assertThat(dir.resolve("index.m3u8")).exists();

                String initName = initFileOf(dir.resolve("index.m3u8"));
                assertThat(initName).as("%s playlistida EXT-X-MAP yo'q", profile.label())
                        .isNotNull();
                assertThat(dir.resolve(initName))
                        .as("%s: playlist `%s` ga havola qiladi, fayl esa yo'q",
                                profile.label(), initName)
                        .exists();

                try (Stream<Path> files = Files.list(dir)) {
                    long segments = files.filter(p -> p.toString().endsWith(".m4s")).count();
                    assertThat(segments).as("%s segmentlari", profile.label()).isPositive();
                }
            }

            // 3. ⚠️ ENG MUHIMI — ABR chegaralari.
            //
            // Barcha variantlarda segmentlar AYNI vaqtlarda boshlanishi
            // shart. Aks holda pleyer sifatni almashtirganda kadr
            // sakraydi yoki oqim uziladi — va buni faqat qurilmada,
            // internet sekinlashgan paytda bilinadi.
            List<String> reference = segmentDurations(output.resolve("1080p/index.m3u8"));
            for (var profile : profiles) {
                assertThat(segmentDurations(output.resolve(profile.label() + "/index.m3u8")))
                        .as("%s segment chegaralari 1080p bilan mos kelmadi", profile.label())
                        .isEqualTo(reference);
            }

        } finally {
            deleteTree(work);
        }
    }

    /**
     * ⚠️ Vertikal video — loyihada birinchi darajali (ТЗ §19 Reels).
     *
     * Sifat KICHIK tomon bo'yicha aniqlanadi: 1080×1920 bu «1080p
     * vertikal», «1920p» emas.
     */
    @Test
    @DisplayName("Vertikal video: o'lchamlar almashmaydi va sifat to'g'ri tanlanadi")
    void portraitPipeline() throws Exception {
        assumeTrue(available, "FFmpeg o'rnatilmagan — test o'tkazib yuborildi");

        Path work = Files.createTempDirectory("hls-portrait");
        try {
            Path source = makeVideo(work, 720, 1280, true);

            VideoMetadata metadata = probeService().probe(source);
            assertThat(metadata.width()).isEqualTo(720);
            assertThat(metadata.height()).isEqualTo(1280);

            var profiles = new VideoProfileSelector(new VideoTranscodingProperties()).select(metadata);
            // 720p manba → 1080p GA CHO'ZILMAYDI.
            assertThat(profiles).hasSize(2);
            assertThat(profiles.get(0).resolution()).isEqualTo("720x1280");

            Path output = work.resolve("out");
            Files.createDirectories(output);
            Path master = transcodingService().transcode(source, output, profiles, metadata, p -> { });

            String masterText = Files.readString(master);
            // Chiqish VERTIKAL qoladi.
            assertThat(masterText).contains("RESOLUTION=720x1280");
            assertThat(masterText).doesNotContain("1080p/index.m3u8");

        } finally {
            deleteTree(work);
        }
    }

    /**
     * ⚠️ Ovozsiz video — `0:a:0` ni ulashga urinish FFmpeg ni
     * «Stream map matches no streams» bilan yiqitardi.
     */
    @Test
    @DisplayName("Ovozsiz video ham HLS beradi")
    void silentVideoPipeline() throws Exception {
        assumeTrue(available, "FFmpeg o'rnatilmagan — test o'tkazib yuborildi");

        Path work = Files.createTempDirectory("hls-silent");
        try {
            Path source = makeVideo(work, 640, 360, false);

            VideoMetadata metadata = probeService().probe(source);
            assertThat(metadata.audioCodec()).isNull();

            var profiles = new VideoProfileSelector(new VideoTranscodingProperties()).select(metadata);
            // Eng past profildan kichik — o'z o'lchamida qoladi.
            assertThat(profiles).hasSize(1);
            assertThat(profiles.get(0).resolution()).isEqualTo("640x360");

            Path output = work.resolve("out");
            Files.createDirectories(output);
            Path master = transcodingService().transcode(source, output, profiles, metadata, p -> { });

            assertThat(master).exists();
            assertThat(Files.readString(master)).contains("480p/index.m3u8");

        } finally {
            deleteTree(work);
        }
    }

    @Test
    @DisplayName("Progress hisoblanadi va 100 dan oshmaydi")
    void progressIsReported() throws Exception {
        assumeTrue(available, "FFmpeg o'rnatilmagan — test o'tkazib yuborildi");

        Path work = Files.createTempDirectory("hls-progress");
        try {
            Path source = makeVideo(work, 640, 360, true);
            VideoMetadata metadata = probeService().probe(source);
            var profiles = new VideoProfileSelector(new VideoTranscodingProperties()).select(metadata);

            Path output = work.resolve("out");
            Files.createDirectories(output);

            List<Integer> reported = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
            transcodingService().transcode(source, output, profiles, metadata, reported::add);

            assertThat(reported).isNotEmpty();
            // ⚠️ 100 ni faqat READY qo'yadi — «progress 100, lekin hali
            // TRANSCODING» chalkash holat bo'lardi.
            assertThat(reported).allSatisfy(p -> assertThat(p).isBetween(0, 99));
            // O'sib boradi, orqaga qaytmaydi.
            assertThat(reported).isSorted();

        } finally {
            deleteTree(work);
        }
    }

    /**
     * Playlistdagi segment davomiyliklari.
     *
     * ⚠️ Aynan MATN sifatida solishtiriladi: FFmpeg ularni
     * {@code #EXTINF:6.000000,} shaklida yozadi va kasrdagi eng kichik
     * farq ham chegaralar ajralganini bildiradi.
     */
    /**
     * Playlist havola qilgan init faylining nomi.
     *
     * {@code #EXT-X-MAP:URI="init_0.mp4"} → {@code init_0.mp4}
     */
    private String initFileOf(Path playlist) throws IOException {
        return Files.readAllLines(playlist).stream()
                .filter(line -> line.startsWith("#EXT-X-MAP:"))
                .map(line -> {
                    int from = line.indexOf("URI=\"");
                    if (from < 0) {
                        return null;
                    }
                    int start = from + 5;
                    int end = line.indexOf('"', start);
                    return end < 0 ? null : line.substring(start, end);
                })
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<String> segmentDurations(Path playlist) throws IOException {
        return Files.readAllLines(playlist).stream()
                .filter(line -> line.startsWith("#EXTINF:"))
                .toList();
    }
}
