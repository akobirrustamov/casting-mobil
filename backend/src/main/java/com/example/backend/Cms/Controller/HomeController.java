package com.example.backend.Cms.Controller;

import com.example.backend.Cms.Dto.HomeFeedDto;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Service.HomeFeedService;
import com.example.backend.Admin.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mobil ilova bosh sahifasi (ТЗ §31).
 *
 * <h2>Nima uchun ochiq</h2>
 * Bosh sahifa ro'yxatdan o'tmagan mehmonga ham ko'rinadi — aks holda odam
 * ilovani ochib, nima borligini ko'rmasdan ro'yxatdan o'tishi kerak
 * bo'lardi. Kontentni TOMOSHA qilish esa alohida tekshiriladi
 * ({@code /api/v1/app/watch}).
 *
 * Token yuborilsa u hisobga olinadi: faol obunasi borlarga reklama
 * ko'rsatilmaydi.
 */
@RestController
@RequestMapping("/api/v1/app/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeFeedService homeFeedService;

    @GetMapping
    public ResponseEntity<HomeFeedDto> home(@RequestParam(defaultValue = "UZ") Locale locale) {
        return ResponseEntity.ok(homeFeedService.build(CurrentUser.getOrNull(), locale));
    }
}
