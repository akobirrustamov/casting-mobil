package com.example.backend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "casting_user")
public class CastingUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Specifies auto-increment behavior
    private Integer id;
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
    private Integer firstChan;
    private Integer secondChan;
    private Boolean isWebShow;

    @OneToMany
    private List<Attachment> photos = new ArrayList<>();

    public CastingUser(String telegramId, String castingType, String gender, String name, String region, String nationality, LocalDateTime birthday, Integer age, Integer height, String hairColor, String eyeColor, String clothSize, String shoeSize, String bust, String waist, String son, String email, String phone, String telegram, String facebook, String instagram, Double price, Integer status, List<Attachment> photos, LocalDateTime createdAt) {
        this.telegramId = telegramId;
        this.castingType = castingType;
        this.gender = gender;
        this.name = name;
        this.region = region;
        this.nationality = nationality;
        this.birthday = birthday;
        this.age = age;
        this.height = height;
        this.hairColor = hairColor;
        this.eyeColor = eyeColor;
        this.clothSize = clothSize;
        this.shoeSize = shoeSize;
        this.bust = bust;
        this.waist = waist;
        this.son = son;
        this.email = email;
        this.phone = phone;
        this.telegram = telegram;
        this.facebook = facebook;
        this.instagram = instagram;
        this.price = price;
        this.status = status;
        this.photos = photos;
        this.createdAt = createdAt;
    }
}
