package com.example.backend.Cms.Service.Video;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Transcoding sozlamalari.
 *
 * <h2>⚠️ Bitratelar kodda QOTIRILMAYDI</h2>
 * Ular server quvvatiga, kanal kengligiga va sifat talabiga qarab
 * o'zgaradi. Kodda tarqoq yozilsa har o'zgarish uchun yangi reliz
 * kerak bo'lardi (§10).
 *
 * <h2>Standart zinapoya</h2>
 * Sozlama berilmasa quyidagi uch variant ishlatiladi. Ular ТЗ dagi
 * qiymatlarga mos va H.264 uchun keng qabul qilingan:
 *
 * <pre>
 *   1080p — 5000k
 *    720p — 2800k
 *    480p — 1200k
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.video.transcoding")
public class VideoTranscodingProperties {

    private List<TranscodingProfile> profiles = new ArrayList<>(List.of(
            new TranscodingProfile(1080, "5000k", "128k"),
            new TranscodingProfile(720, "2800k", "128k"),
            new TranscodingProfile(480, "1200k", "96k")));

    /**
     * Balandlik bo'yicha KAMAYISH tartibida.
     *
     * ⚠️ Tartib muhim: {@code master.m3u8} da variantlar sifat bo'yicha
     * tartiblangan bo'lishi kerak, aks holda ba'zi pleyerlar birinchi
     * variantni sukut deb oladi va u eng past sifat bo'lib qolardi.
     *
     * Sozlamada tartib buzilgan bo'lishi mumkin — bu yerda tuzatiladi,
     * ya'ni sozlama yozgan odam tartibni eslab qolishi shart emas.
     */
    public List<TranscodingProfile> sortedByQuality() {
        return profiles.stream()
                .filter(p -> p.getHeight() > 0)
                .sorted(Comparator.comparingInt(TranscodingProfile::getHeight).reversed())
                .toList();
    }
}
