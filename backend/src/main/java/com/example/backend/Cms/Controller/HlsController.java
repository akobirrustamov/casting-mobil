package com.example.backend.Cms.Controller;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.Video.HlsPlaylistService;
import com.example.backend.Cms.Service.Video.PlaybackTicketService;
import com.example.backend.Cms.Service.Video.SignedUrlProvider;
import com.example.backend.Entity.User;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * HLS playlistlari — pullik kontent himoyasi (§4.10).
 *
 * <h2>Muammo</h2>
 * Segmentlar CDN'dan kelganda ular Spring Boot'dan O'TMAYDI, ya'ni
 * {@code AccessService} tekshiruvi yo'qoladi. Ochiq CDN manzili esa
 * bir marta nusxalansa, pullik film istalgan joyda tarqalardi.
 *
 * <h2>Yechim: playlist bizdan, segmentlar ombordan</h2>
 * <pre>
 *   pleyer → BIZ:    master.m3u8      (huquq tekshiriladi)
 *   pleyer → BIZ:    720p/index.m3u8  (huquq QAYTA tekshiriladi)
 *   pleyer → OMBOR:  segment_00001.m4s (imzolangan havola)
 * </pre>
 *
 * ⚠️ Video baribir serverimizdan O'TMAYDI. Playlist — bir necha
 * kilobayt matn; gigabaytlar to'g'ridan-to'g'ri ombordan keladi.
 *
 * <h2>⚠️ Nega chipta manzil ichida</h2>
 * Pleyer o'zi so'rov yuboradi va sarlavhalarni butun oqim uchun bir
 * marta oladi — ular segment so'roviga ham qo'shilardi. S3 esa
 * {@code Authorization} va imzolangan havolani BIRGA qabul qilmaydi.
 * Batafsil — {@link PlaybackTicketService}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HlsController {

    private final MediaAssetRepo mediaAssetRepo;
    private final AccessService accessService;
    private final PlaybackTicketService ticketService;
    private final HlsPlaylistService playlistService;

    /**
     * Segment havolasini imzolovchi.
     *
     * ⚠️ {@code Optional}: bean faqat S3 ombori sozlanganda bo'ladi
     * ({@code app.storage.provider=s3}). Lokal omborda HLS umuman
     * tarqatilmaydi va bu endpoint 404 qaytaradi.
     */
    private final Optional<SignedUrlProvider> signedUrls;

    /** Playlist yo'llari faqat shu kengaytma bilan tugaydi. */
    private static final String PLAYLIST_SUFFIX = ".m3u8";

    /**
     * Playlistni qaytaradi — master ham, variant ham.
     *
     * @param path {@code /master.m3u8} yoki {@code /720p/index.m3u8}
     * @param ticket {@link PlaybackTicketService} bergan chipta
     */
    @GetMapping("/api/v1/app/media/{id}/hls/{*path}")
    public ResponseEntity<String> playlist(@PathVariable Long id,
                                           @PathVariable String path,
                                           @RequestParam("t") String ticket) {

        MediaAsset asset = mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));

        String hlsDir = hlsDirOf(asset);
        String relative = safeRelative(path);

        // Chipta KIMLIGINI aytadi, huquqni esa AccessService beradi —
        // shuning uchun obuna tugagan zahoti kirish yopiladi.
        User viewer;
        try {
            viewer = ticketService.holderOf(ticket, id);
        } catch (IllegalArgumentException e) {
            // Yaroqsiz chipta ham «topilmadi» — media bor-yo'qligi
            // oshkor qilinmaydi, /raw dagi qoida bilan bir xil.
            throw BusinessException.notFound("Media", id);
        }

        if (!accessService.canReadMedia(viewer, asset)) {
            throw BusinessException.notFound("Media", id);
        }

        String body = playlistService.rewrite(hlsDir + "/" + relative,
                key -> rewriteTarget(id, hlsDir, key, ticket));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                // ⚠️ Keshlanmaydi: ichida muddati cheklangan imzolar bor
                // va eskirgan playlist «video ochilmadi» bo'lib ko'rinardi.
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    /**
     * Playlist ichidagi yo'lni pleyer ochadigan manzilga aylantiradi.
     *
     * <ul>
     *   <li>{@code .m3u8} → BIZNING endpoint, o'sha chipta bilan;</li>
     *   <li>qolgani (segment, {@code init.mp4}) → imzolangan havola.</li>
     * </ul>
     */
    private String rewriteTarget(Long mediaId, String hlsDir, String key, String ticket) {
        if (key.endsWith(PLAYLIST_SUFFIX)) {
            String relative = key.startsWith(hlsDir + "/")
                    ? key.substring(hlsDir.length() + 1)
                    : key;
            return "/api/v1/app/media/" + mediaId + "/hls/" + relative
                    + "?t=" + UriUtils.encode(ticket, StandardCharsets.UTF_8);
        }
        return signedUrls.orElseThrow().sign(key);
    }

    /**
     * Media qaysi papkada HLS'ga aylantirilgan.
     *
     * ⚠️ {@code hlsMasterKey} bo'sh bo'lsa — transcoding tugamagan.
     * Bu «xato» emas, «hali yo'q»; klient eski {@code /raw} yo'liga
     * qaytadi.
     */
    private String hlsDirOf(MediaAsset asset) {
        String master = asset.getHlsMasterKey();
        if (master == null || master.isBlank()
                || signedUrls.isEmpty() || !signedUrls.get().isAvailable()) {
            throw BusinessException.notFound("Media", asset.getId());
        }
        int slash = master.lastIndexOf('/');
        return slash < 0 ? "" : master.substring(0, slash);
    }

    /**
     * So'ralgan yo'lni tozalaydi.
     *
     * <h2>⚠️ Faqat {@code .m3u8}</h2>
     * Bu endpoint PLAYLIST uchun. Segmentni ham bersa, gigabaytlar
     * Spring Boot orqali oqib, butun HLS ishining ma'nosi qolmasdi —
     * video serverdan o'tmasligi asosiy talab edi.
     *
     * <h2>{@code ..} — ikkinchi qavat</h2>
     * Spring'ning {@code StrictHttpFirewall} bunday so'rovni bizga
     * yetib kelishidan OLDIN 400 bilan rad etadi (kodlangan
     * {@code %2e%2e} ni ham).
     *
     * ⚠️ Shunday bo'lsa-da tekshiruv qoladi: u arzon, va firewall
     * sozlamasi bir kuni yumshatilsa yagona to'siq shu bo'lib qoladi.
     * Usiz {@code ../../videos/99/hls/…} bilan BOSHQA filmning
     * playlisti o'qilardi va uning segmentlari imzolab berilardi.
     */
    private String safeRelative(String path) {
        String relative = path == null ? "" : path;
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }

        if (relative.isEmpty()
                || relative.contains("..")
                || relative.startsWith("/")
                || !relative.endsWith(PLAYLIST_SUFFIX)) {
            throw BusinessException.validation("Noto'g'ri playlist yo'li");
        }
        return relative;
    }
}
