package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Enums.StaffStatus;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Admin panelga kirgan xodim haqidagi ma'lumot.
 *
 * Parol, token va boshqa maxfiy maydonlar BU YERGA TUSHMAYDI.
 */
@Data
@Builder
public class AdminUserDto {

    private UUID id;
    private String name;
    private String phone;
    private String email;
    private String avatarUrl;
    private PlatformRole role;

    /** WORKER uchun ma'noli. Yuqori rollarda bo'sh - ularda rol darajasida to'liq huquq. */
    private Set<Permission> permissions;

    /** Frontend menyuni shu asosda yasaydi. Xavfsizlik EMAS - backend baribir tekshiradi. */
    private Set<PlatformRole> creatableRoles;

    /**
     * So'rov yuborgan xodim shu hisobni boshqara oladimi.
     *
     * Ko'rish va boshqarish AJRATILGAN: HYPER_ADMIN barcha hisoblarni
     * ko'radi (audit uchun), lekin o'ziga teng rolni boshqara olmaydi.
     * Panel shu bayroqqa qarab tugmalarni faolsizlantiradi.
     *
     * ⚠️ Bu FAQAT interfeys uchun. Haqiqiy tekshiruv backendda, har bir
     * amalda alohida bajariladi.
     */
    private Boolean manageable;

    // ─── Xodim profili (V10) ─────────────────────────────────────────────
    //
    // Bu maydonlar eski `users` jadvalida YO'Q — u muzlatilgan. Ular
    // `cms_staff_profile` dan keladi. Profilsiz eski hisoblar uchun
    // status ACTIVE, qolganlari null bo'ladi.

    /** ACTIVE · INACTIVE (faolsizlantirilgan) · BLOCKED */
    private StaffStatus status;

    /** Kim yaratgan. null = tizim (AutoRun yoki seeder). */
    private UUID createdBy;

    /** Yaratuvchining ismi — ro'yxatda id ko'rsatish foydasiz. */
    private String createdByName;

    private LocalDateTime createdAt;

    /** null = hali hech qachon kirmagan. */
    private LocalDateTime lastLoginAt;

    /** Faolsizlantirish yoki bloklash sababi. */
    private String statusReason;
}
