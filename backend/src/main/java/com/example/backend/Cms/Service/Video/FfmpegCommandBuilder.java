package com.example.backend.Cms.Service.Video;

import com.example.backend.Cms.Service.Video.VideoProfileSelector.SelectedProfile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HLS yaratish uchun FFmpeg buyrug'ini quradi.
 *
 * <h2>Nega jarayondan ajratilgan</h2>
 * Buyruq — bu yerdagi butun mantiq. FFmpeg ni ishga tushirish esa
 * oddiy protsess chaqiruvi. Ajratilgach buyruq FFmpeg O'RNATILMAGAN
 * mashinada ham to'liq sinaladi.
 *
 * <h2>Bitta chaqiruv, uchta variant emas</h2>
 * Har variant uchun alohida FFmpeg ishga tushirilsa, manba video
 * UCH MARTA dekodlanardi. Ikki soatlik film uchun bu protsessor
 * vaqtining uch barobari.
 *
 * {@code -var_stream_map} bitta chaqiruvda barcha variantlarni
 * yasaydi: manba bir marta o'qiladi, natija esa uchga bo'linadi.
 */
final class FfmpegCommandBuilder {

    /**
     * Segment uzunligi, soniya.
     *
     * ⚠️ Apple tavsiyasi — 6 soniya. Qisqaroq bo'lsa fayllar soni va
     * so'rovlar ko'payadi, uzunroq bo'lsa sifat almashishi sekinlashadi
     * (pleyer keyingi segmentni kutishi kerak).
     */
    private static final int SEGMENT_SECONDS = 6;

    private FfmpegCommandBuilder() {
    }

    /**
     * @param ffmpegPath ijro etuvchi fayl
     * @param source     manba video
     * @param outputDir  natija papkasi — variantlar shu yerda
     * @param profiles   tanlangan variantlar, sifat bo'yicha kamayishda
     * @param hasAudio   manbada ovoz bormi
     */
    static List<String> build(String ffmpegPath,
                              Path source,
                              Path outputDir,
                              List<SelectedProfile> profiles,
                              boolean hasAudio) {

        if (profiles == null || profiles.isEmpty()) {
            throw new VideoProcessingException("Variantlar ro'yxati bo'sh");
        }

        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);

        // Mavjud faylni so'ramasdan qayta yozadi. Qayta urinishda
        // papkada eski qoldiqlar bo'lishi mumkin va savol berish
        // jarayonni abadiy kutishga qo'yardi.
        command.add("-y");

        // Faqat xatolar. FFmpeg ning odatiy chiqishi juda ko'p va u
        // progress oqimiga aralashardi.
        command.add("-v");
        command.add("error");

        command.add("-i");
        command.add(source.toString());

        // ------------------------------------------------- oqimlarni ulash
        //
        // Har variant uchun manba oqimlari qayta ulanadi.
        for (int i = 0; i < profiles.size(); i++) {
            command.add("-map");
            command.add("0:v:0");
            if (hasAudio) {
                command.add("-map");
                command.add("0:a:0");
            }
        }

        // ------------------------------------------------------- kodeklar
        //
        // H.264 + AAC — mobil moslik uchun. Boshqa kodeklar (HEVC, AV1)
        // kichikroq fayl beradi, lekin eski qurilmalarda ochilmaydi
        // (§10).
        command.add("-c:v");
        command.add("libx264");
        command.add("-profile:v");
        // `main` — 1080p gacha barcha mobil qurilmalarda ishlaydi.
        // `high` biroz samaraliroq, lekin eski Android'da muammo beradi.
        command.add("main");
        command.add("-preset");
        command.add("veryfast");
        command.add("-pix_fmt");
        // ⚠️ MAJBURIY. Manba 10-bitli bo'lsa FFmpeg uni saqlab qolardi
        // va natija ko'p qurilmada ochilmasdi.
        command.add("yuv420p");

        if (hasAudio) {
            command.add("-c:a");
            command.add("aac");
            command.add("-ac");
            command.add("2");
        }

        // ------------------------------------- ⚠️ SEGMENT CHEGARALARI
        //
        // ABR ishlashi uchun BARCHA variantlarda segmentlar AYNI
        // vaqtlarda boshlanishi shart. Aks holda pleyer sifatni
        // almashtirganda kadr sakraydi yoki oqim umuman uziladi.
        //
        // `force_key_frames` har 6 soniyada kalit kadr qo'yadi —
        // kadr chastotasidan QAT'I NAZAR. GOP o'lchamini qo'lda
        // hisoblash (fps × 6) noto'g'ri fps da chegaralarni siljitardi,
        // va `ffprobe` fps ni har doim ham bermaydi.
        command.add("-force_key_frames");
        command.add("expr:gte(t,n_forced*" + SEGMENT_SECONDS + ")");

