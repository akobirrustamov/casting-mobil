package com.example.backend.Cms.Service.Video;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@code ffprobe} ning JSON chiqishini tahlil qiladi.
 *
 * <h2>Nega jarayondan ALOHIDA</h2>
 * Bu yerda haqiqiy mantiq bor: qaysi oqim video, aylantirish qanday
 * hisobga olinadi, {@code "25/1"} kabi kasr qanday o'qiladi.
 * {@code ffprobe} ni ishga tushirish esa oddiy protsess chaqiruvi.
 *
 * Ajratilganda tahlil {@code ffprobe} O'RNATILMAGAN mashinada ham
 * to'liq sinaladi — namuna JSON'lar bilan.
 */
final class FfprobeOutputParser {

    private FfprobeOutputParser() {
    }

    static VideoMetadata parse(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return empty();
        }

        JsonNode video = videoStream(root.path("streams"));
        JsonNode audio = firstStreamOfType(root.path("streams"), "audio");
        JsonNode format = root.path("format");

        int rotation = rotationOf(video);
        Integer rawWidth = intOrNull(video.path("width"));
        Integer rawHeight = intOrNull(video.path("height"));

        // ⚠️ Aylantirilgan videoda ko'rsatiladigan o'lchamlar ALMASHADI.
        //
        // Telefonda vertikal olingan video faylda ko'pincha 1920×1080
        // bo'lib yotadi va 90° aylantirish belgisi bilan keladi.
        // Belgi e'tiborga olinmasa profil tanlash uni gorizontal deb
        // hisoblardi va natija cho'zilgan yoki noto'g'ri o'lchamda
        // chiqardi.
        boolean swapped = rotation == 90 || rotation == 270;

