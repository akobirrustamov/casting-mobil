package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.Dto.PageResponse;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.TranscodingJob;
import com.example.backend.Cms.Service.Video.TranscodingJobService;
import com.example.backend.Cms.Service.Video.VideoSystemHealth;
import com.example.backend.Cms.Enums.VideoProcessingStatus;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Entity.User;
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

    /** Transcoding holati — panel uni ko'rsatadi. */
    private final TranscodingJobService transcodingJobs;

    /**
     * Server transcoding uchun tayyormi.
     *
     * ⚠️ Navbat bilan BIRGA qaytariladi, alohida endpoint emas.
     * Alohida bo'lsa panel uni so'rashni unutardi va admin yana
     * «videolar nega yiqilyapti» degan savol bilan qolardi.
     */
    private final VideoSystemHealth videoSystemHealth;
    private final AccessService accessService;
    private final MediaUsageService mediaUsageService;

    /** Panelda oldindan ko'rish uchun chipta — {@code /preview} ga qarang. */
    private final com.example.backend.Cms.Service.Video.PlaybackTicketService ticketService;

    /**
     * S3 imzolangan havola — faqat {@code app.storage.provider=s3} da.
     *
     * ⚠️ {@code Optional}: lokal saqlashda bunday bin YO'Q va uni
     * majburiy qilish butun kontekstni ko'tarmasdi.
     */
    private final java.util.Optional<com.example.backend.Cms.Service.Video.SignedUrlProvider> signedUrls;

    /** Chiptali HLS havolasi — moslashuvchan oqim uchun. */
    private final com.example.backend.Cms.Service.Video.PlaybackUrlService playbackUrlService;

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
    public ResponseEntity<Resource> raw(@PathVariable Long id,
                                        @RequestParam(value = "t", required = false) String ticket) {
        MediaAsset asset = readable(id, ticket);
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
                                                   @RequestHeader HttpHeaders headers,
                                                   @RequestParam(value = "t", required = false) String ticket)
            throws IOException {
        // ⚠️ Chipta bu yerda ham SHART. Pleyer birinchi so'rovni
        // Range'siz yuboradi, keyingi hammasini Range bilan — ya'ni
        // faqat yuqoridagi metodga qo'shilsa, video ochilib darrov
        // to'xtardi.
        MediaAsset asset = readable(id, ticket);
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
        return readable(id, null);
    }

    /**
     * Media yozuvi + entitlement tekshiruvi.
     *
     * <h2>⚠️ Nega chipta URL'da, sarlavhada emas</h2>
     * Brauzerning {@code <video src>} elementi hech qanday
     * {@code Authorization} sarlavhasi YUBORMAYDI — u oddiy GET
     * qiladi. Ya'ni panel xodimi o'zi yuklagan videoni ko'ra
     * olmasdi: token bormi-yo'qmi, element uni uzatmaydi.
     *
     * Shu sababli {@code HlsController} dagi mexanizm qayta
     * ishlatiladi: chipta KIMLIGINI aytadi, huquqni esa har so'rovda
     * {@code AccessService} beradi. Chipta bitta media'ga bog'langan
     * va muddati cheklangan.
     *
     * ⚠️ Chipta ixtiyoriy: token bilan keladigan so'rovlar (panel
     * ro'yxati, mobil ilova) avvalgidek ishlaydi.
     */
    private MediaAsset readable(Long id, String ticket) {
        MediaAsset asset = mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));

        User viewer = CurrentUser.getOrNull();

        // Chipta bo'lsa — egasi kim ekanini o'shandan olamiz.
        if (viewer == null && ticket != null && !ticket.isBlank()) {
            try {
                viewer = ticketService.holderOf(ticket, id);
            } catch (IllegalArgumentException e) {
                // Yaroqsiz chipta ham "topilmadi" — media bor-yo'qligi
                // oshkor qilinmaydi.
                throw BusinessException.notFound("Media", id);
            }
        }

        // Ruxsat yo'q bo'lsa "topilmadi" - fayl bor-yo'qligini ham oshkor qilmaymiz.
        if (!accessService.canReadMedia(viewer, asset)) {
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
    /**
     * Media kutubxonasi saralanadigan ustunlar (§95).
     *
     * ⚠️ Hajm bo'yicha saralash amaliy: eng katta fayllarni topib,
     * ishlatilmayotganlarini tozalash uchun kerak bo'ladi.
     */
    private static final com.example.backend.Admin.SortWhitelist MEDIA_SORT =
            com.example.backend.Admin.SortWhitelist.of("createdAt")
                    .add("filename", "originalFilename")
                    .add("size", "sizeBytes");

    @GetMapping("/api/v1/app/admin/media")
    public ResponseEntity<PageResponse<MediaDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size,
            @RequestParam(required = false) MediaType type,
            @RequestParam(required = false) MediaStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) VideoProcessingStatus transcoding,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {

        require(Permission.MEDIA_VIEW);
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                MEDIA_SORT.resolve(sort, dir));

        // Holat ko'rsatilmasa - faqat tayyor fayllar.
        MediaStatus effectiveStatus = status == null ? MediaStatus.READY : status;
        String needle = (q == null || q.isBlank()) ? null : q.trim();

        var result = mediaAssetRepo.library(type, effectiveStatus, needle, transcoding, pageable);

        // ⚠️ Ishlar BITTA so'rovda olinadi. Har media uchun alohida
        // so'rov 40 elementli sahifada 40 ta ortiqcha murojaat —
        // klassik N+1.
        var jobs = transcodingJobs.forMediaIds(
                result.getContent().stream().map(MediaAsset::getId).toList());

        return ResponseEntity.ok(PageResponse.of(
                result, asset -> MediaDto.from(asset, jobs.get(asset.getId()))));
    }

    /**
     * Bitta fayl haqida ma'lumot.
     *
     * <h2>Nima uchun kerak</h2>
     * Media maydonlariga (`MediaField`) faqat `mediaId` uziladi — fayl
     * nomi ham, formati ham u yerda yo'q. Mavjud epizod ochilganda
     * panel `.mkv` biriktirilganini boshqa hech qanday yo'l bilan
     * bilolmaydi va admin faylni jimgina qora ekran bilan qoldirardi.
     *
     * ⚠️ Arxivlangan fayl ham QAYTARILADI. Bu ataylab: mavjud
     * kontentda arxivlangan faylga havola bo'lishi mumkin va panel
     * uni ko'rsata olishi kerak. Kutubxona ro'yxati (`list`) esa
     * arxivlanganlarni yashiradi — u yangi fayl TANLASH uchun.
     */
    @GetMapping("/api/v1/app/admin/media/{id}")
    public ResponseEntity<MediaDto> one(@PathVariable Long id) {
        require(Permission.MEDIA_VIEW);
        MediaAsset asset = mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));

        return ResponseEntity.ok(
                MediaDto.from(asset, transcodingJobs.forMedia(id).orElse(null)));
    }

    /**
     * Yiqilgan transcoding'ni qayta urinish (§18).
     *
     * <h2>Nega {@code MEDIA_UPLOAD}, yangi ruxsat emas</h2>
     * Kim video yuklay olsa, qayta urinish ham o'sha ishning davomi.
     * Yangi ruxsat mavjud rollarni qayta sozlashni talab qilardi va
     * hech qanday yangi chegara qo'shmasdi.
     *
     * ⚠️ Faqat TUGAGAN ish qayta urinishga qabul qilinadi
     * ({@code READY} yoki {@code FAILED}). Ishlab turgan ishni
     * navbatga qaytarish ikkita FFmpeg ni bitta media ustida
     * ishlatardi.
     */
    @PostMapping("/api/v1/app/admin/media/{id}/retry-transcoding")
    public ResponseEntity<MediaDto> retryTranscoding(@PathVariable Long id) {
        require(Permission.MEDIA_UPLOAD);

        MediaAsset asset = mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));

        TranscodingJob job = transcodingJobs.retry(id);
        return ResponseEntity.ok(MediaDto.from(asset, job));
    }

    /**
     * Navbat holati — panel boshqa ish bor-yo'qligini bilishi uchun.
     *
     * ⚠️ Panel shu asosda davriy yangilashni TO'XTATADI: hamma ish
     * tugagan bo'lsa so'rov yubormaydi. Doimiy so'rov ochiq turgan
     * panel serverga bekorga yuk berardi.
     */
    @GetMapping("/api/v1/app/admin/media/transcoding-queue")
    public ResponseEntity<QueueDto> transcodingQueue() {
        require(Permission.MEDIA_VIEW);
        return ResponseEntity.ok(QueueDto.builder()
                .queued(transcodingJobs.count(VideoProcessingStatus.QUEUED))
                .running(transcodingJobs.count(
                        VideoProcessingStatus.PROBING,
                        VideoProcessingStatus.TRANSCODING,
                        VideoProcessingStatus.UPLOADING))
                .failed(transcodingJobs.count(VideoProcessingStatus.FAILED))
                .system(videoSystemHealth.check())
                .build());
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

    /**
     * Panelda videoni OLDINDAN KO'RISH uchun havola.
     *
     * <h2>⚠️ Nima uchun alohida endpoint kerak bo'ldi</h2>
     * Xodim video yuklardi, lekin uni HECH QACHON ko'ra olmasdi —
     * panelda pleyer umuman yo'q edi. Ya'ni afisha to'g'ri
     * biriktirilganini, video buzuq emasligini yoki dublyaj mos
     * kelishini tekshirishning yagona yo'li — kontentni nashr qilib,
     * ilovadan ochish edi.
     *
     * <h2>Nega chipta</h2>
     * Brauzerning {@code <video>} elementi {@code Authorization}
     * sarlavhasini yubormaydi. Chipta esa manzilning o'zida keladi —
     * {@code HlsController} da xuddi shu sabab bilan shunday qilingan.
     *
     * ⚠️ Chipta HUQUQ BERMAYDI, faqat kimligini aytadi. Har so'rovda
     * {@code AccessService} qayta tekshiradi — ya'ni xodim paneldan
     * chiqarilsa havola o'sha zahoti ishlamay qoladi.
     *
     * ⚠️ Bu HLS emas, ASL fayl. Panel uchun ataylab: qo'shimcha
     * kutubxona kerak emas va har brauzerda ishlaydi. Katta fayl
     * to'liq tortilmaydi — pleyer {@code Range} bilan faqat kerakli
     * bo'lakni oladi.
     */
    @GetMapping("/api/v1/app/admin/media/{id}/preview")
    public ResponseEntity<PreviewDto> preview(@PathVariable Long id) {
        require(Permission.CONTENT_VIEW);

        MediaAsset asset = mediaAssetRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Media", id));

        User actor = CurrentUser.get();
        if (!accessService.canReadMedia(actor, asset)) {
            throw BusinessException.notFound("Media", id);
        }

        return ResponseEntity.ok(PreviewDto.builder()
                .mediaId(id)
                // ⚠️ HLS BIRINCHI o'rinda: u sifatni tomoshabin
                // internetiga qarab o'zi tanlaydi (1080 / 720 / 480).
                //
                // Asl fayl esa BITTA sifat — 4K manbada admin
                // sekin internetda uni umuman ocholmasdi.
                //
                // `null` bo'lsa transcoding hali tugamagan yoki
                // yiqilgan — klient asl faylga qaytadi.
                .hlsUrl(playbackUrlService.hlsUrlFor(actor, asset))
                .url(previewUrl(actor, asset))
                .type(asset.getType())
                .mimeType(asset.getMimeType())
                .build());
    }

    /**
     * Oldindan ko'rish manzili.
     *
     * <h2>⚠️ S3 da fayl SERVER ORQALI berilmaydi</h2>
     * Avval bu yerda chiptali {@code /raw} qaytarilardi va u kichik
     * fayllarda ishlagandek ko'rinardi. 591 MB lik 4K manbada esa
     * o'lchab ko'rildi:
     *
     *   boshidan 256 KB  →  60 soniyadan ortiq
     *   o'rtasidan seek  →  umuman ulgurmadi
     *
     * Sababi: bo'lak so'ralganda ham ob'ekt S3 dan SERVERGA tortiladi
     * va shundan keyin brauzerga uzatiladi. Ya'ni har seek — yangi
     * yuklab olish.
     *
     * Imzolangan havola bilan brauzer S3 ga TO'G'RIDAN-TO'G'RI murojaat
     * qiladi va {@code Range} ni ombor o'zi bajaradi. Bu HLS
     * segmentlari uchun allaqachon shunday ishlaydi.
     *
     * ⚠️ Lokal saqlashda imzolash yo'q — o'shanda chiptali
     * {@code /raw} qoladi. U yerda fayl diskda, ya'ni sekinlik yo'q.
     */
    private String previewUrl(User actor, MediaAsset asset) {
        if (signedUrls.isPresent() && signedUrls.get().isAvailable()) {
            return signedUrls.get().sign(asset.getStorageKey());
        }

        return "/api/v1/app/media/" + asset.getId() + "/raw?t="
                + org.springframework.web.util.UriUtils.encode(
                        ticketService.issue(actor, asset.getId()),
                        java.nio.charset.StandardCharsets.UTF_8);
    }

    @lombok.Data
    @lombok.Builder
    public static class PreviewDto {
        private Long mediaId;
        /**
         * Moslashuvchan oqim — sifat internetga qarab tanlanadi.
         *
         * ⚠️ Transcoding tugamagan bo'lsa {@code null}. Klient
         * o'shanda {@code url} ga (asl fayl) qaytadi.
         */
        private String hlsUrl;
        /** Chipta bilan to'liq manzil — to'g'ridan-to'g'ri `<video src>` ga. */
        private String url;
        private MediaType type;
        private String mimeType;
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
                // ⚠️ Kengaytma ham hisobga olinadi — sabab MediaType.detect da.
                .type(MediaType.detect(contentType, file.getOriginalFilename()))
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

    /**
     * Video qayta ishlash holati — admin panel uchun.
     *
     * ⚠️ Panelda endi IKKITA holat bor va ikkalasi ham «READY» so'zini
     * ishlatadi:
     *
     * <pre>
     *   MediaDto.status              READY · ARCHIVED   — kutubxonada ko'rinadimi
     *   MediaDto.transcoding.status  QUEUED … FAILED    — HLS tayyormi
     * </pre>
     *
     * Chalkashlik TARJIMA bilan hal qilinadi: panelda birinchisi
     * «Kutubxonada / Arxivda», ikkinchisi «Video qayta ishlash» deb
     * ataladi. Backend nomlari O'ZGARMAYDI — ular API shartnomasi.
     */
    /**
     * Navbat qisqacha holati.
     *
     * ⚠️ {@code READY} sanalmaydi: u vaqt o'tishi bilan cheksiz o'sadi
     * va panelga hech narsa aytmaydi. Panelga kerak bo'lgani —
     * «hali ish bormi».
     */
    @Data
    @Builder
    public static class QueueDto {
        private long queued;
        private long running;
        private long failed;

        /**
         * Server holati: FFmpeg bormi, diskda joy bormi.
         *
         * ⚠️ Bu navbat sonlaridan ko'ra MUHIMROQ. «3 ta yiqildi»
         * degan raqam adminni buzuq fayl izlashga yuborardi, sabab
         * esa FFmpeg umuman o'rnatilmagani bo'lishi mumkin.
         */
        private VideoSystemHealth.Report system;

        /** Panel davriy yangilashni davom ettirishi kerakmi. */
        public boolean isActive() {
            return queued > 0 || running > 0;
        }
    }

    @Data
    @Builder
    public static class TranscodingDto {

        private VideoProcessingStatus status;

        /**
         * 0..99. {@code READY} da 100.
         *
         * ⚠️ Bu faqat KO'RSATISH uchun. Tayyorlikni {@code status}
         * aytadi — «progress 100» hech qachon tayyorlik belgisi
         * bo'lmagan.
         */
        private Integer progress;

        /**
         * Yiqilish sababi. {@code FAILED} dan boshqa holatda {@code null}.
         *
         * ⚠️ Ko'rsatilishi SHART. Faqat «yiqildi» deyish adminni logga
         * qarashga majbur qilardi, logga esa uning kirishi yo'q.
         */
        private String error;

        private Integer attempts;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;

        /**
         * Qayta urinish mumkinmi.
         *
         * ⚠️ Klient buni O'ZI hisoblamaydi. Aks holda «tugagan ish»
         * qoidasi ikki joyda yashardi va ular ajralib ketardi —
         * masalan panel tugmani ko'rsatardi, server esa 422 qaytarardi.
         */
        private boolean retryable;

        static TranscodingDto from(TranscodingJob job) {
            // ⚠️ Media TURI bu yerda tekshirilmaydi va bu ataylab.
            //
            // Ish faqat VIDEO uchun yaratiladi — buni
            // `TranscodingJobService.enqueue` kafolatlaydi va u yerda
            // test bilan qo'riqlangan. Ya'ni ish bor bo'lsa, media
            // albatta video.
            //
            // Bu yerda takroriy tekshiruv qo'shilsa, u HECH QACHON
            // ishlamaydigan shox bo'lardi: uni sinab bo'lmaydi va
            // shuning uchun u to'g'ri ekaniga ishonch ham yo'q.
            if (job == null) {
                return null;
            }
            return TranscodingDto.builder()
                    .status(job.getStatus())
                    .progress(job.getProgress())
                    .error(job.getError())
                    .attempts(job.getAttempts())
                    .startedAt(job.getStartedAt())
                    .finishedAt(job.getFinishedAt())
                    .retryable(job.getStatus() != null && job.getStatus().isFinished())
                    .build();
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

        /**
         * Pleyer bu faylni to'g'ridan-to'g'ri o'ynata oladimi.
         *
         * ⚠️ VIDEO uchungina ma'noli; rasm va hujjatda `null`.
         *
         * `.mkv` va `.avi` omborga qabul qilinadi (manba fayl kerak
         * bo'lishi mumkin), lekin HTML5 pleyer ularni ochmaydi.
         * Panel shu bayroq bo'yicha ogohlantiradi — usiz admin
         * videoni epizodga biriktirib, foydalanuvchida QORA EKRAN
         * yaratardi va buni hech kim darhol sezmasdi.
         */
        private Boolean playable;

        /**
         * Video qayta ishlash holati. VIDEO bo'lmagan media uchun
         * {@code null}.
         *
         * ⚠️ {@code null} — «ish yo'q», «ish yiqilgan» EMAS. Rasm va
         * hujjatga transcoding umuman tegishli emas, shuning uchun
         * ular uchun bo'sh obyekt ham qaytarilmaydi.
         *
         * Video uchun ham {@code null} bo'lishi mumkin: eski,
         * transcoding joriy qilinishidan oldin yuklangan fayllar.
         */
        private TranscodingDto transcoding;

        private LocalDateTime createdAt;

        /**
         * Transcoding ma'lumotisiz — yuklash va arxivlash javoblari uchun.
         *
         * ⚠️ Bu yerda ish HALI bo'lmasligi mumkin (yuklash endi
         * tugadi) yoki u kerak emas (arxivlash). Ortiqcha so'rov
         * qilinmaydi.
         */
        static MediaDto from(MediaAsset a) {
            return from(a, null);
        }

        static MediaDto from(MediaAsset a, TranscodingJob job) {
            return MediaDto.builder()
                    .transcoding(TranscodingDto.from(job))
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
                    // Rasm/hujjat uchun `null`: "o'ynatib bo'lmaydi" EMAS,
                    // "bu savol umuman tegishli emas".
                    .playable(a.getType() == MediaType.VIDEO
                            ? MediaType.isPlayable(a.getOriginalFilename())
                            : null)
                    .createdAt(a.getCreatedAt())
                    .build();
        }
    }
}
