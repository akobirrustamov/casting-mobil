package com.example.backend.Cms.Service.Storage;

import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Rasm fayli uchun hajm chegarasi.
 *
 * <h2>⚠️ Nega ALOHIDA chegara kerak</h2>
 * Umumiy chegara ({@code app.upload.max-bytes}) 5 GB — u VIDEO uchun
 * o'lchangan. Rasm uchun bu amalda chegara yo'qligini bildiradi:
 * admin tasodifan 200 MB lik RAW faylni afisha sifatida yuklasa,
 * server uni qabul qilardi va u har bir kartochkada tomoshabinga
 * yuborilardi.
 *
 * <h2>⚠️ Nega ikkala yo'lda ham</h2>
 * Fayl serverga IKKI yo'l bilan keladi:
 *
 * <ul>
 *   <li>{@code POST /app/admin/media} — to'g'ridan-to'g'ri, rasmlar
 *       uchun. Bu yerda hech qanday hajm tekshiruvi YO'Q edi;</li>
 *   <li>{@code POST /app/admin/uploads} — bo'laklab, videolar uchun.
 *       Bu yerda faqat 5 GB lik umumiy chegara bor edi.</li>
 * </ul>
 *
 * Ikkovi bitta qoidaga bo'ysunishi uchun tekshiruv shu yerda —
 * alohida yozilsa, ular vaqt o'tib ajralib ketardi va odam
 * chegarani chetlab o'tish yo'lini topardi.
 *
 * <h2>⚠️ Faqat RASMGA tegishli</h2>
 * Video bu tekshiruvdan o'tmaydi: u tabiatan katta va o'z chegarasi
 * bor. Tur {@link MediaType#detect} bilan aniqlanadi — ya'ni mime
 * ham, kengaytma ham hisobga olinadi.
 */
@Component
public class ImageSizeLimit {

    /**
     * Rasm uchun eng katta hajm — sukut bo'yicha 10 MB.
     *
     * ⚠️ Sozlama panel yozuvlari bilan mos bo'lishi kerak
     * ({@code frontend/src/adminpanel/mediaSpecs.js}, {@code maxMb}).
     * Ular ajralib qolsa, panel «10 MB gacha» deb turadi-yu, server
     * rad etadi — va odam nima uchun ekanini tushunmaydi.
     */
    @Value("${app.upload.max-image-bytes:10485760}")
    private long maxImageBytes;

    public long maxBytes() {
        return maxImageBytes;
    }

    public long maxMb() {
        return maxImageBytes / 1024 / 1024;
    }

    /**
     * Rasm bo'lsa hajmini tekshiradi.
     *
     * ⚠️ Video va noma'lum tur TEKSHIRILMAYDI — ular boshqa
     * chegaraga bo'ysunadi.
     *
     * @param sizeBytes {@code null} yoki manfiy bo'lsa tekshirilmaydi:
     *                  hajmi noma'lum bo'lgan oqimni bu yerda to'xtatib
     *                  bo'lmaydi, uni chaqiruvchi hal qiladi
     */
    public void check(String filename, String mimeType, Long sizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            return;
        }
        if (MediaType.detect(mimeType, filename) != MediaType.IMAGE) {
            return;
        }
        if (sizeBytes > maxImageBytes) {
            throw BusinessException.validation(
                    "Rasm juda katta: " + (sizeBytes / 1024 / 1024) + " MB. "
                            + "Ruxsat etilgan chegara: " + maxMb() + " MB");
        }
    }
}
