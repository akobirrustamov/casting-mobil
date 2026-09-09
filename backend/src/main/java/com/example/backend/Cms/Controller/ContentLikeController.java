package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Service.ContentLikeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * «Yoqdi» tugmasi (ТЗ §46 — kontent ko'rsatkichlari).
 *
 * <h2>Nima uchun PUT/DELETE, «toggle» emas</h2>
 * Sabab {@link ContentLikeService} da yozilgan: takrorlangan so'rov
 * natijani teskarisiga aylantirmasligi kerak.
 *
 * <h2>Kirish TALAB QILINADI</h2>
 * Bu yo'l {@code /api/**} qoidasiga tushadi, ya'ni tokensiz 401.
 * Ataylab: «yoqdi» kim bosgani bilan bog'liq, aks holda bitta odam uni
 * cheksiz marta qo'ya olardi. Ilova mehmonga sanoqni KO'RSATADI (u
 * {@code /watch} javobida keladi), lekin bosishda kirish ekraniga
 * yuboradi.
 */
@RestController
@RequestMapping("/api/v1/app/content/{contentId}/like")
@RequiredArgsConstructor
public class ContentLikeController {

    private final ContentLikeService likeService;

    @PutMapping
    public ResponseEntity<LikeResponse> like(@PathVariable Long contentId) {
        return ResponseEntity.ok(LikeResponse.of(
                likeService.like(CurrentUser.get(), contentId)));
    }

    @DeleteMapping
    public ResponseEntity<LikeResponse> unlike(@PathVariable Long contentId) {
        return ResponseEntity.ok(LikeResponse.of(
                likeService.unlike(CurrentUser.get(), contentId)));
    }

    /**
     * Javobda sanoq ham qaytadi.
     *
     * ⚠️ Klient sanoqni o'zi oshirib qo'ya olardi, lekin unda ikkita
     * telefonda ikki xil son turardi: birinchisi bosganda ikkinchisi
     * bilmaydi. Server qaytargan qiymat esa ikkalasida ham to'g'ri.
     */
    @Data
    public static class LikeResponse {
        private boolean liked;
        private long likeCount;

        static LikeResponse of(ContentLikeService.LikeState state) {
            LikeResponse dto = new LikeResponse();
            dto.liked = state.liked();
            dto.likeCount = state.likeCount();
            return dto;
        }
    }
}
