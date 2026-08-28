package com.example.backend.Cms.Service.Video;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Transcoding uchun server tayyormi.
 *
 * <h2>⚠️ Nima uchun kerak — jimgina nosozlik</h2>
 * FFmpeg serverga o'rnatilmagan bo'lsa hozir shunday bo'lardi: admin
 * video yuklaydi, ish navbatga tushadi, uch marta urinib yiqiladi va
 * {@code FAILED} bo'ladi. Keyingi video ham. Va keyingisi ham.
 *
 * Har bir ishning xato matni to'g'ri («ffprobe ishga tushmadi»), lekin
 * hech kim «ular BIRGA yiqilyapti, ya'ni muammo videolarda emas,
 * SERVERDA» degan xulosaga kelmaydi. Admin buzuq fayl izlab yurardi.
 *
 * Bu klass javobni bir joyda beradi: panelda ko'rinadi va ishga
 * tushishda logga yoziladi.
 *
 * <h2>⚠️ Ilova baribir KO'TARILADI</h2>
 * FFmpeg yo'qligi butun saytni to'xtatmaydi. Video eski yo'l bilan
 * ({@code /raw}) ko'rsatilishda davom etadi, katalog, to'lovlar va
 * casting moduli umuman tegilmaydi.
 *
 * Transcoding — QO'SHIMCHA imkoniyat. Uning yo'qligi uchun ishlaydigan
 * saytni yiqitish nomutanosib bo'lardi.
 */
@Slf4j
@Service
public class VideoSystemHealth {

    @Value("${app.video.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.video.ffprobe-path:ffprobe}")
    private String ffprobePath;

    @Value("${app.video.temp-dir:backend/files/.transcoding}")
    private String tempDir;

    /**
     * Ish boshlash uchun kerakli eng kam bo'sh joy.
     *
     * <h2>⚠️ Nega chegara kerak</h2>
     * Disk to'lganda {@code Files.copy} yarim yo'lda uziladi, ish
     * yiqiladi va qayta urinadi — yana yarim fayl yozib. Uchala
     * urinish ham diskni yanada to'ldiradi.
     *
     * Undan ham yomoni: disk to'lgach PostgreSQL ham yozolmay qoladi
     * va nosozlik video bilan umuman bog'liq bo'lmagan joyda
     * ko'rinadi.
     *
     * Shuning uchun joy kam bo'lsa ish UMUMAN boshlanmaydi.
     */
    @Value("${app.video.min-free-disk:10GB}")
    private DataSize minFreeDisk;

    /**
     * Manba hajmiga nisbatan qancha joy kerak.
     *
     * ⚠️ Manbaning o'zi + uchta HLS variant bir vaqtda diskda turadi.
     * O'lchovda variantlar birgalikda manbadan kichik chiqdi, lekin
     * zaxira olinadi: chegara kam baholansa disk aynan ish o'rtasida
     * to'lardi.
     */
    private static final double SPACE_MULTIPLIER = 2.5;

    /** Versiya so'roviga berilgan vaqt — u darhol javob berishi kerak. */
    private static final long VERSION_TIMEOUT_SECONDS = 10;

    /**
     * Transcoding uchun ZARUR kodlovchilar.
     *
     * ⚠️ {@code FfmpegCommandBuilder} aynan shularni yozadi. Biri
     * o'zgarsa ikkinchisi ham o'zgarishi kerak — aks holda tekshiruv
     * mavjud bo'lmagan narsani so'rardi yoki kerakligini
     * o'tkazib yuborardi.
     */
    private static final List<String> REQUIRED_ENCODERS = List.of("libx264", "aac");

    // ------------------------------------------------------------- tekshirish

