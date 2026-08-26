package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.DonationTransaction;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Donat tranzaksiyasi — hisobotdagi ro'yxat uchun (ТЗ §42).
 *
 * <h2>O'zgarmas tarix</h2>
 * Bu yozuvlar hech qachon tahrirlanmaydi va o'chirilmaydi — moliyaviy
 * tarix. Shuning uchun DTO'da ham «yangilash» tushunchasi yo'q.
 */
@Data
@Builder
public class DonationTransactionDto {

    private Long id;
    private UUID senderId;
    private String senderName;
    private DonationTargetType targetType;
    private Long targetId;

    /**
     * Nishon nomi — ijodkor ismi yoki kontent sarlavhasi.
     *
     * ⚠️ Ilgari panelda faqat «CREATOR #5» ko'rinardi va admin donat
     * KIMGA berilganini bila olmasdi. Yuboruvchining ismi qaytarilardi,
     * oluvchiniki esa yo'q — asimmetriya buni e'tibordan chetda
     * qolgan deb ko'rsatadi.
     *
     * O'chirilgan yoki topilmagan nishonda {@code null} — panel
     * o'shanda «#5» ko'rsatadi. Bu halol: nom yo'q, taxmin qilinmaydi.
     */
    private String targetName;
    private CurrencyKind kind;
    private Long amount;
    private LocalDateTime createdAt;

    public static DonationTransactionDto from(DonationTransaction d) {
        return DonationTransactionDto.builder()
                .id(d.getId())
                .senderId(d.getSender() == null ? null : d.getSender().getId())
                .senderName(d.getSender() == null ? null : d.getSender().getName())
                .targetType(d.getTargetType())
                .targetId(d.getTargetId())
                .kind(d.getKind())
                .amount(d.getAmount())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
