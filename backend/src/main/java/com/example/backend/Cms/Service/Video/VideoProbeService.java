package com.example.backend.Cms.Service.Video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@code ffprobe} orqali video xususiyatlarini aniqlaydi.
 *
 * <h2>Nega transcodingdan OLDIN</h2>
 * Sifat variantlari manba o'lchamiga bog'liq: 720p videodan 1080p
 * yasash sifat qo'shmaydi, faqat disk va protsessorni sarflaydi (§9).
 * Buni bilish uchun avval o'lchamni aniqlash kerak.
 *
 * Yon foyda: {@code MediaAsset} dagi {@code durationSeconds},
 * {@code width}, {@code height} maydonlari nihoyat to'ldiriladi —
 * ular allaqachon bor edi, lekin ularni o'lchaydigan narsa yo'q edi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProbeService {

    private final ObjectMapper objectMapper;

    /**
     * {@code ffprobe} ijro etuvchi fayli.
     *
     * Sukut bo'yicha {@code PATH} dan izlanadi. Konteynerda yoki
     * standart bo'lmagan o'rnatishda to'liq yo'l beriladi.
     */
    @Value("${app.video.ffprobe-path:ffprobe}")
    private String ffprobePath;

    /**
     * Kutish muddati.
     *
     * ⚠️ Chegarasiz bo'lsa buzuq fayl {@code ffprobe} ni abadiy osib
     * qo'yishi mumkin va u bilan birga butun worker to'xtardi —
     * navbat esa o'sib boraverardi.
     */
    @Value("${app.video.probe-timeout:30s}")
    private Duration timeout;

    /**
     * Faylni tekshiradi.
     *
     * @throws VideoProcessingException {@code ffprobe} topilmasa, muddati
     *         o'tsa yoki fayl o'qilmasa. ⚠️ Jimgina bo'sh natija
     *         QAYTARILMAYDI: u «video 0×0» degan ma'noni berardi va
     *         profil tanlash uni tushunarsiz tarzda rad etardi.
     */
    public VideoMetadata probe(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            throw new VideoProcessingException("Fayl topilmadi: " + file);
        }

        List<String> command = List.of(
                ffprobePath,
                "-v", "error",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                // ⚠️ `--` dan keyin yo'l ARGUMENT deb qabul qilinadi.
                // Usiz `-` bilan boshlanadigan fayl nomi bayroq deb
                // talqin qilinardi.
                file.toString());

        Process process = start(command);
        try {
            String stdout = read(process.getInputStream());
            String stderr = read(process.getErrorStream());

            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new VideoProcessingException(
                        "ffprobe javob bermadi (" + timeout.toSeconds() + "s): " + file.getFileName());
            }

            if (process.exitValue() != 0) {
                throw new VideoProcessingException(
                        "ffprobe xatosi: " + firstLine(stderr));
            }

            JsonNode root = objectMapper.readTree(stdout);
            VideoMetadata metadata = FfprobeOutputParser.parse(root);

            if (!metadata.isUsable()) {
                // ⚠️ `ffprobe` 0 bilan tugadi, lekin o'lchamlarni
                // topmadi — bu odatda fayl video emasligini bildiradi
                // (masalan kengaytmasi almashtirilgan audio).
                throw new VideoProcessingException(
                        "Video o'lchamlari aniqlanmadi: " + file.getFileName());
            }

            log.debug("ffprobe: {} → {}x{} {}s {}",
                    file.getFileName(), metadata.width(), metadata.height(),
                    metadata.durationSeconds(), metadata.videoCodec());
            return metadata;

        } catch (IOException e) {
            throw new VideoProcessingException("ffprobe chiqishi o'qilmadi", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoProcessingException("ffprobe kutilishi uzildi", e);
        } finally {
            process.destroy();
        }
    }

    // --------------------------------------------------------- ichki qism

    private Process start(List<String> command) {
        try {
            // ⚠️ `ProcessBuilder` ro'yxat bilan — QOBIQ orqali emas.
            // Qobiq ishlatilsa fayl nomidagi bo'sh joy yoki `;` belgisi
            // buyruqni bo'lib yuborardi.
            return new ProcessBuilder(command).start();
        } catch (IOException e) {
            // Eng ko'p uchraydigan sabab — ffprobe umuman o'rnatilmagan.
            // Xabar buni ANIQ aytadi, aks holda «IOException» dan nima
            // qilish kerakligi tushunarsiz bo'lardi.
            throw new VideoProcessingException(
                    "ffprobe ishga tushmadi (`" + ffprobePath + "`). "
                            + "O'rnatilganini va `app.video.ffprobe-path` to'g'riligini tekshiring", e);
        }
    }

    private String read(InputStream stream) throws IOException {
        try (InputStream in = stream) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Xato xabari uchun — ffprobe ko'p qatorli chiqish beradi. */
    private String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "(sababsiz)";
        }
        int newline = text.indexOf('\n');
        String line = newline < 0 ? text : text.substring(0, newline);
        return line.length() <= 300 ? line.trim() : line.substring(0, 300).trim();
    }
}