    /**
     * Hozirgi holat.
     *
     * ⚠️ KESHLANMAYDI. FFmpeg ishlab turgan serverga o'rnatilishi
     * mumkin va keshlangan «yo'q» javobi adminni ilovani qayta ishga
     * tushirishga majbur qilardi.
     *
     * Chaqiruv arzon: ikkita {@code -version} va bitta disk so'rovi.
     * Panel uni navbat bilan birga, bir necha soniyada bir marta
     * so'raydi.
     */
    public Report check() {
        Tool ffmpeg = probeTool(ffmpegPath);
        Tool ffprobe = probeTool(ffprobePath);

        Path temp = Paths.get(tempDir);
        long free = freeSpace(temp);

        List<String> problems = new ArrayList<>();
        if (!ffmpeg.isAvailable()) {
            problems.add("FFmpeg topilmadi (`" + ffmpegPath + "`) — video HLS'ga o'girilmaydi");
        } else {
            // ⚠️ FFmpeg ishga tushishi YETARLI EMAS.
            //
            // Ba'zi yig'malar (minimal statik build, litsenziya
            // sababli kesilgan distributiv paketlari) `libx264` siz
            // keladi. Bunday FFmpeg `-version` ga chiroyli javob
            // beradi, lekin transcoding «Unknown encoder 'libx264'»
            // bilan yiqiladi — va bu faqat birinchi video yuklangach
            // ma'lum bo'lardi.
            missingEncoders(ffmpeg).forEach(encoder -> problems.add(
                    "FFmpeg da `" + encoder + "` kodlovchisi yo'q — "
                            + "boshqa yig'ma o'rnatilishi kerak"));
        }
        if (!ffprobe.isAvailable()) {
            problems.add("ffprobe topilmadi (`" + ffprobePath + "`) — video tekshirilmaydi");
        }
        if (free >= 0 && free < minFreeDisk.toBytes()) {
            problems.add("Diskda joy kam: " + gb(free) + " GB qoldi, kamida "
                    + gb(minFreeDisk.toBytes()) + " GB kerak");
        }

        return Report.builder()
                .ffmpeg(ffmpeg)
                .ffprobe(ffprobe)
                .freeDiskBytes(free < 0 ? null : free)
                .minFreeDiskBytes(minFreeDisk.toBytes())
                .problems(problems)
                .build();
    }

    /**
     * Shu hajmdagi manba uchun diskda joy bormi.
     *
     * @param sourceBytes manba fayl hajmi; noma'lum bo'lsa {@code null}
     *
     * ⚠️ Noma'lum hajm {@code true} beradi — «bilmayman» sababli ishni
     * to'xtatish uni HECH QACHON bajarilmaydigan qilardi. Eng kam bo'sh
     * joy sharti baribir tekshiriladi.
     */
    public boolean hasRoomFor(Long sourceBytes) {
        long free = freeSpace(Paths.get(tempDir));
        if (free < 0) {
            // Diskni o'qib bo'lmadi — bu tekshiruv ishni to'xtatish
            // uchun asos emas.
            return true;
        }
        if (free < minFreeDisk.toBytes()) {
            return false;
        }
        if (sourceBytes == null || sourceBytes <= 0) {
            return true;
        }
        return free >= (long) (sourceBytes * SPACE_MULTIPLIER);
    }

    /** Qancha joy kerakligini xato xabarida ko'rsatish uchun. */
    public long requiredBytesFor(long sourceBytes) {
        return (long) (sourceBytes * SPACE_MULTIPLIER);
    }

    // --------------------------------------------------------- ishga tushish

