package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.UploadSession;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Repository.UploadSessionRepo;
import com.example.backend.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Katta fayllarni bo'laklab yuklash.
 *
 * <h2>Oqim</h2>
 * <ol>
 *   <li>{@link #begin} — sessiya ochiladi, bo'lak o'lchami aytiladi;</li>
 *   <li>{@link #saveChunk} — har bir bo'lak alohida so'rovda keladi;</li>
 *   <li>{@link #receivedChunks} — uzilishdan keyin nimalar yetib kelganini
 *       so'rash mumkin, klient faqat yetishmaganini qayta yuboradi;</li>
 *   <li>{@link #complete} — bo'laklar tartib bilan yig'ilib, yakuniy fayl
 *       {@link StorageService} orqali saqlanadi.</li>
 * </ol>
 *
 * <h2>Xotira</h2>
 * Hech bir bosqichda fayl RAM'ga to'liq yuklanmaydi. Bo'lak kelganda
 * to'g'ridan-to'g'ri diskka yoziladi, yig'ishda esa {@link SequenceInputStream}
 * bo'laklarni ketma-ket oqim sifatida beradi.
 *
 * <h2>Nega tugallanmagan fayl media bo'lib qolmaydi</h2>
 * {@code MediaAsset} FAQAT yig'ish muvaffaqiyatli tugagach yaratiladi.
 * Yarim yuklangan sessiya media kutubxonasida umuman ko'rinmaydi.
 */
@Slf4j
@Service
public class ChunkedUploadService {

    /** Bo'lak o'lchami. Kichik bo'lsa so'rov ko'p, katta bo'lsa qayta yuborish qimmat. */
    private static final int CHUNK_SIZE = 5 * 1024 * 1024;

    /** Bitta bo'lak uchun qattiq chegara - suiiste'molga qarshi. */
    private static final long MAX_CHUNK_BYTES = 8L * 1024 * 1024;

    private static final String PENDING = "PENDING";
    private static final String COMPLETED = "COMPLETED";
    private static final String ABORTED = "ABORTED";

    /**
     * Tugallanmagan bo'laklar shu yerda. Xizmat ko'rsatiladigan
     * daraxtdan tashqarida.
     *
     * ⚠️ Sozlanadi. Ilgari bu qat'iy `backend/files/.uploads` edi va
     * TEST ishga tushirishlari ham aynan shu papkaga yozardi. Test
     * sessiyalari esa boshqa bazada yashaydi, ya'ni sutkalik tozalash
     * ({@link #cleanUpAbandoned}) ularni HECH QACHON topa olmasdi:
     * u faqat bazadagi PENDING yozuvlar bo'yicha yuradi. Natijada
     * ishlab chiqish muhitida yetim bo'laklar to'planib borardi —
     * bir necha kunda gigabaytlab.
     */
    private final Path tempRoot;

    public ChunkedUploadService(UploadSessionRepo sessionRepo,
                                MediaAssetRepo mediaAssetRepo,
                                StorageService storageService,
                                @Value("${app.upload.temp-dir:backend/files/.uploads}") String tempDir) {
        this.sessionRepo = sessionRepo;
        this.mediaAssetRepo = mediaAssetRepo;
        this.storageService = storageService;
        this.tempRoot = Paths.get(tempDir);
    }

    private final UploadSessionRepo sessionRepo;
    private final MediaAssetRepo mediaAssetRepo;
    private final StorageService storageService;

    /** Yakuniy fayl uchun yuqori chegara. Sozlanadi, chunki bu biznes qarori. */
    @Value("${app.upload.max-bytes:5368709120}")
    private long maxBytes;

    // ------------------------------------------------------------------ ochish

    @Transactional
    public UploadSession begin(UUID actorId, String filename, Long sizeBytes,
                               String mimeType, String folder) {
        if (filename == null || filename.isBlank()) {
            throw BusinessException.validation("Fayl nomi ko'rsatilmagan");
        }
        if (sizeBytes == null || sizeBytes <= 0) {
            throw BusinessException.validation("Fayl o'lchami noto'g'ri");
        }
        if (sizeBytes > maxBytes) {
            throw BusinessException.validation(
                    "Fayl juda katta. Ruxsat etilgan chegara: " + (maxBytes / 1024 / 1024) + " MB");
        }
        // Kengaytmani ENG BOSHIDA tekshiramiz. Aks holda foydalanuvchi butun
        // faylni yuborib bo'lgach, yig'ish paytida rad javobini olardi.
        if (!storageService.accepts(filename)) {
            throw BusinessException.validation("Bu turdagi fayl qabul qilinmaydi: " + filename);
        }

        int totalChunks = (int) ((sizeBytes + CHUNK_SIZE - 1) / CHUNK_SIZE);

        UploadSession session = UploadSession.builder()
                .id(UUID.randomUUID().toString())
                .originalFilename(filename)
                .mimeType(mimeType)
                .sizeBytes(sizeBytes)
                .chunkSize(CHUNK_SIZE)
                .totalChunks(totalChunks)
                .folder(folder == null || folder.isBlank() ? "content" : folder)
                .status(PENDING)
                .createdBy(actorId)
                .createdAt(LocalDateTime.now())
                .build();

        return sessionRepo.save(session);
    }

    // ------------------------------------------------------------------ bo'lak

    /**
     * Bitta bo'lakni saqlaydi.
     *
     * Bo'lak allaqachon kelgan bo'lsa qayta yoziladi - klient uzilishdan keyin
     * qaysi bo'lak to'liq yetganini bilmasligi mumkin, shuning uchun qayta
     * yuborish XAVFSIZ bo'lishi kerak (idempotent).
     */
    public long saveChunk(UploadSession session, int index, InputStream body) {
        if (index < 0 || index >= session.getTotalChunks()) {
            throw BusinessException.validation(
                    "Bo'lak raqami noto'g'ri: " + index + " (jami " + session.getTotalChunks() + ")");
        }
        if (!PENDING.equals(session.getStatus())) {
            throw BusinessException.validation("Sessiya yopilgan: " + session.getStatus());
        }

        Path dir = sessionDir(session.getId());
        Path part = dir.resolve(index + ".part");
        Path tmp = dir.resolve(index + ".part.tmp");

        try {
            Files.createDirectories(dir);
            long written = copyLimited(body, tmp);
            // Avval vaqtinchalik nomga yozamiz: yozish yarmida uzilsa,
            // yarim bo'lak "tayyor" deb hisoblanib qolmasin.
            Files.move(tmp, part, StandardCopyOption.REPLACE_EXISTING);
            return written;
        } catch (IOException e) {
            quietlyDelete(tmp);
            log.error("Bo'lak saqlanmadi: sessiya={} bo'lak={}", session.getId(), index, e);
            throw new BusinessException("STORAGE_ERROR", "Bo'lak saqlanmadi",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            // ⚠️ `copyLimited` chegaradan oshganda BusinessException
            // ko'taradi — u IOException EMAS. Ilgari shu holatda
            // yarim yozilgan `.tmp` diskda qolib ketardi va uni hech
            // kim tozalamasdi: sessiya papkasi faqat `complete` yoki
            // `abort` da o'chiriladi, bunday yuklash esa ikkalasiga
            // ham yetib bormasdi.
            quietlyDelete(tmp);
            throw e;
        }
    }

    /** Diskda haqiqatan yotgan bo'laklar - davom ettirish uchun. */
    public List<Integer> receivedChunks(UploadSession session) {
        Path dir = sessionDir(session.getId());
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<Integer> result = new ArrayList<>();
            files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".part"))
                    .forEach(n -> {
                        try {
                            result.add(Integer.parseInt(n.substring(0, n.length() - 5)));
                        } catch (NumberFormatException ignored) {
                            // Begona fayl - e'tiborsiz qoldiramiz.
                        }
                    });
            Collections.sort(result);
            return result;
        } catch (IOException e) {
            log.warn("Bo'laklar ro'yxatini o'qib bo'lmadi: {}", session.getId(), e);
            return List.of();
        }
    }

    // ----------------------------------------------------------------- yig'ish

    @Transactional
    public MediaAsset complete(UploadSession session) {
        if (COMPLETED.equals(session.getStatus())) {
            // Takroriy so'rov - avval yaratilgan mediani qaytaramiz.
            return mediaAssetRepo.findById(session.getMediaAssetId())
                    .orElseThrow(() -> BusinessException.notFound("Media",
                            session.getMediaAssetId()));
        }
        if (!PENDING.equals(session.getStatus())) {
            throw BusinessException.validation("Sessiya yopilgan: " + session.getStatus());
        }

        List<Integer> received = receivedChunks(session);
        if (received.size() != session.getTotalChunks()) {
            List<Integer> missing = new ArrayList<>();
            for (int i = 0; i < session.getTotalChunks(); i++) {
                if (!received.contains(i)) {
                    missing.add(i);
                }
            }
            throw BusinessException.validation(
                    "Bo'laklar to'liq emas. Yetishmayapti: " + missing);
        }

        String key;
        try (InputStream joined = concatenated(session, received)) {
            key = storageService.store(joined, session.getOriginalFilename(), session.getFolder());
        } catch (IOException | UncheckedIOException e) {
            // ⚠️ `UncheckedIOException` ham: bo'lak oqimlari endi yig'ish
            // PAYTIDA ochiladi va u yerda tekshirilgan xato ko'tarilmaydi.
            log.error("Bo'laklarni yig'ib bo'lmadi: {}", session.getId(), e);
            throw new BusinessException("STORAGE_ERROR", "Fayl yig'ilmadi",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        long actualSize = storageService.load(key).exists() ? sizeOf(key) : 0L;

        MediaAsset asset = mediaAssetRepo.save(MediaAsset.builder()
                .storageKey(key)
                .originalFilename(session.getOriginalFilename())
                // ⚠️ Kengaytma ham hisobga olinadi: brauzer `.m4v` uchun
                // MIME bermasligi mumkin va video `DOCUMENT` bo'lib
                // qolardi — ya'ni video tanlash oynasida KO'RINMASDI.
                .type(MediaType.detect(session.getMimeType(), session.getOriginalFilename()))
                .mimeType(session.getMimeType())
                .sizeBytes(actualSize > 0 ? actualSize : session.getSizeBytes())
                .status(MediaStatus.READY)
                .createdBy(session.getCreatedBy())
                .createdAt(LocalDateTime.now())
                .build());

        session.setStatus(COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setMediaAssetId(asset.getId());
        sessionRepo.save(session);

        deleteSessionDir(session.getId());
        return asset;
    }

    @Transactional
    public void abort(UploadSession session) {
        session.setStatus(ABORTED);
        sessionRepo.save(session);
        deleteSessionDir(session.getId());
    }

    // ---------------------------------------------------------------- tozalash

    /**
     * Tashlab ketilgan sessiyalar diskni to'ldirmasin.
     *
     * Klient yarmida yopilib qolsa, bo'laklar abadiy qolib ketardi. Sutkadan
     * oshgan tugallanmagan sessiyalar o'chiriladi.
     */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanUpAbandoned() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<UploadSession> stale =
                sessionRepo.findAllByStatusAndCreatedAtBefore(PENDING, cutoff);
        for (UploadSession session : stale) {
            deleteSessionDir(session.getId());
            session.setStatus(ABORTED);
            sessionRepo.save(session);
        }
        if (!stale.isEmpty()) {
            log.info("Tashlab ketilgan {} ta yuklash sessiyasi tozalandi", stale.size());
        }
    }

    // ------------------------------------------------------------- ichki qism

    private Path sessionDir(String sessionId) {
        // Id server tomonida UUID sifatida yasaladi, lekin tekshiruv baribir
        // turadi: kalit bazadan kelsa ham ildizdan chiqib ketmasin.
        Path dir = tempRoot.resolve(sessionId).normalize();
        if (!dir.startsWith(tempRoot)) {
            throw BusinessException.validation("Sessiya identifikatori noto'g'ri");
        }
        return dir;
    }

    /**
     * Bo'laklarni tartib bilan bitta uzluksiz oqimga ulaydi.
     *
     * ⚠️ Oqimlar BIRMA-BIR ochiladi, hammasi birdan emas.
     *
     * Ilgari bu yerda barcha bo'laklar oldindan ochilib ro'yxatga
     * yig'ilardi. 5 GB video = 5 MB lik 1024 ta bo'lak, ya'ni 1024 ta
     * bir vaqtda ochiq fayl deskriptori. Linux'da standart yumshoq
     * chegara — 1024 ta. Natijada eng KATTA fayllar, ya'ni aynan shu
     * mexanizm yaratilgan fayllar, butun yuklash tugagach oxirgi
     * qadamda "Too many open files" bilan yiqilardi.
     *
     * {@link SequenceInputStream} tugagan oqimni o'zi yopadi, shuning
     * uchun bir vaqtda faqat BITTA bo'lak ochiq turadi.
     */
    private InputStream concatenated(UploadSession session, List<Integer> ordered) {
        Path dir = sessionDir(session.getId());
        List<Integer> sorted = ordered.stream().sorted(Comparator.naturalOrder()).toList();

        return new SequenceInputStream(new Enumeration<>() {
            private int next = 0;

            @Override
            public boolean hasMoreElements() {
                return next < sorted.size();
            }

            @Override
            public InputStream nextElement() {
                Path part = dir.resolve(sorted.get(next++) + ".part");
                try {
                    return Files.newInputStream(part);
                } catch (IOException e) {
                    // `Enumeration` tekshirilgan xatoni ko'tara olmaydi.
                    // `complete` uni ochib, odatiy STORAGE_ERROR beradi.
                    throw new UncheckedIOException(e);
                }
            }
        });
    }

    /** Bo'lak chegaradan oshsa yozishni to'xtatadi - disk to'ldirilmasin. */
    private long copyLimited(InputStream in, Path target) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        try (OutputStream out = Files.newOutputStream(target)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_CHUNK_BYTES) {
                    throw BusinessException.validation("Bo'lak juda katta");
                }
                out.write(buffer, 0, read);
            }
        }
        return total;
    }

    private long sizeOf(String key) {
        try {
            return storageService.load(key).contentLength();
        } catch (IOException e) {
            return 0L;
        }
    }

    private void deleteSessionDir(String sessionId) {
        Path dir = sessionDir(sessionId);
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(this::quietlyDelete);
        } catch (IOException e) {
            log.warn("Sessiya papkasi tozalanmadi: {}", sessionId, e);
        }
        quietlyDelete(dir);
    }

    private void quietlyDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("O'chirib bo'lmadi: {}", path, e);
        }
    }
}
