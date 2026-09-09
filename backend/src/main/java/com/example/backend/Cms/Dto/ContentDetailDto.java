package com.example.backend.Cms.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Kontent kartochkasi — ilovaning kontent sahifasi uchun (ТЗ §14, §46).
 *
 * <h2>Nima uchun bu DTO paydo bo'ldi</h2>
 * Ilovada kontent sahifasi ochilganda ma'lumot IKKI joydan yig'ilardi:
 * {@code /watch/**} (sarlavha, davomiylik, huquq) va bosh sahifa keshi
 * (afisha, qisqa tavsif). Boshqa hech narsa yo'q edi — na yil, na janr,
 * na to'liq tavsif, na aktyorlar. Bazada esa ularning HAMMASI bor
 * ({@code Content.genres}, {@code credits}, {@code translations},
 * {@code media}).
 *
 * Kesh orqali ishlash yana bir narsani buzardi: to'g'ridan-to'g'ri havola
 * bilan kirilganda («deep link») bosh sahifa keshi bo'sh bo'ladi va sahifa
 * afishasiz ochilardi.
 *
 * <h2>⚠️ Bu yerda video havolasi YO'Q</h2>
 * Kartochka — KATALOG ma'lumoti, uni mehmon ham ko'radi. Fayl manzili
 * faqat {@code /watch/**} dan, huquq tasdiqlangandan keyin chiqadi.
 * Aks holda kartochkaning o'zi pullik filmni berib yuborardi.
 */
@Data
@Builder
public class ContentDetailDto {

    private Long id;
    private String slug;

    private String title;
    private String shortDescription;
    /** To'liq tavsif — sahifadagi «ko'proq» matni. Qisqasi kartochkalarda. */
    private String description;

    /** MOVIE, SERIES, PODCAST va h.k. */
    private String contentType;
    /** SINGLE, EPISODIC, SEASONAL — ilova qaysi ekranni ochishini shu aytadi. */
    private String structureType;
    /** LANDSCAPE yoki VERTICAL. */
    private String orientation;
    private String accessPolicy;

    private String ageRating;

    /**
     * Chiqarilgan yili — premyera sanasi, bo'lmasa nashr sanasi.
     *
     * ⚠️ Alohida «year» ustuni YO'Q, shuning uchun sana bo'lmasa
     * {@code null} qaytadi. Joriy yilni qo'yish har bir eski filmni
     * yangidek ko'rsatardi.
     */
    private Integer year;

    /**
     * Asar TILI ({@code Content.language}) — «uz», «ru», «ko» kabi kod.
     *
     * ⚠️ Bu MAMLAKAT emas. Bazada mamlakat ustuni yo'q, shuning uchun
     * uni tilga qarab o'ylab topib bo'lmaydi: ruscha film Rossiyaniki
     * degani emas. Ilova buni til nomi sifatida ko'rsatadi.
     */
    private String language;

    /** Yaxlit kontentning davomiyligi. Ko'p qismlida {@code null}. */
    private Integer durationSeconds;
    /** Nashr qilingan qismlar soni. Yaxlit kontentda {@code null}. */
    private Integer episodeCount;
    /** Mavsumlar soni. Faqat SEASONAL da to'ladi. */
    private Integer seasonCount;

    /** Vertikal afisha (2:3) — ro'yxatlardagi kartochka bilan bir xil. */
    private Long posterMediaId;
    /**
     * Keng fon rasmi (COVER) — sahifa tepasidagi kadr.
     *
     * {@code null} bo'lsa ilova afishani ishlatadi: maketda tepada keng
     * kadr turadi, lekin har bir kontentga u yuklanmagan bo'lishi mumkin.
     */
    private Long coverMediaId;
    /** Treyler roligi ({@code MediaRole.TRAILER}), bo'lmasa TEASER. */
    private Long trailerMediaId;

    /**
     * Galereya kadrlari — maketdagi «Qiziq sahnalar».
     *
     * ⚠️ Bular RASM, video parcha emas: vaqt belgisi bilan sahna
     * kesimlari bazada saqlanmaydi. Maketdagi taymkodlar shuning uchun
     * ko'rsatilmaydi — o'ylab topilgan raqam sahnaning qayeridan
     * ekanini yolg'on aytardi.
     */
    private List<Long> galleryMediaIds;

    /** Barcha janrlar so'ralgan tilda. Kartochkada faqat birinchisi bo'ladi. */
    private List<String> genres;

    private Long viewCount;
    private Long likeCount;
    /** Ko'rinadigan (moderatsiyadan o'tgan) izohlar soni. */
    private Long commentCount;
    /** Shu kontentga yuborilgan yulduzlar yig'indisi. */
    private Long starsReceived;

    /** Shu odam «yoqdi» bosganmi. Mehmonda doim {@code false}. */
    private boolean liked;

    /** Aktyorlar va boshqa ijodkorlar — tartib admin belgilagani bo'yicha. */
    private List<CastMember> cast;

    @Data
    @Builder
    public static class CastMember {
        private Long creatorId;
        private String slug;
        private String name;
        private Long photoMediaId;
        /** ACTOR, DIRECTOR, PRODUCER va h.k. */
        private String profession;
        /** Filmda o'ynagan qahramon ismi — maketda ism ostida turadi. */
        private String characterName;
    }

    /**
     * Donat reytingi qatori (ТЗ §42, maketdagi «Top 10 Donatchilar»).
     *
     * <h2>Nima uchun ism ochiq</h2>
     * Reyting — ommaviy ro'yxat, uning butun mazmuni «kim qo'llab-quvvatladi»
     * degan savolga javob berish. Shu sababli faqat ism va rasm chiqadi:
     * telefon, pochta va boshqa shaxsiy ma'lumot bu yerga TUSHMAYDI.
     */
    @Data
    @Builder
    public static class Donor {
        /** O'rin — 1 dan boshlanadi. Ilova uni qayta hisoblamaydi. */
        private int rank;
        private String name;
        /**
         * Rasm HAVOLASI ({@code User.avatarUrl}), media id emas.
         *
         * ⚠️ Foydalanuvchi rasmi Google'dan keladi va tashqi manzilda
         * yotadi — u {@code /api/v1/app/media/{id}/raw} orqali
         * berilmaydi. Ilova bu maydonni tayyor URL sifatida oladi.
         */
        private String avatarUrl;
        /** Yuborilgan yulduzlar yig'indisi. */
        private Long stars;
    }
}
