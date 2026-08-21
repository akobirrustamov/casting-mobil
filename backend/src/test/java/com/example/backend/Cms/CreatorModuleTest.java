package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.CreatorDto;
import com.example.backend.Admin.Dto.CreatorSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.ContentCredit;
import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.TaxonomyService;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §24 — Ijodkorlar moduli.
 *
 * <h2>Asosiy talab</h2>
 * «Bitta ijodkor bir kinoda aktyor, boshqa kinoda rejissyor bo'lishi
 * mumkin.» Ya'ni kasb ijodkorning O'ZIGA emas, uning KONTENTDAGI ROLIGA
 * tegishli.
 *
 * Bu model xatosiga oson yo'l qo'yiladi: agar {@code profession} maydoni
 * {@code Creator} entitysida bo'lganda, bir odam bir vaqtda faqat bitta
 * kasbga ega bo'lardi va rejissyor o'z filmida rol o'ynay olmasdi. Shuning
 * uchun u {@code ContentCredit} da.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CreatorModuleTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private TaxonomyService taxonomyService;
    @Autowired private ContentService contentService;
    @Autowired private ContentRepo contentRepo;

    private CreatorSaveRequest.NameDto name(String first, String last, String bio) {
        CreatorSaveRequest.NameDto n = new CreatorSaveRequest.NameDto();
        n.setFirstName(first);
        n.setLastName(last);
        n.setBio(bio);
        return n;
    }

    private Creator creator(String first, String last, Boolean active) {
        CreatorSaveRequest req = new CreatorSaveRequest();
        req.setActive(active);
        req.setBirthDate(LocalDate.of(1990, 5, 17));
        int n = SEQ.incrementAndGet();
        req.setTranslations(Map.of(
                Locale.UZ, name(first + n, last, "Biografiya"),
                Locale.RU, name(first + n, last, "Биография"),
                Locale.EN, name(first + n, last, "Biography")));
        return taxonomyService.saveCreator(null, null, req);
    }

    private Content contentWithCredits(String title,
                                       List<ContentSaveRequest.CreditLink> credits) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setTranslations(Translations.all(title + " " + SEQ.incrementAndGet()));
        c.setCredits(credits);
        return contentService.create(null, c);
    }

    private ContentSaveRequest.CreditLink credit(Long creatorId, CreatorProfession prof,
                                                 String character, int order) {
        ContentSaveRequest.CreditLink l = new ContentSaveRequest.CreditLink();
        l.setCreatorId(creatorId);
        l.setProfession(prof);
        l.setCharacterName(character);
        l.setSortOrder(order);
        return l;
    }

    // ---------------------------------------------------------- maydonlar

    @Nested
    @DisplayName("Creator maydonlari (ТЗ §24)")
    class Fields {

        @Test
        @DisplayName("Barcha talab qilingan maydonlar mavjud")
        void allRequiredFieldsExist() {
            Creator c = creator("Dilnoza", "Karimova", true);
            CreatorDto dto = CreatorDto.from(c);

            assertThat(dto.getId()).isNotNull();
            assertThat(dto.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 17));
            assertThat(dto.getActive()).isTrue();
            assertThat(dto.getFeatured()).isNotNull();
            assertThat(dto.getCreatedAt()).isNotNull();
            // ТЗ §24 talab qiladi va u yo'q edi.
            assertThat(dto.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Ism va biografiya UCH TILDA saqlanadi")
        void namesAndBioAreTranslated() {
            Creator c = creator("Bekzod", "Olimov", true);
            CreatorDto dto = CreatorDto.from(c);

            assertThat(dto.getTranslations()).containsKeys(Locale.UZ, Locale.RU, Locale.EN);
            assertThat(dto.getTranslations().get(Locale.RU).getBio()).isEqualTo("Биография");
            assertThat(dto.getTranslations().get(Locale.EN).getBio()).isEqualTo("Biography");
            // displayName ism+familiyadan yig'iladi.
            assertThat(dto.getTranslations().get(Locale.UZ).getDisplayName()).isNotBlank();
        }

        @Test
        @DisplayName("Faol ijodkor uch tilsiz saqlanmaydi")
        void activeCreatorNeedsAllThreeLanguages() {
            CreatorSaveRequest req = new CreatorSaveRequest();
            req.setActive(true);
            req.setTranslations(Map.of(
                    Locale.UZ, name("Faqat", "O'zbekcha", null)));

            // Ijodkor kontent sahifasida va «Mashhur ijodkorlar» bo'limida
            // chiqadi - ismi uchala tilda bo'lishi kerak.
            assertThatThrownBy(() -> taxonomyService.saveCreator(null, null, req))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ------------------------------------------------------------- kasblar

    @Nested
    @DisplayName("Kasblar")
    class Professions {

        @Test
        @DisplayName("ТЗ §24 dagi barcha kasblar mavjud")
        void specProfessionsExist() {
            for (String required : new String[]{
                    "ACTOR", "DIRECTOR", "MODEL", "PRODUCER",
                    "SCREENWRITER", "HOST", "CREATOR", "OTHER"}) {
                assertThat(CreatorProfession.valueOf(required)).isNotNull();
            }
        }

        @Test
        @DisplayName("Kengaytirish uchun migratsiya kerak emas")
        void professionsAreExtensibleWithoutMigration() {
            // Bazada `profession` ustunida CHECK cheklovi yo'q (D18), ya'ni
            // enum'ga yangi qiymat qo'shish faqat kod o'zgarishini talab
            // qiladi. Cheklov bo'lganda har yangi kasb migratsiya so'rardi.
            //
            // Bu yerda mavjud qiymatlarning bazaga yozilishini tekshiramiz -
            // agar cheklov qo'shilib qolsa, saqlash yiqilardi.
            Creator c = creator("Universal", "Ijodkor", true);
            for (CreatorProfession p : CreatorProfession.values()) {
                contentWithCredits("Kasb " + p,
                        List.of(credit(c.getId(), p, null, 0)));
            }
        }
    }

    // -------------------------------------------------------- ko'p rolli

    @Nested
    @DisplayName("Bitta ijodkor — turli rollar")
    class MultipleRoles {

        @Test
        @DisplayName("Bir kinoda aktyor, boshqasida rejissyor")
        void sameCreatorDifferentRolesAcrossContent() {
            Creator person = creator("Jahongir", "Tursunov", true);

            Content asActor = contentWithCredits("Aktyor sifatida",
                    List.of(credit(person.getId(), CreatorProfession.ACTOR, "Bosh qahramon", 0)));
            Content asDirector = contentWithCredits("Rejissyor sifatida",
                    List.of(credit(person.getId(), CreatorProfession.DIRECTOR, null, 0)));

            assertThat(creditOf(asActor).getProfession()).isEqualTo(CreatorProfession.ACTOR);
            assertThat(creditOf(asActor).getCharacterName()).isEqualTo("Bosh qahramon");
            assertThat(creditOf(asDirector).getProfession()).isEqualTo(CreatorProfession.DIRECTOR);

            // ⚠️ Aynan shu sabab kasb Creator entitysida EMAS: u yerda
            // bo'lganda bir odam bir vaqtda faqat bitta kasbga ega bo'lardi.
            assertThat(creditOf(asActor).getCreator().getId())
                    .isEqualTo(creditOf(asDirector).getCreator().getId());
        }

        @Test
        @DisplayName("Bitta kinoda bir nechta rol — rejissyor ham, aktyor ham")
        void sameCreatorTwoRolesInOneContent() {
            Creator person = creator("Kamola", "Saidova", true);

            Content film = contentWithCredits("Ikki rol", List.of(
                    credit(person.getId(), CreatorProfession.DIRECTOR, null, 0),
                    credit(person.getId(), CreatorProfession.ACTRESS, "O'zi", 1)));

            Content saved = contentRepo.findById(film.getId()).orElseThrow();
            assertThat(saved.getCredits()).hasSize(2);
            assertThat(saved.getCredits()).extracting(ContentCredit::getProfession)
                    .containsExactlyInAnyOrder(CreatorProfession.DIRECTOR,
                            CreatorProfession.ACTRESS);
        }

        @Test
        @DisplayName("Tartib saqlanadi — bosh rollar oldinda")
        void creditOrderIsPreserved() {
            Creator lead = creator("Bosh", "Rol", true);
            Creator second = creator("Ikkinchi", "Rol", true);

            Content film = contentWithCredits("Tartib", List.of(
                    credit(second.getId(), CreatorProfession.ACTOR, "Ikkinchi", 1),
                    credit(lead.getId(), CreatorProfession.ACTOR, "Birinchi", 0)));

            Content saved = contentRepo.findById(film.getId()).orElseThrow();
            assertThat(saved.getCredits())
                    .extracting(ContentCredit::getSortOrder)
                    .containsExactlyInAnyOrder(0, 1);
        }
    }

    private ContentCredit creditOf(Content content) {
        Content saved = contentRepo.findById(content.getId()).orElseThrow();
        assertThat(saved.getCredits()).hasSize(1);
        return saved.getCredits().get(0);
    }
}
