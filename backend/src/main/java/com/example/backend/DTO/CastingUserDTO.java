package com.example.backend.DTO;

import com.example.backend.Entity.Attachment;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CastingUserDTO {
    private String telegramId;
    private String castingType;
    private String gender;
    private String name;
    private String region;
    private String nationality;
    private LocalDateTime birthday;
    private Integer age;
    private Integer height;
    private String hairColor;
    private String eyeColor;
    private String clothSize;
    private String shoeSize;
    private String bust; //kokrak
    private String waist; //bel
    private String son;
    private String email;
    private String phone;
    private String telegram;
    private String facebook;
    private String instagram;
    private Double price;
    private LocalDateTime createdAt;
    private Integer status;
    private List<UUID> photos = new ArrayList<>();

}
