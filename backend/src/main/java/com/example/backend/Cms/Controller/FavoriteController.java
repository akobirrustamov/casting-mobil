package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Enums.FavoriteType;
import com.example.backend.Cms.Service.FavoriteService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sevimlilar ro'yxati — qurilmalar orasida saqlanadi.
 *
 * <h2>⚠️ Qaysi nosozlik tuzatilyapti</h2>
 * Ro'yxat FAQAT telefonda saqlanardi. Ilovani qayta o'rnatsa
 * yo'qolardi, ikkinchi qurilmada bo'sh bo'lardi, telefon almashsa
 * hammasi ketardi.
 *
 * <h2>Har javob TO'LIQ ro'yxat</h2>
 * Qo'shish ham, o'chirish ham yangilangan ro'yxatni qaytaradi.
 * Klient bitta so'rov bilan holatini server bilan tenglaydi va o'z
 * nusxasini o'zi hisoblab yurmaydi — ikki tomon ajralib ketishi
 * uchun bitta yo'qolgan javob yetarli bo'lardi.
 *
 * <h2>Anonim foydalanuvchi</h2>
 * Bu endpoint token talab qiladi. Kirmagan odam yurakcha bosa oladi,
 * lekin ro'yxat telefonda qoladi — kirgach klient uni
 * {@code POST} bilan yuboradi va ro'yxatlar BIRLASHADI.
 */
@RestController
@RequestMapping("/api/v1/app/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * @param type nima saqlanayotgani. Sukut — {@code CREATOR}: ilova
     *             bugun faqat shuni saqlaydi va eski klient parametrni
     *             umuman yubormaydi
     */
    @GetMapping
    public ResponseEntity<FavoritesResponse> list(
            @RequestParam(defaultValue = "CREATOR") FavoriteType type) {

        return ResponseEntity.ok(response(type,
                favoriteService.list(CurrentUser.get(), type)));
    }

    /**
     * Ro'yxatga qo'shadi.
     *
     * ⚠️ Ro'yxat qabul qiladi, bitta element emas. Sabab —
     * kirishdan keyingi birlashtirish: telefonda yig'ilgan o'nlab
     * yurakcha bitta so'rovda yuboriladi. Bittalab yuborilsa, sekin
     * tarmoqda ularning bir qismi yo'lda qolardi.
     */
    @PostMapping
    public ResponseEntity<FavoritesResponse> add(@RequestBody FavoritesRequest body) {
        FavoriteType type = body == null || body.getType() == null
                ? FavoriteType.CREATOR : body.getType();

        return ResponseEntity.ok(response(type, favoriteService.add(
                CurrentUser.get(), type, body == null ? List.of() : body.getTargetIds())));
    }

    /**
     * Ro'yxatdan olib tashlaydi.
     *
     * ⚠️ Mavjud bo'lmagan element uchun ham 200. Klient so'rovni
     * takrorlagan bo'lishi mumkin va natija bir xil: element
     * ro'yxatda yo'q.
     */
    @DeleteMapping
    public ResponseEntity<FavoritesResponse> remove(
            @RequestParam(defaultValue = "CREATOR") FavoriteType type,
            @RequestParam Long targetId) {

        return ResponseEntity.ok(response(type,
                favoriteService.remove(CurrentUser.get(), type, targetId)));
    }

    private FavoritesResponse response(FavoriteType type, List<Long> targetIds) {
        return FavoritesResponse.builder()
                .type(type)
                // ⚠️ Hech qachon `null` emas. Bo'sh ro'yxat — bu
                // «hech narsa saqlanmagan», va klientda u oddiy bo'sh
                // massiv bo'lishi kerak, `null.length` xatosi emas.
                .targetIds(targetIds == null ? List.of() : targetIds)
                .build();
    }

    // ------------------------------------------------------------------- DTO

    @Data
    public static class FavoritesRequest {
        private FavoriteType type;
        private List<Long> targetIds;
    }

    @Data
    @Builder
    public static class FavoritesResponse {
        private FavoriteType type;
        private List<Long> targetIds;
    }
}
