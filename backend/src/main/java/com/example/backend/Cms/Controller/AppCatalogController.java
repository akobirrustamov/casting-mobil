package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Dto.CatalogCategoryDto;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Service.AppCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Katalog kategoriyalari — mobil ilova uchun.
 *
 * <h2>Nima uchun ochiq</h2>
 * Bosh sahifa bilan bir xil sabab: mehmon ilovada nima borligini ko'ra
 * olishi kerak. Tomosha qilish huquqi baribir alohida tekshiriladi
 * ({@code /api/v1/app/watch}), bu yerda esa video havolasi umuman yo'q.
 *
 * Token yuborilsa hisobga olinadi — undan foydalanuvchi tanlagan til olinadi.
 *
 * <h2>Ikkita endpoint, chunki qatorlar mustaqil yuklanadi</h2>
 * Ro'yxat yengil (nomlar va sonlar), qatorlar esa og'ir. Klient avval
 * ro'yxatni oladi, keyin BO'SH bo'lmaganlarini bittalab so'raydi: birinchi
 * qator darhol chiziladi, bittasi xato bersa qolganlari ishlayveradi.
 */
@RestController
@RequestMapping("/api/v1/app/catalog")
@RequiredArgsConstructor
public class AppCatalogController {

    private final AppCatalogService catalogService;

    /** Faol kategoriyalar, admin bergan tartibda. Kartochkalarsiz. */
    @GetMapping("/categories")
    public ResponseEntity<List<CatalogCategoryDto>> categories(
            @RequestParam(defaultValue = "UZ") Locale locale) {
        return ResponseEntity.ok(catalogService.categories(CurrentUser.getOrNull(), locale));
    }

    /**
     * Bitta kategoriyaning bitta sahifasi.
     *
     * {@code size} — sahifada nechta kartochka. Standarti 20, yuqori
     * chegarasi 100: usiz bitta so'rov butun katalogni tortardi. Bosh
     * sahifadagi qator 10 tani so'raydi, «Barchasi» ekrani 20 tadan.
     *
     * {@code page} — 0 dan. Javobdagi {@code hasMore} keyingi sahifa
     * borligini aytadi, {@code total} esa butun kategoriyadagi sonni.
     */
    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<CatalogCategoryDto> category(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "UZ") Locale locale,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(catalogService.category(
                CurrentUser.getOrNull(), locale, categoryId, page, size));
    }
}
