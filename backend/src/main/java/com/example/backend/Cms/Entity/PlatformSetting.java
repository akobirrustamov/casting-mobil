package com.example.backend.Cms.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Platforma sozlamasi — kalit/qiymat.
 *
 * Nega jadval, `application.properties` emas: bu qiymatlarni ADMIN o'zgartiradi
 * va o'zgarish darhol kuchga kirishi kerak, deploy kutmasdan. ТЗ aynan shuni
 * talab qiladi: qism narxi, yulduz kursi, tanga kursi, qurilma limiti.
 *
 * Ma'lum kalitlar {@link com.example.backend.Cms.Service.SettingKeys} da.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_platform_setting")
public class PlatformSetting {

    @Id
    @Column(name = "setting_key", length = 128)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 1000)
    private String value;

    /** Admin panelda nima ekanini tushuntirish uchun. */
    @Column(length = 500)
    private String description;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
