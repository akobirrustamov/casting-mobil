package com.example.backend.Cms.Service.Video;

import com.example.backend.Cms.Service.StorageService;
import com.example.backend.Cms.Service.Storage.MediaContentTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Tayyor HLS'ni omborga yuklaydi.
 *
 * <h2>Kalit tuzilishi</h2>
 * <pre>
 *   videos/{mediaId}/hls/master.m3u8
 *   videos/{mediaId}/hls/1080p/index.m3u8
 *   videos/{mediaId}/hls/1080p/init.mp4
 *   videos/{mediaId}/hls/1080p/segment_00001.m4s
 * </pre>
 *
 * ⚠️ Kalit media identifikatoridan yasaladi, epizod yoki fasldan emas.
 * Bitta media bir nechta qismga biriktirilishi mumkin
 * ({@code EpisodeVideo} — alohida jadval), ya'ni epizodga bog'lash
 * media qayta biriktirilganda kalitni yaroqsiz qilardi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HlsUploadService {

    private final StorageService storageService;

    /**
     * Papkani rekursiv yuklaydi.
     *
     * <h2>⚠️ Playlist ENG OXIRIDA yuklanadi</h2>
     * {@code master.m3u8} paydo bo'lishi «video tayyor» degani.
     * U birinchi yuklansa, segmentlar hali kelmagan paytda pleyer uni
     * o'qib, mavjud bo'lmagan fayllarni so'rardi — foydalanuvchi esa
     * uzilib qoladigan videoni ko'rardi.
     *
     * Shuning uchun tartib: segmentlar → variant playlistlari →
     * {@code master.m3u8}.
     *
     * @return {@code master.m3u8} ning ombordagi kaliti
     */
    public String upload(Path hlsDir, Long mediaId) {
        String prefix = "/videos/" + mediaId + "/hls";

        List<Path> files = collect(hlsDir);
        if (files.isEmpty()) {
            throw new VideoProcessingException("Yuklash uchun HLS fayllari topilmadi");
        }

        String masterKey = null;
        int uploaded = 0;

        for (Path file : files) {
            String relative = hlsDir.relativize(file).toString().replace('\\', '/');
            String key = prefix + "/" + relative;

            if ("master.m3u8".equals(relative)) {
                // Oxirida yuklanadi — pastdagi izohga qarang.
                masterKey = key;
                continue;
            }

            put(file, key);
            uploaded++;
        }

        if (masterKey == null) {
            throw new VideoProcessingException("master.m3u8 topilmadi");
        }

        // ⚠️ Endi — hamma narsa joyida bo'lgandan KEYIN.
        put(hlsDir.resolve("master.m3u8"), masterKey);

        log.info("HLS yuklandi: media={} fayl={}", mediaId, uploaded + 1);
        return masterKey;
    }

    // --------------------------------------------------------- ichki qism

    /**
     * Papkadagi barcha fayllar.
     *
     * ⚠️ Tartib QAT'IY: {@code Files.walk} tizimga bog'liq tartib
     * beradi va u ishga tushirishlar orasida farq qilishi mumkin.
     * Barqaror tartib xatoni takrorlashni osonlashtiradi.
     */
    private List<Path> collect(Path root) {
        if (!Files.isDirectory(root)) {
            throw new VideoProcessingException("HLS papkasi topilmadi: " + root);
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            throw new VideoProcessingException("HLS papkasi o'qilmadi", e);
        }
    }

    /**
     * Bitta fayl.
     *
     * ⚠️ {@code Content-Type} kengaytmadan aniqlanadi. Berilmasa CDN
     * {@code octet-stream} qaytaradi va pleyer playlistni umuman
     * tanimaydi (§13).
     */
    private void put(Path file, String key) {
        try (InputStream in = Files.newInputStream(file)) {
            storageService.storeAt(in, key, MediaContentTypes.of(file.getFileName().toString()));
        } catch (IOException e) {
            throw new VideoProcessingException("Fayl o'qilmadi: " + file.getFileName(), e);
        }
    }
}
