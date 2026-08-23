package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.Dto.PageResponse;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.MediaUsageService;
import com.example.backend.Cms.Service.StorageService;
import com.example.backend.Enums.Permission;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Media kutubxonasi.
 *
 * Faylning o'zini berish ({@code /raw}) RASMLAR uchun ochiq - afishalar
 * {@code <img>} tegida ko'rsatiladi va ular baribir mobil ilovada hammaga
 * ko'rinadi. Mavjud {@code /api/v1/file/getFile/**} ham shunday ishlaydi.
 *
 * VIDEO esa ochiq EMAS: entitlement
 * ({@link com.example.backend.Cms.Service.AccessService#canReadMedia})
 * tekshiriladi. Aks holda pullik qismni id ni terib yuklab olish mumkin
 * bo'lardi. Ro'yxat va yuklash ruxsat talab qiladi.
 */
@RestController
@RequiredArgsConstructor
public class MediaController {

    private final MediaAssetRepo mediaAssetRepo;
    private final StorageService storageService;
    private final PermissionService permissionService;
    private final AccessService accessService;
    private final MediaUsageService mediaUsageService;

    // ------------------------------------------------------------ ochiq qism

    /**
     * Fayl mazmuni - to'liq.
     *
     * <h2>Nega Range alohida metodda</h2>
     * Bitta metod {@code ResponseEntity<?>} qaytarsa, Spring javob turini
     * {@code Object} deb hisoblaydi va {@code ResourceRegion} uchun konverter
     * TOPA OLMAYDI - natijada 500. Shuning uchun {@code headers = "Range"}
     * sharti bilan ikkita alohida handler: har biri o'z turini aniq e'lon qiladi.
     */
    @GetMapping("/api/v1/app/media/{id}/raw")
    public ResponseEntity<Resource> raw(@PathVariable Long id) {
        MediaAsset asset = readable(id);
        Resource resource = storageService.load(asset.getStorageKey());

        return ResponseEntity.ok()
                .contentType(contentTypeOf(resource))
                .cacheControl(cacheFor(asset))
                // Pleyerga "men bo'laklab bera olaman" deb bildiramiz.
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header("Content-Security-Policy", STRICT_CSP)
                .body(resource);
    }

    /**
     * Fayl mazmuni - bo'lak (206 Partial Content).
     *
     * Pleyer videoni butunlay yuklab olmaydi: u {@code Range} sarlavhasi bilan
     * kerakli bo'lakni so'raydi. Shusiz oldinga o'tkazish (seek) ishlamaydi va
     * har ochilishda butun fayl tortiladi.
     */
    @GetMapping(value = "/api/v1/app/media/{id}/raw", headers = "Range")
    public ResponseEntity<ResourceRegion> rawRange(@PathVariable Long id,
                                                   @RequestHeader HttpHeaders headers)
            throws IOException {
        MediaAsset asset = readable(id);
        Resource resource = storageService.load(asset.getStorageKey());

        List<HttpRange> ranges = headers.getRange();
        if (ranges.isEmpty()) {
            // Buzuq Range sarlavhasi - butun faylni bo'lak sifatida beramiz.
            long length = resource.contentLength();
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(contentTypeOf(resource))
                    .cacheControl(cacheFor(asset))
                    .header("Content-Security-Policy", STRICT_CSP)
                    .body(new ResourceRegion(resource, 0, length));
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(contentTypeOf(resource))
                .cacheControl(cacheFor(asset))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header("Content-Security-Policy", STRICT_CSP)
                .body(ranges.get(0).toResourceRegion(resource));
    }

    /** Media yozuvi + entitlement tekshiruvi. */
    private MediaAsset readable(Long id) {
        MediaAsset asset = mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));

        // Ruxsat yo'q bo'lsa "topilmadi" - fayl bor-yo'qligini ham oshkor qilmaymiz.
        if (!accessService.canReadMedia(CurrentUser.getOrNull(), asset)) {
            throw BusinessException.notFound("Media", id);
        }
        return asset;
    }

    private org.springframework.http.MediaType contentTypeOf(Resource resource) {
        return MediaTypeFactory.getMediaType(resource)
                .orElse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * Fayl mazmuni uchun qat'iy CSP.
     *
     * <h2>Nega kerak</h2>
     * Ruxsat etilgan kengaytmalar orasida SVG bor, SVG esa oddiy rasm emas -
     * uning ichida {@code <script>} bo'lishi mumkin. Foydalanuvchi bu URL'ga
     * to'g'ridan-to'g'ri o'tsa, skript SAYT DOMENIDA ishlaydi va sessiyaga
     * tegishi mumkin (saqlangan XSS).
     *
     * {@code nosniff} bu yerda yordam bermaydi: turi haqiqatan
     * {@code image/svg+xml}, ya'ni brauzer uni to'g'ri deb hisoblaydi.
     *
     * {@code default-src 'none'} + {@code sandbox} skriptni ham, tashqi
     * so'rovlarni ham to'xtatadi. {@code <img src>} orqali ko'rsatishga ta'sir
     * qilmaydi - u yerda skript baribir ishlamaydi.
     */
    private static final String STRICT_CSP = "default-src 'none'; sandbox";

    private CacheControl cacheFor(MediaAsset asset) {
        // Pullik video keshda qolmasligi kerak: obuna tugagach ham ochilib ketardi.
        return asset.getType() == MediaType.VIDEO
                ? CacheControl.noStore()
                // Rasm o'zgarmaydi (yangi fayl = yangi id), shuning uchun uzoq kesh.
                : CacheControl.maxAge(Duration.ofDays(30)).cachePublic();
    }
    // ----------------------------------------------------------- admin qismi

    /**
     * Media kutubxonasi (ТЗ §26).
     *
     * <h2>Arxivlanganlar sukut bo'yicha KO'RSATILMAYDI</h2>
     * Kutubxonaning asosiy vazifasi — yangi kontentga fayl tanlash. Eskirgan
     * fayllar ro'yxatda turaversa, admin ularni bilmasdan qayta ishlatardi.
     *
     * Ularni ko'rish uchun {@code status=ARCHIVED} beriladi.
     *
     * @param q asl fayl nomi bo'yicha qidiruv
     */
    @GetMapping("/api/v1/app/admin/media")
    public ResponseEntity<PageResponse<MediaDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size,
            @RequestParam(required = false) MediaType type,
            @RequestParam(required = false) MediaStatus status,
            @RequestParam(required = false) String q) {

        require(Permission.MEDIA_VIEW);
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        // Holat ko'rsatilmasa - faqat tayyor fayllar.
        MediaStatus effectiveStatus = status == null ? MediaStatus.READY : status;
        String needle = (q == null || q.isBlank()) ? null : q.trim();

        return ResponseEntity.ok(PageResponse.of(
                mediaAssetRepo.library(type, effectiveStatus, needle, pageable),
                MediaDto::from));
    }

    /**
     * Faylni arxivlaydi — kutubxonadan yashiradi (ТЗ §26 «remove/archive»).
     *
     * <h2>Nega o'chirish emas</h2>
     * Fayl 12 xil joydan havola qilinishi mumkin. O'chirish sahifalarda
     * sinib qolgan rasm va o'ynamaydigan video demakdir.
     *
     * Arxivlash xavfsiz: mavjud havolalar ISHLASHDA DAVOM ETADI, faqat
     * admin uni yangi kontentga qo'shib yubormaydi.
     *
     * Butunlay o'chirish ham bor ({@code DELETE}), lekin u faqat hech
     * qayerda ishlatilmagan fayl uchun.
     */
    @PostMapping("/api/v1/app/admin/media/{id}/archive")
    public ResponseEntity<MediaDto> archive(@PathVariable Long id) {
        require(Permission.MEDIA_DELETE);
        MediaAsset asset = mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));

        asset.setStatus(MediaStatus.ARCHIVED);
        return ResponseEntity.ok(MediaDto.from(mediaAssetRepo.save(asset)));
    }

    @PostMapping("/api/v1/app/admin/media/{id}/restore")
    public ResponseEntity<MediaDto> restore(@PathVariable Long id) {
        require(Permission.MEDIA_DELETE);
        MediaAsset asset = mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));

        asset.setStatus(MediaStatus.READY);
        return ResponseEntity.ok(MediaDto.from(mediaAssetRepo.save(asset)));
    }

    @PostMapping("/api/v1/app/admin/media")
    public ResponseEntity<MediaDto> upload(@RequestParam MultipartFile file,
                                           @RequestParam(defaultValue = "content") String folder) {
        require(Permission.MEDIA_UPLOAD);

        String key = storageService.store(file, folder);
        String contentType = file.getContentType() == null ? "" : file.getContentType();

        MediaAsset asset = mediaAssetRepo.save(MediaAsset.builder()
                .storageKey(key)
                .originalFilename(file.getOriginalFilename())
                .type(contentType.startsWith("video") ? MediaType.VIDEO
                        : contentType.startsWith("image") ? MediaType.IMAGE : MediaType.DOCUMENT)
                .mimeType(file.getContentType())
                .sizeBytes(file.getSize())
                .status(MediaStatus.READY)
                .createdBy(CurrentUser.get().getId())
                .build());

        return ResponseEntity.ok(MediaDto.from(asset));
    }

    /**
     * Fayl qayerda ishlatilyapti.
     *
     * O'chirishdan oldin admin ko'rishi uchun: «bu afisha 3 ta kontentda
     * ishlatilyapti» degan javob «foreign key constraint violation» dan
     * ancha foydali.
     */
    @GetMapping("/api/v1/app/admin/media/{id}/usage")
    public ResponseEntity<List<MediaUsageService.Usage>> usage(@PathVariable Long id) {
        require(Permission.MEDIA_VIEW);
        mediaAssetRepo.findById(id).orElseThrow(() -> BusinessException.notFound("Media", id));
        return ResponseEntity.ok(mediaUsageService.usages(id));
    }

    /**
     * Faylni o'chiradi.
     *
     * <h2>Ishlatilayotgan fayl o'chirilmaydi</h2>
     * Aks holda sahifalarda sinib qolgan rasmlar va o'ynamaydigan videolar
     * paydo bo'lardi. 409 qaytadi va javobda AYNAN qayerda ishlatilayotgani
     * yoziladi — admin avval o'sha joyni tuzatadi.
     *
     * <h2>Nega bu endpoint kerak edi</h2>
     * {@code MEDIA_DELETE} ruxsati mavjud edi, lekin uni ishlatadigan
     * endpoint YO'Q edi. Ya'ni panelda bor, amalda hech narsa qilmaydigan
     * huquq — bu xodimga noto'g'ri tasavvur beradi.
     */
    @DeleteMapping("/api/v1/app/admin/media/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        require(Permission.MEDIA_DELETE);

        MediaAsset asset = mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));

        List<MediaUsageService.Usage> usages = mediaUsageService.usages(id);
        if (!usages.isEmpty()) {
            String where = usages.stream()
                    .map(u -> u.getWhere() + " (" + u.getCount() + ")")
                    .collect(java.util.stream.Collectors.joining(", "));
            throw new BusinessException("MEDIA_IN_USE",
                    "Fayl ishlatilmoqda, avval bo'shating: " + where,
                    org.springframework.http.HttpStatus.CONFLICT);
        }

        // Avval baza yozuvi: fayl o'chib, yozuv qolsa — media kutubxonasida
        // ochilmaydigan element ko'rinardi. Teskarisi esa shunchaki
        // egasiz fayl, u tozalash vazifasi bilan yo'q qilinadi.
        mediaAssetRepo.delete(asset);
        storageService.delete(asset.getStorageKey());

        return ResponseEntity.noContent().build();
    }

    private void require(Permission permission) {
        if (!permissionService.hasPermission(CurrentUser.get(), permission)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
        }
    }

    @Data
    @Builder
    public static class MediaDto {
        private Long id;
        private String url;
        private String originalFilename;
        private MediaType type;
        private String mimeType;
        private Long sizeBytes;
        private Integer width;
        private Integer height;
        private Integer durationSeconds;

        /** READY yoki ARCHIVED — panel arxivlanganini ajratib ko'rsatadi. */
        private MediaStatus status;

        private LocalDateTime createdAt;

        static MediaDto from(MediaAsset a) {
            return MediaDto.builder()
                    .id(a.getId())
                    .url("/api/v1/app/media/" + a.getId() + "/raw")
                    .originalFilename(a.getOriginalFilename())
                    .type(a.getType())
                    .mimeType(a.getMimeType())
                    .sizeBytes(a.getSizeBytes())
                    .width(a.getWidth())
                    .height(a.getHeight())
                    .durationSeconds(a.getDurationSeconds())
                    .status(a.getStatus())
                    .createdAt(a.getCreatedAt())
                    .build();
        }
    }
}
