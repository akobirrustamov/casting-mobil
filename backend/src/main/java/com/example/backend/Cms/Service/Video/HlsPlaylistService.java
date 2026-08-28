package com.example.backend.Cms.Service.Video;

import com.example.backend.Cms.Service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * HLS playlistlarini qayta yozadi.
 *
 * <h2>Nima uchun kerak</h2>
 * Ombordagi playlist NISBIY yo'llar bilan yozilgan:
 *
 * <pre>
 *   master.m3u8        →  720p/index.m3u8
 *   720p/index.m3u8    →  init.mp4, segment_00001.m4s
 * </pre>
 *
 * Ular to'g'ridan-to'g'ri berilsa, pleyer ularni CDN ildizidan
 * izlaydi va pullik kontent HIMOYASIZ qoladi: havolani nusxalab
 * tarqatsa, istalgan odam ko'raveradi.
 *
 * Shuning uchun playlist server orqali beriladi va yo'llar
 * almashtiriladi:
 *
 * <pre>
 *   master   →  variant playlistlari BIZNING serverga
 *   variant  →  segmentlar IMZOLANGAN havolaga
 * </pre>
 *
 * ⚠️ Segmentlar baribir server orqali O'TMAYDI. Playlist — bir necha
 * kilobayt matn, segmentlar esa gigabaytlar; ular imzolangan havola
 * bilan to'g'ridan-to'g'ri ombordan keladi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HlsPlaylistService {

    private final StorageService storageService;

    /**
     * Playlistni o'qib, ichidagi yo'llarni almashtiradi.
     *
     * @param playlistKey ombordagi kalit
     * @param rewrite     nisbiy yo'l → yangi manzil
     */
    public String rewrite(String playlistKey, Function<String, String> rewrite) {
        String body = read(playlistKey);
        String base = parentOf(playlistKey);

        List<String> out = new ArrayList<>();
        for (String line : body.split("\n", -1)) {
            out.add(rewriteLine(line, base, rewrite));
        }
        return String.join("\n", out);
    }

    /**
     * Bitta qator.
     *
     * ⚠️ Yo'l IKKI joyda uchraydi va ikkalasi ham almashtirilishi
     * kerak:
     *
     * <ul>
     *   <li>oddiy qator — segment yoki variant playlisti;</li>
     *   <li>{@code #EXT-X-MAP:URI="init.mp4"} — init fayli.</li>
     * </ul>
     *
     * Ikkinchisi unutilsa video umuman ochilmasdi: init fayli
     * bo'lmasa fMP4 segmentlarini dekodlab bo'lmaydi.
     */
    private String rewriteLine(String line, String base, Function<String, String> rewrite) {
        String trimmed = line.trim();

        if (trimmed.isEmpty()) {
            return line;
        }

        // `#EXT-X-MAP:URI="init.mp4"`
        if (trimmed.startsWith("#EXT-X-MAP:")) {
            int from = line.indexOf("URI=\"");
            if (from < 0) {
                return line;
            }
            int start = from + 5;
            int end = line.indexOf('"', start);
            if (end < 0) {
                return line;
            }
            String uri = line.substring(start, end);
            return line.substring(0, start) + rewrite.apply(join(base, uri)) + line.substring(end);
        }

        // Qolgan `#` qatorlari — metama'lumot, ularda yo'l yo'q.
        if (trimmed.startsWith("#")) {
            return line;
        }

        // Oddiy qator — yo'lning o'zi.
        return rewrite.apply(join(base, trimmed));
    }

    private String read(String key) {
        try (InputStream in = storageService.load(key).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Playlist o'qilmadi: {}", key, e);
            throw new VideoProcessingException("Playlist o'qilmadi", e);
        }
    }

    /** {@code /videos/7/hls/720p/index.m3u8} → {@code /videos/7/hls/720p} */
    private String parentOf(String key) {
        int slash = key.lastIndexOf('/');
        return slash < 0 ? "" : key.substring(0, slash);
    }

    /**
     * Nisbiy yo'lni kalitga aylantiradi.
     *
     * ⚠️ Mutlaq manzil TEGILMAYDI. Playlistda tashqi havola bo'lishi
     * mumkin (masalan reklama oqimi) va uni kalit deb talqin qilish
     * uni buzardi.
     */
    private String join(String base, String path) {
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/")) {
            return path;
        }
        return base.isEmpty() ? path : base + "/" + path;
    }
}
