package com.example.backend.DTO;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsDTO {
    private String titleUz;
    private String descriptionUz;
    private String titleRu;
    private String descriptionRu;
    private String link;
    private UUID mainPhoto;
    private List<UUID> photos = new ArrayList<>();


}
