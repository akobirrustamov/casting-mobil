package com.example.backend.Cms.Service.Video;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Server transcoding uchun tayyormi.
 *
 * <h2>⚠️ Bu test JIMGINA NOSOZLIKNI qo'riqlaydi</h2>
 * FFmpeg o'rnatilmagan serverda har bir video uch marta urinib
 * yiqilardi. Har bir ishning xato matni to'g'ri, lekin hech kim
 * «ular BIRGA yiqilyapti, ya'ni muammo serverda» degan xulosaga
 * kelmasdi — admin buzuq fayl izlab yurardi.
 *
 * <h2>Nega haqiqiy FFmpeg ishlatilmaydi</h2>
 * U CI da yo'q, ya'ni test u yerda ma'nosiz «o'tkazib yuborildi»
 * bo'lardi. O'rniga FFmpeg kabi javob beradigan qisqa skript
 * yasaladi — u har joyda bir xil ishlaydi.
 *
 * Tekshirilayotgan narsa FFmpeg emas — BIZNING xulosamiz: dastur
 * ishga tushdimi, kerakli kodlovchi bormi, va yo'qligi panelga
 * chiqadimi.
 */
class VideoSystemHealthTest {

    private static final String MISSING = "/hech/qayerda/yoq/ffmpeg";

    /** Haqiqiy {@code ffmpeg -encoders} chiqishining bir bo'lagi. */
    private static final String FULL_ENCODERS = """
            Encoders:
             V..... = Video
             ------
             V....D libx264              libx264 H.264 / AVC
             V....D libx265              libx265 H.265 / HEVC
             A....D aac                  AAC (Advanced Audio Coding)
            """;

    /**
     * ⚠️ TUZOQ: bu ro'yxatda `libx264` bor, `aac` esa YO'Q.
     *
     * Undagi `aac_at` va `libfdk_aac` nomlari ichida «aac» so'zi
     * uchraydi. Oddiy `contains("aac")` bu ro'yxatni to'g'ri deb
     * qabul qilardi va tekshiruv hech qachon muammo topmasdi.
     */
    private static final String TRAP_ENCODERS = """
            Encoders:
             V....D libx264              libx264 H.264 / AVC
             A..... aac_at               aac (AudioToolbox) (codec aac)
             A....D libfdk_aac           Fraunhofer FDK AAC (codec aac)
            """;

    private static final String NO_X264 = """
            Encoders:
             V....D mpeg4                MPEG-4 part 2
             A....D aac                  AAC (Advanced Audio Coding)
            """;

