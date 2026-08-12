package com.example.backend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "news")
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Specifies auto-increment behavior
    private Integer id;
    private String titleUz;
    private String titleRu;
    @Column(length = 10000)
    private String descriptionUz;
    @Column(length = 10000)
    private String descriptionRu;
    @Column(length = 10000)
    private String link;
    @OneToOne
    private Attachment mainPhoto;
    @OneToMany
    private List<Attachment> photos = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    public News(String titleUz, String titleRu, String descriptionUz, String descriptionRu, String link, Attachment mainPhoto, List<Attachment> photos) {
        this.titleUz = titleUz;
        this.titleRu = titleRu;
        this.descriptionUz = descriptionUz;
        this.descriptionRu = descriptionRu;
        this.link = link;
        this.mainPhoto = mainPhoto;
        this.photos = photos;
    }
}
