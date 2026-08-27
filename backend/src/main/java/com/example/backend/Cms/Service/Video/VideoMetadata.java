package com.example.backend.Cms.Service.Video;

/**
 * {@code ffprobe} aniqlagan video xususiyatlari.
 *
 * <h2>Nega hamma maydon {@code null} bo'lishi mumkin</h2>
 * {@code ffprobe} ba'zi qiymatlarni bera olmaydi: buzuq faylda
 * davomiylik {@code "N/A"} bo'ladi, oqim sifatida yozilgan videoda
 * bitrate umuman ko'rsatilmaydi.
 *
 * Bunday holatda 0 yozish YOLG'ON bo'lardi — «davomiyligi nol soniya»
 * bilan «davomiyligi noma'lum» butunlay boshqa narsa va ikkinchisi
 * pleyer uchun muhim.
 *
 * @param width          ko'rsatiladigan kenglik (aylantirish hisobga olingan)
 * @param height         ko'rsatiladigan balandlik (aylantirish hisobga olingan)
 * @param durationSeconds davomiylik
 * @param fps            kadr chastotasi
 * @param videoCodec     masalan {@code h264}
 * @param audioCodec     masalan {@code aac}. Ovozsiz videoda {@code null}
 * @param bitrate        umumiy bitrate, bit/sek
 */
public record VideoMetadata(
        Integer width,
        Integer height,
        Integer durationSeconds,
        Double fps,
        String videoCodec,
        String audioCodec,
        Long bitrate) {

    /**
     * Sifat variantlarini tanlash uchun yetarli ma'lumot bormi.
     *
     * ⚠️ Balandliksiz profil tanlab bo'lmaydi: qaysi variantlar
     * kerakligi aynan shunga bog'liq (720p manbadan 1080p yasalmaydi).
     */
    public boolean isUsable() {
        return width != null && width > 0 && height != null && height > 0;
    }
}
