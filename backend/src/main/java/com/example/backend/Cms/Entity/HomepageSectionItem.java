package com.example.backend.Cms.Entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Qo'lda yig'ilgan bosh sahifa qatorining bitta elementi (ТЗ §31).
 *
 * <h2>Nima uchun bayroq yetarli emas</h2>
 * {@code content.featured} va {@code content.popular} — bitta bayroq, ya'ni
 * bitta qator. Maxsus qatorlar esa bir nechta bo'ladi («Ramazon tanlovi»,
 * «Yangi yil kinolari») va bitta film bir nechtasida turishi mumkin.
 *
 * Tartib elementning o'zida saqlanadi: bir xil film bir qatorda birinchi,
 * boshqasida oxirgi bo'lishi mumkin.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_homepage_section_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_homepage_item", columnNames = {"section_id", "content_id"}),
        indexes = @Index(name = "idx_homepage_item_order",
                columnList = "section_id,sort_order"))
public class HomepageSectionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private HomepageSection section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
