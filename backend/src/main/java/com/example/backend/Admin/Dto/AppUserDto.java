package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Entity.UserBalance;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Entity.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mobil foydalanuvchi — admin panel uchun.
 *
 * Parol va token bu yerga TUSHMAYDI.
 */
@Data
@Builder
public class AppUserDto {

    private UUID id;
    private String name;
    private String phone;
    private String email;
    private String avatarUrl;

    private UserStatus status;
    private String blockedReason;

    private LocalDateTime premiumUntil;
    private Boolean premiumActive;

    private BigDecimal moneyBalance;
    private Long starsBalance;
    private Long coinBalance;

    /**
     * Ro'yxatdan o'tgan sana (ТЗ §35).
     *
     * ⚠️ {@code UserAccount.createdAt} EMAS: u dangasa yaratiladi va
     * «admin birinchi marta tekkan vaqt» ni bildiradi. V17 dan oldin
     * ro'yxatdan o'tganlarda {@code null} — sana bilinmaydi va o'ylab
     * topilmaydi.
     */
    private LocalDateTime createdAt;

    private LocalDateTime lastActiveAt;
    private Integer activeDevices;

    public static AppUserDto from(User u, UserAccount account, UserBalance balance, int devices) {
        return AppUserDto.builder()
                .id(u.getId())
                .name(u.getName())
                .phone(u.getPhone())
                .email(u.getEmail())
                .avatarUrl(u.getAvatarUrl())
                .status(account == null ? UserStatus.ACTIVE : account.getStatus())
                .blockedReason(account == null ? null : account.getBlockedReason())
                .premiumUntil(account == null ? null : account.getPremiumUntil())
                .premiumActive(account != null && account.hasActivePremium())
                .moneyBalance(balance == null ? BigDecimal.ZERO : balance.getMoneyBalance())
                .starsBalance(balance == null ? 0L : balance.getStarsBalance())
                .coinBalance(balance == null ? 0L : balance.getCoinBalance())
                .createdAt(u.getCreatedAt())
                .lastActiveAt(account == null ? null : account.getLastActiveAt())
                .activeDevices(devices)
                .build();
    }
}
