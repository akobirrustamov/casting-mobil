package com.example.backend.Cms.Dto;

import com.example.backend.Cms.Enums.HomepageSectionType;
import com.example.backend.Cms.Enums.Locale;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Mobil ilova bosh sahifasi (ТЗ §31).
 *
 * <h2>Nima uchun butun sahifa bitta javobda</h2>
 * Bosh sahifa klientda QOTIRILMAYDI: qaysi bo'limlar bor, ular qanday
 * tartibda va nima deb ataladi — hammasi backendda. Aks holda bo'lim
 * qo'shish yoki tartibini o'zgartirish uchun ilovaning yangi versiyasini
 * do'konga chiqarish kerak bo'lardi.
 *
 * <h2>Nima uchun bo'sh bo'lim ham qaytadi emas</h2>
 * Elementi yo'q bo'lim ro'yxatga umuman kirmaydi — klient bo'sh sarlavha
 * chizib qo'ymasin. Bu «ma'lumot yo'q bo'lsa bo'sh holat ko'rsat» qoidasi:
 * soxta element o'ylab topilmaydi.
 */
@Data
@Builder
public class HomeFeedDto {

    private Locale locale;

    /** Reklama ko'rsatiladimi — faol obunasi bo'lganlarga ko'rsatilmaydi. */
    private Boolean showAds;

    private List<Section> sections;

    @Data
    @Builder
    public static class Section {
        private Long id;
        private HomepageSectionType type;
        private String title;
        private Integer sortOrder;

        /** Bo'lim turiga qarab faqat bittasi to'ladi, qolganlari bo'sh. */
        @Builder.Default
        private List<ContentCard> content = List.of();
        @Builder.Default
        private List<BannerCard> banners = List.of();
        @Builder.Default
        private List<CategoryCard> categories = List.of();
        @Builder.Default
        private List<CreatorCard> creators = List.of();
    }

    @Data
    @Builder
    public static class ContentCard {
        private Long id;
        private String slug;
        private String title;
        private String shortDescription;
        private String contentType;
        private String orientation;
        private String accessPolicy;
        private String ageRating;
        private Long posterMediaId;
    }

    /** Reklama va premyera — bir xil ko'rinishdagi kartochka. */
    @Data
    @Builder
    public static class BannerCard {
        private Long id;
        private String title;
        private String subtitle;
        private String description;
        private String buttonText;
        private Boolean buttonEnabled;
        private Long imageMediaId;
        private Long videoMediaId;
        private String linkType;
        private String linkUrl;
        private String internalTargetType;
        private Long internalTargetId;
    }

    @Data
    @Builder
    public static class CategoryCard {
        private Long id;
        private String slug;
        private String name;
        private Long iconMediaId;
    }

    @Data
    @Builder
    public static class CreatorCard {
        private Long id;
        private String slug;
        private String displayName;
        private Long photoMediaId;
    }
}
