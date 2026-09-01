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

    /**
     * BITTA darajani ko'rsatadi — fayl menejeridagi kabi.
     *
     * <h2>⚠️ Bu `scan()` dan tubdan ARZON</h2>
     * {@code delimiter="/"} berilganda S3 ichki papkalarga
     * KIRMAYDI: u faqat shu darajadagi fayllarni va ichki papka
     * NOMLARINI qaytaradi.
     *
     * Ya'ni `videos/` ni ochish 192 ta obyektni emas, 3 ta papka
     * nomini o'qiydi. Butun omborni skanerlash esa har papkani
     * ochishda takrorlanardi va sahifa sekinlashardi.
     *
     * ⚠️ Shu sababli natija KESHLANMAYDI — u har doim jonli.
     * Skanerlash hisoboti eskirishi mumkin, bu esa yo'q.
     */
    public Level browse(String prefix) {
        String safe = prefix == null ? "" : prefix;

        List<String> folders = new ArrayList<>();
        List<Item> files = new ArrayList<>();
        String token = null;

        do {
            ListObjectsV2Request.Builder request = ListObjectsV2Request.builder()
                    .bucket(properties.getBucket())
                    .prefix(safe)
                    // ⚠️ Aynan shu qator «papka» tushunchasini beradi.
                    .delimiter("/")
                    .maxKeys(1000);
            if (token != null) {
                request.continuationToken(token);
            }

            ListObjectsV2Response response = s3.listObjectsV2(request.build());

            response.commonPrefixes().forEach(p -> folders.add(p.prefix()));
            for (S3Object object : response.contents()) {
                // ⚠️ Papkaning o'zi ham obyekt sifatida qaytishi mumkin
                // (`videos/` kabi, nol baytli). Uni fayl deb ko'rsatish
                // adminni chalkashtirardi.
                if (!object.key().equals(safe)) {
                    files.add(new Item(object.key(), object.size()));
                }
            }

            token = Boolean.TRUE.equals(response.isTruncated())
                    ? response.nextContinuationToken() : null;
        } while (token != null && files.size() < MAX_OBJECTS);

        return new Level(safe, folders, files);
    }

    @Data
    public static class Level {
        private final String prefix;
        /** Ichki papkalar — to'liq prefiks bilan (`videos/146/`). */
        private final List<String> folders;
        private final List<Item> files;
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
