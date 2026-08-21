package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.CreatorProfession;
import jakarta.persistence.*;
import lombok.*;

/**
 * Ijodkorning aynan shu kontentdagi roli.
 *
 * Kasb Creator'da emas, shu yerda: bitta odam bir kinoda aktyor,
 * boshqasida rejissyor bo'lishi mumkin.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_content_credit", indexes = {
        @Index(name = "idx_credit_content", columnList = "content_id,sort_order"),
        @Index(name = "idx_credit_creator", columnList = "creator_id")
})
public class ContentCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private Creator creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CreatorProfession profession;

    /** Qahramon ismi - aktyorlar uchun. Ixtiyoriy. */
    @Column(name = "character_name", length = 255)
    private String characterName;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
