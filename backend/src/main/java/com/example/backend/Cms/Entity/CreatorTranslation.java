package com.example.backend.Cms.Entity;

import com.example.backend.Cms.Enums.Locale;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cms_creator_translation", uniqueConstraints =
        @UniqueConstraint(name = "uk_creator_locale", columnNames = {"creator_id", "locale"}))
public class CreatorTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private Creator creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Locale locale;

    @Column(name = "first_name", length = 128)
    private String firstName;

    @Column(name = "last_name", length = 128)
    private String lastName;

    @Column(name = "middle_name", length = 128)
    private String middleName;

    /** Ro'yxatlarda ko'rsatiladigan nom. Bo'sh bo'lsa firstName + lastName. */
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(length = 4000)
    private String bio;
}
