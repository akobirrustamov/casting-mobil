package com.example.backend.Cms.Service.Video;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Timeweb CDN «Secure token» havolalari.
 *
 * <h2>Nima uchun bu S3 imzosidan yaxshiroq</h2>
 * Hozirgi {@link PresignedUrlProvider} imzoni S3 uchun yasaydi, keyin
 * domenni CDN'ga almashtiradi. Natijada imzo CDN'ga umuman YETIB
 * BORMAYDI — CDN origin'ga o'z kalitlari bilan boradi. Ya'ni imzo bor,
 * lekin u hech nimani tekshirmaydi: manzilni bilgan odam imzosiz ham
 * oladi.
 *
 * <pre>
 *   curl https://cdn.uzcasting.com/videos/146/hls/master.m3u8
 *   → HTTP 200, IMZOSIZ
 * </pre>
 *
 * Secure token esa CDN ning O'ZI tekshiradigan imzo. Noto'g'ri yoki
 * muddati o'tgan token bilan CDN 403 qaytaradi va so'rov origin'ga
 * umuman bormaydi.
 *
 * <h2>Algoritm (Timeweb hujjati)</h2>
 * <pre>
 *   raw     = SECRET + PATH + IP + EXPIRES
 *   token   = base64(md5_bytes(raw)), so'ng  + → -,  / → _,  = olib tashlanadi
 *   manzil  = {CDN}/md5({token},{EXPIRES}){PATH}
 * </pre>
 *
 * ⚠️ MD5 dan <b>xom baytlar</b> olinadi ({@code digest()}), o'n oltilik
 * satr EMAS. Farqi ko'rinmaydi — ikkalasi ham o'xshash token beradi —
 * lekin o'n oltilik bilan CDN har bir segmentga 403 qaytaradi.
 *
 * ⚠️ CDN domeni imzoga KIRMAYDI. Shuning uchun bitta imzo HTTP va
 * HTTPS uchun ham ishlaydi.
 *
 * <h2>⚠️ IP tekshiruvi ataylab ISHLATILMAYDI</h2>
 * Timeweb imzoga tomoshabin manzilini qo'shishga ruxsat beradi. Video
 * uchun bu ZARARLI: telefon Wi-Fi'dan uyali tarmoqqa o'tganda manzil
 * almashadi va film o'rtasida to'xtab qolardi. Nosozlik esa
 * takrorlanmaydigan bo'lardi — uyda tekshirganda hammasi joyida.
 *
 * Shuning uchun {@code IP} bo'sh satr. Hujjatdagi formulada u
 * o'rnida hech narsa turmaydi.
 *
 * <h2>⚠️ Muddat OYNAGA tekislanadi</h2>
 * Token {@code EXPIRES} ga bog'liq. Har foydalanuvchiga o'z vaqti
 * berilsa, har biri BOSHQA manzil olardi — CDN uchun bu boshqa fayl
 * degani. 3000 kishi bitta filmni ko'rsa kesh umuman ishlamasdi va
 * butun trafik omborga tushardi.
 *
 * Shuning uchun {@code EXPIRES} joriy oynaning oxiriga + TTL ga
 * tenglashtiriladi. Bitta oyna ichidagi hamma bir xil manzil oladi,
 * CDN esa uni bir marta keshlaydi.
 *
 * <h2>Qachon yoqiladi</h2>
 * Faqat {@code app.video.cdn.secure-token.secret} berilganda. Kalit
 * bo'lmasa bu bean umuman yaratilmaydi va tizim eskicha
 * {@link PresignedUrlProvider} bilan ishlaydi.
 *
 * ⚠️ Bu ATAYLAB shunday: kalitni qo'yish — ONGLI qadam. Aks holda
 * yangi kod serverga chiqishi bilan barcha video birdan 403 bo'lardi,
 * chunki CDN tomonida Secure token hali yoqilmagan bo'lardi.
 *
 * <h2>Ikki tomon birga yoqiladi</h2>
 * <ol>
 *   <li>Timeweb panelida CDN uchun Secure token yoqiladi va kalit
 *       olinadi;</li>
 *   <li>o'sha kalit serverdagi {@code application.properties} ga
 *       yoziladi;</li>
 *   <li>shundan keyingina bucket «Приватный» qilinadi.</li>
 * </ol>
 *
 * Tartib buzilsa video ochilmay qoladi: kalitsiz kod imzosiz havola
 * beradi, CDN esa uni rad etadi.
 *
 * <h2>⚠️ Nega {@code @ConditionalOnProperty} EMAS</h2>
 * U qatorning BORLIGINI tekshiradi, qiymatini emas. Ya'ni
 * {@code app.video.cdn.secure-token.secret=} (kalit o'chirilgan, qator
 * qolgan) uni QANOATLANTIRADI — sozlamalarda esa bu odatiy hol.
 *
 * Natijasi jim va yomon bo'lardi: bean yaratilardi, {@code isAvailable()}
 * esa {@code false} qaytarardi, va tizim S3 imzosiga ham QAYTMASDAN
 * imzosiz havola berardi. Ya'ni «kalitni vaqtincha o'chirib turay»
 * degan zararsiz ko'ringan qadam himoyani butunlay olib tashlardi.
 *
 * Ifoda esa qiymatning o'zini ko'radi. Buni {@code SignedUrlProviderWiringTest}
 * qo'riqlaydi.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnExpression("!'${app.video.cdn.secure-token.secret:}'.isBlank()")
