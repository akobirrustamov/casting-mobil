package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.HomepageSectionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Mobil ilova bosh sahifasining bitta bo'limi.
 *
 * Buyurtmachi talabi: «Bu yerdagi barcha bo'limlarni admin panel orqali
 * o'chirib/yoqish funksiyasi bo'lishi kerak». Shuning uchun bosh sahifa
 * klientda qotirilmaydi — u shu jadvaldan quriladi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_homepage_section",
        uniqueConstraints = @UniqueConstraint(name = "uk_homepage_type", columnNames = "type"),
        indexes = @Index(name = "idx_homepage_order", columnList = "enabled,sort_order"))
public class HomepageSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private HomepageSectionType type;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** Nechta element ko'rsatilsin. null — klient o'zi hal qiladi. */
    @Column(name = "item_limit")
    private Integer itemLimit;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HomepageSectionTranslation> translations = new ArrayList<>();

    public void addTranslation(HomepageSectionTranslation t) {
        t.setSection(this);
        this.translations.add(t);
    }
}
