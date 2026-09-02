package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Dto.HomeFeedDto;
import com.example.backend.Cms.Entity.WatchProgress;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.WatchTargetType;
import com.example.backend.Cms.Service.HomeFeedService;
import com.example.backend.Cms.Service.WatchProgressService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * «Ko'rishda davom eting» — qayerda to'xtaganini eslab qolish.
 *
 * <h2>⚠️ Nega {@link WatchController} ga qo'shilmadi</h2>
 * U <b>faqat o'qiydi</b> va butunlay {@code @Transactional(readOnly)}:
 * «ko'ra olamanmi va qaysi faylni o'ynatay». Progress esa YOZADI va
 * har 15 soniyada keladi.
 *
 * Bir joyga qo'yilsa, eng tez-tez chaqiriladigan yozuv endpointi
 * huquq tekshiruvi bilan bir sinfda turardi — va ularning kesh
 * qoidalari qarama-qarshi.
 *
 * <h2>Ikki xil manzil emas, bitta</h2>
 * Ko'rish ikki endpointdan boradi ({@code /watch/{episodeId}} va
 * {@code /watch/content/{contentId}}), lekin progress bitta:
 * {@code type} ni yo'lda emas, parametrda oladi. Aks holda ayni
 * mantiq ikki marta yozilardi.
 *
 * <h2>Token MAJBURIY</h2>
 * Anonim odam uchun saqlash mumkin emas — kimga tegishli ekani
 * noma'lum. Klient bunday holatda faqat o'z xotirasiga yozadi va
 * kirgandan keyin yuboradi.
 */
@RestController
@RequestMapping("/api/v1/app/watch-progress")
@RequiredArgsConstructor
public class WatchProgressController {

    private final WatchProgressService service;

    /**
     * Kartochkani yig'ish uchun.
     *
     * ⚠️ Ayni metod bosh sahifadagi qatorlarni ham chizadi. Bu yerda
     * takrorlansa, kartochka asta-sekin qolganlaridan farq qila
     * boshlardi.
     */
    private final HomeFeedService homeFeedService;