    /**
     * FFmpeg o'rnida ishlaydigan skript yasaydi.
     *
     * <h2>⚠️ Nega haqiqiy FFmpeg emas</h2>
     * U CI da yo'q, ya'ni test u yerda ma'nosiz «o'tkazib yuborildi»
     * bo'lardi. Skript esa har joyda bir xil javob beradi va
     * tekshirilayotgan narsa FFmpeg emas — BIZNING xulosamiz:
     * kerakli kodlovchi bormi va yo'qligi panelga chiqadimi.
     */
    private static String fakeFfmpeg(Path dir, String name, String encoders) {
        try {
            Path script = dir.resolve(name);
            Files.writeString(script, """
                    #!/bin/sh
                    if [ "$1" = "-version" ]; then
                      echo "ffmpeg version 9.0.1 (soxta)"
                      exit 0
                    fi
                    cat <<'ENCODERS'
                    %s
                    ENCODERS
                    """.formatted(encoders.strip()));
            script.toFile().setExecutable(true);
            return script.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private VideoSystemHealth health(String ffmpeg, String ffprobe,
                                     Path tempDir, DataSize minFree) {
        VideoSystemHealth h = new VideoSystemHealth();
        ReflectionTestUtils.setField(h, "ffmpegPath", ffmpeg);
        ReflectionTestUtils.setField(h, "ffprobePath", ffprobe);
        ReflectionTestUtils.setField(h, "tempDir", tempDir.toString());
        ReflectionTestUtils.setField(h, "minFreeDisk", minFree);
        return h;
    }

    private VideoSystemHealth healthy(Path tempDir) {
        String ffmpeg = fakeFfmpeg(tempDir, "ffmpeg", FULL_ENCODERS);
        return health(ffmpeg, ffmpeg, tempDir, DataSize.ofBytes(1));
    }

    @Nested
    @DisplayName("FFmpeg mavjudligi")
    class Tools {

        @Test
        @DisplayName("Ikkalasi ham ishlasa — muammo yo'q")
        void bothPresent(@TempDir Path temp) {
            VideoSystemHealth.Report report = healthy(temp).check();

            assertThat(report.isHealthy()).isTrue();
            assertThat(report.getProblems()).isEmpty();
            assertThat(report.getFfmpeg().isAvailable()).isTrue();
            assertThat(report.getFfprobe().isAvailable()).isTrue();
        }

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV — aynan shu holat uchun klass yozilgan.
         */
        @Test
        @DisplayName("FFmpeg yo'q bo'lsa — muammo AYTILADI")
        void missingFfmpegIsReported(@TempDir Path temp) {
            VideoSystemHealth.Report report = health(
                    MISSING, fakeFfmpeg(temp, "ffprobe", FULL_ENCODERS),
                    temp, DataSize.ofBytes(1)).check();

            assertThat(report.isHealthy()).isFalse();
            assertThat(report.getProblems()).hasSize(1);
            // Xabar YO'LNI ham aytadi — aks holda admin qayerga
            // qarashni bilmasdi.
            assertThat(report.getProblems().get(0)).contains("FFmpeg", MISSING);
            assertThat(report.getFfmpeg().isAvailable()).isFalse();
        }

        @Test
        @DisplayName("ffprobe yo'q bo'lsa — u ham AYTILADI")
        void missingFfprobeIsReported(@TempDir Path temp) {
            VideoSystemHealth.Report report = health(
                    fakeFfmpeg(temp, "ffmpeg", FULL_ENCODERS), MISSING,
                    temp, DataSize.ofBytes(1)).check();

            assertThat(report.isHealthy()).isFalse();
            assertThat(report.getProblems().get(0)).contains("ffprobe");
        }

        /**
         * ⚠️ Ikkalasi ALOHIDA tekshiriladi.
         *
         * Bittasi topilib, ikkinchisi topilmasligi haqiqiy holat:
         * ba'zi paket menejerlari ularni alohida beradi.
         */
        @Test
        @DisplayName("Ikkalasi ham yo'q bo'lsa — IKKI muammo")
        void bothMissingGiveTwoProblems(@TempDir Path temp) {
            VideoSystemHealth.Report report =
                    health(MISSING, MISSING, temp, DataSize.ofBytes(1)).check();

            assertThat(report.getProblems()).hasSize(2);
        }

        /**
         * ⚠️ FFmpeg ISHGA TUSHISHI yetarli emas.
         *
         * Ba'zi yig'malar {@code libx264} siz keladi. Bunday FFmpeg
         * {@code -version} ga chiroyli javob beradi, lekin transcoding
         * «Unknown encoder» bilan yiqiladi — va bu faqat birinchi
         * video yuklangach ma'lum bo'lardi.
         */
        @Test
        @DisplayName("Kodlovchi yo'q bo'lsa — muammo AYTILADI")
        void missingEncoderIsReported(@TempDir Path temp) {
            String ffmpeg = fakeFfmpeg(temp, "ffmpeg", NO_X264);
            VideoSystemHealth.Report report = health(
                    ffmpeg, fakeFfmpeg(temp, "ffprobe", NO_X264),
                    temp, DataSize.ofBytes(1)).check();

            assertThat(report.isHealthy()).isFalse();
            assertThat(report.getProblems()).hasSize(1);
            assertThat(report.getProblems().get(0)).contains("libx264");

            // ⚠️ Dastur O'ZI joyida — muammo boshqa narsada. Aks
            // holda admin FFmpeg'ni qayta o'rnatib, hech narsa
            // o'zgarmaganini ko'rardi.
            assertThat(report.getFfmpeg().isAvailable()).isTrue();
        }

        /**
         * ⚠️ ENG NOZIK TEKSHIRUV.
         *
         * Ro'yxatda {@code aac_at} va {@code libfdk_aac} bor, sof
         * {@code aac} esa yo'q. Oddiy {@code contains("aac")} buni
         * to'g'ri deb qabul qilardi — va tekshiruv HECH QACHON
         * muammo topmasdi.
         */
        @Test
        @DisplayName("O'xshash nom kodlovchi o'rniga qabul qilinmaydi")
        void similarNameIsNotAMatch(@TempDir Path temp) {
            String ffmpeg = fakeFfmpeg(temp, "ffmpeg", TRAP_ENCODERS);
            VideoSystemHealth.Report report = health(
                    ffmpeg, fakeFfmpeg(temp, "ffprobe", TRAP_ENCODERS),
                    temp, DataSize.ofBytes(1)).check();

            assertThat(report.getProblems()).hasSize(1);
            assertThat(report.getProblems().get(0)).contains("aac");
        }

        /**
         * ⚠️ Ro'yxat o'qilmasa muammo E'LON QILINMAYDI.
         *
         * «Bilmayman» sababli ishlaydigan serverni nosoz deb ko'rsatish
         * yomonroq: admin mavjud bo'lmagan muammoni tuzatishga
         * urinardi.
         */
        @Test
        @DisplayName("Ro'yxat o'qilmasa kodlovchi muammosi aytilmaydi")
        void unreadableListingIsNotAProblem(@TempDir Path temp) throws IOException {
            // `-version` ga javob beradi, qolganida yiqiladi.
            Path script = temp.resolve("qisman");
            Files.writeString(script, """
                    #!/bin/sh
                    if [ "$1" = "-version" ]; then echo "ffmpeg version 9"; exit 0; fi
                    exit 1
                    """);
            script.toFile().setExecutable(true);

            VideoSystemHealth.Report report = health(
                    script.toString(), script.toString(),
                    temp, DataSize.ofBytes(1)).check();

            assertThat(report.isHealthy()).isTrue();
        }

        /**
         * ⚠️ Faylning BORLIGI yetarli emas — u ishga tushishi kerak.
         *
         * Papkaga ishora qiluvchi yo'l, ijro huquqi yo'q fayl yoki
         * boshqa arxitektura uchun yig'ilgan ikkilik — hammasida fayl
         * «bor», lekin ishlamaydi.
         */
        @Test
        @DisplayName("Papka yo'li dastur deb qabul QILINMAYDI")
        void directoryIsNotAnExecutable(@TempDir Path temp) {
            VideoSystemHealth.Report report = health(
                    temp.toString(), fakeFfmpeg(temp, "ffprobe", FULL_ENCODERS),
                    temp, DataSize.ofBytes(1)).check();

            assertThat(report.getFfmpeg().isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Disk joyi")
    class Disk {

        /** Hech qanday diskda bo'lmaydigan chegara. */
        private static final DataSize IMPOSSIBLE = DataSize.ofTerabytes(9000);

        @Test
        @DisplayName("Joy kam bo'lsa — muammo aytiladi")
        void lowDiskIsReported(@TempDir Path temp) {
            String ffmpeg = fakeFfmpeg(temp, "ffmpeg", FULL_ENCODERS);
            VideoSystemHealth.Report report =
                    health(ffmpeg, ffmpeg, temp, IMPOSSIBLE).check();

            assertThat(report.isHealthy()).isFalse();
            assertThat(report.getProblems().get(0)).contains("joy kam");
        }

        @Test
        @DisplayName("Joy kam bo'lsa ish BOSHLANMAYDI")
        void lowDiskBlocksWork(@TempDir Path temp) {
            String ffmpeg = fakeFfmpeg(temp, "ffmpeg", FULL_ENCODERS);
            assertThat(health(ffmpeg, ffmpeg, temp, IMPOSSIBLE).hasRoomFor(null)).isFalse();
        }

        @Test
        @DisplayName("Joy yetarli bo'lsa ish boshlanadi")
        void enoughDiskAllowsWork(@TempDir Path temp) {
            assertThat(healthy(temp).hasRoomFor(null)).isTrue();
        }

        /**
         * ⚠️ Umumiy chegara va AYNAN shu fayl — boshqa savollar.
         *
         * 40 GB bo'sh joy odatiy video uchun ko'p, 30 GB lik 4K manba
         * uchun esa yetmaydi: unga manba + variantlar uchun ~75 GB
         * kerak.
         */
        @Test
        @DisplayName("Juda katta manba uchun joy yetmaydi")
        void hugeSourceDoesNotFit(@TempDir Path temp) {
            assertThat(healthy(temp).hasRoomFor(Long.MAX_VALUE / 4)).isFalse();
        }

        @Test
        @DisplayName("Kichik manba sig'adi")
        void smallSourceFits(@TempDir Path temp) {
            assertThat(healthy(temp).hasRoomFor(1024L)).isTrue();
        }

        /**
         * ⚠️ Noma'lum hajm ishni TO'XTATMAYDI.
         *
         * Aks holda hajmi yozilmagan eski media yozuvlari hech qachon
         * transcoding qilinmasdi — va sabab hech qayerda
         * ko'rinmasdi.
         */
        @Test
        @DisplayName("Hajmi noma'lum manba to'xtatilmaydi")
        void unknownSizeIsAllowed(@TempDir Path temp) {
            VideoSystemHealth h = healthy(temp);

            assertThat(h.hasRoomFor(null)).isTrue();
            assertThat(h.hasRoomFor(0L)).isTrue();
        }

        /**
         * Manbadan 2.5 barobar ko'p joy talab qilinadi: manbaning o'zi
         * va uchta variant bir vaqtda diskda turadi.
         */
        @Test
        @DisplayName("Talab manbadan KATTA")
        void requirementExceedsSource(@TempDir Path temp) {
            assertThat(healthy(temp).requiredBytesFor(1_000_000L))
                    .isGreaterThan(1_000_000L);
        }

        /**
         * ⚠️ Vaqtinchalik papka birinchi ishgacha MAVJUD BO'LMAYDI.
         *
         * Bo'sh joy o'lchanmasa {@code hasRoomFor} har doim
         * {@code false} qaytarardi va transcoding umuman
         * boshlanmasdi — hech qanday xato ko'rsatmasdan.
         */
        @Test
        @DisplayName("Papka hali yaratilmagan bo'lsa ham o'lchanadi")
        void measuresBeforeDirectoryExists(@TempDir Path temp) {
            Path notYet = temp.resolve("hali/yaratilmagan/papka");

            String ffmpeg = fakeFfmpeg(temp, "ffmpeg", FULL_ENCODERS);
            VideoSystemHealth.Report report =
                    health(ffmpeg, ffmpeg, notYet, DataSize.ofBytes(1)).check();

            assertThat(report.getFreeDiskBytes())
                    .as("bo'sh joy o'lchanmadi — transcoding hech qachon boshlanmasdi")
                    .isNotNull()
                    .isGreaterThan(0L);
        }
    }

    @Nested
    @DisplayName("⚠️ Kodlovchilar ro'yxati buyruq bilan MOS")
    class EncoderContract {

        /**
         * ⚠️ Ikki joy bir-biridan ajralib ketishi mumkin.
         *
         * {@code VideoSystemHealth} «libx264 bormi» deb tekshiradi,
         * {@code FfmpegCommandBuilder} esa aynan shu nomni buyruqqa
         * yozadi. Biri o'zgarsa ikkinchisi eskirib qolardi:
         *
         * <ul>
         *   <li>buyruq {@code libx265} ga o'tsa, tekshiruv mavjud
         *       bo'lmagan talabni tekshirib, HAQIQIY muammoni
         *       o'tkazib yuborardi;</li>
         *   <li>tekshiruvdan nom olib tashlansa, kerakli kodlovchi
         *       yo'qligi faqat birinchi videoda bilinardi.</li>
         * </ul>
         *
         * Bu test manba matnini o'qiydi — mo'rt ko'rinadi, lekin
         * boshqa yo'l bilan bu bog'liqlikni ushlab bo'lmaydi.
         */
        @Test
        @DisplayName("Buyruqdagi har bir kodlovchi tekshiriladi")
        void checkedEncodersMatchTheCommand() throws IOException {
            String builder = Files.readString(Path.of(
                    "src/main/java/com/example/backend/Cms/Service/Video/"
                            + "FfmpegCommandBuilder.java"));
            String checker = Files.readString(Path.of(
                    "src/main/java/com/example/backend/Cms/Service/Video/"
                            + "VideoSystemHealth.java"));

            // Buyruq `-c:v` va `-c:a` dan keyin nomni yozadi.
            for (String encoder : List.of("libx264", "aac")) {
                assertThat(builder)
                        .as("buyruq `%s` ni ishlatmay qo'ydi — tekshiruv ham "
                                + "yangilanishi kerak", encoder)
                        .contains("\"" + encoder + "\"");
                assertThat(checker)
                        .as("`%s` tekshiruvdan tushib qolgan — uning yo'qligi "
                                + "faqat birinchi videoda bilinardi", encoder)
                        .contains("\"" + encoder + "\"");
            }
        }
    }

    @Nested
    @DisplayName("Ishga tushish xabari")
    class Startup {

        /**
         * ⚠️ Ilova FFmpeg'siz ham KO'TARILADI.
         *
         * Transcoding — qo'shimcha imkoniyat. Uning yo'qligi uchun
         * ishlaydigan saytni yiqitish nomutanosib bo'lardi: katalog,
         * to'lovlar va casting moduli unga umuman bog'liq emas.
         */
        @Test
        @DisplayName("FFmpeg yo'q bo'lsa ham ISTISNO tashlanmaydi")
        void startupNeverThrows(@TempDir Path temp) {
            health(MISSING, MISSING, temp, DataSize.ofTerabytes(9000))
                    .reportOnStartup();
        }
    }
}
