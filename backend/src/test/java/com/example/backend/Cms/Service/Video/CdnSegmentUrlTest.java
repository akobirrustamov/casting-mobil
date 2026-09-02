package com.example.backend.Cms.Service.Video;

import com.example.backend.Cms.Service.Storage.S3Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Segment manzili CDN domeniga o'giriladi.
 *
 * <h2>⚠️ Nima uchun bu test</h2>
 * Bu o'girish ikki xil buzilishi mumkin va ikkalasi ham «video
 * ochilmadi» bo'lib ko'rinadi — sabab esa manzilda:
 *
 * <ul>
 *   <li><b>Bucket yo'lda qolib ketsa</b> — CDN uni papka deb qidiradi
 *       va 404 beradi. CDN origin allaqachon o'sha bucket
 *       ({@code {bucket}.s3.twcstorage.ru}), ya'ni yo'l bucketning
 *       ichidan boshlanishi kerak;</li>
 *   <li><b>So'rov qatori yo'qolsa</b> — imzo yo'qoladi. Ochiq
 *       bucketda bu bilinmaydi, yopilgan zahoti hamma video birdan
 *       to'xtaydi.</li>
 * </ul>
 *
 * <h2>Nega imzo saqlanadi, garchi CDN uni tekshirmasa ham</h2>
 * CDN origin'ga O'ZINING AWS kalitlari bilan boradi, ya'ni
 * havoladagi imzo S3 ga yetib bormaydi. Lekin u o'chirilsa, bucketni
 * yopgan kuni butun sxemani qaytadan yozish kerak bo'lardi.
 */
class CdnSegmentUrlTest {

    private static final String BUCKET = "00847558-22cb-4af0-bdbf-d750dfbdac8a";

    /** Haqiqiy imzolangan manzil shakli — S3 dan olingan. */
    private static final String SIGNED =
            "https://s3.twcstorage.ru/" + BUCKET + "/videos/146/hls/480p/segment_00000.m4s"
                    + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260902T063053Z"
                    + "&X-Amz-Expires=14400&X-Amz-Signature=210db5c5a7432bda";

    private PresignedUrlProvider provider(String cdnBase) {
        S3Properties props = new S3Properties();
        props.setBucket(BUCKET);

        CdnUrlService cdn = new CdnUrlService();
        ReflectionTestUtils.setField(cdn, "cdnBaseUrl", cdnBase);

        return new PresignedUrlProvider(null, props, cdn);
    }

    /** Sinov uchun `private` metodni chaqiramiz — mantiq shu yerda. */
    private String rewrite(String cdnBase, String signed) {
        return (String) ReflectionTestUtils.invokeMethod(provider(cdnBase), "viaCdn", signed);
    }

    @Test
    @DisplayName("Domen almashadi, bucket yo'ldan tushadi")
    void rewritesHostAndDropsBucket() {
        String out = rewrite("https://cdn.uzcasting.com", SIGNED);

        assertThat(out).startsWith("https://cdn.uzcasting.com/videos/146/hls/480p/segment_00000.m4s");
        assertThat(out).doesNotContain(BUCKET);
        assertThat(out).doesNotContain("s3.twcstorage.ru");
    }

    /**
     * ⚠️ So'rov qatori TO'LIQ saqlanadi.
     *
     * Bucket ochiq bo'lgani uchun imzosiz ham ishlaydi — ya'ni bu
     * yo'qolsa hech narsa sinmaydi va xato SEZILMAY qoladi. Bucket
     * yopilgan kuni esa hamma video birdan to'xtardi.
     */
    @Test
    @DisplayName("Imzo saqlanadi")
    void keepsSignature() {
        String out = rewrite("https://cdn.uzcasting.com", SIGNED);

        assertThat(out).contains("X-Amz-Signature=210db5c5a7432bda");
        assertThat(out).contains("X-Amz-Date=20260902T063053Z");
        assertThat(out).contains("X-Amz-Expires=14400");
    }

    /**
     * ⚠️ CDN sozlanmagan bo'lsa manzil TEGILMAYDI.
     *
     * Lokal ishlab chiqishda aynan shu kerak: CDN ning CORS
     * ro'yxatiga `localhost` ni qo'shib bo'lmaydi, ya'ni lokalda CDN
     * orqali olingan segmentni brauzer bloklardi.
     */
    @Test
    @DisplayName("CDN sozlanmagan — S3 manzili o'zgarishsiz")
    void withoutCdnReturnsOriginal() {
        assertThat(rewrite("", SIGNED)).isEqualTo(SIGNED);
        assertThat(rewrite(null, SIGNED)).isEqualTo(SIGNED);
    }

    /** Oxirida qiyshiq chiziq bo'lsa ham ikkilanmaydi. */
    @Test
    @DisplayName("Sozlamadagi oxirgi / qo'shalanmaydi")
    void handlesTrailingSlash() {
        String out = rewrite("https://cdn.uzcasting.com/", SIGNED);

        assertThat(out).startsWith("https://cdn.uzcasting.com/videos/");
        assertThat(out).doesNotContain("//videos");
    }

    /**
     * ⚠️ Subdomen uslubi: bucket yo'lda emas, xostda.
     *
     * SDK sozlamasiga qarab imzolangan manzil ikki xil bo'lishi
     * mumkin. Ikkinchi shaklda yo'lda kesiladigan narsa yo'q va
     * uni «kesib» qo'ysak segment nomining boshi yo'qolardi.
     */
    @Test
    @DisplayName("Bucket subdomenda bo'lsa yo'l tegilmaydi")
    void handlesVirtualHostedStyle() {
        String signed = "https://" + BUCKET + ".s3.twcstorage.ru"
                + "/videos/146/hls/480p/segment_00000.m4s?X-Amz-Signature=abc";

        String out = rewrite("https://cdn.uzcasting.com", signed);

        assertThat(out).isEqualTo(
                "https://cdn.uzcasting.com/videos/146/hls/480p/segment_00000.m4s?X-Amz-Signature=abc");
    }

    /**
     * ⚠️ Buzuq manzil tomoshani TO'XTATMAYDI.
     *
     * O'girish — optimizatsiya, video esa asosiy vazifa. Kutilmagan
     * shaklda xato tashlansa, butun oqim yiqilardi.
     */
    @Test
    @DisplayName("Buzuq manzilda xato tashlanmaydi")
    void brokenUrlDoesNotThrow() {
        String broken = "bu manzil emas";
        assertThat(rewrite("https://cdn.uzcasting.com", broken)).isEqualTo(broken);
    }
}
