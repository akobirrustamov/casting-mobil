package com.example.backend.Cms.Controller;

import com.example.backend.Cms.Service.SettingKeys;
import com.example.backend.Cms.Service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mobil ilova sozlamalari — ochiq.
 *
 * {@code paymentsVisible}: to'lovga oid bo'limlar (Premium, tariflar, obuna,
 * promokod, balans, donat, sotib olish) ko'rinadimi. SUPER_ADMIN admin
 * panelda almashtiradi, default — yashirin.
 */
@RestController
@RequestMapping("/api/v1/app/config")
@RequiredArgsConstructor
public class AppConfigController {

    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<AppConfig> config() {
        boolean paymentsVisible = "true".equalsIgnoreCase(
                settingsService.get(SettingKeys.MOBILE_PAYMENTS_VISIBLE));
        return ResponseEntity.ok(new AppConfig(paymentsVisible));
    }

    public record AppConfig(boolean paymentsVisible) {
    }
}