    /**
     * Holatni saqlaydi.
     *
     * ⚠️ {@code PUT}, {@code POST} emas: bir video uchun bitta yozuv
     * bor va takroriy so'rov yangi narsa YARATMAYDI. Klient sekin
     * tarmoqda so'rovni takrorlashi odatiy hol.
     */
    @PutMapping("/{type}/{targetId}")
    public ResponseEntity<ProgressResponse> save(@PathVariable WatchTargetType type,
                                                 @PathVariable Long targetId,
                                                 @RequestBody ProgressRequest body) {

        WatchProgress saved = service.save(
                CurrentUser.get(), type, targetId,
                body == null ? null : body.getPosition(),
                body == null ? null : body.getDuration(),
                body == null ? null : body.getQuality());

        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * Bitta video uchun saqlangan holat — pleyer ochilishida.
     *
     * ⚠️ Yozuv yo'q bo'lsa 404 EMAS, {@code null} li javob. «Hali
     * ko'rilmagan» — bu xato emas, normal holat, va klientda uni
     * xatolar oqimidan ajratib olish ortiqcha ish bo'lardi.
     */
    @GetMapping("/{type}/{targetId}")
    public ResponseEntity<ProgressResponse> find(@PathVariable WatchTargetType type,
                                                 @PathVariable Long targetId) {

        return ResponseEntity.ok(service.find(CurrentUser.get(), type, targetId)
                .map(this::toResponse)
                .orElse(null));
    }

    /**
     * «Ko'rishda davom eting» lentasi — bosh sahifa uchun.
     *
     * ⚠️ Har element KARTOCHKA bilan keladi: afisha, sarlavha, janr.
     * Progress jadvalida faqat raqamlar bor, lentaga esa chizadigan
     * narsa kerak. Klient ularni alohida so'rasa, yigirma element
     * uchun yigirma so'rov ketardi — bosh sahifa ochilishida.
     *
     * Kartochka bosh sahifadagi qatorlar bilan AYNI ko'rinishda
     * ({@code HomeFeedService.contentCard}) — klient bir xil
     * komponentni ishlatadi.
     */
    @GetMapping("/continue")
    public ResponseEntity<ContinueResponse> continueWatching(
            @RequestParam(defaultValue = "UZ") Locale locale) {

        List<ContinueItem> items = service
                .continueWatchingCards(CurrentUser.get(), locale)
                .stream()
                .map(item -> ContinueItem.builder()
                        .progress(toResponse(item.progress()))
                        .episodeNumber(item.episodeNumber())
                        .content(homeFeedService.contentCard(item.content(), locale))
                        .build())
                .toList();

        return ResponseEntity.ok(ContinueResponse.builder().items(items).build());
    }

    /**
     * Ro'yxatdan olib tashlaydi.
     *
     * ⚠️ Yo'q yozuv uchun ham 200: klient so'rovni takrorlagan
     * bo'lishi mumkin va natija bir xil — element ro'yxatda yo'q.
     */
    @DeleteMapping("/{type}/{targetId}")
    public ResponseEntity<Void> forget(@PathVariable WatchTargetType type,
                                       @PathVariable Long targetId) {
        service.forget(CurrentUser.get(), type, targetId);
        return ResponseEntity.noContent().build();
    }

    private ProgressResponse toResponse(WatchProgress p) {
        return ProgressResponse.builder()
                .type(p.getType())
                .targetId(p.getTargetId())
                .position(p.getPositionSeconds())
                .duration(p.getDurationSeconds())
                .quality(p.getQuality())
                .completed(p.isCompleted())
                .percent(percent(p))
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    /**
     * Ko'rilgan ulush, 0–100.
     *
     * ⚠️ Davomiylik noma'lum bo'lsa {@code null} — nol EMAS. Nol
     * «hali boshlanmagan» degani va progress chizig'i bo'sh
     * ko'rinardi, holbuki odam yarmini ko'rgan bo'lishi mumkin.
     */
    private Integer percent(WatchProgress p) {
        Integer duration = p.getDurationSeconds();
        if (duration == null || duration <= 0) {
            return null;
        }
        return Math.min(100, (int) Math.round(p.getPositionSeconds() * 100.0 / duration));
    }

    @Data
    public static class ProgressRequest {

        /** Qayerda to'xtadi, soniyada. */
        private Integer position;

        /**
         * Pleyerdagi HAQIQIY davomiylik.
         *
         * ⚠️ Qismdagi qiymat admin qo'lda kiritgani va u noto'g'ri
         * bo'lishi mumkin. Foiz esa pleyer ko'rsatgan chiziqqa mos
         * kelishi kerak.
         */
        private Integer duration;

        /** {@code auto}, {@code 1080p}, {@code 720p} yoki {@code 480p}. */
        private String quality;
    }

    @Data
    @Builder
    public static class ProgressResponse {
        private WatchTargetType type;
        private Long targetId;
        private Integer position;
        private Integer duration;
        private String quality;
        private boolean completed;

        /** 0–100, yoki {@code null} — davomiylik noma'lum. */
        private Integer percent;

        /**
         * Oxirgi marta qachon yangilangani.
         *
         * ⚠️ Klientga KERAK. U pozitsiyani ikki joyda saqlaydi:
         * telefonda (darhol o'qish uchun) va serverda (qurilmalar
         * orasida). Ochilishda ikkalasi ham bo'lishi mumkin va ular
         * MOS KELMASLIGI mumkin — masalan oxirgi seans internetsiz
         * o'tgan bo'lsa.
         *
         * Bu maydonsiz «qaysi biri yangiroq» degan savolga javob
         * yo'q edi va klient taxmin qilishga majbur bo'lardi. Har
         * ikki taxmin ham xato: server tanlansa oflayn ko'rilgani
         * yo'qolardi, lokal tanlansa boshqa qurilmadagi keyingi
         * seans yo'qolardi.
         */
        private LocalDateTime updatedAt;
    }

    /** Lentadagi bitta element: qayerda to'xtagani + chizish uchun kartochka. */
    @Data
    @Builder
    public static class ContinueItem {

        private ProgressResponse progress;

        /**
         * Kontent kartochkasi — afisha, sarlavha, janr.
         *
         * Bosh sahifadagi qatorlar bilan AYNI shakl, shuning uchun
         * klient bir xil komponentni ishlatadi.
         */
        private HomeFeedDto.ContentCard content;

        /**
         * Qism raqami — «3-qism» deb yozish uchun.
         *
         * Yaxlit kontentda (film, klip) {@code null}: u yerda qism
         * degan tushuncha yo'q va «1-qism» deb yozish yolg'on
         * bo'lardi.
         */
        private Integer episodeNumber;
    }

    @Data
    @Builder
    public static class ContinueResponse {

        /**
         * ⚠️ Hech qachon {@code null} emas. Bo'sh ro'yxat — bu
         * «hech narsa ko'rilmagan», va klientda
         * {@code null.length} xatosini keltirib chiqarmasligi kerak.
         */
        private List<ContinueItem> items;
    }
}
