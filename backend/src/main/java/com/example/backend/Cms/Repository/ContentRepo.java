package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Enums.ContentOrientation;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.ContentVisibility;
import com.example.backend.Cms.Enums.PublicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContentRepo extends JpaRepository<Content, Long> {

    Optional<Content> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Admin ro'yxati.
     *
     * <h2>Nega bu yerda {@code @EntityGraph} YO'Q</h2>
     * Ilgari bor edi va bu jimgina unumdorlik tuzog'i yaratardi: to-many
     * to'plamni ({@code translations}) fetch join bilan olib, ustiga
     * sahifalash so'ralsa, Hibernate SQL'da {@code limit} ISHLATA OLMAYDI —
     * u BARCHA satrlarni bazadan tortib, sahifani XOTIRADA kesadi
     * (ogohlantirish {@code HHH90003004}).
     *
     * 12 ta kontentda sezilmaydi. 10 000 tada — har bir ro'yxat so'rovi
     * butun jadvalni tortadi.
     *
     * Shuning uchun ikki bosqich: bu yerda toza sahifa (haqiqiy
     * {@code limit/offset}), keyin {@link #findAllByIdIn} bitta so'rov bilan
     * to'plamlarni to'ldiradi. Ikkalasi ham bitta tranzaksiyada, ya'ni
     * ikkinchi so'rovdan keyin sahifadagi obyektlar tayyor bo'ladi.
     */
    Page<Content> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Content> findAllByDeletedAtIsNullAndStatus(PublicationStatus status, Pageable pageable);

    Page<Content> findAllByDeletedAtIsNullAndContentType(ContentType type, Pageable pageable);

    /**
     * Sahifadagi elementlar uchun to'plamlarni bitta so'rovda oladi.
     *
     * Sahifalash YO'Q, shuning uchun fetch join xavfsiz. Natija persistence
     * context'ni "isitadi": sahifadagi o'sha obyektlar initsializatsiya
     * qilinadi va N+1 bo'lmaydi.
     */
    @EntityGraph(attributePaths = {"translations", "category"})
    List<Content> findAllByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"translations"})
    List<Content> findAllByDeletedAtIsNullAndOrientationAndStatusOrderByPublicationDateDesc(
            ContentOrientation orientation, PublicationStatus status, Pageable pageable);

    // ---------------------------------------------------------- bosh sahifa
    //
    // Quyidagilar §31 bosh sahifa qatorlari uchun. Ro'yxat qisqa (bo'lim
    // chegarasi odatda 20 ta), shuning uchun Pageable emas, oddiy List.
    //
    // ⚠️ Bu yerda @EntityGraph ATAYLAB YO'Q: Content da ikkita to'plam bor
    // (translations, media) va ikkalasini birdan fetch join qilish
    // MultipleBagFetchException beradi. N+1 esa entity ustidagi
    // @BatchSize bilan hal qilingan.

    List<Content> findAllByDeletedAtIsNullAndFeaturedTrueAndStatus(PublicationStatus status);

    List<Content> findAllByDeletedAtIsNullAndPopularTrueAndStatus(PublicationStatus status);

    List<Content> findAllByDeletedAtIsNullAndContentTypeAndStatus(
            ContentType contentType, PublicationStatus status);

    List<Content> findAllByDeletedAtIsNullAndOrientationAndStatus(
            ContentOrientation orientation, PublicationStatus status);

    /**
     * Kategoriya bo'yicha kontent — katalog qatorlari uchun
     * ({@code GET /api/v1/app/catalog/categories/{id}}).
     *
     * ⚠️ Kategoriya kontent TURI emas (§13): «Drama» — mavzu, MINI_SERIES —
     * tuzilish. Shuning uchun bu yerda {@code contentType} bo'yicha shart
     * YO'Q: bitta kategoriyada film ham, podkast ham bo'lishi mumkin.
     *
     * Ko'rinish (PRIVATE / UNLISTED) bu yerda tekshirilmaydi — u
     * {@link com.example.backend.Cms.Service.HomeFeedService#isVisible}
     * da, bosh sahifa qatorlari bilan BITTA qoidadan chiqadi.
     */
    List<Content> findAllByDeletedAtIsNullAndCategoryIdAndStatus(
            Long categoryId, PublicationStatus status);

    // ---------------------------------------------------- hisobot filtrlari
    //
    // ТЗ §47: hisobotlar kontent, kategoriya va ijodkor bo'yicha
    // filtrlansin. Faqat ID'lar qaytariladi — hisobot ularni kunlik
    // jamlanma ustidan filtr sifatida ishlatadi, ya'ni butun entity
    // yuklanmaydi.

    @Query("select c.id from Content c where c.category.id = :categoryId and c.deletedAt is null")
    List<Long> findIdsByCategory(@Param("categoryId") Long categoryId);

    /**
     * Ijodkor qatnashgan kontent.
     *
     * ⚠️ {@code distinct} SHART: bitta odam bitta kinoda ham aktyor, ham
     * rejissyor bo'lishi mumkin (§24) — u holda kontent ikki marta
     * chiqardi va hisobotda ikki barobar ko'rinardi.
     */
    @Query("""
            select distinct cc.content.id from ContentCredit cc
            where cc.creator.id = :creatorId and cc.content.deletedAt is null
            """)
    List<Long> findIdsByCreator(@Param("creatorId") Long creatorId);

    /** Oxirgi qo'shilgan kontent (ТЗ §48 jadvali). */
    @EntityGraph(attributePaths = "translations")
    List<Content> findAllByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

    /** Sarlavha bo'yicha qidiruv - uchala tilda ham. */
    @Query("""
            select distinct c from Content c
            join c.translations t
            where c.deletedAt is null
              and lower(t.title) like lower(concat('%', :q, '%'))
            """)
    Page<Content> search(@Param("q") String q, Pageable pageable);

    long countByDeletedAtIsNullAndStatus(PublicationStatus status);

    long countByDeletedAtIsNull();

    /**
     * Shu kategoriyaga bog'langan kontent soni (ТЗ §16).
     *
     * ⚠️ O'chirilgan kontent hisobga OLINMAYDI: u soft delete bilan
     * yashiringan va uni tiklash mumkin, lekin kategoriyasi
     * o'chirilgan bo'lsa tiklangan kontent kategoriyasiz qolardi.
     * Shuning uchun shart faqat TIRIK kontent bo'yicha — aks holda
     * bir marta arxivlangan kontent kategoriyani abadiy qulflab
     * qo'yardi.
     */
    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);

    /** Shu janr biriktirilgan tirik kontent soni (ТЗ §17). */
    long countByGenres_IdAndDeletedAtIsNull(Long genreId);

    /**
     * Har bir kategoriyada nechta KO'RINADIGAN kontent bor — bitta so'rovda.
     *
     * <h2>Nima uchun {@link #countByCategoryIdAndDeletedAtIsNull} yaramaydi</h2>
     * U qoralamani ham, PRIVATE ni ham sanaydi. Katalogda esa son odam
     * ochib ko'ra oladigan narsani bildirishi kerak: «Drama (7)» deb yozib,
     * ichida 2 ta kontent chiqishi — o'ylab topilgan raqam.
     *
     * <h2>Nima uchun guruhlangan</h2>
     * Kategoriya soni cheklanmagan. Har biriga alohida {@code count}
     * yuborish katalog ro'yxatini N ta so'rovga aylantirardi.
     *
     * ⚠️ {@code visibility is null} ham ko'rinadigan deb sanaladi: maydon
     * qoida joriy qilinishidan oldingi satrlarda bo'sh qolgan, va
     * {@code HomeFeedService.isVisible} ham aynan shunday qaraydi. Ikkala
     * joyda bitta qoida bo'lishi shart — aks holda son va ro'yxat
     * ajralib ketardi.
     */
    @Query("""
            select c.category.id, count(c) from Content c
            where c.deletedAt is null
              and c.status = :status
              and c.category is not null
              and (c.visibility is null or c.visibility = :visibility)
            group by c.category.id
            """)
    List<Object[]> countVisibleByCategory(
            @Param("status") PublicationStatus status,
            @Param("visibility") ContentVisibility visibility);
}