        return new VideoMetadata(
                swapped ? rawHeight : rawWidth,
                swapped ? rawWidth : rawHeight,
                durationSeconds(format, video),
                frameRate(video),
                textOrNull(video.path("codec_name")),
                textOrNull(audio.path("codec_name")),
                longOrNull(format.path("bit_rate")));
    }

    private static VideoMetadata empty() {
        return new VideoMetadata(null, null, null, null, null, null, null);
    }

    /**
     * Haqiqiy video oqimini tanlaydi.
     *
     * <h2>⚠️ Muqova rasmi ham «video oqim» bo'lib ko'rinadi</h2>
     * Albom muqovasi joylashtirilgan {@code .mp4} da ikkita video oqim
     * bo'ladi: haqiqiy video va {@code mjpeg} formatidagi BITTA KADR.
     * Muqova ko'pincha oqimlar ro'yxatida BIRINCHI turadi.
     *
     * Oddiygina birinchisini olsak, 600×600 muqova o'lchamlari
     * videoning o'lchami deb qabul qilinardi — va butun transcoding
     * shu asosda qurilardi.
     *
     * {@code ffprobe} muqovani {@code disposition.attached_pic = 1}
     * bilan belgilaydi.
     */
    private static JsonNode videoStream(JsonNode streams) {
        if (!streams.isArray()) {
            return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
        }

        JsonNode fallback = com.fasterxml.jackson.databind.node.MissingNode.getInstance();
        for (JsonNode stream : streams) {
            if (!"video".equals(stream.path("codec_type").asText(null))) {
                continue;
            }
            if (stream.path("disposition").path("attached_pic").asInt(0) == 1) {
                // Muqova — faqat boshqa hech narsa topilmasa ishlatiladi.
                if (fallback.isMissingNode()) {
                    fallback = stream;
                }
                continue;
            }
            return stream;
        }
        return fallback;
    }

    private static JsonNode firstStreamOfType(JsonNode streams, String type) {
        if (streams.isArray()) {
            for (JsonNode stream : streams) {
                if (type.equals(stream.path("codec_type").asText(null))) {
                    return stream;
                }
            }
        }
        return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }

    /**
     * Aylantirish burchagi, darajada.
     *
     * ⚠️ {@code ffprobe} uni IKKI xil joyda beradi: eski fayllarda
     * {@code tags.rotate}, yangilarida {@code side_data_list} ichidagi
     * Display Matrix. Faqat bittasiga qarash yarim holatlarni
     * o'tkazib yuborardi.
     *
     * Manfiy burchak ({@code -90}) musbatga keltiriladi.
     */
    private static int rotationOf(JsonNode video) {
        int rotation = 0;

        JsonNode tag = video.path("tags").path("rotate");
        if (!tag.isMissingNode()) {
            rotation = (int) parseDouble(tag.asText(null), 0);
        }

        JsonNode sideData = video.path("side_data_list");
        if (sideData.isArray()) {
            for (JsonNode entry : sideData) {
                if (entry.has("rotation")) {
                    rotation = (int) entry.path("rotation").asDouble(0);
                    break;
                }
            }
        }

        int normalised = rotation % 360;
        return normalised < 0 ? normalised + 360 : normalised;
    }

    /**
     * Davomiylik.
     *
     * ⚠️ Avval {@code format}, keyin video oqimi. Ba'zi konteynerlarda
     * ({@code .mkv} bunga moyil) umumiy davomiylik ko'rsatilmaydi,
     * lekin oqim darajasida bo'ladi.
     */
    private static Integer durationSeconds(JsonNode format, JsonNode video) {
        double fromFormat = parseDouble(format.path("duration").asText(null), -1);
        double fromStream = parseDouble(video.path("duration").asText(null), -1);
        double seconds = fromFormat > 0 ? fromFormat : fromStream;

        if (seconds <= 0) {
            return null;
        }
        // Yaxlitlanadi: davomiylik sekundlarda saqlanadi (`MediaAsset`).
        return (int) Math.round(seconds);
    }

    /**
     * Kadr chastotasi.
     *
     * ⚠️ {@code ffprobe} uni KASR sifatida beradi: {@code "25/1"},
     * {@code "30000/1001"} (NTSC ning 29.97 i). Noma'lum bo'lsa
     * {@code "0/0"} qaytadi — nolga bo'lish shu yerda kutib olinadi.
     */
    private static Double frameRate(JsonNode video) {
        String raw = textOrNull(video.path("avg_frame_rate"));
        if (raw == null) {
            raw = textOrNull(video.path("r_frame_rate"));
        }
        if (raw == null) {
            return null;
        }

        int slash = raw.indexOf('/');
        if (slash < 0) {
            double value = parseDouble(raw, -1);
            return value > 0 ? value : null;
        }

        double numerator = parseDouble(raw.substring(0, slash), 0);
        double denominator = parseDouble(raw.substring(slash + 1), 0);
        if (denominator == 0 || numerator <= 0) {
            return null;
        }
        // Ikki xonagacha: 29.970029… kabi qiymat foydasiz aniqlikda.
        return Math.round(numerator / denominator * 100.0) / 100.0;
    }

    // ------------------------------------------------------- yordamchilar

    private static String textOrNull(JsonNode node) {
        String value = node.asText(null);
        return value == null || value.isBlank() || "N/A".equals(value) ? null : value;
    }

    private static Integer intOrNull(JsonNode node) {
        return node.isMissingNode() || !node.canConvertToInt() ? null : node.asInt();
    }

    private static Long longOrNull(JsonNode node) {
        String value = textOrNull(node);
        if (value == null) {
            return null;
        }
        double parsed = parseDouble(value, -1);
        return parsed > 0 ? (long) parsed : null;
    }

    /** Matnni songa aylantiradi. Aylanmasa — berilgan zaxira qiymat. */
    private static double parseDouble(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            // "N/A" va shunga o'xshash qiymatlar — bu XATO emas,
            // ffprobe ning odatiy javobi.
            return fallback;
        }
    }
}
