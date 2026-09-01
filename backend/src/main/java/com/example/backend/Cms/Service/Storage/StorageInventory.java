package com.example.backend.Cms.Service.Storage;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.ArrayList;
import java.util.List;

/**
 * Omborda NIMA borligini sanaydi.
 *
 * <h2>Nega alohida servis</h2>
 * {@code StorageService} bitta fayl bilan ishlaydi: yoz, o'qi, o'chir.
 * Butun omborni ko'rib chiqish — boshqa vazifa va boshqa narxga ega.
 *
 * <h2>⚠️ BU QIMMAT AMAL</h2>
 * Har 1000 obyekt uchun bitta so'rov. Bugun 200 ta obyekt bor, ertaga
 * 200 000 bo'lishi mumkin — o'shanda skanerlash daqiqalar oladi va
 * S3 so'rovlari pul turadi.
 *
 * Shuning uchun u:
 * <ul>
 *   <li>hech qachon O'ZI ishga tushmaydi — faqat admin so'raganda;</li>
 *   <li>natijasi keshlanadi ({@code StorageStatsService});</li>
 *   <li>chegarasi bor — {@code MAX_OBJECTS} dan oshsa to'xtaydi va
 *       buni ochiq aytadi.</li>
 * </ul>
 *
 * ⚠️ Chegara bo'lmasa juda katta ombor xotirani to'ldirib, ilovani
 * yiqitardi. Yarim ma'lumot berib «to'liq emas» deb aytish
 * yiqilishdan yaxshiroq.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class StorageInventory {

    /**
     * Ko'pi bilan shuncha obyekt sanaladi.
     *
     * ⚠️ Bu son xotiraga qarab tanlangan: har element ~150 bayt, ya'ni
     * 200 000 element ~30 MB. Undan kattasi hisobot uchun ham
     * ma'nosiz — admin 200 000 qatorni ko'rib chiqmaydi.
     */
    public static final int MAX_OBJECTS = 200_000;

    private final S3Client s3;
    private final S3Properties properties;

    /**
     * Butun omborni ko'rib chiqadi.
     *
     * @return topilgan obyektlar va skanerlash to'liq bo'lganmi
     */
    public Scan scan() {
        List<Item> items = new ArrayList<>();
        String token = null;
        boolean complete = true;

        do {
            ListObjectsV2Request.Builder request = ListObjectsV2Request.builder()
                    .bucket(properties.getBucket())
                    .maxKeys(1000);
            if (token != null) {
                request.continuationToken(token);
            }

            ListObjectsV2Response response = s3.listObjectsV2(request.build());
            for (S3Object object : response.contents()) {
                items.add(new Item(object.key(), object.size()));

                if (items.size() >= MAX_OBJECTS) {
                    // ⚠️ To'xtaymiz, lekin JIM emas: hisobotda
                    // «to'liq emas» deb ko'rsatiladi.
                    log.warn("Ombor skanerlashi chegaraga yetdi: {} obyekt. "
                            + "Hisobot to'liq emas.", MAX_OBJECTS);
                    return new Scan(items, false);
                }
            }

            token = Boolean.TRUE.equals(response.isTruncated())
                    ? response.nextContinuationToken() : null;
        } while (token != null);

        return new Scan(items, complete);
    }

    @Data
    public static class Item {
        private final String key;
        private final long sizeBytes;
    }

    @Data
    public static class Scan {
        private final List<Item> items;
        /** {@code false} — chegaraga yetdi, raqamlar to'liq emas. */
        private final boolean complete;
    }
}
