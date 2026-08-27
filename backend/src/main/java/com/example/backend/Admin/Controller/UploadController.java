package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.UploadSession;
import com.example.backend.Cms.Enums.UploadMode;
import com.example.backend.Cms.Repository.UploadSessionRepo;
import com.example.backend.Cms.Service.ChunkedUploadService;
import com.example.backend.Entity.User;
import com.example.backend.Enums.Permission;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Katta fayllarni bo'laklab yuklash (video uchun).
 *
 * <h2>Nega alohida oqim</h2>
 * Oddiy {@code POST /api/v1/app/admin/media} bitta multipart so'rov bilan ishlaydi
 * va prod chegarasi 50 MB. Epizod videosi bunga sig'maydi, sig'gan taqdirda
 * ham ulanish uzilsa hammasi boshidan boshlanardi.
 *
 * <h2>Klient qanday ishlatadi</h2>
 * <pre>
 * POST   /api/v1/app/admin/uploads                  → uploadId, chunkSize
 * PUT    /api/v1/app/admin/uploads/{id}/chunks/{n}  → bo'lak baytlari (xom tana)
 * GET    /api/v1/app/admin/uploads/{id}             → qaysi bo'laklar yetib kelgan
 * POST   /api/v1/app/admin/uploads/{id}/complete    → yig'ish, media yaratish
 * DELETE /api/v1/app/admin/uploads/{id}             → bekor qilish
 * </pre>
 *
 * Uzilishdan keyin klient {@code GET} bilan holatni so'raydi va faqat
 * YETISHMAGAN bo'laklarni qayta yuboradi.
 *
 * <h2>Bo'lak xom tanada, multipart emas</h2>
 * Multipart bo'lsa {@code spring.servlet.multipart} chegarasi yana ishlab
 * ketardi. Xom {@code application/octet-stream} tanasi to'g'ridan-to'g'ri
 * oqim sifatida o'qiladi va diskka yoziladi — RAM'da to'planmaydi.
 */
