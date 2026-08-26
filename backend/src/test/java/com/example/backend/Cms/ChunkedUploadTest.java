package com.example.backend.Cms;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.UploadSession;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Repository.UploadSessionRepo;
import com.example.backend.Cms.Service.ChunkedUploadService;
import com.example.backend.Cms.Service.StorageService;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sun.management.UnixOperatingSystemMXBean;
import org.springframework.test.util.AopTestUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Katta video fayllarni bo'laklab yuklash.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * <ul>
 *   <li>bo'laklar TO'G'RI TARTIBDA yig'iladi — aks holda video buziladi;</li>
 *   <li>uzilishdan keyin davom ettirish ishlaydi;</li>
 *   <li>bo'lak yetishmasa fayl YARATILMAYDI — yarim video media
 *       kutubxonasiga tushib qolmasin;</li>
 *   <li>man etilgan kengaytma ENG BOSHIDA rad etiladi, gigabaytlab
 *       ma'lumot yuborilgandan keyin emas.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class ChunkedUploadTest {

    @Autowired
    private ChunkedUploadService uploadService;

    @Autowired
    private UploadSessionRepo sessionRepo;

    @Autowired
    private MediaAssetRepo mediaAssetRepo;

    @Autowired
    private StorageService storageService;

    @Value("${app.upload.temp-dir}")
    private String tempDir;

    private final UUID actor = UUID.randomUUID();

    /** Bo'lak o'lchami 5 MB, shuning uchun ikki bo'lak uchun shuncha kerak. */
    private static final int CHUNK = 5 * 1024 * 1024;

    private byte[] pattern(int size, int seed) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) ((i + seed) % 251);
        }
        return data;
    }

    private InputStream stream(byte[] data) {
        return new ByteArrayInputStream(data);
    }

    private byte[] readStored(String key) throws IOException {
        try (InputStream in = storageService.load(key).getInputStream()) {
            return in.readAllBytes();
        }
    }

    @Nested
    @DisplayName("To'liq oqim")
    class HappyPath {

        @Test
        @DisplayName("Ikki bo'lak yig'ilib bitta fayl bo'ladi va tartib saqlanadi")
        void chunksAreAssembledInOrder() throws IOException {
            byte[] first = pattern(CHUNK, 1);
            byte[] second = pattern(1000, 99);
            long total = first.length + second.length;

            UploadSession session = uploadService.begin(
                    actor, "katta-video.mp4", total, "video/mp4", "content");

            assertThat(session.getTotalChunks()).isEqualTo(2);

            // Ataylab TESKARI tartibda yuboramiz - yig'ish raqamga qarab
            // bo'lishi kerak, kelish tartibiga emas.
            uploadService.saveChunk(session, 1, stream(second));
            uploadService.saveChunk(session, 0, stream(first));

            MediaAsset asset = uploadService.complete(session);

            assertThat(asset.getType().name()).isEqualTo("VIDEO");
            assertThat(asset.getSizeBytes()).isEqualTo(total);

            byte[] stored = readStored(asset.getStorageKey());
            assertThat(stored).hasSize((int) total);
            // Chegara aynan joyida bo'lsin: oxirgi bayt 1-bo'lakdan,
            // keyingisi 2-bo'lakdan boshlanadi.
            assertThat(stored[CHUNK - 1]).isEqualTo(first[CHUNK - 1]);
            assertThat(stored[CHUNK]).isEqualTo(second[0]);
        }

        @Test
        @DisplayName("Yuklangan fayl nomi server tomonida yasaladi")
        void storageKeyIsServerGenerated() {
            UploadSession session = uploadService.begin(
                    actor, "../../etc/passwd.mp4", 10L, "video/mp4", "content");
            uploadService.saveChunk(session, 0, stream(pattern(10, 0)));
            MediaAsset asset = uploadService.complete(session);

            // Foydalanuvchi nomidan faqat kengaytma olinadi.
            assertThat(asset.getStorageKey()).doesNotContain("..");
            assertThat(asset.getStorageKey()).doesNotContain("passwd");
            assertThat(asset.getStorageKey()).endsWith(".mp4");
        }
    }

    @Nested
    @DisplayName("Uzilish va davom ettirish")
    class Resume {

        @Test
        @DisplayName("Yetib kelgan bo'laklar ro'yxati qaytadi")
        void receivedChunksAreReported() {
            UploadSession session = uploadService.begin(
                    actor, "uzilgan.mp4", (long) CHUNK + 500, "video/mp4", "content");

            assertThat(uploadService.receivedChunks(session)).isEmpty();

            uploadService.saveChunk(session, 0, stream(pattern(CHUNK, 3)));
            assertThat(uploadService.receivedChunks(session)).containsExactly(0);

            uploadService.saveChunk(session, 1, stream(pattern(500, 4)));
            assertThat(uploadService.receivedChunks(session)).containsExactly(0, 1);
        }

        @Test
        @DisplayName("Bo'lakni qayta yuborish xavfsiz")
        void resendingChunkIsSafe() throws IOException {
            byte[] data = pattern(2000, 7);
            UploadSession session = uploadService.begin(
                    actor, "takror.mp4", 2000L, "video/mp4", "content");

            // Klient uzilgach qaysi bo'lak to'liq yetganini bilmasligi mumkin,
            // shuning uchun qayta yuborish natijani buzmasligi kerak.
            uploadService.saveChunk(session, 0, stream(data));
            uploadService.saveChunk(session, 0, stream(data));

            MediaAsset asset = uploadService.complete(session);
            assertThat(readStored(asset.getStorageKey())).isEqualTo(data);
        }
    }

    @Nested
    @DisplayName("Rad etilishi kerak bo'lgan holatlar")
    class Rejections {

        @Test
        @DisplayName("Bo'lak yetishmasa fayl YARATILMAYDI")
        void incompleteUploadCreatesNoMedia() {
            long before = mediaAssetRepo.count();
            UploadSession session = uploadService.begin(
                    actor, "chala.mp4", (long) CHUNK + 100, "video/mp4", "content");
            uploadService.saveChunk(session, 0, stream(pattern(CHUNK, 2)));

            assertThatThrownBy(() -> uploadService.complete(session))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Yetishmayapti");

            // Yarim video media kutubxonasida ko'rinmasligi kerak.
            assertThat(mediaAssetRepo.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("Man etilgan kengaytma ENG BOSHIDA rad etiladi")
        void forbiddenExtensionIsRejectedUpFront() {
            assertThatThrownBy(() -> uploadService.begin(
                    actor, "zararli.exe", 100L, "application/octet-stream", "content"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("qabul qilinmaydi");

            // Sessiya umuman ochilmasligi kerak.
            assertThat(sessionRepo.findAll())
                    .noneMatch(s -> "zararli.exe".equals(s.getOriginalFilename()));
        }

        @Test
        @DisplayName("Noto'g'ri bo'lak raqami rad etiladi")
        void chunkIndexIsValidated() {
            UploadSession session = uploadService.begin(
                    actor, "kichik.mp4", 100L, "video/mp4", "content");

            assertThatThrownBy(() -> uploadService.saveChunk(session, 5, stream(pattern(10, 0))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Bo'lak raqami noto'g'ri");

            assertThatThrownBy(() -> uploadService.saveChunk(session, -1, stream(pattern(10, 0))))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Bekor qilingan sessiyaga bo'lak qo'shib bo'lmaydi")
        void abortedSessionRejectsChunks() {
            UploadSession session = uploadService.begin(
                    actor, "bekor.mp4", 100L, "video/mp4", "content");
            uploadService.abort(session);

            assertThatThrownBy(() -> uploadService.saveChunk(session, 0, stream(pattern(10, 0))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Sessiya yopilgan");
        }

        @Test
        @DisplayName("O'lcham chegarasi tekshiriladi")
        void sizeIsValidated() {
            assertThatThrownBy(() -> uploadService.begin(
                    actor, "nol.mp4", 0L, "video/mp4", "content"))
                    .isInstanceOf(BusinessException.class);

            assertThatThrownBy(() -> uploadService.begin(
                    actor, "ulkan.mp4", Long.MAX_VALUE, "video/mp4", "content"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("juda katta");
        }
    }
    @Nested
    @DisplayName("Yig'ishda fayl deskriptorlari")
    class FileDescriptors {

        /**
         * 5 GB video = 5 MB lik 1024 ta bo'lak. Linux'da ochiq fayllar
         * uchun standart yumshoq chegara ham 1024 ta. Ya'ni oqimlar
         * oldindan ochilsa, aynan ENG KATTA fayllar — bu mexanizm
         * o'zi kimlar uchun yaratilgan bo'lsa — butun yuklash tugagach,
         * eng oxirgi qadamda yiqilardi.
         *
         * ⚠️ Bu test bo'laklarni DISKKA to'g'ridan-to'g'ri yozadi:
         * 300 ta haqiqiy 5 MB lik bo'lak 1.5 GB bo'lardi va sinov uchun
         * bu ortiqcha. Yig'ish mantig'i uchun bo'lak o'lchami muhim emas.
         */
        @Test
        @DisplayName("Bo'laklar birma-bir ochiladi, hammasi birdan emas")
        void chunkStreamsAreOpenedLazily() throws Exception {
            long baseline = openFileCount();
            assumeTrue(baseline > 0, "Bu OS ochiq fayllar sonini bermaydi");

            final int parts = 300;
            UploadSession session = uploadService.begin(
                    actor, "juda-katta.mp4", (long) parts * CHUNK, "video/mp4", "content");
            assertThat(session.getTotalChunks()).isEqualTo(parts);

            // ⚠️ Yo'l QAT'IY yozilmaydi — u `app.upload.temp-dir` orqali
            // sozlanadi va test profilida vaqtinchalik papkaga ko'chgan.
            Path dir = Paths.get(tempDir, session.getId());
            Files.createDirectories(dir);
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < parts; i++) {
                Files.write(dir.resolve(i + ".part"), new byte[]{(byte) i});
                indices.add(i);
            }

            Method concat = ChunkedUploadService.class
                    .getDeclaredMethod("concatenated", UploadSession.class, List.class);
            concat.setAccessible(true);
            ChunkedUploadService target = AopTestUtils.getTargetObject(uploadService);

            long before = openFileCount();
            try (InputStream joined = (InputStream) concat.invoke(target, session, indices)) {
                long opened = openFileCount() - before;
                // Oldingi amalga oshirishda bu 300 bo'lardi.
                assertThat(opened)
                        .as("yig'ish oqimi yasalganda ochilgan fayllar")
                        .isLessThan(10);

                // Va u haqiqatan ishlaydi: hamma bayt tartib bilan keladi.
                byte[] all = joined.readAllBytes();
                assertThat(all).hasSize(parts);
                assertThat(all[0]).isEqualTo((byte) 0);
                assertThat(all[parts - 1]).isEqualTo((byte) (parts - 1));
            }

            // Oqim yopilgach deskriptorlar qaytarilgan bo'lsin.
            assertThat(openFileCount() - before).isLessThan(10);
        }

        private long openFileCount() {
            OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            return os instanceof UnixOperatingSystemMXBean unix
                    ? unix.getOpenFileDescriptorCount()
                    : -1;
        }
    }

    @Nested
    @DisplayName("Fayl turini aniqlash")
    class TypeDetection {

        /**
         * ⚠️ Bu jimgina buziladigan yo'nalish edi.
         *
         * MIME turini brauzer beradi va `.m4v` kabi kengaytmalar uchun
         * u ko'pincha BO'SH bo'ladi — klient esa uni
         * `application/octet-stream` ga aylantiradi. Ilgari tur faqat
         * MIME bo'yicha aniqlanardi, ya'ni bunday video `DOCUMENT`
         * bo'lib saqlanardi.
         *
         * Yuklash MUVAFFAQIYATLI tugardi, xato ko'rsatilmasdi — lekin
         * qism muharriridagi video tanlash oynasi (`type=VIDEO`) uni
         * ko'rsatmasdi. Admin uchun bu «video yuklanmadi» bo'lib
         * ko'rinardi, aslida fayl joyida edi.
         */
        @Test
        @DisplayName("MIME bo'lmasa kengaytma bo'yicha VIDEO deb aniqlanadi")
        void videoWithoutMimeIsStillVideo() {
            UploadSession session = uploadService.begin(
                    actor, "kino.m4v", 100L, "application/octet-stream", "content");
            uploadService.saveChunk(session, 0, stream(pattern(100, 3)));

            MediaAsset asset = uploadService.complete(session);

            assertThat(asset.getType()).isEqualTo(MediaType.VIDEO);
        }

        @Test
        @DisplayName("MIME umuman berilmasa ham kengaytma yetarli")
        void nullMimeFallsBackToExtension() {
            UploadSession session = uploadService.begin(
                    actor, "afisha.png", 100L, null, "content");
            uploadService.saveChunk(session, 0, stream(pattern(100, 4)));

            assertThat(uploadService.complete(session).getType()).isEqualTo(MediaType.IMAGE);
        }

        @Test
        @DisplayName("mkv va avi ham VIDEO — lekin o'ynatib bo'lmaydi deb belgilanadi")
        void archiveFormatsAreVideoButNotPlayable() {
            for (String name : List.of("kino.mkv", "kino.avi")) {
                UploadSession session = uploadService.begin(
                        actor, name, 100L, "application/octet-stream", "content");
                uploadService.saveChunk(session, 0, stream(pattern(100, 6)));

                MediaAsset asset = uploadService.complete(session);

                // Kutubxonaning VIDEO filtrida ko'rinsin.
                assertThat(asset.getType()).as(name).isEqualTo(MediaType.VIDEO);
                // ⚠️ Lekin panel ogohlantirishi SHART: HTML5 pleyer bu
                // formatlarni ochmaydi va biriktirilgan epizod
                // foydalanuvchida qora ekran berardi.
                assertThat(MediaType.isPlayable(name)).as(name + " o'ynatiladimi").isFalse();
            }
        }

        @Test
        @DisplayName("mp4 o'ynatiladigan deb belgilanadi")
        void mp4IsPlayable() {
            assertThat(MediaType.isPlayable("kino.mp4")).isTrue();
            assertThat(MediaType.isPlayable("kino.m4v")).isTrue();
            assertThat(MediaType.isPlayable("kino.webm")).isTrue();
            assertThat(MediaType.isPlayable("kino.mov")).isTrue();
        }

        @Test
        @DisplayName("MIME bor bo'lsa u USTUN turadi")
        void mimeWinsWhenPresent() {
            UploadSession session = uploadService.begin(
                    actor, "hujjat.pdf", 100L, "application/pdf", "content");
            uploadService.saveChunk(session, 0, stream(pattern(100, 5)));

            assertThat(uploadService.complete(session).getType()).isEqualTo(MediaType.DOCUMENT);
        }
    }

}
