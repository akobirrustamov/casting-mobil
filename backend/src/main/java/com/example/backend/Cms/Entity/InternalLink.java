package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.InternalTargetType;
import com.example.backend.Cms.Enums.LinkType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Havola — reklama, premyera va bildirishnoma uchun UMUMIY (§28).
 *
 * {@code @Embeddable}: alohida jadval kerak emas, ustunlar egasining
 * jadvaliga qo'shiladi. Shu sababli havola mantiqini uch joyda takrorlash
 * o'rniga bir marta yozamiz.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class InternalLink {

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", length = 16)
    @Builder.Default
    private LinkType linkType = LinkType.NONE;

    /** EXTERNAL uchun. */
    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "internal_target_type", length = 32)
    private InternalTargetType internalTargetType;

    @Column(name = "internal_target_id")
    private Long internalTargetId;

    /** Havola to'liq to'ldirilganmi — klient bosiladigan qilib ko'rsatadimi. */
    public boolean isActionable() {
        if (linkType == null || linkType == LinkType.NONE) {
            return false;
        }
        if (linkType == LinkType.EXTERNAL) {
            return linkUrl != null && !linkUrl.isBlank();
        }
        return internalTargetType != null && internalTargetId != null;
    }
}