@RestController
@RequestMapping("/api/v1/app/admin/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final ChunkedUploadService uploadService;
    private final UploadSessionRepo sessionRepo;
    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<UploadSessionDto> begin(@RequestBody BeginRequest request) {
        User actor = requireUploader();

        UploadSession session = uploadService.begin(
                actor.getId(), request.getFilename(), request.getSizeBytes(),
                request.getMimeType(), request.getFolder());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UploadSessionDto.of(session, List.of()));
    }

    /**
     * Bo'lakni qabul qiladi.
     *
     * Takroriy yuborish xavfsiz: bo'lak qayta yoziladi. Klient uzilishdan
     * keyin qaysi bo'lak to'liq yetganini bilmasligi mumkin.
     */
    @PutMapping("/{id}/chunks/{index}")
    public ResponseEntity<ChunkAccepted> uploadChunk(@PathVariable String id,
                                                     @PathVariable int index,
                                                     HttpServletRequest request)
            throws IOException {
        UploadSession session = ownSession(id);

        long written;
        try (InputStream body = request.getInputStream()) {
            written = uploadService.saveChunk(session, index, body);
        }

        List<Integer> received = uploadService.receivedChunks(session);
        return ResponseEntity.ok(ChunkAccepted.builder()
                .index(index)
                .bytes(written)
                .receivedChunks(received.size())
                .totalChunks(session.getTotalChunks())
                .build());
    }

    /**
     * Bo'laklar uchun imzolangan havolalar — FAQAT S3 rejimida.
     *
     * <h2>Nega hammasi birdan emas</h2>
     * 20 GB lik fayl 2048 ta bo'lakdan iborat. Hammasini bitta javobda
     * berish ~600 KB JSON degani va ularning ko'pi ishlatilmasdan
     * muddati o'tardi.
     *
     * Klient kerak bo'lganda keyingi guruhni so'raydi.
     *
     * ⚠️ Havolalar LOGGA yozilmaydi — ular imzo bilan birga o'sha
     * bo'lakka to'liq yozish huquqini beradi.
     */
    @PostMapping("/{id}/parts")
    public ResponseEntity<PartUrls> partUrls(@PathVariable String id,
                                             @RequestBody PartUrlRequest request) {
        UploadSession session = ownSession(id);

        int from = request.getFrom() == null ? 0 : request.getFrom();
        int count = request.getCount() == null ? DEFAULT_PART_BATCH : request.getCount();
        if (count < 1 || count > MAX_PART_BATCH) {
            throw BusinessException.validation(
                    "count 1 dan " + MAX_PART_BATCH + " gacha bo'lishi kerak");
        }

        int last = Math.min(from + count, session.getTotalChunks());
        List<PartUrl> urls = new ArrayList<>();
        for (int index = from; index < last; index++) {
            urls.add(PartUrl.builder()
                    .index(index)
                    .url(uploadService.presignedPartUrl(session, index))
                    .build());
        }

        return ResponseEntity.ok(PartUrls.builder()
                .uploadId(session.getId())
                .chunkSize(session.getChunkSize())
                .totalChunks(session.getTotalChunks())
                .parts(urls)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UploadSessionDto> status(@PathVariable String id) {
        UploadSession session = ownSession(id);
        return ResponseEntity.ok(
                UploadSessionDto.of(session, uploadService.receivedChunks(session)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<MediaController.MediaDto> complete(@PathVariable String id) {
        UploadSession session = ownSession(id);
        MediaAsset asset = uploadService.complete(session);
        return ResponseEntity.ok(MediaController.MediaDto.from(asset));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> abort(@PathVariable String id) {
        uploadService.abort(ownSession(id));
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------- ichki qism

    private User requireUploader() {
        User actor = CurrentUser.get();
        if (!permissionService.hasPermission(actor, Permission.MEDIA_UPLOAD)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + Permission.MEDIA_UPLOAD);
        }
        return actor;
    }

    /**
     * Sessiyani egasi uchun ochadi.
     *
     * Boshqa xodimning sessiyasiga tegib bo'lmaydi: aks holda bir xodim
     * ikkinchisining yarim yuklangan fayliga bo'lak qo'shib, natijani
     * buzishi yoki almashtirib yuborishi mumkin edi.
     */
    private UploadSession ownSession(String id) {
        User actor = requireUploader();
        UploadSession session = sessionRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Yuklash sessiyasi", id));

        if (!session.getCreatedBy().equals(actor.getId())) {
            // "Topilmadi": boshqa xodimning sessiyasi bor-yo'qligini ham aytmaymiz.
            throw BusinessException.notFound("Yuklash sessiyasi", id);
        }
        return session;
    }

    // ------------------------------------------------------------------- DTO

    @Data
    public static class BeginRequest {
        @NotBlank
        private String filename;

        @NotNull
        private Long sizeBytes;

        private String mimeType;

        /** Yakuniy fayl papkasi. Bo'sh bo'lsa "content". */
        private String folder;
    }

    /** Bir so'rovda beriladigan havolalar soni — sukut. */
    private static final int DEFAULT_PART_BATCH = 20;

    /** Ko'pi bilan. Chegarasiz bo'lsa klient 10 000 tasini so'rashi mumkin edi. */
    private static final int MAX_PART_BATCH = 200;

    @Data
    public static class PartUrlRequest {
        /** Qaysi bo'lakdan boshlab. 0 dan. */
        private Integer from;
        private Integer count;
    }

    @Data
    @Builder
    public static class PartUrls {
        private String uploadId;
        private Integer chunkSize;
        private Integer totalChunks;
        private List<PartUrl> parts;
    }

    @Data
    @Builder
    public static class PartUrl {
        /** Klient raqami — 0 dan. S3 ga 1 dan yuboriladi, aylantirishni server qiladi. */
        private int index;
        private String url;
    }

    @Data
    @Builder
    public static class UploadSessionDto {
        private String uploadId;
        private String originalFilename;
        private Long sizeBytes;
        private Integer chunkSize;
        private Integer totalChunks;
        private String status;
        /** Yetib kelgan bo'laklar - klient faqat qolganini yuboradi. */
        private List<Integer> receivedChunks;
        private Long mediaAssetId;

        /**
         * {@code CHUNKED} — bo'lak {@code PUT .../chunks/{n}} orqali;
         * {@code S3_MULTIPART} — {@code POST .../parts} dan havola olinadi.
         *
         * ⚠️ Klient rejimni O'ZI tanlamaydi va taxmin qilmaydi: server
         * aytadi. Aks holda sozlama o'zgarganda klient eski yo'ldan
         * yuborishda davom etardi.
         */
        private UploadMode uploadMode;

        static UploadSessionDto of(UploadSession s, List<Integer> received) {
            return UploadSessionDto.builder()
                    .uploadMode(s.getUploadMode())
                    .uploadId(s.getId())
                    .originalFilename(s.getOriginalFilename())
                    .sizeBytes(s.getSizeBytes())
                    .chunkSize(s.getChunkSize())
                    .totalChunks(s.getTotalChunks())
                    .status(s.getStatus())
                    .receivedChunks(received)
                    .mediaAssetId(s.getMediaAssetId())
                    .build();
        }
    }

    @Data
    @Builder
    public static class ChunkAccepted {
        private int index;
        private long bytes;
        private int receivedChunks;
        private int totalChunks;
    }
}
