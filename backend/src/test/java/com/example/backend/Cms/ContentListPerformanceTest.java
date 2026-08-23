package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.PageHydrator;
import com.example.backend.support.CapturingStatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ro'yxat sahifalash HAQIQATAN bazada kesilishini qo'riqlaydi.
 *
 * <h2>Qanday nosozlikni ushlaydi</h2>
 * Kimdir {@code Page} qaytaradigan metodga {@code @EntityGraph} bilan
 * to-many to'plam ({@code translations}) qo'shsa, Hibernate SQL {@code limit}
 * ni ISHLATA OLMAY qoladi: fetch join bitta yozuvni bir necha satrga yoyadi
 * va {@code limit} noto'g'ri kesardi. Shuning uchun u butun jadvalni tortib,
 * sahifani XOTIRADA kesadi ({@code HHH90003004}).
 *
 * Bu jimgina buziladi: natija to'g'ri ko'rinadi, testlar o'tadi. Faqat
 * ma'lumot o'sgach ma'lum bo'ladi — har bir ro'yxat so'rovi butun jadvalni
 * tortadi.
 *
 * <h2>Nega SQL tekshiriladi, hisoblagich emas</h2>
 * Statistika hisoblagichlari bu yerda ishlamaydi: test yozuvlarni o'sha
 * tranzaksiyada yaratsa, ular allaqachon kontekstda turadi va "yuklash" deb
 * hisoblanmaydi. Yagona ishonchli dalil — yuborilgan SQL matni.
 */
@SpringBootTest
@ActiveProfiles("test")
class ContentListPerformanceTest {

    private static final int TOTAL = 25;
    private static final String MARKER = "Unumdorlik sinovi";

    @Autowired
    private ContentService contentService;

    @Autowired
    private ContentRepo contentRepo;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    @Autowired
    private com.example.backend.Cms.Repository.GenreRepo genreRepo;

    @Autowired
    private com.example.backend.Cms.Service.TaxonomyService taxonomyService;

    @BeforeEach
    void seed() {
        long existing = contentRepo.findAll().stream()
                .filter(c -> c.getTranslations().stream().anyMatch(
                        t -> t.getTitle() != null && t.getTitle().startsWith(MARKER)))
                .count();
        for (long i = existing; i < TOTAL; i++) {
            ContentSaveRequest c = new ContentSaveRequest();
            c.setContentType(ContentType.MOVIE);
            c.setStructureType(StructureType.SINGLE);
            c.setAccessPolicy(AccessPolicy.FREE);
            c.setStatus(PublicationStatus.PUBLISHED);
            c.setTranslations(Map.of(
                    Locale.UZ, TranslationDto.ofTitle(MARKER + " " + i),
                    Locale.RU, TranslationDto.ofTitle(MARKER + " ru " + i),
                    Locale.EN, TranslationDto.ofTitle(MARKER + " en " + i)));
            contentService.create(null, c);
        }
        enrich();
    }