    /**
     * Ishga tushishda bir marta ogohlantiradi.
     *
     * ⚠️ {@code WARN} darajasida va aniq matn bilan. «Nimadir
     * noto'g'ri» degan xabar loglarda yo'qolib ketardi.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void reportOnStartup() {
        Report report = check();

        if (report.isHealthy()) {
            log.info("Video transcoding tayyor: {} · {} · diskda {} GB bo'sh",
                    report.getFfmpeg().getVersion(),
                    report.getFfprobe().getVersion(),
                    report.getFreeDiskBytes() == null
                            ? "?" : gb(report.getFreeDiskBytes()));
            return;
        }

        log.warn("⚠️ Video transcoding ISHLAMAYDI. Sayt ishlaydi, videolar "
                + "eski yo'l bilan ko'rsatiladi, lekin HLS yaratilmaydi:");
        report.getProblems().forEach(problem -> log.warn("   • {}", problem));
    }

    // --------------------------------------------------------- ichki qism

    /**
     * Dasturni {@code -version} bilan chaqiradi.
     *
     * ⚠️ Faylning BORLIGINI tekshirish yetarli emas: yo'l papkaga
     * ishora qilishi, ijro huquqi bo'lmasligi yoki fayl boshqa
     * arxitektura uchun yig'ilgan bo'lishi mumkin. Bularning
     * hammasida fayl «bor», lekin ishga tushmaydi.
     */
    private Tool probeTool(String executable) {
        Process process;
        try {
            process = new ProcessBuilder(executable, "-version")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            return Tool.missing(executable, e.getMessage());
        }

        try {
            String output = read(process.getInputStream());
            if (!process.waitFor(VERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Tool.missing(executable, "javob bermadi");
            }
            if (process.exitValue() != 0) {
                return Tool.missing(executable, "xato kod " + process.exitValue());
            }
            return Tool.available(executable, firstLine(output));

        } catch (IOException e) {
            return Tool.missing(executable, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Tool.missing(executable, "kutilish uzildi");
        } finally {
            process.destroy();
        }
    }

    /**
     * Kerakli kodlovchilardan qaysilari yo'q.
     *
     * ⚠️ Ro'yxat {@code FfmpegCommandBuilder} bilan BIR XIL bo'lishi
     * shart. U {@code libx264} va {@code aac} ni qattiq yozadi;
     * {@code FfmpegEncoderContractTest} ikkalasi ajralib ketmasligini
     * qo'riqlaydi.
     *
     * Ro'yxat o'qib bo'lmasa BO'SH qaytadi — «bilmayman» sababli
     * ishlaydigan serverni nosoz deb e'lon qilish yomonroq bo'lardi.
     */
    private List<String> missingEncoders(Tool ffmpeg) {
        String listing = runQuietly(ffmpeg.getPath(), "-hide_banner", "-encoders");
        if (listing == null) {
            return List.of();
        }
        return REQUIRED_ENCODERS.stream()
                .filter(encoder -> !containsEncoder(listing, encoder))
                .toList();
    }

    /**
     * ⚠️ Nom ALOHIDA so'z bo'lishi kerak.
     *
     * Oddiy {@code contains("aac")} har doim rost bo'lardi:
     * ro'yxatda {@code aac_at}, {@code libfdk_aac} va tavsiflarda
     * «AAC» bor. Ya'ni tekshiruv hech qachon muammo topmasdi.
     */
    private boolean containsEncoder(String listing, String encoder) {
        return listing.lines()
                .map(String::trim)
                .anyMatch(line -> {
                    // ` V....D libx264   libx264 H.264 …`
                    String[] parts = line.split("\\s+");
                    return parts.length >= 2 && parts[1].equals(encoder);
                });
    }

    /** Chiqishni qaytaradi; ishga tushmasa {@code null}. */
    private String runQuietly(String executable, String... args) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.addAll(List.of(args));

        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException e) {
            return null;
        }
        try {
            String output = read(process.getInputStream());
            if (!process.waitFor(VERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            return process.exitValue() == 0 ? output : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            process.destroy();
        }
    }

    /**
     * Papkadagi bo'sh joy, baytda. Aniqlanmasa {@code -1}.
     *
     * ⚠️ Papka hali yaratilmagan bo'lishi mumkin (birinchi ishgacha).
     * Bunday holatda MAVJUD ota papka so'raladi — bo'sh joy baribir
     * o'sha diskda o'lchanadi.
     */
    private long freeSpace(Path path) {
        Path existing = path.toAbsolutePath();
        while (existing != null && !Files.isDirectory(existing)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            return -1;
        }
        try {
            return Files.getFileStore(existing).getUsableSpace();
        } catch (IOException e) {
            log.debug("Disk holati o'qilmadi: {}", existing, e);
            return -1;
        }
    }

    private String read(InputStream stream) throws IOException {
        try (InputStream in = stream) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "(versiya noma'lum)";
        }
        int newline = text.indexOf('\n');
        String line = (newline < 0 ? text : text.substring(0, newline)).trim();
        return line.length() <= 120 ? line : line.substring(0, 120);
    }

    /** Bir xonali aniqlik — panelda o'qish uchun. */
    private static String gb(long bytes) {
        return String.format("%.1f", bytes / 1024.0 / 1024.0 / 1024.0);
    }

    // ------------------------------------------------------------------- DTO

    @Data
    @Builder
    public static class Tool {
        private String path;
        private boolean available;

        /** {@code ffmpeg version 9.0.1 …} — mavjud bo'lmasa {@code null}. */
        private String version;

        /** Nima uchun ishga tushmadi. Mavjud bo'lsa {@code null}. */
        private String error;

        static Tool available(String path, String version) {
            return Tool.builder().path(path).available(true).version(version).build();
        }

        static Tool missing(String path, String error) {
            return Tool.builder().path(path).available(false).error(error).build();
        }
    }

    @Data
    @Builder
    public static class Report {
        private Tool ffmpeg;
        private Tool ffprobe;

        /**
         * Bo'sh joy, baytda.
         *
         * ⚠️ {@code null} — «o'lchab bo'lmadi», NOL emas. Nol
         * ko'rsatilsa panel «disk to'lgan» deb ogohlantirardi va admin
         * mavjud bo'lmagan muammoni tuzatishga urinardi.
         */
        private Long freeDiskBytes;

        private long minFreeDiskBytes;

        /** Bo'sh ro'yxat — hammasi joyida. */
        private List<String> problems;

        public boolean isHealthy() {
            return problems == null || problems.isEmpty();
        }
    }
}
