package com.example.backend.Cms.Service.Video;

import lombok.Getter;
import lombok.Setter;

/**
 * Bitta sifat varianti.
 *
 * <h2>Nega bitrate MATN</h2>
 * {@code "5000k"} — FFmpeg ning o'z formati. Uni raqamga aylantirib,
 * keyin qaytadan matnga o'girish faqat xato imkoniyatini qo'shardi
 * (nol soni adashishi oson). Qiymat sozlamadan FFmpeg ga o'zgarishsiz
 * o'tadi.
 */
@Getter
@Setter
public class TranscodingProfile {

    /**
     * Sifat balandligi — «720p» dagi 720.
     *
     * ⚠️ Bu KADRning balandligi emas, sifat DARAJASI. Vertikal videoda
     * u KENGLIKKA qo'llanadi: 1080×1920 lik video — bu «1080p
     * vertikal», «1920p» emas. Batafsil: {@link VideoProfileSelector}.
     */
    private int height;

    /** Masalan {@code 5000k}. */
    private String videoBitrate;

    /** Masalan {@code 128k}. */
    private String audioBitrate = "128k";

    public TranscodingProfile() {
    }

    public TranscodingProfile(int height, String videoBitrate, String audioBitrate) {
        this.height = height;
        this.videoBitrate = videoBitrate;
        this.audioBitrate = audioBitrate;
    }

    /** Yorliq — S3 kalitida va HLS papkasida ishlatiladi: {@code 720p}. */
    public String label() {
        return height + "p";
    }
}
