package com.example.backend.Cms.Service.Video;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Manba videosiga qarab sifat variantlarini tanlaydi.
 *
 * <h2>⚠️ Yuqoriga cho'zilmaydi</h2>
 * 720p manbadan 1080p yasash sifat QO'SHMAYDI: yo'q piksellarni
 * o'ylab topib bo'lmaydi. U faqat disk joyini va protsessor vaqtini
 * sarflaydi, foydalanuvchi esa kattaroq faylni yuklab, ayni sifatni
 * ko'radi (§9).
 *
 * <h2>Vertikal video</h2>
 * «720p» — bu kadr balandligi emas, sifat DARAJASI. Odamlar
 * 1080×1920 lik videoni «1080p vertikal» deyishadi, «1920p» emas.
 *
 * Shuning uchun taqqoslash KICHIK tomon bo'yicha boradi va
 * masshtablashda ham aynan u belgilanadi. Loyihada vertikal kontent
 * birinchi darajali (ТЗ §19 — Reels), ya'ni bu chekka holat emas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(VideoTranscodingProperties.class)
public class VideoProfileSelector {

    private final VideoTranscodingProperties properties;

    /**
     * Shu video uchun yaratiladigan variantlar — sifat bo'yicha
     * kamayish tartibida.
     *
     * ⚠️ HAR DOIM kamida bitta variant qaytaradi. Bo'sh ro'yxat
     * «transcoding qilinmasin» degani bo'lardi va video HLS'siz
     * qolib, pleyerda umuman ochilmasdi.
     */
    public List<SelectedProfile> select(VideoMetadata source) {
        if (source == null || !source.isUsable()) {
            throw new VideoProcessingException(
                    "Video o'lchamlari noma'lum — profil tanlab bo'lmaydi");
        }

        int quality = qualityHeight(source);
        List<TranscodingProfile> ladder = properties.sortedByQuality();

        if (ladder.isEmpty()) {
            throw new VideoProcessingException(
                    "Transcoding profillari sozlanmagan (app.video.transcoding.profiles)");
        }

        List<SelectedProfile> selected = new ArrayList<>();
        for (TranscodingProfile profile : ladder) {
            if (profile.getHeight() <= quality) {
                selected.add(scaled(profile, source));
            }
        }

        if (selected.isEmpty()) {
            // ⚠️ Manba eng past profildan ham kichik (masalan 360p,
            // zinapoyaning pastki pog'onasi esa 480p).
            //
            // Bu yerda hech narsa qaytarmaslik videoni HLS'siz
            // qoldirardi. O'rniga eng past profil olinadi, LEKIN
            // o'lchamlar manbadan oshmaydi — `scaled` buni ta'minlaydi.
            TranscodingProfile lowest = ladder.get(ladder.size() - 1);
            log.info("Manba {} p — eng past profildan ({} p) kichik, o'z o'lchamida qoldiriladi",
                    quality, lowest.getHeight());
            selected.add(scaled(lowest, source));
        }

        return selected;
    }

    /**
     * Sifat darajasi — «p» raqami.
     *
     * Gorizontal videoda bu balandlik, vertikalda kenglik. Ya'ni
     * KICHIK tomon.
     */
    private int qualityHeight(VideoMetadata source) {
        return Math.min(source.width(), source.height());
    }

    /**
     * Profil uchun chiqish o'lchamlarini hisoblaydi.
     *
     * <h2>⚠️ O'lchamlar JUFT bo'lishi SHART</h2>
     * H.264 ning {@code yuv420p} formati toq o'lchamni qabul
     * qilmaydi: xromatik kanallar ikki barobar kichik va ular butun
     * songa bo'linishi kerak.
     *
     * 1920×1079 lik manbani 720 balandlikka keltirsak kenglik
     * 1281.4 → 1281 chiqadi va FFmpeg xato beradi. Shuning uchun
     * juftga yaxlitlanadi.
     *
     * <h2>Manbadan katta qilinmaydi</h2>
     * Eng past profil ham manbadan katta bo'lsa (360p video, 480p
     * profil), o'lchamlar manbaning O'ZIDA qoldiriladi.
     */
    private SelectedProfile scaled(TranscodingProfile profile, VideoMetadata source) {
        boolean portrait = source.height() > source.width();
        int sourceShort = Math.min(source.width(), source.height());
        int sourceLong = Math.max(source.width(), source.height());

        // Cho'zilmaslik qoidasi: qisqa tomon manbadan oshmaydi.
        int shortSide = Math.min(profile.getHeight(), sourceShort);
        int longSide = (int) Math.round((double) shortSide * sourceLong / sourceShort);

        int width = even(portrait ? shortSide : longSide);
        int height = even(portrait ? longSide : shortSide);

        return new SelectedProfile(profile, width, height);
    }

    /** Eng yaqin juft songa. Kamida 2 — nol o'lchamli kadr bo'lmaydi. */
    private int even(int value) {
        int result = value - (value % 2);
        return Math.max(2, result);
    }

    /**
     * Tanlangan profil va uning aniq o'lchamlari.
     *
     * @param profile sozlamadagi profil (bitrate, yorliq)
     * @param width   chiqish kengligi — juft
     * @param height  chiqish balandligi — juft
     */
    public record SelectedProfile(TranscodingProfile profile, int width, int height) {

        /** {@code 720p} — HLS papkasi va S3 kaliti uchun. */
        public String label() {
            return profile.label();
        }

        /**
         * {@code 1080x1920} — o'qish uchun qulay shakl.
         *
         * ⚠️ Bu {@code master.m3u8} uchun EMAS. U yerdagi
         * {@code RESOLUTION} atributini FFmpeg o'zi yozadi va u
         * HAQIQIY chiqish o'lchamlaridan oladi — bu qiymatdan emas.
         *
         * Faqat log va testlar uchun.
         */
        public String resolution() {
            return width + "x" + height;
        }
    }
}