public class SecureTokenUrlProvider implements SignedUrlProvider {

    private final CdnUrlService cdnUrlService;

    /** Timeweb panelida CDN uchun yasalgan maxfiy kalit. */
    @Value("${app.video.cdn.secure-token.secret:}")
    private String secret;

    /**
     * Havola qancha yashaydi — {@link PresignedUrlProvider} bilan
     * bir xil sozlama, chunki ma'no ham bir xil.
     */
    @Value("${app.video.signed-url-ttl:4h}")
    private Duration ttl;

    /** Kesh oynasi — qarang: yuqoridagi «Muddat OYNAGA tekislanadi». */
    @Value("${app.video.signed-url-window:1h}")
    private Duration window;

    /**
     * ⚠️ CDN manzili ham kerak. Kalit berilib, manzil berilmasa bu
     * sozlama xatosi: token yasay olamiz, lekin uni qayerga
     * qo'yishni bilmaymiz.
     *
     * Bunday holatda {@code false} qaytariladi va tizim imzosiz
     * ishlashda davom etadi — video ochilmay qolgandan ko'ra shu
     * yaxshiroq. Ogohlantirish jurnalga yoziladi.
     */
    @Override
    public boolean isAvailable() {
        if (secret == null || secret.isBlank()) {
            return false;
        }
        if (cdnUrlService.base() == null) {
            log.warn("Secure token kaliti berilgan, lekin app.video.cdn.base-url bo'sh — "
                    + "token yasalmaydi");
            return false;
        }
        return true;
    }

    @Override
    public String sign(String storageKey) {
        String base = cdnUrlService.base();
        if (base == null || secret == null || secret.isBlank()) {
            // isAvailable() allaqachon tekshiradi, lekin bean to'g'ridan
            // to'g'ri chaqirilsa ham havola qaytsin.
            return cdnUrlService.masterUrl(storageKey);
        }

        String path = absolutePath(storageKey);
        long expires = expiresAt();

        return base + "/md5(" + token(path, expires) + "," + expires + ")" + path;
    }

    /**
     * Joriy oynaning oxiri + TTL.
     *
     * ⚠️ Oyna ichidagi barcha so'rovlar uchun AYNAN bir xil son
     * chiqadi — kesh aynan shunga tayanadi.
     *
     * Havolaning eng qisqa umri {@code ttl} ga teng (oyna oxirida
     * olgan odam uchun), eng uzuni {@code window + ttl}.
     */
    private long expiresAt() {
        long windowSeconds = Math.max(1, window.toSeconds());
        long slot = Instant.now().getEpochSecond() / windowSeconds;
        return (slot + 1) * windowSeconds + ttl.toSeconds();
    }

    /**
     * Hujjatdagi formula: {@code SECRET + PATH + IP + EXPIRES}.
     *
     * ⚠️ {@code IP} bo'sh — yuqoridagi izohga qarang.
     */
    private String token(String path, long expires) {
        String raw = secret + path + expires;
        byte[] digest = md5(raw.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(digest)
                .replace('+', '-')
                .replace('/', '_')
                .replace("=", "");
    }

    private byte[] md5(byte[] input) {
        try {
            return MessageDigest.getInstance("MD5").digest(input);
        } catch (NoSuchAlgorithmException e) {
            // MD5 har bir Java ish muhitida bor (JLS talab qiladi).
            // Bu yerga tushish — ish muhiti buzilgan degani.
            throw new IllegalStateException("MD5 topilmadi", e);
        }
    }

    /**
     * Kalitni {@code /} bilan boshlanadigan yo'lga aylantiradi.
     *
     * ⚠️ Imzo AYNAN shu satr ustidan hisoblanadi va manzilga ham
     * AYNAN shu qo'yiladi. Ikkalasi bir joydan olinishi shart:
     * bittasida boshidagi qiyshiq chiziq bo'lib, ikkinchisida
     * bo'lmasa, CDN 403 qaytarardi.
     */
    private String absolutePath(String storageKey) {
        String key = storageKey == null ? "" : storageKey;
        return key.startsWith("/") ? key : "/" + key;
    }
}