        // ⚠️ Sahna almashganda FFmpeg O'ZI kalit kadr qo'yadi va u
        // variantlarda turli joyda chiqadi — chegaralar yana ajraladi.
        command.add("-sc_threshold");
        command.add("0");

        // ------------------------------------------------ variant sozlamalari
        for (int i = 0; i < profiles.size(); i++) {
            SelectedProfile profile = profiles.get(i);

            command.add("-filter:v:" + i);
            command.add("scale=" + profile.width() + ":" + profile.height());

            command.add("-b:v:" + i);
            command.add(profile.profile().getVideoBitrate());

            // ⚠️ `maxrate` va `bufsize` — bitrate CHEGARASI. Ularsiz
            // FFmpeg ko'rsatilgan qiymatni o'rtacha deb qabul qiladi va
            // murakkab sahnalarda uni bir necha barobar oshirib
            // yuborishi mumkin. Sekin kanalda bu uzilish demakdir.
            command.add("-maxrate:v:" + i);
            command.add(profile.profile().getVideoBitrate());
            command.add("-bufsize:v:" + i);
            command.add(doubled(profile.profile().getVideoBitrate()));

            if (hasAudio) {
                command.add("-b:a:" + i);
                command.add(profile.profile().getAudioBitrate());
            }
        }

        // --------------------------------------------------------- HLS
        command.add("-f");
        command.add("hls");

        command.add("-hls_time");
        command.add(String.valueOf(SEGMENT_SECONDS));

        // VOD: butun davomiylik oldindan ma'lum, pleyer oldinga o'ta
        // oladi. Usiz u oqimni jonli efir deb hisoblardi.
        command.add("-hls_playlist_type");
        command.add("vod");

        // ⚠️ Segmentlar SAQLANADI. Sukut qiymat oxirgi bir nechtasini
        // qoldirib, qolganini o'chiradi — jonli efir uchun to'g'ri,
        // VOD uchun esa fayllarning yarmi yo'qolishini bildiradi.
        command.add("-hls_list_size");
        command.add("0");

        // fMP4 — `.ts` emas. Sabab roadmap §3.4 da.
        command.add("-hls_segment_type");
        command.add("fmp4");

        command.add("-hls_fmp4_init_filename");
        command.add("init.mp4");

        command.add("-hls_segment_filename");
        command.add(outputDir.resolve("%v").resolve("segment_%05d.m4s").toString());

        // ⚠️ `master.m3u8` ni FFmpeg ning O'ZI yozadi.
        //
        // Uni qo'lda yozish mumkin edi, lekin unda `CODECS` atributini
        // ham qo'lda hisoblash kerak bo'lardi (`avc1.4d401f` kabi
        // qatorlar profil va darajaga bog'liq). Noto'g'ri qiymat esa
        // pleyerni oqimni umuman ochmaslikka olib boradi — va bu
        // faqat qurilmada bilinadi.
        command.add("-master_pl_name");
        command.add("master.m3u8");

        command.add("-var_stream_map");
        command.add(variantMap(profiles, hasAudio));

        // Progress mashina o'qiydigan shaklda stdout ga.
        command.add("-progress");
        command.add("pipe:1");

        command.add(outputDir.resolve("%v").resolve("index.m3u8").toString());

        return command;
    }

    /**
     * {@code v:0,a:0,name:1080p v:1,a:1,name:720p …}
     *
     * ⚠️ {@code name:} — bu {@code %v} nima bo'lishini belgilaydi.
     * Usiz papkalar {@code 0}, {@code 1}, {@code 2} deb atalardi va
     * S3 dagi kalitdan qaysi sifat ekanini bilib bo'lmasdi.
     */
    private static String variantMap(List<SelectedProfile> profiles, boolean hasAudio) {
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < profiles.size(); i++) {
            String entry = "v:" + i;
            if (hasAudio) {
                entry += ",a:" + i;
            }
            entry += ",name:" + profiles.get(i).label();
            entries.add(entry);
        }
        return String.join(" ", entries);
    }

    /**
     * Bufer o'lchami — bitratening ikki barobari.
     *
     * {@code "5000k"} → {@code "10000k"}. Format saqlanadi, chunki u
     * sozlamadan keladi va FFmpeg ga o'zgarishsiz o'tishi kerak.
     */
    private static String doubled(String bitrate) {
        if (bitrate == null || bitrate.isBlank()) {
            throw new VideoProcessingException("Profil bitrate'i ko'rsatilmagan");
        }

        String trimmed = bitrate.trim();
        String digits = trimmed.chars()
                .takeWhile(Character::isDigit)
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining());

        if (digits.isEmpty()) {
            throw new VideoProcessingException("Bitrate tushunarsiz: " + bitrate);
        }

        String suffix = trimmed.substring(digits.length());
        return (Long.parseLong(digits) * 2) + suffix;
    }
}
