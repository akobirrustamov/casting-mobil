package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Genre;
import com.example.backend.Cms.Entity.GenreTranslation;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Repository.GenreRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.HomeFeedService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Janrlar tartibi — BARQAROR bo'lsin.
 *
 * <h2>⚠️ Qanday nosozlik tuzatildi</h2>
 * Bitta kontent har so'rovda BOSHQA janr ko'rsatardi. Bosh sahifada
 * «Romantika», sahifani yangilagach «Komediya», keyin yana
 * «Romantika» — bir xil so'rov, bir xil ma'lumot, boshqa javob.
 *
 * <h2>Nega hech kim sezmagan</h2>
 * {@code Content.genres} maydoni {@code LinkedHashSet} deb e'lon
 * qilingan va shuning uchun tartibli KO'RINADI. Lekin bu qiymat
 * faqat yangi obyektda ishlatiladi: bazadan yuklashda Hibernate uni
 * {@code PersistentSet} bilan almashtiradi, ichida esa
 * {@code HashSet} turadi.
 *
 * {@link Genre} da {@code equals}/{@code hashCode} yozilmagan, ya'ni
 * {@code Object} nikidan foydalaniladi — u obyekt MANZILIGA bog'liq.
 * Har so'rovda janrlar yangi obyekt bo'lib yuklanadi, hash boshqa
 * chiqadi, tartib o'zgaradi.
 *
 * <h2>⚠️ Nega test seansni TOZALAYDI</h2>
 * Bitta seansda to'plam bir marta yuklanadi va keshda qoladi — ya'ni
 * takroriy o'qish har doim bir xil javob berardi va test sinmasdi.
 * Nosozlik faqat QAYTA yuklashda ko'rinadi, chunki u yangi obyektlar
 * va yangi hash demakdir.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContentGenreOrderTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    /** Qayta yuklashlar soni — tasodifan mos kelib qolmasligi uchun. */
    private static final int RELOADS = 12;

    @Autowired private ContentService contentService;
    @Autowired private HomeFeedService homeFeedService;
    @Autowired private GenreRepo genreRepo;
    @Autowired private ContentRepo contentRepo;
    @Autowired private EntityManager entityManager;

    // ------------------------------------------------------------- yordamchi

    private Genre genre(String uz, String ru, int sortOrder) {
        int n = SEQ.incrementAndGet();
        Genre g = Genre.builder()
                .slug("janr-" + n)
                .sortOrder(sortOrder)
                .active(true)
                .build();
        g.addTranslation(GenreTranslation.builder().locale(Locale.UZ).name(uz).build());
        g.addTranslation(GenreTranslation.builder().locale(Locale.RU).name(ru).build());
        return genreRepo.save(g);
    }

    private Content contentWith(Set<Long> genreIds) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setLanguage("uz");
        c.setGenreIds(genreIds);
        // Uchala til ham majburiy — nashr qilingan kontent uchun
        // `ContentService` shuni talab qiladi.
        c.setTranslations(Map.of(
                Locale.UZ, TranslationDto.ofTitle("Film " + SEQ.incrementAndGet()),
                Locale.RU, TranslationDto.ofTitle("Фильм"),
                Locale.EN, TranslationDto.ofTitle("Movie")));
        return contentService.create(null, c);
    }

    /** Kontentni SEANSDAN TASHQARI qayta yuklaydi. */
    private Content reload(Long id) {
        entityManager.flush();
        entityManager.clear();
        return contentRepo.findById(id).orElseThrow();
    }

    // ---------------------------------------------------------------- testlar

    /**
     * ⚠️ ASOSIY TEKSHIRUV.
     *
     * Ayni so'rov ayni javobni berishi kerak. Buzilganda bu test
     * o'n ikki qayta yuklashdan kamida bittasida boshqa janr ko'rib
     * yiqiladi.
     */
    @Test
    @DisplayName("Bir xil kontent har safar BIR XIL janr ko'rsatadi")
    void genreIsStableAcrossReloads() {
        Genre komediya = genre("Komediya", "Комедия", 1);
        Genre romantika = genre("Romantika", "Романтика", 2);
        Genre drama = genre("Drama", "Драма", 3);

        Long id = contentWith(new LinkedHashSet<>(
                List.of(drama.getId(), romantika.getId(), komediya.getId()))).getId();

        List<String> seen = IntStream.range(0, RELOADS)
                .mapToObj(i -> homeFeedService.contentCard(reload(id), Locale.UZ).getGenre())
                .distinct()
                .toList();

        assertThat(seen)
                .as("har so'rovda boshqa janr chiqmasin")
                .hasSize(1);
    }

    /**
     * ⚠️ Tartibni {@code sortOrder} belgilaydi — biriktirilgan tartib
     * emas.
     *
     * Yuqoridagi testda janrlar ATAYLAB teskari tartibda biriktirilgan
     * (drama, romantika, komediya). Barqarorlikning o'zi yetmaydi:
     * u tasodifiy, lekin O'ZGARMAS tartibda ham bajarilardi.
     */
    @Test
    @DisplayName("Birinchi janr — eng kichik sortOrder ga ega bo'lgani")
    void picksLowestSortOrder() {
        Genre komediya = genre("Komediya", "Комедия", 1);
        Genre romantika = genre("Romantika", "Романтика", 2);
        Genre drama = genre("Drama", "Драма", 3);

        Long id = contentWith(new LinkedHashSet<>(
                List.of(drama.getId(), romantika.getId(), komediya.getId()))).getId();

        assertThat(homeFeedService.contentCard(reload(id), Locale.UZ).getGenre())
                .isEqualTo("Komediya");
    }

    /**
     * ⚠️ ENG MUHIM TEKSHIRUV — tartib TILGA BOG'LIQ BO'LMASIN.
     *
     * Nom bo'yicha tartiblash barqarorlikni bersa ham, xatoning
     * yarmini joyida qoldirardi: «Komediya» o'zbek alifbosida
     * «Romantika» dan oldin, ruschada esa «Комедия» «Романтика» dan
     * oldin — bu misolda mos keladi, lekin umuman olganda ayni
     * kontent har tilda BOSHQA janr ko'rsatishi mumkin edi.
     *
     * Bu yerda janrlar shunday tanlangan: o'zbekcha va ruscha
     * alifbo tartibi QARAMA-QARSHI.
     */
    @Test
    @DisplayName("Janr har tilda AYNI bo'ladi, faqat tarjimasi boshqa")
    void sameGenreInEveryLanguage() {
        // «Anime» < «Biografiya» o'zbekchada, «Аниме» > «Биография» emas —
        // ruschada esa «Биография» < «Аниме» (Б dan A oldin emas: А < Б).
        // Ya'ni ikki tilda alifbo tartibi qarama-qarshi.
        Genre birinchi = genre("Biografiya", "Аниме", 1);
        Genre ikkinchi = genre("Anime", "Биография", 2);

        Long id = contentWith(new LinkedHashSet<>(
                List.of(ikkinchi.getId(), birinchi.getId()))).getId();

        assertThat(homeFeedService.contentCard(reload(id), Locale.UZ).getGenre())
                .isEqualTo("Biografiya");
        assertThat(homeFeedService.contentCard(reload(id), Locale.RU).getGenre())
                .as("ayni janrning ruscha tarjimasi, boshqa janr emas")
                .isEqualTo("Аниме");
    }

    /** Janrsiz kontentda maydon bo'sh — bu xato emas. */
    @Test
    @DisplayName("Janri yo'q kontentda null")
    void noGenreIsNull() {
        Long id = contentWith(new LinkedHashSet<>()).getId();

        assertThat(homeFeedService.contentCard(reload(id), Locale.UZ).getGenre()).isNull();
    }
}
