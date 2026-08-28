package com.example.backend.Cms.Service.Video;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Ombor kalitini pleyer ochadigan manzilga aylantiradi.
 *
 * <h2>⚠️ Bazada KALIT saqlanadi, to'liq URL emas</h2>
 * {@code hlsMasterKey} = {@code /videos/7/hls/master.m3u8}.
 *
 * To'liq URL saqlansa, CDN domeni o'zgarganda ming qatorli
 * {@code UPDATE} kerak bo'lardi — va u paytgacha barcha eski
 * videolar ochilmay qolardi. Domen sozlamada bo'lsa, o'zgarish
 * bitta qatorni tahrirlash bilan tugaydi.
 *
 * <h2>CDN sozlanmagan bo'lsa</h2>
 * {@code null} qaytariladi. Bu «HLS yo'q» degani va klient eski
 * yo'lga ({@code /api/v1/app/media/{id}/raw}) qaytadi.
 *
 * ⚠️ Bu ATAYLAB: sozlanmagan CDN uchun o'ylab topilgan manzil
 * qaytarish pleyerni ishlamaydigan havolaga yuborardi va nosozlik
 * «video buzuq» bo'lib ko'rinardi — sabab esa oddiy sozlama
 * yetishmasligi edi.
 */
@Slf4j
@Service
public class CdnUrlService {

    /**
     * CDN ildizi — masalan {@code https://video.uzcasting.site}.
     *
     * Bo'sh bo'lsa HLS manzillari berilmaydi.
     */
    @Value("${app.video.cdn.base-url:}")
    private String cdnBaseUrl;

    public boolean isConfigured() {
        return cdnBaseUrl != null && !cdnBaseUrl.isBlank();
    }

    /**
     * Kalitdan to'liq manzil yasaydi.
     *
     * @param key {@code MediaAsset.hlsMasterKey}. {@code null} bo'lsa
     *            natija ham {@code null} — bu media transcoding
     *            qilinmagan degani
     * @return mutlaq URL yoki {@code null}
     */
    public String masterUrl(String key) {
        if (key == null || key.isBlank() || !isConfigured()) {
            return null;
        }
        return join(cdnBaseUrl, key);
    }

    /**
     * Ildiz va kalitni bitta {@code /} bilan ulaydi.
     *
     * ⚠️ Ikkala tomonda ham qiyshiq chiziq bo'lishi mumkin:
     * sozlamada {@code https://cdn.example.com/} va kalitda
     * {@code /videos/…}. Tekshirilmasa {@code //videos/…} chiqardi —
     * ba'zi CDN'lar buni boshqa yo'l deb qabul qiladi va 404 beradi.
     */
    private String join(String base, String key) {
        String left = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String right = key.startsWith("/") ? key : "/" + key;
        return left + right;
    }
}
