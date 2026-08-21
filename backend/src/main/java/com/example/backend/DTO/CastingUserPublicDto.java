package com.example.backend.DTO;

import com.example.backend.Entity.Attachment;
import com.example.backend.Entity.CastingUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Ochiq katalog uchun anketa — FAQAT vitrina maydonlari.
 *
 * <h2>Nega kerak</h2>
 * Ilgari {@code GET /api/v1/casting-user/web} butun {@code CastingUser} entity'sini
 * JSON qilib qaytarardi: telefon, email, telegram, facebook, instagram, aniq
 * tug'ilgan sana va tana o'lchovlari (ko'krak/bel/son) — tokensiz, hammaga.
 * Jumladan voyaga yetmaganlarniki ham (bazada {@code age: 17} anketalar bor).
 * Bu texnik qarz emas, yuridik risk edi.
 *
 * <h2>Nima olib tashlandi</h2>
 * {@code phone}, {@code email}, {@code telegram}, {@code facebook},
 * {@code instagram}, {@code telegramId}, {@code bust}, {@code waist},
 * {@code son}, {@code clothSize}, {@code shoeSize}, {@code price},
 * {@code status}, {@code firstChan}, {@code secondChan}.
 *
 * <h2>Klientlar sinmaydi</h2>
 * Sayt katalogi ({@code frontend/src/pages/models/Models.js}) faqat
 * {@code id, name, castingType, age, birthday, photos} ishlatadi.
 * Mobil ilova ({@code mobile/src/features/creators/types.ts}) — yuqoridagilar
 * va {@code gender, region, nationality, height, hairColor, eyeColor}.
 * Ikkalasiga ham kerak bo'lgan maydonlar shu DTO'da bor.
 *
 * <h2>Tug'ilgan sana haqida</h2>
 * {@code birthday} qoldirilgan: sayt undan yoshni hisoblaydi. Bu ham shaxsiy
 * ma'lumot, lekin uni olib tashlash klientni sindiradi. To'g'ri yechim —
 * klientlarni {@code age} ga o'tkazish, so'ng {@code birthday} ni olib tashlash.
 * Alohida task sifatida qayd etilgan (roadmap.md → B2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CastingUserPublicDto {

    private Integer id;
    private String name;
    private String castingType;
    private String gender;
    private String region;
    private String nationality;
    private Integer age;
    private LocalDateTime birthday;
    private Integer height;
    private String hairColor;
    private String eyeColor;
    private Boolean isWebShow;
    private List<PublicPhoto> photos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicPhoto {
        private UUID id;
        private Boolean isWebShow;
    }

    public static CastingUserPublicDto from(CastingUser u) {
        List<PublicPhoto> photos = (u.getPhotos() == null ? List.<Attachment>of() : u.getPhotos())
                .stream()
                .map(p -> PublicPhoto.builder()
                        .id(p.getId())
                        .isWebShow(p.getIsWebShow())
                        .build())
                .toList();

        return CastingUserPublicDto.builder()
                .id(u.getId())
                .name(u.getName())
                .castingType(u.getCastingType())
                .gender(u.getGender())
                .region(u.getRegion())
                .nationality(u.getNationality())
                .age(u.getAge())
                .birthday(u.getBirthday())
                .height(u.getHeight())
                .hairColor(u.getHairColor())
                .eyeColor(u.getEyeColor())
                .isWebShow(u.getIsWebShow())
                .photos(photos)
                .build();
    }
}