    /** H2 PostgreSQL rejimida chegara "limit", ba'zi dialektlarda "fetch first". */
    private boolean hasLimit(String sql) {
        String lower = sql.toLowerCase();
        return lower.contains(" limit ") || lower.contains("fetch first");
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("Sahifa so'rovida SQL limit bo'ladi - kesish bazada")
    void pageQueryUsesSqlLimit() {
        CapturingStatementInspector.clear();

        Page<Content> page = contentRepo.findAllByDeletedAtIsNull(PageRequest.of(0, 5));

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(TOTAL);

        List<String> selects = CapturingStatementInspector.selectsFrom("cms_content");
        assertThat(selects).isNotEmpty();

        // ⚠️ ASOSIY TEKSHIRUV.
        assertThat(selects)
                .as("Kontent sahifasi SQL limit bilan olinishi kerak. Limitsiz bo'lsa - "
                        + "Hibernate butun jadvalni tortib, sahifani xotirada kesyapti "
                        + "(HHH90003004). Sabab odatda Page metodiga qo'shilgan "
                        + "@EntityGraph bilan to-many to'plam.")
                .anyMatch(this::hasLimit);
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("To'ldirish bitta so'rov - har satr uchun alohida emas")
    void hydrationIsOneQuery() {
        Page<Content> page = contentRepo.findAllByDeletedAtIsNull(PageRequest.of(0, 5));

        CapturingStatementInspector.clear();
        PageHydrator.warm(page, Content::getId, contentRepo::findAllByIdIn);
        page.getContent().forEach(c -> assertThat(c.getTranslations()).isNotEmpty());

        // To'ldirishdan keyin tarjimalarga tegish YANGI so'rov chiqarmasligi kerak.
        long selects = CapturingStatementInspector.captured().stream()
                .filter(sql -> sql.toLowerCase().startsWith("select"))
                .count();

        assertThat(selects)
                .as("To'ldirish bitta so'rov bo'lishi kerak. Ko'p bo'lsa - N+1: "
                        + "har bir kontent uchun alohida tarjima so'rovi ketyapti.")
                .isLessThanOrEqualTo(2);
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("Sahifa hajmi 10 barobar oshsa ham so'rovlar soni o'zgarmaydi")
    void queryCountDoesNotGrowWithPageSize() {
        long small = countSelectsForPage(2);
        long big = countSelectsForPage(20);

        assertThat(big)
                .as("2 ta va 20 ta element uchun so'rovlar soni bir xil bo'lishi kerak. "
                        + "Farq bo'lsa - element soniga bog'liq so'rov bor (N+1).")
                .isEqualTo(small);
    }

    private long countSelectsForPage(int size) {
        CapturingStatementInspector.clear();
        Page<Content> page = contentRepo.findAllByDeletedAtIsNull(PageRequest.of(0, size));
        PageHydrator.warm(page, Content::getId, contentRepo::findAllByIdIn);
        page.getContent().forEach(c -> c.getTranslations().size());
        return CapturingStatementInspector.captured().stream()
                .filter(sql -> sql.toLowerCase().startsWith("select"))
                .count();
    }

    // ------------------------------------------------- DTO qurish narxi

    /**
     * ⚠️ Yuqoridagi testlar faqat TARJIMALARGA tegadi. Endpoint esa
     * to'liq DTO quradi va u boshqa to'plamlarni ham o'qiydi.
     *
     * Aynan shu sababli §66 dagi tuzatish yangi xavf tug'dirdi:
     * {@code genreIds} va {@code credits} qo'shilganda ular LAZY
     * to'plamlar bo'lib, sahifadagi har bir satr uchun alohida so'rov
     * chiqarishi mumkin edi. Ya'ni ma'lumot yo'qolishini tuzatib,
     * o'rniga yuzlab so'rov qo'yish.
     */
    @Test
    @Transactional(readOnly = true)
    @DisplayName("⚠️ To'liq DTO qurishda so'rovlar soni sahifa hajmiga bog'liq emas")
    void dtoBuildDoesNotScaleWithPageSize() {
        long small = countSelectsForDto(2);
        long big = countSelectsForDto(20);

        assertThat(big)
                .as("2 ta va 20 ta element uchun so'rovlar soni bir xil bo'lishi kerak. "
                        + "Farq bo'lsa - DTO ichida N+1 bor: kolleksiya har satr "
                        + "uchun alohida yuklanyapti.")
                .isEqualTo(small);

        // Tenglikning o'zi yetarli emas: ikkalasi ham 100 bo'lsa test
        // baribir o'tardi. Mutlaq chegara ham kerak.
        assertThat(big)
                .as("sahifa uchun bir necha to'plam so'rovi kutiladi, o'nlab emas")
                .isLessThanOrEqualTo(12);
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("Detektor haqiqatan so'rovlarni sanaydi")
    void detectorActuallyCounts() {
        assertThat(countSelectsForDto(5))
                .as("nol bo'lsa - detektor ishlamayapti, test hech narsani isbotlamaydi")
                .isPositive();
    }

    /** Bir nechta kontentga janr va ijodkor biriktiradi. */
    private void enrich() {
        var genre = genreRepo.save(com.example.backend.Cms.Entity.Genre.builder()
                .slug("perf-janr-" + System.nanoTime())
                .active(true).sortOrder(1).build());

        var creatorReq = new com.example.backend.Admin.Dto.CreatorSaveRequest();
        creatorReq.setActive(true);
        var tr = new java.util.LinkedHashMap<Locale,
                com.example.backend.Admin.Dto.CreatorSaveRequest.NameDto>();
        for (Locale loc : Locale.values()) {
            var n = new com.example.backend.Admin.Dto.CreatorSaveRequest.NameDto();
            n.setDisplayName("Perf ijodkor " + loc);
            tr.put(loc, n);
        }
        creatorReq.setTranslations(tr);
        var creator = taxonomyService.saveCreator(null, null, creatorReq);

        // ⚠️ Aniq tartib: o'lchov ham shu tartibda sahifa oladi. Tartibsiz
        // bo'lsa boyitilgan va o'lchanayotgan satrlar mos kelmasdi va
        // test tasodifiy yiqilardi.
        for (Content c : contentRepo.findAllByDeletedAtIsNull(
                PageRequest.of(0, 50, org.springframework.data.domain.Sort.by("id")))
                .getContent()) {
            ContentSaveRequest req = new ContentSaveRequest();
            req.setContentType(c.getContentType());
            req.setStructureType(c.getStructureType());
            req.setAccessPolicy(c.getAccessPolicy());
            req.setStatus(c.getStatus());
            req.setVersion(c.getVersion());
            req.setTranslations(Map.of(
                    Locale.UZ, TranslationDto.ofTitle(MARKER + " " + c.getId()),
                    Locale.RU, TranslationDto.ofTitle(MARKER + " ru " + c.getId()),
                    Locale.EN, TranslationDto.ofTitle(MARKER + " en " + c.getId())));
            req.setGenreIds(java.util.Set.of(genre.getId()));
            var link = new ContentSaveRequest.CreditLink();
            link.setCreatorId(creator.getId());
            link.setProfession(com.example.backend.Cms.Enums.CreatorProfession.ACTOR);
            link.setSortOrder(0);
            req.setCredits(java.util.List.of(link));
            contentService.update(null, c.getId(), req);
        }
    }

    private long countSelectsForDto(int size) {
        // ⚠️ Kontekstni tozalamasak, o'lchov YOLG'ON bo'ladi: shu
        // tranzaksiyada yaratilgan yoki o'qilgan entity'lar allaqachon
        // sessiyada turadi va Hibernate ular uchun so'rov yubormaydi.
        // Natijada N+1 bor bo'lsa ham nol ko'rinadi.
        em.flush();
        em.clear();

        CapturingStatementInspector.clear();
        Page<Content> page = contentRepo.findAllByDeletedAtIsNull(PageRequest.of(0, size));
        PageHydrator.warm(page, Content::getId, contentRepo::findAllByIdIn);

        // Endpoint aynan shuni qiladi.
        // ⚠️ O'lchov FAQAT ma'lumot bor bo'lganda ma'noli. Janr va
        // ijodkor biriktirilmagan kontentda to'plamlar bo'sh bo'lib,
        // N+1 bor bo'lsa ham ko'rinmasdi.
        long withGenres = page.getContent().stream()
                .map(com.example.backend.Admin.Dto.ContentListDto::from)
                .filter(d -> d.getGenreIds() != null && !d.getGenreIds().isEmpty())
                .count();
        assertThat(withGenres)
                .as("o'lchov janr biriktirilgan satrlarda o'tkazilsin")
                .isEqualTo(page.getContent().size());

        return CapturingStatementInspector.captured().stream()
                .filter(sql -> sql.toLowerCase().startsWith("select"))
                .count();
    }
}
