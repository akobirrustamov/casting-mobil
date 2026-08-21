package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentListDto;
import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Admin.TestStaffFactory;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.support.Translations;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Entity.User;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §15 — {@code visibility} va {@code language}.
 *
 * <h2>Nega bu ikkisi alohida maydon</h2>
 * <ul>
 *   <li>{@code status} hayot siklini bildiradi (DRAFT → PUBLISHED →
 *       ARCHIVED), {@code visibility} esa TOPILISHINI. Nashr qilingan film
 *       premyeradan oldin {@code UNLISTED} bo'lishi mumkin: havola bilan
 *       ochiladi, katalogda hali chiqmaydi.</li>
 *   <li>{@code language} — asarning ASL tili. Tarjimalar bilan bir narsa
 *       EMAS: sarlavha va tavsif baribir uch tilda saqlanadi, bu maydon
 *       esa asar qaysi tilda suratga olinganini aytadi. Koreys seriali
 *       ruscha tarjimasi bilan ham koreyscha qoladi.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestStaffFactory.class)
class ContentVisibilityTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private ContentService contentService;
    @Autowired private AccessService accessService;
    @Autowired private ContentRepo contentRepo;
    @Autowired private TestStaffFactory staff;
    @Autowired private UserRepo userRepo;

    private Content published(ContentVisibility visibility, String language) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setVisibility(visibility);
        c.setLanguage(language);
        c.setTranslations(Map.of(
                Locale.UZ, TranslationDto.ofTitle("Film " + SEQ.incrementAndGet()),
                Locale.RU, TranslationDto.ofTitle("Фильм"),
                Locale.EN, TranslationDto.ofTitle("Movie")));
        return contentService.create(null, c);
    }

    /** Panel xodimi — PRIVATE kontentni ko'ra oladi. */
    private User staffUser() {
        staff.tokenForRole("+998900003001", PlatformRole.ADMIN,
                EnumSet.noneOf(Permission.class));
        return userRepo.findByPhone("+998900003001").orElseThrow();
    }

    // -------------------------------------------------------- visibility

    @Nested
    @DisplayName("Visibility — status'dan alohida")
    class Visibility {

        @Test
        @DisplayName("Standart qiymat PUBLIC — mavjud xatti-harakat o'zgarmaydi")
        void defaultsToPublic() {
            ContentSaveRequest c = new ContentSaveRequest();
            c.setContentType(ContentType.MOVIE);
            c.setStructureType(StructureType.SINGLE);
            c.setAccessPolicy(AccessPolicy.FREE);
            c.setStatus(PublicationStatus.PUBLISHED);
            c.setTranslations(Translations.all("Standart"));

            Content content = contentService.create(null, c);
            assertThat(content.getVisibility()).isEqualTo(ContentVisibility.PUBLIC);
            assertThat(accessService.canWatch(null, content).isAllowed()).isTrue();
        }

        @Test
        @DisplayName("UNLISTED — havola bilan OCHILADI, katalogda yo'q")
        void unlistedIsReachableByLink() {
            Content content = published(ContentVisibility.UNLISTED, "uz");

            // Premyeradan oldingi ko'rik: havolasi borlar ko'radi.
            assertThat(accessService.canWatch(null, content).isAllowed()).isTrue();
            assertThat(content.getVisibility().listedInCatalog()).isFalse();
        }

        @Test
        @DisplayName("PRIVATE — oddiy foydalanuvchi havola bilan ham ocholmaydi")
        void privateIsStaffOnly() {
            Content content = published(ContentVisibility.PRIVATE, "uz");

            var anonymous = accessService.canWatch(null, content);
            assertThat(anonymous.isAllowed()).isFalse();
            assertThat(anonymous.getReason())
                    .isEqualTo(com.example.backend.Cms.Service.AccessDecision
                            .Reason.NOT_PUBLISHED);
        }

        @Test
        @DisplayName("PRIVATE — panel xodimi ko'ra oladi")
        void privateIsVisibleToStaff() {
            Content content = published(ContentVisibility.PRIVATE, "uz");

            // Tayyorlanayotgan kontentni tekshirish uchun.
            assertThat(accessService.canWatch(staffUser(), content).isAllowed()).isTrue();
        }

        @Test
        @DisplayName("Tahrirlashda visibility berilmasa — eskisi saqlanadi")
        void visibilityIsNotResetOnUpdate() {
            Content content = published(ContentVisibility.UNLISTED, "uz");

            ContentSaveRequest update = new ContentSaveRequest();
            update.setContentType(ContentType.MOVIE);
            update.setStructureType(StructureType.SINGLE);
            update.setAccessPolicy(AccessPolicy.FREE);
            update.setStatus(PublicationStatus.PUBLISHED);
            update.setVisibility(null);   // klient bu maydonni yubormadi
            update.setTranslations(Translations.all("Faqat sarlavha o'zgardi"));
            contentService.update(null, content.getId(), update);

            Content after = contentRepo.findById(content.getId()).orElseThrow();
            assertThat(after.getVisibility())
                    .as("Sarlavhani tahrirlash kontentni tasodifan katalogga "
                            + "chiqarib yubormasligi kerak")
                    .isEqualTo(ContentVisibility.UNLISTED);
        }
    }

    // ----------------------------------------------------------- language

    @Nested
    @DisplayName("Language — tarjimalardan alohida")
    class Language {

        @Test
        @DisplayName("Asl til saqlanadi va tarjimalarga tegmaydi")
        void originalLanguageIsIndependent() {
            // Koreys seriali: asl tili ko, lekin sarlavhasi uch tilda.
            Content content = published(ContentVisibility.PUBLIC, "ko");

            assertThat(content.getLanguage()).isEqualTo("ko");
            assertThat(content.getTranslations())
                    .as("Asl til boshqa bo'lsa ham tarjimalar uch tilda qoladi")
                    .hasSize(3);
        }

        @Test
        @DisplayName("Til ixtiyoriy — berilmasa null")
        void languageIsOptional() {
            Content content = published(ContentVisibility.PUBLIC, null);
            assertThat(content.getLanguage()).isNull();
        }

        @Test
        @DisplayName("DTO ikkala maydonni ham qaytaradi")
        void dtoCarriesBothFields() {
            Content content = published(ContentVisibility.UNLISTED, "tr");
            ContentListDto dto = ContentListDto.from(content);

            // Qaytmasa, muharrir ularni yuklay olmasdi va saqlashda
            // yo'qolardi (B17/B23 bilan bir xil sinf).
            assertThat(dto.getVisibility()).isEqualTo(ContentVisibility.UNLISTED);
            assertThat(dto.getLanguage()).isEqualTo("tr");
        }
    }
}
