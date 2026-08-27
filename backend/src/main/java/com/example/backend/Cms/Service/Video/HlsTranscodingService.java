package com.example.backend.Cms.Service.Video;

import com.example.backend.Cms.Service.Video.VideoProfileSelector.SelectedProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Manba videodan HLS yaratadi.
 *
 * <h2>Natija tuzilishi</h2>
 * <pre>
 *   outputDir/
 *     master.m3u8
 *     1080p/index.m3u8 · init.mp4 · segment_00001.m4s …
 *      720p/…
 *      480p/…
 * </pre>
 *
 * <h2>⚠️ Bu metod UZOQ ishlaydi</h2>
 * Ikki soatlik film uchun o'nlab daqiqa. U HTTP so'rovi ichida
 * chaqirilmaydi va tranzaksiya ichida ham turmasligi kerak — aks holda
 * baza ulanishi shuncha vaqt band bo'lardi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HlsTranscodingService {

    @Value("${app.video.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    /**
     * Transcoding qiladi.
     *
     * @param source     manba fayl
     * @param outputDir  natija papkasi — chaqiruvchi yaratadi va tozalaydi
     * @param profiles   tanlangan variantlar
     * @param metadata   {@code ffprobe} natijasi — ovoz bor-yo'qligi va
     *                   davomiylik progress uchun kerak
     * @param onProgress 0..99 oralig'ida chaqiriladi
     * @return {@code master.m3u8} ga yo'l
     */
    public Path transcode(Path source,
                          Path outputDir,
                          List<SelectedProfile> profiles,
                          VideoMetadata metadata,
                          IntConsumer onProgress) {

        boolean hasAudio = metadata != null && metadata.audioCodec() != null;
        List<String> command = FfmpegCommandBuilder.build(
                ffmpegPath, source, outputDir, profiles, hasAudio);

        // ⚠️ Variant papkalari OLDINDAN yaratiladi. FFmpeg ularni
        // o'zi yasamaydi va "No such file or directory" bilan yiqilardi
        // — sabab esa buyruqdan umuman ko'rinmasdi.
        createVariantDirectories(outputDir, profiles);

        log.info("Transcoding boshlandi: {} → {} variant", source.getFileName(), profiles.size());
        run(command, metadata, onProgress);

        Path master = outputDir.resolve("master.m3u8");
        if (!Files.isRegularFile(master)) {
            // FFmpeg 0 bilan tugadi-yu playlist yo'q — bu jimgina
            // buzilish bo'lardi: keyingi bosqich bo'sh papkani S3 ga
            // yuklab, videoni READY deb belgilardi.
            throw new VideoProcessingException("master.m3u8 yaratilmadi");
        }
        return master;
    }

    // --------------------------------------------------------- ichki qism

    private void createVariantDirectories(Path outputDir, List<SelectedProfile> profiles) {
        try {
            for (SelectedProfile profile : profiles) {
                Files.createDirectories(outputDir.resolve(profile.label()));
            }
        } catch (IOException e) {
            throw new VideoProcessingException("Natija papkalari yaratilmadi", e);
        }
    }

    /**
     * FFmpeg ni ishga tushiradi va progressni o'qiydi.
     *
     * ⚠️ Kutish muddati YO'Q va bu ataylab: transcoding qancha
     * davom etishini oldindan bilib bo'lmaydi (fayl hajmi, server
     * bandligi). Osilib qolishdan himoya — progress oqimining o'zi:
     * FFmpeg tirik bo'lsa u yozib turadi.
     */
    private void run(List<String> command, VideoMetadata metadata, IntConsumer onProgress) {
        Process process = start(command);
        StringBuilder errors = new StringBuilder();

        // Xato oqimini ALOHIDA ipda o'qiymiz.
        //
        // ⚠️ Usiz FFmpeg osilib qolardi: operatsion tizim bufer
        // to'lganda yozishni to'xtatadi, biz esa stdout ni o'qib
        // turgan bo'lardik va hech kim stderr ni bo'shatmasdi.
        Thread errorReader = new Thread(() -> {
            try (BufferedReader reader = reader(process.getErrorStream())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errors.append(line).append('\n');
                }
            } catch (IOException e) {
                log.debug("FFmpeg xato oqimi o'qilmadi", e);
            }
        }, "ffmpeg-stderr");
        errorReader.setDaemon(true);
        errorReader.start();

        try (BufferedReader reader = reader(process.getInputStream())) {
            readProgress(reader, metadata, onProgress);

            int exit = process.waitFor();
            errorReader.join(5_000);

            if (exit != 0) {
                throw new VideoProcessingException(
                        "FFmpeg xatosi (" + exit + "): " + lastLines(errors.toString()));
            }
        } catch (IOException e) {
            throw new VideoProcessingException("FFmpeg chiqishi o'qilmadi", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new VideoProcessingException("Transcoding uzildi", e);
        } finally {
            process.destroy();
        }
    }

    /**
     * {@code -progress pipe:1} chiqishini o'qiydi.
     *
     * FFmpeg uni {@code kalit=qiymat} qatorlari bilan beradi:
     * <pre>
     *   out_time_us=12500000
     *   progress=continue
     * </pre>
     *
     * ⚠️ Foiz faqat DAVOMIYLIK ma'lum bo'lganda hisoblanadi. Aks holda
     * bo'linuvchi yo'q va har qanday raqam o'ylab topilgan bo'lardi
     * — «Ma'lumot mavjud bo'lmagan statistikani fake qilib chiqarma».
     */
    private void readProgress(BufferedReader reader, VideoMetadata metadata, IntConsumer onProgress)
            throws IOException {

        Integer totalSeconds = metadata == null ? null : metadata.durationSeconds();
        boolean canReport = totalSeconds != null && totalSeconds > 0 && onProgress != null;

        String line;
        int lastReported = -1;

        while ((line = reader.readLine()) != null) {
            if (!canReport || !line.startsWith("out_time_us=")) {
                continue;
            }

            long micros = parseLong(line.substring("out_time_us=".length()));
            if (micros < 0) {
                continue;
            }

            int percent = (int) Math.min(99, micros / 1_000_000 * 100 / totalSeconds);

            // Har qatorda emas, faqat foiz O'ZGARGANDA yoziladi:
            // FFmpeg progressni sekundiga bir necha marta beradi va
            // har birini bazaga yozish keraksiz yuk bo'lardi.
            if (percent > lastReported) {
                lastReported = percent;
                onProgress.accept(percent);
            }
        }
    }

    private Process start(List<String> command) {
        try {
            return new ProcessBuilder(command).start();
        } catch (IOException e) {
            throw new VideoProcessingException(
                    "ffmpeg ishga tushmadi (`" + ffmpegPath + "`). "
                            + "O'rnatilganini va `app.video.ffmpeg-path` to'g'riligini tekshiring", e);
        }
    }

    private BufferedReader reader(java.io.InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            // FFmpeg ba'zan `N/A` yozadi — bu xato emas.
            return -1;
        }
    }

    /** Xato xabari uchun oxirgi qatorlar — sabab odatda oxirida. */
    private String lastLines(String text) {
        if (text == null || text.isBlank()) {
            return "(sababsiz)";
        }
        String[] lines = text.strip().split("\n");
        int from = Math.max(0, lines.length - 5);
        String tail = String.join(" | ", List.of(lines).subList(from, lines.length));
        return tail.length() <= 1000 ? tail : tail.substring(tail.length() - 1000);
    }
}
