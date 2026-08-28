package com.example.backend.Cms.Service.Video;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Pleyerga beriladigan HLS manzili.
 *
 * <h2>Ikki yo'l bor va tanlov OMBORGA bog'liq</h2>
 * <ul>
 *   <li><b>S3 sozlangan</b> — manzil bizning proksi endpointimizga
 *       ishora qiladi ({@link com.example.backend.Cms.Controller.HlsController}).
 *       Segmentlar imzolangan havola bilan ombordan keladi, ya'ni
 *       pullik kontent himoyalangan;</li>
 *   <li><b>S3 yo'q</b> — eski xatti-harakat: to'g'ridan-to'g'ri CDN
 *       manzili. Imzolash imkoni bo'lmagani uchun boshqa yo'l ham yo'q.</li>
 * </ul>
 *
 * ⚠️ Ikkinchi holat ATAYLAB saqlanadi. Lokal omborda ishlayotgan
 * dasturchi uchun hech narsa o'zgarmaydi — aks holda S3'siz muhitda
 * HLS umuman yo'qolib, «mahalliyda ishlamayapti» degan uzun izlanish
 * boshlanardi.
 */
@Service
@RequiredArgsConstructor
public class PlaybackUrlService {

    private final CdnUrlService cdnUrlService;
    private final PlaybackTicketService ticketService;

    /** Faqat S3 ombori sozlanganda mavjud. */
    private final Optional<SignedUrlProvider> signedUrls;

    /**
     * Shu tomoshabin uchun HLS master manzili.
     *
     * ⚠️ Bu metod HUQUQ TEKSHIRMAYDI. Uni chaqirgan joy allaqachon
     * {@code AccessService} qarorini olgan bo'lishi shart —
     * {@code WatchController} da manbalar faqat ruxsat berilganda
     * yig'iladi.
     *
     * @return mutlaq CDN manzili, nisbiy proksi yo'li, yoki
     *         {@code null} — transcoding tugamagan
     */
    public String hlsUrlFor(User viewer, MediaAsset asset) {
        if (asset == null) {
            return null;
        }

        String master = asset.getHlsMasterKey();
        if (master == null || master.isBlank()) {
            // «Hali yo'q» — klient eski `/raw` yo'liga qaytadi.
            return null;
        }

        if (signedUrls.isEmpty() || !signedUrls.get().isAvailable()) {
            return cdnUrlService.masterUrl(master);
        }

        // ⚠️ NISBIY yo'l qaytariladi. Server o'zining tashqi domenini
        // ishonchli bilmaydi (proksi ortida `Host` almashishi mumkin),
        // klient esa o'z `BASE_URL` ini biladi. Taxmin qilingan domen
        // jimgina noto'g'ri havola berardi.
        return "/api/v1/app/media/" + asset.getId() + "/hls/" + fileNameOf(master)
                + "?t=" + UriUtils.encode(ticketService.issue(viewer, asset.getId()),
                        StandardCharsets.UTF_8);
    }

    /** {@code /videos/7/hls/master.m3u8} → {@code master.m3u8} */
    private String fileNameOf(String key) {
        int slash = key.lastIndexOf('/');
        return slash < 0 ? key : key.substring(slash + 1);
    }
}
