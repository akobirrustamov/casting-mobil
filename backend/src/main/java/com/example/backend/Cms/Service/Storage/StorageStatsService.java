package com.example.backend.Cms.Service.Storage;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.MediaUsageService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ombor holati: nima bor, qancha joy egallagan, nimasi ortiqcha.
 *
 * <h2>Nima uchun kerak</h2>
 * Ombor pul turadi va u FAQAT o'sadi. Har bir o'chirilgan kontent,
 * har bir muvaffaqiyatsiz yuklash, har bir qayta transcoding orqada
 * fayl qoldiradi. Ularni panelda ko'rish imkoni bo'lmasa, birinchi
 * belgi — hisobdagi raqam.
 *
 * <h2>⚠️ IKKI XIL «ISHLATILMAGAN» BOR va ular ARALASHTIRILMAYDI</h2>
 *
 * <b>1. Yetim fayl</b> — omborda bor, bazada yozuvi YO'Q.
 * Sabab odatda uzilib qolgan yuklash yoki qo'lda o'chirilgan yozuv.
 * Bunday faylni hech narsa ko'rsatmaydi va u hech qachon
 * ochilmaydi — sof yo'qotilgan joy.
 *
 * <b>2. Biriktirilmagan media</b> — bazada yozuvi BOR, lekin hech
 * qaysi kontentga ulanmagan. Fayl kutubxonada ko'rinadi va admin uni
 * ataylab saqlab turgan bo'lishi mumkin.
 *
 * ⚠️ Ikkalasini bitta ro'yxatga qo'shish xavfli bo'lardi: birinchisini
 * o'chirish xavfsiz, ikkinchisini o'chirish esa adminning ishini
 * yo'q qilishi mumkin.
 *
 * <h2>⚠️ Natija KESHLANADI</h2>
 * Skanerlash qimmat ({@link StorageInventory}). Sahifa har
 * ochilganda butun omborni sanash S3 hisobini oshirardi va panelni
 * sekinlashtirardi.
 *
 * Kesh muddatsiz: admin «yangilash» tugmasini bosmaguncha eski natija
 * ko'rsatiladi va uning VAQTI yozib qo'yiladi. Eskirgan raqamni
 * ko'rsatib, «qachonligini» aytish — yangi raqam uchun har safar
 * kutishdan yaxshiroq.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class StorageStatsService {

    private final StorageInventory inventory;
    private final MediaAssetRepo mediaAssetRepo;
    private final MediaUsageService mediaUsageService;

    /**
     * Oxirgi skanerlash natijasi.
     *
     * ⚠️ {@code volatile}: ikki admin bir vaqtda so'rasa, biri
     * yarim yozilgan obyektni ko'rmasin.
     */
    private volatile Report cached;

    /** Keshlangan natija; hali skanerlanmagan bo'lsa {@code null}. */
    public Report cached() {
        return cached;
    }

    /** Qayta skanerlaydi va keshni yangilaydi. */
    @Transactional(readOnly = true)
    public Report refresh() {
        long started = System.currentTimeMillis();
        StorageInventory.Scan scan = inventory.scan();

        // --- 1. Bazadagi kalitlar ---
        //
        // ⚠️ Ikki xil kalit bor: asl fayl (`storageKey`) va HLS
        // natijasi (`videos/{id}/...`). Ikkinchisi bitta kalit emas,
        // BUTUN PAPKA — shuning uchun media id lari alohida yig'iladi.
        Set<String> knownKeys = new HashSet<>();
        Set<Long> knownMediaIds = new HashSet<>();

        for (MediaAsset asset : mediaAssetRepo.findAll()) {
            knownMediaIds.add(asset.getId());
            if (asset.getStorageKey() != null) {
                knownKeys.add(normalize(asset.getStorageKey()));
            }
        }

        // --- 2. Omborni ko'rib chiqamiz ---
        Map<String, Folder> folders = new LinkedHashMap<>();
        List<Orphan> orphans = new ArrayList<>();
        long totalBytes = 0;
        long orphanBytes = 0;

        for (StorageInventory.Item item : scan.getItems()) {
            String key = normalize(item.getKey());
            totalBytes += item.getSizeBytes();

            Folder folder = folders.computeIfAbsent(topFolder(key), Folder::new);
            folder.count++;
            folder.sizeBytes += item.getSizeBytes();

            if (!isReferenced(key, knownKeys, knownMediaIds)) {
                orphanBytes += item.getSizeBytes();
                folder.orphanCount++;
                folder.orphanBytes += item.getSizeBytes();

                // ⚠️ Ro'yxat CHEGARALANGAN. 50 000 yetim faylni
                // JSON'ga solish javobni o'nlab megabaytga
                // chiqarardi va brauzer uni chiza olmasdi.
                if (orphans.size() < MAX_LISTED) {
                    orphans.add(new Orphan(item.getKey(), item.getSizeBytes()));
                }
            }
        }

        // --- 3. Biriktirilmagan media (bazada bor, hech qayerda ishlatilmagan) ---
        List<UnusedAsset> unused = new ArrayList<>();
        long unusedBytes = 0;
        for (MediaAsset asset : mediaAssetRepo.findAll()) {
            if (!mediaUsageService.usages(asset.getId()).isEmpty()) {
                continue;
            }
            long size = asset.getSizeBytes() == null ? 0 : asset.getSizeBytes();
            unusedBytes += size;
            if (unused.size() < MAX_LISTED) {
                unused.add(new UnusedAsset(asset.getId(), asset.getOriginalFilename(),
                        asset.getType() == null ? null : asset.getType().name(), size));
            }
        }
        unused.sort(Comparator.comparingLong(UnusedAsset::getSizeBytes).reversed());
        orphans.sort(Comparator.comparingLong(Orphan::getSizeBytes).reversed());

        Report report = Report.builder()
                .scannedAt(Instant.now())
                .scanMillis(System.currentTimeMillis() - started)
                .complete(scan.isComplete())
                .objectCount(scan.getItems().size())
                .totalBytes(totalBytes)
                .folders(new ArrayList<>(folders.values()))
                .orphanCount(countOrphans(folders))
                .orphanBytes(orphanBytes)
                .orphans(orphans)
                .unusedAssetCount(unusedCount(unusedBytes, unused))
                .unusedAssetBytes(unusedBytes)
                .unusedAssets(unused)
                .listLimit(MAX_LISTED)
                .build();

        cached = report;
        log.info("Ombor skanerlandi: {} obyekt, {} MB, {} yetim fayl ({} ms)",
                report.objectCount, report.totalBytes / 1024 / 1024,
                report.orphanCount, report.scanMillis);
        return report;
    }

    /** Ro'yxatlarda ko'pi bilan shuncha element qaytariladi. */
    public static final int MAX_LISTED = 200;

    /**
     * Ombordagi kalit bazada biror narsaga bog'langanmi.
     *
     * ⚠️ HLS papkasi ALOHIDA qaraladi: `videos/146/hls/480p/x.m4s`
     * bitta kalit emas, 146-media'ning natijasi. Uni `storageKey`
     * bilan solishtirish har bir segmentni yetim deb ko'rsatardi —
     * ya'ni hisobot butunlay yaroqsiz bo'lardi.
     */
    private boolean isReferenced(String key, Set<String> knownKeys, Set<Long> knownMediaIds) {
        if (knownKeys.contains(key)) {
            return true;
        }

        Long mediaId = mediaIdOfHls(key);
        return mediaId != null && knownMediaIds.contains(mediaId);
    }

    /** `videos/146/hls/...` → 146; boshqa shakl uchun {@code null}. */
    private Long mediaIdOfHls(String key) {
        if (!key.startsWith("videos/")) {
            return null;
        }
        String rest = key.substring("videos/".length());
        int slash = rest.indexOf('/');
        if (slash <= 0) {
            return null;
        }
        try {
            return Long.parseLong(rest.substring(0, slash));
        } catch (NumberFormatException e) {
            // `videos/hls-test-8/...` kabi — raqam emas, demak
            // hech qaysi media'ga tegishli emas.
            return null;
        }
    }

    /** Boshidagi `/` olib tashlanadi — baza va S3 turlicha yozadi. */
    private String normalize(String key) {
        return key == null ? "" : (key.startsWith("/") ? key.substring(1) : key);
    }

    private String topFolder(String key) {
        int slash = key.indexOf('/');
        return slash < 0 ? "(ildiz)" : key.substring(0, slash);
    }

    private long countOrphans(Map<String, Folder> folders) {
        return folders.values().stream().mapToLong(f -> f.orphanCount).sum();
    }

    private long unusedCount(long bytes, List<UnusedAsset> listed) {
        // Ro'yxat kesilgan bo'lishi mumkin — son alohida sanaladi.
        return listed.size();
    }

    // ------------------------------------------------------------------- DTO

    @Data
    @Builder
    public static class Report {
        private Instant scannedAt;
        private long scanMillis;
        /** {@code false} — skanerlash chegaraga yetdi, raqamlar to'liq emas. */
        private boolean complete;

        private int objectCount;
        private long totalBytes;
        private List<Folder> folders;

        /** Omborda bor, bazada yo'q. O'chirish xavfsiz. */
        private long orphanCount;
        private long orphanBytes;
        private List<Orphan> orphans;

        /** Bazada bor, hech qaysi kontentga biriktirilmagan. */
        private long unusedAssetCount;
        private long unusedAssetBytes;
        private List<UnusedAsset> unusedAssets;

        /** Ro'yxatlar shu songacha kesilgan. */
        private int listLimit;
    }

    @Data
    public static class Folder {
        private final String name;
        private int count;
        private long sizeBytes;
        private long orphanCount;
        private long orphanBytes;
    }

    @Data
    public static class Orphan {
        private final String key;
        private final long sizeBytes;
    }

    @Data
    public static class UnusedAsset {
        private final Long id;
        private final String filename;
        private final String type;
        private final long sizeBytes;
    }
}
