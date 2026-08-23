package com.example.backend.Cms;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Video bo'laklab uzatilishini qo'riqlaydi (HTTP Range).
 *
 * <h2>Nega bu muhim</h2>
 * Pleyer videoni butunlay yuklamaydi - u kerakli bo'lakni so'raydi. Server
 * 206 qaytarmasa: oldinga o'tkazish ishlamaydi, va har ochilishda butun fayl
 * tortiladi (foydalanuvchi trafigi va server yuki).
 *
 * <h2>Bir marta yiqilgan joy</h2>
 * Avval bitta metod {@code ResponseEntity<?>} qaytarardi. Spring javob turini
 * {@code Object} deb hisoblab {@code ResourceRegion} uchun konverter topa
 * olmadi va Range so'rovi 500 bilan tugadi. Shuning uchun endi ikkita alohida
 * handler bor va bu test aynan shu regressiyani ushlab turadi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaRangeDeliveryTest {

    /** LocalStorageService ildizi - nisbiy, shuning uchun bu yerda ham nisbiy. */
    private static final Path ROOT = Paths.get("backend", "files", "cms-range-test");

    private static final int SIZE = 10_000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaAssetRepo mediaAssetRepo;

    private Path file;
    private Long mediaId;

    private Long seed() throws IOException {
        Files.createDirectories(ROOT);
        file = ROOT.resolve("range-namuna.jpg");
        byte[] bytes = new byte[SIZE];
        for (int i = 0; i < SIZE; i++) {
            bytes[i] = (byte) (i % 251);
        }
        Files.write(file, bytes);

        // Rasm - ochiq, ya'ni test entitlementga emas, aynan uzatishga qaraydi.
        MediaAsset asset = mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/cms-range-test/range-namuna.jpg")
                .originalFilename("range-namuna.jpg")
                .type(MediaType.IMAGE)
                .mimeType("image/jpeg")
                .sizeBytes((long) SIZE)
                .status(MediaStatus.READY)
                .createdAt(LocalDateTime.now())
                .build());
        mediaId = asset.getId();
        return mediaId;
    }

    @AfterEach
    void cleanUp() throws IOException {
        if (file != null) {
            Files.deleteIfExists(file);
        }
        if (mediaId != null) {
            mediaAssetRepo.deleteById(mediaId);
        }
    }

    @Test
    @DisplayName("Range so'ralsa 206 va to'g'ri Content-Range qaytadi")
    void rangeRequestReturnsPartialContent() throws Exception {
        Long id = seed();

        mockMvc.perform(get("/api/v1/app/media/" + id + "/raw")
                        .header("Range", "bytes=1000-1999"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 1000-1999/" + SIZE))
                .andExpect(header().longValue("Content-Length", 1000));
    }

    @Test
    @DisplayName("Range so'ralmasa to'liq fayl va Accept-Ranges beriladi")
    void plainRequestReturnsWholeFile() throws Exception {
        Long id = seed();

        mockMvc.perform(get("/api/v1/app/media/" + id + "/raw"))
                .andExpect(status().isOk())
                // Pleyer shu sarlavhaga qarab bo'laklab so'rashni boshlaydi.
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().longValue("Content-Length", SIZE));
    }

    @Test
    @DisplayName("Fayl javobida qat'iy CSP bo'ladi - SVG ichidagi skript ishlamasin")
    void mediaResponsesCarryStrictCsp() throws Exception {
        Long id = seed();

        // Ruxsat etilgan kengaytmalar orasida SVG bor, uning ichida <script>
        // bo'lishi mumkin. CSP bo'lmasa u sayt domenida ishlab ketardi.
        mockMvc.perform(get("/api/v1/app/media/" + id + "/raw"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'none'; sandbox"));

        mockMvc.perform(get("/api/v1/app/media/" + id + "/raw")
                        .header("Range", "bytes=0-99"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'none'; sandbox"));
    }

    @Test
    @DisplayName("Oxirgi baytlar so'ralsa ham to'g'ri bo'lak qaytadi")
    void suffixRangeWorks() throws Exception {
        Long id = seed();

        // "bytes=-500" = oxirgi 500 bayt. Pleyer mp4 moov atomini shunday qidiradi.
        mockMvc.perform(get("/api/v1/app/media/" + id + "/raw")
                        .header("Range", "bytes=-500"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range",
                        "bytes " + (SIZE - 500) + "-" + (SIZE - 1) + "/" + SIZE));
    }
}
