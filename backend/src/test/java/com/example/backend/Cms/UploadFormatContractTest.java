package com.example.backend.Cms;

import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaTypeFactory;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fayl formatlari bo'yicha SERVER va PANEL kelishuvi.
 *
 * <h2>Nima uchun bu test kerak</h2>
 * Ruxsat etilgan kengaytmalar IKKI joyda yozilgan: serverda
 * ({@code LocalStorageService.ALLOWED}) va fayl tanlash oynasida
 * ({@code MediaPicker} ning {@code accept} atributi). Ular bir-birini
 * KO'RMAYDI, ya'ni jimgina ajralib ketishi mumkin:
 *
 * <ul>
 *   <li>panelda BOR, serverda YO'Q — admin faylni tanlaydi, uzoq
 *       yuklaydi va oxirida 422 oladi, nima uchun ekanini
 *       tushunmasdan;</li>
 *   <li>serverda BOR, panelda YO'Q — qo'llab-quvvatlanadigan format
 *       jimgina foydalanib bo'lmaydigan bo'lib qoladi, chunki
 *       brauzer uni tanlashga ruxsat bermaydi.</li>
 * </ul>
 *
 * Ikkalasi ham nosozlik sifatida KO'RINMAYDI — shuning uchun test.
 */
@SpringBootTest
@ActiveProfiles("test")
class UploadFormatContractTest {

    private static final Path PICKER =
            Path.of("../frontend/src/adminpanel/components/MediaPicker.jsx");

    @Autowired
    private StorageService storageService;

    /** Panelning `accept` satridagi `.xxx` kengaytmalari. */
    private Set<String> pickerVideoExtensions() throws IOException {
        String src = Files.readString(PICKER);

        // `accept={type === 'VIDEO' ? ... : ...}` ning VIDEO shoxi.
        int start = src.indexOf("accept={type === 'VIDEO'");
        assertThat(start).as("MediaPicker dagi accept atributi").isGreaterThan(0);
        String branch = src.substring(start, src.indexOf(" : ", start));

        Set<String> found = new TreeSet<>();
        Matcher m = Pattern.compile("\\.([a-z0-9]{2,5})\\b").matcher(branch);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }

    @Test
    @DisplayName("Panel taklif qiladigan har bir video formatini server QABUL qiladi")
    void pickerOffersOnlyWhatServerAccepts() throws IOException {
        for (String extension : pickerVideoExtensions()) {
            assertThat(storageService.accepts("kino." + extension))
                    .as("panel `.%s` ni taklif qiladi, server esa rad etadi", extension)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Server qabul qiladigan har bir video formatini panel TAKLIF qiladi")
    void serverAcceptsNothingThePickerHides() throws IOException {
        Set<String> offered = pickerVideoExtensions();

        // ⚠️ Ro'yxat bu yerda ATAYLAB qo'lda yozilgan. `ALLOWED` dan
        // o'qib olinsa test o'z-o'zini tasdiqlardi: ikkala tomon ham
        // bitta manbadan kelib, ajralishni umuman ko'rsatmasdi.
        for (String extension : List.of("mp4", "mov", "webm", "m4v", "mkv", "avi")) {
            assertThat(storageService.accepts("kino." + extension))
                    .as("server `.%s` ni qabul qilishi kutilgan edi", extension)
                    .isTrue();
            assertThat(offered)
                    .as("server `.%s` ni qabul qiladi, lekin panel uni taklif qilmaydi", extension)
                    .contains(extension);
        }
    }

    @Test
    @DisplayName("Har bir video formati VIDEO deb aniqlanadi — DOCUMENT bo'lib qolmaydi")
    void everyVideoFormatIsDetectedAsVideo() {
        for (String extension : List.of("mp4", "mov", "webm", "m4v", "mkv", "avi")) {
            // MIME'siz: brauzer ba'zi formatlar uchun uni bermaydi.
            assertThat(MediaType.detect("application/octet-stream", "kino." + extension))
                    .as("`.%s` kutubxonaning VIDEO filtrida ko'rinishi kerak", extension)
                    .isEqualTo(MediaType.VIDEO);
        }
    }

    @Test
    @DisplayName("Yetkazishda Content-Type to'g'ri beriladi")
    void everyVideoFormatHasAKnownContentType() {
        // ⚠️ `application/octet-stream` bo'lsa brauzer faylni o'ynatish
        // o'rniga YUKLAB olardi.
        for (String extension : List.of("mp4", "mov", "webm", "m4v", "mkv", "avi")) {
            assertThat(MediaTypeFactory.getMediaType("kino." + extension))
                    .as("`.%s` uchun Content-Type", extension)
                    .isPresent()
                    .hasValueSatisfying(t -> assertThat(t.getType()).isEqualTo("video"));
        }
    }
}
