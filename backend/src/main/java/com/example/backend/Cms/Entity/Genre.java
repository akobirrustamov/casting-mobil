package com.example.backend.Cms.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Janr. Bitta kontent bir nechta janrga ega bo'lishi mumkin.
 * Kategoriyadan farqi: kategoriya - menyu bo'limi, janr - teg.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_genre", indexes = {
        @Index(name = "idx_genre_slug", columnList = "slug", unique = true)
})
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String slug;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "genre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GenreTranslation> translations = new ArrayList<>();

    public void addTranslation(GenreTranslation translation) {
        translation.setGenre(this);
        this.translations.add(translation);
    }
}
